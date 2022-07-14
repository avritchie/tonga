package mainPackage;

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
}
