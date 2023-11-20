package mainPackage;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import mainPackage.counters.TableData;

public class TongaAnnotationTable extends TongaTable {

    private int[] colWids = new int[]{40, 80, 60, 60, 80, 80, 100, 60, 80, 80, 60, 40};

    public TongaAnnotationTable(JTable table, int index) {
        super(table, index);
    }

    @Override
    public void setColumnProperties(TableColumnModel tcm) {
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(colWids[i]);
        }
        tcm.getColumn(11).setCellRenderer(new CellColorRenderer());
    }

    private class CellColorRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            TongaColor tc = (TongaColor) table.getValueAt(row, column);
            c.setBackground(tc.getColorAWT());
            c.setForeground(tc.getColorAWT());
            return c;
        }
    }

    public TableData newData() {
        return new TableData(
                new String[]{"ID", "Type", "X", "Y", "Width", "Height", "Area", "Nodes", "Length", "Angle", "Group", "Color"},
                new String[]{"Annotation ID", "Annotation type",
                    "The X coordinate of the first node of the annotation", "The Y coordinate of the first node of the annotation",
                    "Width of the annotation bounding box", "Height of the annotation bounding box",
                    "Annotation area size", "Number of nodes in this annotation",
                    "Annotation (line) length or perimeter/circumference", "Sum of all annotation angles in degrees",
                    "User-defined annotation group", "Annotation color"});
    }

    @Override
    protected void backendDelete(int[] rows) {
        TongaAnnotator.delete(rows);
    }

    @Override
    protected void backendClear() {
        TongaAnnotator.deleteAll();
    }
}
