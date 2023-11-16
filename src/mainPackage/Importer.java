/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

public abstract class Importer {

    List<File> files;
    File file;
    int destination;
    boolean formatissue = false;
    boolean stackissue = false;
    boolean cancelled = false;
    int failures = 0;
    int stacks = 0;
    int images = 0;
    int layers = 0;

    public void importFile(List<File> fileList) {
        files = fileList;
        Thread thread = new Thread(() -> {
            Tonga.loader().setIterations(files.size());
            int imagesNow = Tonga.picList.size();
            iterate();
            scale(imagesNow, Tonga.picList.size());
            if (!cancelled) {
                if (Settings.settingBatchProcessing()) {
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>");
                    } else {
                        Tonga.refreshChanges(files.get(0), message() + " as file pointers.");
                    }
                } else {
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font> "
                                + (stackissue ? "Stack images can not be imported as layers." : "")
                                + (formatissue ? " Unsupported file format." : ""));
                    } else {
                        Tonga.refreshChanges(files.get(0), message()
                                + (stacks > 0 && (images > 0 || layers > 0) ? " and " : "") + (stacks > 0 ? stacks + " stack image(s)" : "")
                                + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : ".")
                                + (stackissue ? "Stack images can not be imported as layers." : ""));
                    }
                }
            }
        });
        Tonga.bootThread(thread, "Importer", false, true);
    }

    protected void readFile() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        try {
            if (Settings.settingBatchProcessing()) {
                readBatch();
            } else {
                if (!readStack(false)) {
                    MappedImage mi = source();
                    //failed to read
                    if (mi == null) {
                        Tonga.log.debug("Will try the Bio-Formats importer instead");
                        if (readStack(true)) {
                            Tonga.log.debug("Imported as a stack using a Bio-Formats importer");
                        } else {
                            Tonga.log.debug("Can not import using Bio-Formats importer");
                        }
                    } else {
                        read(mi);
                    }
                }
            }
            Tonga.loader().appendProgress(1.0);
        } catch (ClosedChannelException ex) {
            Tonga.catchError(ex, "The Bio-Formats importer was interrupted while importing.");
        } catch (Exception ex) {
            try {
                failures++;
                int fs = (int) Files.size(file.toPath());
                if (file.isDirectory()) {
                    Tonga.catchError(ex, "Folder importing is not supported.");
                } else if (fs == 0) {
                    Tonga.catchError(ex, "The file is empty.");
                } else {
                    boolean success = rawImport();
                    if (!success) {
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.catchError(ex, "The file could not be imported because the format is unsupported.");
                        } else if (ex instanceof IOException) {
                            Tonga.catchError(ex, "The file could not be imported because of an IO error.");
                        } else {
                            Tonga.catchError(ex, "The file could not be imported because of an unknown error.");
                        }
                    } else {
                        failures--;
                        Tonga.loader().appendToNext();
                    }
                }
            } catch (IOException ex1) {
                Tonga.catchError(ex, "IO error occurred.");
            }
        }
    }

    private MappedImage source() throws IOException, FileNotFoundException, ServiceException, FormatException {
        if (file.getAbsolutePath().toLowerCase().endsWith(".mrxs")) {
            Tonga.log.info("This image seems to be a Mirax file");
            return IO.getMiraxPreviewImage(file);
        } else {
            return IO.getImageFromFile(file);
        }
    }

    private void scale(int imagesBeginning, int imagesNow) {
        if (Settings.settingAutoscaleType() == Settings.Autoscale.CHANNEL) {
            int newImgs = imagesNow - imagesBeginning;
            if (newImgs == 0) {
                Tonga.log.info("No images were imported. No scaling will be performed.");
                return;
            }
            int expLayerCount = Tonga.getImage(imagesBeginning).layerCount();
            if (newImgs > 1) {
                int[] indexes = new int[expLayerCount];
                for (int i = 0; i < expLayerCount; i++) {
                    indexes[i] = i;
                }
                if (!Tonga.layerStructureMatches(imagesBeginning, imagesNow-1, indexes)) {
                    Tonga.log.info("The new images don't share a layer structure. No scaling will be performed.");
                    return;
                }
            }
            TongaImage[] newImages = new TongaImage[newImgs];
            for (int i = 0; i < newImgs; i++) {
                newImages[i] = Tonga.getImage(imagesBeginning + i);
            }
            TongaRender.setDisplayRange(expLayerCount, newImages);
        }
    }

    private boolean rawImport() {
        Object[] params = IO.askFormat(file);
        boolean ok = (boolean) params[0];
        if (ok) {
            int w = (int) params[1];
            int f = (int) params[2];
            MappedImage n = new MappedImage(file, w, f);
            try {
                read(n);
            } catch (Exception ex) {
                ok = false;
            }
        }
        return ok;
    }

    //iterate must call "readFile", but never any abstract functions
    abstract void iterate();

    abstract void read(MappedImage mi) throws Exception;

    abstract void readBatch() throws Exception;

    abstract boolean readStack(boolean force) throws IOException, FormatException, ServiceException;

    abstract String message();

}
