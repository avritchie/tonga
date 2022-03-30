package mainPackage.counters;

import java.util.ArrayList;
import java.util.List;
import mainPackage.Tonga;

public class TableData {

    public TableData(String[] titles, String[] descs) {
        columns = titles;
        descriptions = descs;
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

    public static Double getType(Object objs) {
        double value;
        Object obj;
        if (objs instanceof Object[]) {
            obj = ((Object[]) objs)[0];
        } else {
            obj = objs;
        }
        if (obj instanceof Double) {
            value = (Double) obj;
        } else if (obj instanceof Long) {
            value = (Long) obj;
        } else if (obj instanceof Integer) {
            value = (Integer) obj;
        } else {
            Tonga.catchError("Non-numeric result data passed as numeric data");
            return null;
        }
        return value;
    }

    public void rowIntInc(int row, int column) {
        Integer target = (Integer) this.rows.get(row)[column];
        this.rows.get(row)[column] = target + 1;
    }
}
