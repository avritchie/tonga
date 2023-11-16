package mainPackage;

import java.util.Arrays;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import mainPackage.counters.TableData;

public abstract class TongaTable {

    private JTable tableComponent;
    private int tableComponentIndex;
    private int hoverIndex = -1;
    private DefaultTableModel tableModel;
    private TableColumnModel columnModel;
    protected TableData tableData;

    public TongaTable(JTable table, int index) {
        this.tableComponent = table;
        this.tableComponentIndex = index;
        this.tableData = null;
    }

    public TableData getData() {
        return tableData;
    }

    public JTable getTableComponent() {
        return tableComponent;
    }

    public boolean isData() {
        return tableData != null;
    }

    public Object getCell(int row, int column) {
        return tableModel.getValueAt(row, column);
    }

    public int getRowCount() {
        return tableModel.getRowCount();
    }

    public int getColumnCount() {
        return tableModel.getColumnCount();
    }

    public void publishData(TableData tableData, boolean append) {
        if (append) {
            append = dataMatches(tableData);
        }
        if (!append) {
            overwriteData(tableData);
            focus();
        } else {
            appendData(tableData);
            focus();
        }
    }

    private boolean dataMatches(TableData tableData) {
        DefaultTableModel model = (DefaultTableModel) tableComponent.getModel();
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

    public DefaultTableModel createTableModel(TableData tableData) {
        tableModel = newTableModel(tableData);
        return tableModel;
    }

    public abstract DefaultTableModel newTableModel(TableData tableData);

    public TableColumnModel createColumnModel(TableData tableData) {
        columnModel = newColumnModel(tableData);
        return columnModel;
    }

    public abstract TableColumnModel newColumnModel(TableData tableData );

    public void overwriteData(TableData tableData) {
        SwingUtilities.invokeLater(() -> {
            tableComponent.setModel(createTableModel(tableData));
            tableComponent.setColumnModel(createColumnModel(tableData));
            this.tableData = tableData;
        });
    }

    public void appendData(TableData tableData) {
        SwingUtilities.invokeLater(() -> {
            tableData.rows.forEach(d -> {
                addRow(d);
            });
        });
    }

    public void clearData() {
        clearEvent();
        SwingUtilities.invokeLater(() -> {
            tableComponent.setModel(new DefaultTableModel());
            tableData = null;
        });
    }

    public void addRow(Object[] data) {
        tableModel.addRow(data);
        tableData.newRow(data);
    }

    public void addRows(Object[][] data) {
        for (Object[] rowData : data) {
            addRow(rowData);
        }
    }

    protected abstract void clearEvent();

    protected abstract void deleteEvent(int[] row);

    public void deleteRow(int index) {
        tableModel.removeRow(index);
        tableData.delRow(index);
    }

    public void deleteRows(int[] indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            if (tableData.rowCount() == 1) {
                clearData();
                return;
            } else {
                deleteRow(indices[i]);
            }
        }
    }

    public void deleteAllRows() {
        int rc = getRowCount();
        for (int i = 0; i < rc; i++) {
            deleteRow(0);
        }
    }

    //externally invoked deletion
    public void removeRow(int index) {
        deleteEvent(new int[]{index});
        deleteRow(index);
    }

    //externally invoked deletion
    public void removeSelectedRows() {
        int[] indices = getSortedRows(tableComponent.getSelectedRows());
        deleteEvent(indices);
        deleteRows(indices);
    }

    private int[] getSortedRows(int[] viewRows) {
        int[] modelRows = new int[viewRows.length];
        for (int r = 0; r < viewRows.length; r++) {
            modelRows[r] = tableComponent.convertRowIndexToModel(viewRows[r]);
        }
        Arrays.sort(modelRows);
        return modelRows;
    }

    public void focus() {
        SwingUtilities.invokeLater(() -> {
            Tonga.frame().tabbedPane.setSelectedIndex(tableComponentIndex);
            //Tonga.frame().tabbedPane.repaint();
        });
    }

    public void hover(int index) {
        if (isData()) {
            if (hoverIndex != index) {
                updateTooltip(index);
                hoverIndex = index;
            }
        }
    }

    public void updateTooltip(int index) {
        if (isData()) {
            if (tableData.descriptions != null) {
                try {
                    Tonga.setStatus(tableData.descriptions[index]);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    Tonga.log.warn("Description for column {} is missing", index);
                }
            } else {
                Tonga.setStatus(tableData.columns[index]);
            }
        }
    }

}
