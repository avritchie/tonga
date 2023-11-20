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
    private int[] columnWidth;
    protected TableData tableData;

    public TongaTable(JTable table, int index) {
        this.tableComponent = table;
        this.tableComponentIndex = index;
        this.tableData = null;
    }

    private DefaultTableModel newTableModel(TableData tableData) {
        return new DefaultTableModel(tableData.getAsArray(), tableData.columns) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };
    }

    public TableColumnModel newColumnModel(TableData tableData) {
        TableColumnModel tcm = getTableComponent().getColumnModel();
        setColumnProperties(tcm);
        return tcm;
    }

    //implement custom column renderers for each subclass
    public abstract void setColumnProperties(TableColumnModel tcm);

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

    public TableColumnModel createColumnModel(TableData tableData) {
        columnModel = newColumnModel(tableData);
        return columnModel;
    }

    private void changeElement(Runnable action) {
        //allow event invoking either from the ED thread (through GUI)
        //or from other threads (protocols etc.)
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    public void overwriteData(TableData tableData) {
        this.tableData = tableData;
        changeElement(() -> {
            tableComponent.setModel(createTableModel(tableData));
            tableComponent.setColumnModel(createColumnModel(tableData));
        });
    }

    public void clearData() {
        backendClear();
        tableData = null;
        changeElement(() -> {
            tableComponent.setModel(new DefaultTableModel());
        });
    }

    public void addRow(Object[] data) {
        tableData.newRow(data);
        changeElement(() -> {
            tableModel.addRow(data);
        });
    }

    public void deleteRow(int index) {
        tableData.delRow(index);
        changeElement(() -> {
            tableModel.removeRow(index);
        });
    }

    public void addRows(Object[][] data) {
        for (Object[] rowData : data) {
            addRow(rowData);
        }
    }

    public void appendData(TableData tableData) {
        tableData.rows.forEach(d -> {
            addRow(d);
        });
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
        backendDelete(new int[]{index});
        deleteRow(index);
    }

    //externally invoked deletion
    public void removeSelectedRows() {
        int[] indices = getSortedRows(tableComponent.getSelectedRows());
        backendDelete(indices);
        deleteRows(indices);
    }

    //when deleted data from the model/gui ensure deletion of the backend data structure
    protected abstract void backendClear();

    //when deleted data from the model/gui ensure deletion of the backend data structure
    protected abstract void backendDelete(int[] row);

    private int[] getSortedRows(int[] viewRows) {
        int[] modelRows = new int[viewRows.length];
        for (int r = 0; r < viewRows.length; r++) {
            modelRows[r] = tableComponent.convertRowIndexToModel(viewRows[r]);
        }
        Arrays.sort(modelRows);
        return modelRows;
    }

    public void focus() {
        changeElement(() -> {
            Tonga.frame().tabbedPane.setSelectedIndex(tableComponentIndex);
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
