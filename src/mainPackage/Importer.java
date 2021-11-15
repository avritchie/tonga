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
            iterate();
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
                if (!readStack()) {
                    read();
                }
            }
            Tonga.loader().appendProgress(1.0);
        } catch (ClosedChannelException ex) {
            Tonga.catchError(ex, "The Bio-Formats importer was interrupted while importing.");
        } catch (Exception ex) {
            if (file.isDirectory()) {
                Tonga.catchError(ex, "Folder importing is not supported.");
            } else if (ex instanceof FormatException || ex instanceof IllegalStateException) {
                formatissue = true;
                Tonga.catchError(ex, "The file could not be imported because the format is unsupported.");
            } else if (ex instanceof IOException) {
                Tonga.catchError(ex, "The file could not be imported because of an IO error.");
            }
            failures++;
            Tonga.loader().appendToNext();
        }
    }

    //iterate must call "readFile", but never any abstract functions
    abstract void iterate();

    abstract void read() throws Exception;

    abstract void readBatch() throws Exception;

    abstract boolean readStack() throws IOException, FormatException, ServiceException;

    abstract String message();
}
