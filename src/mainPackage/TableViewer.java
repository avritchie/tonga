package mainPackage;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author Victoria
 */
public class TableViewer extends javax.swing.JFrame {

    public TableViewer() {
        initComponents();
        int[] winSize = Tonga.frame().getWindowSizeRecommendation();
        setSize(winSize[0] - 100, winSize[1] - 100);
        setLocationRelativeTo(null);
        setIconImages(Tonga.frame().mainIcons);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                Tonga.frame().resultScrollPane.setViewportView(Tonga.frame().resultTable);
                Tonga.frame().tabbedPane.setSelectedIndex(3);
            }
        });
        this.resultScrollPane.setViewportView(Tonga.frame().resultTable);
        setVisible(true);
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

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane resultScrollPane;
    // End of variables declaration//GEN-END:variables

}
