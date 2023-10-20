package mainPackage.counters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mainPackage.Tonga;

public class TableData {

    public TableData(String[] titles, String[] descs) {
        columns = Arrays.copyOf(titles, titles.length);
        descriptions = Arrays.copyOf(descs, descs.length);
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

    public Object[] newRow(String... param) {
        Object[] row = new Object[columnCount()];
        System.arraycopy(param, 0, row, 0, columnCount());
        rows.add(row);
        return row;
    }

    public void delLastRow() {
        rows.remove(rows.size() - 1);
    }

    public void delRow(int i) {
        rows.remove(i);
    }

    public Object getVal(int row, int column) {
        return rows.get(row)[column];
    }

    public int getInteger(int row, int column) {
        Object container = getVal(row, column);
        if (container instanceof Object[]) {
            return ((Integer) ((Object[]) container)[0]);
        } else {
            return (Integer) container;
        }
    }

    public double getDouble(int row, int column) {
        Object container = getVal(row, column);
        if (container instanceof Object[]) {
            return ((Double) ((Object[]) container)[0]);
        } else {
            return (Double) container;
        }
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

    public TableData copy() {
        TableData td = new TableData(this.columns, this.descriptions);
        for (int i = 0; i < this.rowCount(); i++) {
            Object[] nr = new Object[this.columnCount()];
            System.arraycopy(this.rows.get(i), 0, nr, 0, this.columnCount());
            td.newRow(nr);
        }
        return td;
    }

    public boolean equals(TableData obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        final TableData other = (TableData) obj;
        if (!Arrays.deepEquals(this.columns, other.columns)) {
            return false;
        }
        if (!Arrays.deepEquals(this.descriptions, other.descriptions)) {
            return false;
        }
        if (this.rowCount() != other.rowCount()) {
            return false;
        }
        for (int i = 0; i < this.rowCount(); i++) {
            for (int j = 0; j < this.columnCount(); j++) {
                if (!this.rows.get(i)[j].equals(other.rows.get(i)[j])) {
                    return false;
                }
            }
        }
        return true;
    }

    public void append(TableData itd) {
        for (int j = 0; j < itd.rowCount(); j++) {
            this.newRow(itd.rows.get(j));
        }
    }
}
