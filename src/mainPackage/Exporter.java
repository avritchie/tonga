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

    public boolean export(String n, String ext) {
        n = legalName(n);
        file = new File(Tonga.formatPath(Tonga.frame().filePathField.getText() + "\\" + n + "." + ext));
        System.out.println(file);
        int id = 0;
        //!mainFrame.settingOverwrite()
        while (file.exists()) {
            //Tonga.setStatus("Some file(s) already existed and were skipped");
            file = new File(Tonga.formatPath(Tonga.frame().filePathField.getText() + "\\" + n + id + "." + ext));
            id++;
        }
        file.getParentFile().mkdirs();
        try {
            write();
        } catch (IOException ex) {
            Tonga.catchError(ex, "There were error(s) during the file export");
            return false;
        }
        if (Settings.settingOpenAfterExport()) {
            try {
                Desktop.getDesktop().open(file.getParentFile());
            } catch (IOException ex) {
                Tonga.catchError(ex, "Unable to open the folder.");
            }
        }
        return true;
    }

    abstract void write() throws IOException;

    private String legalName(String n) {
        n = n.replaceAll("[^a-zA-Z0-9]", "");
        if (n.isEmpty()) {
            n = "null";
        }
        return n;
    }
}
