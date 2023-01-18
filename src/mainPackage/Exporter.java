/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public abstract class Exporter {

    File file;

    public boolean exportFile(String n, String ext, String desc, boolean temp) {
        n = IO.legalName(n);
        String tp = temp ? Tonga.getTempPath() : Tonga.frame().filePathField.getText() + "\\";
        file = new File(Tonga.formatPath(tp + n + "." + ext));
        Tonga.log.info("Exporting {} to {}", n, file);
        int id = 0;
        //!mainFrame.settingOverwrite()
        while (file.exists()) {
            //Tonga.setStatus("Some file(s) already existed and were skipped");
            file = new File(Tonga.formatPath(tp + n + id + "." + ext));
            id++;
        }
        file.getParentFile().mkdirs();
        try {
            write();
        } catch (IOException ex) {
            Tonga.catchError(ex, "There were error(s) during the file export.");
            return false;
        }
        if (Settings.settingOpenAfterExport() && !temp) {
            try {
                Desktop.getDesktop().open(file.getParentFile());
            } catch (IOException ex) {
                Tonga.catchError(ex, "Unable to open the folder.");
            }
        }
        if (desc != null) {
            Tonga.setStatus(desc);
            Tonga.log.info(desc);
        }
        return true;
    }

    abstract void write() throws IOException;
}
