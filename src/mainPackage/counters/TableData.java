package mainPackage.counters;

import java.util.ArrayList;
import java.util.List;

public class TableData {

    public TableData(String[] titles, String[] descs) {
        columns = titles;
        descriptions = descs;
        rows = new ArrayList<>();
    }

    public TableData(String[] titles) {
        columns = titles;
        descriptions = null;
        rows = new ArrayList<>();
    }

    public String[] columns;
    public String[] descriptions;
    public List<Object[]> rows;

    public int columnCount() {
        return columns.length;
    }

    public int rowCount() {
        return rows.size();
    }

    public void newRow(Object[] row) {
        rows.add(row);
    }

    public Object[] newRow(String title) {
        Object[] row = new Object[columnCount()];
        row[0] = title;
        for (int j = 1; j < columnCount(); j++) {
            row[j] = box(0);
        }
        rows.add(row);
        return row;
    }

    public void newRow(String... param) {
        Object[] row = new Object[columnCount()];
        System.arraycopy(param, 0, row, 0, columnCount());
        rows.add(row);
    }

    public Object getVal(int row, int column) {
        return rows.get(row)[column];
    }

    public Object[][] getAsArray() {
        Object[][] data = new Object[rows.size()][columns.length];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        return data;
    }

    public static TableData createTable(String[] columns, String[] descs, int rowNumber, String rowTitle) {
        TableData ret = new TableData(columns, descs);
        for (int i = 0; i < rowNumber; i++) {
            ret.newRow(rowTitle)[1] = box(i);
        }
        return ret;
    }

    public static Integer box(int i) {
        return Integer.valueOf(i);
    }

    public void rowIntInc(int row, int column) {
        Integer target = (Integer) this.rows.get(row)[column];
        this.rows.get(row)[column] = target + 1;
    }
}
