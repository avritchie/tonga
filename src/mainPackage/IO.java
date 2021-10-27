package mainPackage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import static mainPackage.Tonga.picList;

public class IO {

    protected static String getFolder(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose a folder");
        chooser.setCurrentDirectory(new File(text));
        int val = chooser.showOpenDialog(Tonga.frame());
        if (val == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return text;
    }

    protected static File[] getFile(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose a file");
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(new File(text));
        int val = chooser.showOpenDialog(Tonga.frame());
        if (val == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFiles();
        }
        return null;
    }

    protected static void toTSVfile(JTable table, File file) {
        try {
            TableModel model = table.getModel();
            try ( FileWriter excel = new FileWriter(file)) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    excel.write(model.getColumnName(i).replaceAll("<[^>]*>", "") + "\t");
                }
                excel.write("\n");
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object obj = model.getValueAt(i, j);
                        String str;
                        if (obj instanceof Double) {
                            str = obj.toString().replace(".", ",");
                        } else {
                            str = obj.toString();
                        }
                        excel.write(str + "\t");
                    }
                    excel.write("\n");
                }
                Tonga.log.info("Table exported as TSV into {}", file);
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Result exporting failed.");
        }
    }

    private static boolean importStack(File file) throws IOException, FormatException, ServiceException {
        if (Settings.settingBatchProcessing()) {
            Tonga.setStatus("Stacked images cannot be opened in the batch-mode since the contents may vary.");
        } else {
            TongaImage[] images = StackImporter.openFile(file);
            picList.addAll(Arrays.asList(images));
            return true;
        }
        return false;
    }

    public static void importStacks(List<File> files) {
        if (Settings.settingBatchProcessing()) {
            Tonga.setStatus("Stacked images cannot be opened in the batch-mode since the contents may vary.");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                int failures = 0;
                boolean formatissue = false;
                int count = picList.size();
                for (int i = 0; i < files.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    File file = files.get(i);
                    try {
                        importStack(file);
                    } catch (IOException | ServiceException | FormatException ex) {
                        if (file.isDirectory()) {
                            Tonga.catchError(ex, "Folder importing is not supported.");
                        } else {
                            Tonga.catchError(ex, "Image file can not be imported.");
                        }
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                }
                count = picList.size() - count;
                if (failures == files.size()) {
                    Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                } else {
                    Tonga.refreshChanges(files.get(0), "Imported image file(s) containing a total of " + count + " new images"
                            + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                }
            }
            );
            Tonga.bootThread(thread, "Importer", false, true);
        }
    }

    public static void importLayers(List<File> files, boolean toAllImages) {
        if (Settings.settingBatchProcessing()) {
            int mod = files.size() / Tonga.imageListModel.size();
            for (int i = 0; i < Tonga.imageListModel.size(); i++) {
                for (int j = 0; j < mod; j++) {
                    File file = files.get(i * mod + j);
                    Tonga.injectNewLayer(file.getAbsolutePath(), "Layer", i);
                }
            }
            Tonga.refreshChanges(files.get(0), "Imported file pointers");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                boolean formatissue = false;
                int failures = 0;
                int stacks = 0;
                int imgEnts;
                int[] imgIds;
                if (toAllImages) {
                    // all images get new layers
                    imgEnts = Tonga.imageListModel.size();
                    imgIds = new int[imgEnts];
                    for (int i = 0; i < imgEnts; i++) {
                        imgIds[i] = i;
                    }
                } else {
                    // only selected images get new layers
                    imgEnts = Tonga.getImageIndexes().length;
                    imgIds = Tonga.getImageIndexes();
                }
                if (files.size() % imgEnts == 0) {
                    int mod = files.size() / imgEnts;
                    for (int i = 0; i < imgEnts; i++) {
                        for (int j = 0; j < mod; j++) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            File file = files.get(i * mod + j);
                            try {
                                if (StackImporter.isStackImage(file)) {
                                    stacks++;
                                } else {
                                    Tonga.injectNewLayer(file, "Layer", imgIds[i]);
                                    Tonga.loader().appendProgress(1.0);
                                }
                            } catch (Exception ex) {
                                if (file.isDirectory()) {
                                    Tonga.catchError(ex, "Folder importing is not supported.");
                                } else {
                                    Tonga.catchError(ex, "Image file can not be imported.");
                                }
                                failures++;
                                if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                                    formatissue = true;
                                    Tonga.log.warn("Unsupported format");
                                }
                                Tonga.loader().appendProgress(1.0);
                            }
                        }
                    }
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font> "
                                + (stacks > 0 ? "Stack images can not be imported as layers." : "")
                                + (formatissue ? " Unsupported file format." : ""));
                    } else {
                        Tonga.refreshChanges(files.get(0), "Imported " + mod + " new layers to " + imgEnts + " images"
                                + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : ".")
                                + (stacks > 0 ? "Stack images can not be imported as layers." : ""));
                    }
                } else {
                    Tonga.setStatus("Number of files has to be divisible with the number of images (" + imgEnts + ")");
                }
            });
            Tonga.bootThread(thread, "Importer", false, true);
        }
    }

    public static void importImages(List<File> files) {
        if (files.size() > 50 && picList.isEmpty() && !Settings.settingBatchProcessing()
                && Tonga.askYesNo("Use the batch mode", "You are trying to import a large number of images at once."
                        + "Do you want to switch to the batch processing mode?<br><br>"
                        + "In the batch mode you can execute protocols to the files directly without a need to import them first. "
                        + "This can be useful if you already know what protocol and settings you want to use.", true, false)) {
            Tonga.frame().boxSettingNoRAM.setSelected(true);
        }
        if (Settings.settingBatchProcessing()) {
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                TongaImage ti = new TongaImage(file);
                picList.add(ti);
                ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Original"));
            }
            Tonga.refreshChanges(files.get(0), "Imported file pointers");
        } else {
            Thread thread = new Thread(() -> {
                Tonga.loader().setIterations(files.size());
                boolean formatissue = false;
                int failures = 0;
                int stacks = 0;
                int images = 0;
                for (int i = 0; i < files.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    File file = files.get(i);
                    try {
                        if (StackImporter.isStackImage(file)) {
                            importStack(file);
                            stacks++;
                        } else {
                            picList.add(new TongaImage(file, "Original"));
                            images++;
                            Tonga.loader().appendProgress(1.0);
                        }
                    } catch (Exception ex) {
                        if (file.isDirectory()) {
                            Tonga.catchError(ex, "Folder importing is not supported.");
                        } else {
                            Tonga.catchError(ex, "Image file can not be imported.");
                        }
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                }
                if (failures == files.size()) {
                    Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                } else {
                    Tonga.refreshChanges(files.get(0), "Imported "
                            + (images > 0 ? images + " new images" + (stacks > 0 ? " and " : "") : "")
                            + (stacks > 0 ? stacks + " stack image(s)" : "")
                            + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                }
            });
            Tonga.bootThread(thread, "Importer", false, true);
        }
    }

    public static void importImage(List<File> files) {
        if (files.size() > 5 && Tonga.askYesNo("Import as images", "You are trying to import ONE new image with " + files.size() + " layers. "
                + "Did you mean to import " + files.size() + " separate images with one layer each instead?", true, false)) {
            importImages(files);
        } else {
            if (Settings.settingBatchProcessing()) {
                File file = files.get(0);
                TongaImage ti = new TongaImage(file);
                ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Original"));
                for (int i = 1; i < files.size(); i++) {
                    file = files.get(i);
                    ti.layerList.add(new TongaLayer(file.getAbsolutePath(), "Layer"));
                }
                picList.add(ti);
                Tonga.refreshChanges(files.get(0), "Imported file pointers");
            } else {
                Thread thread = new Thread(() -> {
                    Tonga.loader().setIterations(files.size());
                    boolean formatissue = false;
                    int failures = 0;
                    int stacks = 0;
                    int layers = 0;
                    TongaImage image = new TongaImage(files.get(0));
                    try {
                        for (int i = 0; i < files.size(); i++) {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            File file = files.get(i);
                            TongaLayer layer;
                            try {
                                if (StackImporter.isStackImage(file)) {
                                    importStack(file);
                                    stacks++;
                                } else {
                                    layer = new TongaLayer(getImageFromFile(file), i == 0 ? "Original" : "Layer");
                                    image.layerList.add(layer);
                                    Tonga.loader().appendProgress(1.0);
                                }
                            } catch (ClosedChannelException ex) {
                                Tonga.log.warn("Interrupted wile BFIO importing.");
                            } catch (Exception ex) {
                                if (file.isDirectory()) {
                                    Tonga.catchError(ex, "Folder importing is not supported.");
                                } else {
                                    Tonga.catchError(ex, "Image file can not be imported.");
                                }
                                failures++;
                                if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                                    formatissue = true;
                                    Tonga.log.warn("Unsupported format");
                                }
                                Tonga.loader().appendToNext();
                            }
                        }
                        layers = image.layerList.size();
                        if (layers > 0) {
                            picList.add(image);
                        }
                    } catch (Exception ex) {
                        Tonga.catchError(ex, "Image file can not be imported.");
                        failures++;
                        if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                            formatissue = true;
                            Tonga.log.warn("Unsupported format");
                        }
                        Tonga.loader().appendToNext();
                    }
                    if (failures == files.size()) {
                        Tonga.refreshChanges(files.get(0), "<font color=\"red\">Image importing failed.</font>" + (formatissue ? " Unsupported file format." : ""));
                    } else {
                        Tonga.refreshChanges(files.get(0), "Imported "
                                + (layers > 0 ? "a new image with " + layers + " layers" + (stacks > 0 ? " and " : "") : "")
                                + (stacks > 0 ? stacks + " stack image(s)" : "")
                                + (failures > 0 ? " but " + failures + " file" + (failures > 1 ? "s" : "") + " failed to be imported." : "."));
                    }
                });
                Tonga.bootThread(thread, "Importer", false, true);
            }
        }
    }

    public static CachedImage getImageFromFile(String file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        return getImageFromFile(new File(file));
    }

    public static ImageData getImageDataFromFile(String file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        return new ImageData(getImageFromFile(new File(file)));
    }

    public static CachedImage getImageFromFile(File file) throws FileNotFoundException, ServiceException, FormatException, IOException {
        if (file.exists()) {
            try {
                try {
                    CachedImage n;
                    n = new CachedImage(file);
                    return n;
                } catch (Exception ex) {
                    Tonga.catchError(ex);
                    return StackImporter.openFile(file)[0].layerList.get(0).layerImage;
                }
            } catch (IOException | ServiceException | FormatException ex) {
                throw ex;
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    public static void exportLayer(boolean all) {
        Thread thread = all
                ? new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    int layer = Tonga.getLayerIndex();
                    if (picList.stream().mapToInt((i) -> i.layerList.size()).min().getAsInt() == Tonga.getLayerList().size()
                            && picList.stream().map((i) -> i.layerList.get(layer).layerName).distinct().count() == 1) {
                        picList.forEach((p) -> {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            exportImage(p, layer);
                            Tonga.loader().loaderProgress(picList.indexOf(p) + 1, picList.size());
                        });
                        Tonga.setStatus("Selected layers of all images were exported into PNG format");
                    } else {
                        Tonga.setStatus("All of the images must have the same layer structure");
                    }
                }) : new Thread(() -> {
                    Tonga.loader().loaderProgress(0, 1);
                    if (exportImage(picList.get(Tonga.getImageIndex()), Tonga.getLayerIndex())) {
                        Tonga.loader().loaderProgress(1, 1);
                        Tonga.setStatus("Selected layer of the image exported into PNG format");
                    }
                });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    public static void exportLayers(boolean all) {
        Thread thread = all
                ? new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    picList.forEach((p) -> {
                        p.layerList.forEach((l) -> {
                            if (Thread.currentThread().isInterrupted()) {
                                return;
                            }
                            exportImage(p, p.layerList.indexOf(l));
                            Tonga.loader().loaderProgress((int) ((picList.indexOf(p)
                                    + (p.layerList.indexOf(l) + 1) / (double) p.layerList.size()) * 100), picList.size() * 100);
                        });
                        Tonga.loader().loaderProgress(picList.indexOf(p) + 1, picList.size());
                    });
                    Tonga.setStatus("All layers of all images were exported into PNG format");
                }) : new Thread(() -> {
                    Tonga.loader().loaderProgress(0, picList.size());
                    TongaImage p = Tonga.getImage();
                    p.layerList.forEach((l) -> {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        exportImage(p, p.layerList.indexOf(l));
                        Tonga.loader().loaderProgress(p.layerList.indexOf(l) + 1, p.layerList.size());
                    });
                    Tonga.loader().loaderProgress(picList.size(), picList.size());
                    Tonga.setStatus("All layers of the selected image were exported into PNG format");
                });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    public static void exportStack(boolean all) {
        Thread thread = new Thread(() -> {
            Tonga.loader().loaderProgress(0, picList.size());
            for (int i = all ? 0 : Tonga.getImageIndex(); i <= (all ? picList.size() - 1 : Tonga.getImageIndex()); i++) {
                TongaImage m = Tonga.picList.get(i);
                boolean stackmem = m.stack;
                m.stack = true;
                exportImage(m, TongaRender.renderImage(Tonga.selectedImageAsImageDataArray(i)));
                Tonga.loader().loaderProgress(i + 1, picList.size());
                m.stack = stackmem;
            }
            Tonga.loader().loaderProgress(picList.size(), picList.size());
            Tonga.setStatus("Stack " + (all ? "images" : "image") + " of " + (all ? "all of the images were" : "the current image was") + " exported into PNG format");
        });
        Tonga.bootThread(thread, "Exporter", false, true);
    }

    private static boolean exportImage(TongaImage p, ImageData i) {
        String name = p.imageName + "_[Stack]";
        return bootExporter(i.toCachedImage(), name);
    }

    private static boolean exportImage(TongaImage p, int layer) {
        String name = p.imageName + "_" + p.layerList.get(layer).layerName;
        return bootExporter(p.layerList.get(layer).layerImage, name);
    }

    private static boolean bootExporter(BufferedImage i, String name) {
        boolean ok = new Exporter() {
            @Override

            void write() throws IOException {
                if (i.getClass() == CachedImage.class) {
                    CachedImage ci = (CachedImage) i;

                    ImageIO.write(ci.bits
                            == 16 ? ci.get8BitCopy() : i, "png", file);
                } else {
                    ImageIO.write(i, "png", file);
                }
            }
        }.export(name, "png");
        return ok;
    }

    public static File exportTable(boolean temp) {
        Tonga.frame().resultHash();
        String fname = "results" + new SimpleDateFormat("ddMMyyyHHmmss").format(new Date());
        File nfile = new File(temp
                ? (Tonga.getTempPath() + fname + ".tsv")
                : Tonga.formatPath(Tonga.frame().filePathField.getText() + "\\" + fname + ".tsv"));
        Thread thread = new Thread(() -> {
            if (!temp) {
                boolean ok = new Exporter() {
                    @Override
                    void write() throws IOException {
                        IO.toTSVfile(Tonga.frame().resultTable, file);
                    }
                }.export(fname, "tsv");
                if (ok) {
                    Tonga.setStatus("Table exported into TSV format");
                    Tonga.log.info("Table exported into TSV format.");
                }
            } else {
                IO.toTSVfile(Tonga.frame().resultTable, nfile);
                launchExcel(nfile);
                Tonga.setStatus("Table exported and fired into Excel");
                Tonga.log.info("Table exported and fired into Excel.");
            }
        });
        Tonga.bootThread(thread, "Exporter", true, true);
        return nfile;
    }

    public static void launchExcel(File file) {
        try {
            switch (Tonga.currentOS()) {
                case WINDOWS:
                    Runtime.getRuntime().exec("cmd /c start excel \"" + file.getAbsolutePath() + "\"");
                    break;
                case MAC:
                    new ProcessBuilder("open", "-a", "Microsoft Excel", file.getAbsolutePath()).start();
                    //exec("open -a \"Microsoft Excel\" \"" + file.getAbsolutePath() + "\"");
                    //exec("open /Applications/Microsoft Excel.app");
                    break;
                case UNKNOWN:
                    Tonga.catchError(new UnsupportedOperationException(), "No excel launcher for this OS.");
                    break;
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Excel can not be started.");
        }
    }

    public static void waitForJFXRunLater() {
        try {
            Semaphore semaphore = new Semaphore(0);
            Platform.runLater(() -> semaphore.release());
            semaphore.acquire();
        } catch (InterruptedException ex) {
            Tonga.catchError(ex, "You should never see this.");
        }
    }

    private static int getLayerNumber(TongaImage[] images) {
        int counter = 0;
        for (TongaImage image : images) {
            counter += image.layerList.size();
        }
        return counter;
    }

    static String fileName(String name) {
        int pp = name.lastIndexOf(".");
        if (pp >= name.length() - 4 && pp > -1) {
            return name.substring(0, pp);
        } else {
            return name;
        }
    }
}
