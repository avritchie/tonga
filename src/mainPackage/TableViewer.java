package mainPackage;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 *
 * @author Victoria
 */
public class TableViewer extends javax.swing.JFrame {

    JScrollPane scrollPane;
    JTable tableComponent;
    int tab;

    public TableViewer() {
        initComponents();
        int[] winSize = Tonga.frame().getWindowSizeRecommendation();
        setSize(winSize[0] - 100, winSize[1] - 100);
        setLocationRelativeTo(null);
        setIconImages(Tonga.frame().mainIcons);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                Tonga.frame().tableWindow = null;
                restoreTable();
                Tonga.frame().tabbedPane.setSelectedIndex(tab);
            }
        });
    }

    public void setTable(JTable table, JScrollPane origin, int tab) {
        if (tableComponent != null) {
            restoreTable();
        }
        this.tab = tab;
        this.scrollPane = origin;
        this.tableComponent = table;
        this.resultScrollPane.setViewportView(tableComponent);
        setVisible(true);
    }

    public void restoreTable() {
        scrollPane.setViewportView(tableComponent);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        resultScrollPane = new javax.swing.JScrollPane();

        setTitle("Results");
        setLocation(new java.awt.Point(0, 0));
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        setType(java.awt.Window.Type.POPUP);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        getAccessibleContext().setAccessibleName("Table");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane resultScrollPane;
    // End of variables declaration//GEN-END:variables

}
