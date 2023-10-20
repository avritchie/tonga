package mainPackage;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import mainPackage.counters.TableData;

public class TongaTable {

    static TableData td = null;
    static int hi = -1;

    public static void publishData(TableData tableData) {
        boolean append = Settings.settingResultsAppend();
        // jos on asetus ja olemassaolevan datan rakenne on sama
        if (append) {
            append = dataMatches(tableData);
        }
        if (!append) {
            overwriteData(tableData);
            refresh();
        } else {
            appendData(tableData);
            refresh();
        }
    }

    public static TableData getData() {
        return td;
    }

    private static boolean dataMatches(TableData tableData) {
        DefaultTableModel model = (DefaultTableModel) Tonga.frame().resultTable.getModel();
        if (model.getRowCount() == 0 && model.getColumnCount() == 0) {
            return false;
        } else if (model.getColumnCount() != tableData.columns.length) {
            return false;
        } else {
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (!model.getColumnName(i).equals(tableData.columns[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void overwriteData(TableData tableData) {
        td = tableData;
        SwingUtilities.invokeLater(() -> {
            Tonga.frame().resultTable.setModel(new DefaultTableModel(tableData.getAsArray(), tableData.columns) {
                @Override
                public Class getColumnClass(int columnIndex) {
                    return tableData.rows.get(0)[columnIndex].getClass();
                }
            });
        });
    }

    public static void appendData(TableData tableData) {
        DefaultTableModel model = (DefaultTableModel) Tonga.frame().resultTable.getModel();
        tableData.rows.forEach(d -> {
            model.addRow(d);
            td.newRow(d);
        });
    }

    public static void clearData() {
        td = null;
        SwingUtilities.invokeLater(() -> {
            Tonga.frame().resultTable.setModel(new DefaultTableModel());
            Tonga.frame().resultTable.repaint();
        });
    }

    public static void deleteRow() {
        int[] sels = Tonga.frame().resultTable.getSelectedRows();
        DefaultTableModel model = (DefaultTableModel) Tonga.frame().resultTable.getModel();
        for (int i = sels.length - 1; i >= 0; i--) {
            if (td.rowCount() == 1) {
                clearData();
                return;
            } else {
                model.removeRow(sels[i]);
                td.delRow(sels[i]);
            }
        }
    }

    private static void refresh() {
        SwingUtilities.invokeLater(() -> {
            Tonga.frame().tabbedPane.repaint();
            Tonga.frame().tabbedPane.setSelectedIndex(3);
        });
    }

    public static void hover(int index) {
        if (isData()) {
            if (hi != index) {
                updateTooltip(index);
                hi = index;
            }
        }
    }

    public static void updateTooltip(int index) {
        if (isData()) {
            if (td.descriptions != null) {
                try {
                    Tonga.setStatus(td.descriptions[index]);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    Tonga.log.warn("Description for column {} is missing", index);
                }
            } else {
                Tonga.setStatus(td.columns[index]);
            }
        }
    }

    private static boolean isData() {
        return Tonga.frame().resultTable.getModel().getColumnCount() > 0;
    }

    public static void importData(File file) {
        try {
            UndoRedo.start();
            String[] lines = Files.readAllLines(file.toPath()).toArray(String[]::new);
            String[] cols = lines[0].replaceAll("\\ufeff", "").split("\t");
            TableData ntd = new TableData(cols, cols);
            Pattern doublen = Pattern.compile("/^\\d+(,\\d+)*$/");
            Pattern intn = Pattern.compile("^(\\d+)*$");
            for (int r = 1; r < lines.length; r++) {
                String[] vals = lines[r].split("\t");
                Object[] row = ntd.newRow(vals[0]);
                for (int c = 1; c < cols.length; c++) {
                    Matcher md = doublen.matcher(vals[c]);
                    Matcher mi = intn.matcher(vals[c]);
                    try {
                        if (mi.find() && !vals[c].isEmpty()) {
                            row[c] = Integer.valueOf(vals[c]);
                        } else if (md.find() && !vals[c].isEmpty()) {
                            row[c] = Double.valueOf(vals[c]);
                        } else {
                            row[c] = vals[c];
                        }
                    } catch (NumberFormatException ex) {
                        Tonga.log.warn("Failed to parse {} as a number.", vals[c]);
                        row[c] = vals[c];
                    }
                }
            }
            publishData(ntd);
            UndoRedo.end();
        } catch (Exception ex) {
            Tonga.catchError(ex, "Could not read the data table file.");
        }
    }

    public static void transposeByImage() {
        if (td == null) {
            Tonga.setStatus("No result data to transpose.");
        } else if (td.columns[0].equals("Image")) {
            UndoRedo.start();
            LinkedHashMap<String, ArrayList<Object>> hm = new LinkedHashMap<>();
            td.rows.forEach(r -> {
                String img = (String) r[0];
                if (!hm.containsKey(img)) {
                    hm.put(img, new ArrayList<>());
                }
                ArrayList<Object> al = hm.get(img);
                al.add(Arrays.copyOfRange(r, 1, r.length));
            });
            int perimg = td.columnCount() - 1;
            int rows = hm.values().stream().mapToInt(a -> a.size()).max().getAsInt();
            String[] images = hm.keySet().toArray(String[]::new);
            ArrayList<Object>[] values = hm.values().toArray(ArrayList[]::new);
            //find uniques
            ArrayList<ArrayList<Object>> uniques = new ArrayList<>();
            ArrayList<Integer> uniInd = new ArrayList<>();
            if (images.length > 1) {
                cols:
                for (int c = 0; c < perimg; c++) {
                    for (int r = 0; r < rows; r++) {
                        if (values[0].size() > r) {
                            Object[] ref = (Object[]) values[0].get(r);
                            for (int i = 0; i < hm.size(); i++) {
                                if (values[i].size() > r) {
                                    Object[] cur = (Object[]) values[i].get(r);
                                    if (cur.length != ref.length || !(cur[c - uniques.size()].equals(ref[c - uniques.size()]))) {
                                        continue cols;
                                    }
                                } else {
                                    continue cols;
                                }
                            }
                        } else {
                            continue cols;
                        }
                    }
                    //unique found
                    ArrayList<Object> uni = new ArrayList<>();
                    for (int r = 0; r < rows; r++) {
                        uni.add(((Object[]) values[0].get(r))[c]);
                        for (int i = 0; i < hm.size(); i++) {
                            Object[] curr = ((Object[]) values[i].get(r));
                            Object[] newr = new Object[curr.length - 1];
                            System.arraycopy(curr, 0, newr, 0, c);
                            System.arraycopy(curr, c + 1, newr, c, newr.length - c);
                            values[i].remove(r);
                            values[i].add(r, newr);
                        }
                    }
                    uniques.add(uni);
                    uniInd.add(c);
                }
            }
            //make descs
            int trimimg = perimg - uniques.size();
            int cols = hm.size() * trimimg + uniques.size();
            String[] colnames = new String[cols];
            String[] coldescs = new String[cols];
            for (int i = 0; i < uniques.size(); i++) {
                colnames[i] = td.columns[uniInd.get(i) + 1];
                coldescs[i] = td.descriptions[uniInd.get(i) + 1];
            }
            for (int i = 0; i < hm.size(); i++) {
                int addIndU = 0;
                for (int c = 0; c < trimimg; c++) {
                    if (uniInd.contains(c)) {
                        addIndU++;
                    }
                    colnames[uniques.size() + c * hm.size() + i] = images[i] + " " + td.columns[c + 1 + addIndU];
                    coldescs[uniques.size() + c * hm.size() + i] = td.descriptions[c + 1 + addIndU];
                }
            }
            //
            TableData ntd = new TableData(colnames, coldescs);
            for (int r = 0; r < rows; r++) {
                Object[] row = new Object[cols];
                for (int u = 0; u < uniques.size(); u++) {
                    row[u] = uniques.get(u).get(r);
                }
                for (int c = 0; c < trimimg; c++) {
                    for (int i = 0; i < hm.size(); i++) {
                        ArrayList<Object> tv = values[i];
                        Object oval;
                        if (tv.size() > r) {
                            oval = ((Object[]) tv.get(r))[c];
                        } else {
                            oval = "";
                        }
                        row[uniques.size() + c * hm.size() + i] = oval;
                    }
                }
                /*for (int i = 0; i < hm.size(); i++) {
                    ArrayList<Object> tv = values[i];
                    if (tv.size() > r) {
                        System.arraycopy((Object[]) tv.get(r), 0, row, uniques.size() + trimimg * i, trimimg);
                    } else {
                        Arrays.fill(row, uniques.size() + trimimg * i, trimimg * i + trimimg, "");
                    }
                }*/
                ntd.newRow(row);
            }
            publishData(ntd);
            UndoRedo.end();
        } else {
            Tonga.setStatus("The result data doesn't have an Image-column as the first column.");
        }
    }
}
