package mainPackage;

import javax.swing.SwingUtilities;

public class Splash extends javax.swing.JFrame {

    static long t;
    static int index;
    static Splash splash;
    static String[] stages;

    public Splash() {
        splash = this;
        index = 0;
        initComponents();
        t = System.nanoTime();
        stages = new String[]{
            "Menu control",
            "Panel control",
            "Panel layout",
            "Interactive control",
            "Context layout",
            "Popup layout",
            "Control layout",
            "Tonga menu",
            "Filters menu",
            "Protocol menu",
            "Counter menu",
            "Debug menu",
            "Main frame",
            "Frame packing",
            "Extra component",
            "Icon",
            "Dialog components",
            "Dialog layout",
            "Info dialog",
            "Wizard",
            "Wizard dialog",
            "Form components",
            "Form dialog",
            "Image panel",
            "Popup",
            "Window handler",
            "Window",
            "Loader",
            "Stack importer",
            "Configuration",
            "Histogram",
            "Pattern",
            "Zoom panel",
            "Main panel",
            "Selectors"
        };
    }

    public static void append(String message, int val) {
        Tonga.log.info("{} initialization completed in {} ms ({}%)", message, (System.nanoTime() - t) / 1000000, val);
        t = System.nanoTime();
        splash.pbar.setIndeterminate(false);
        splash.pbar.setMaximum(2000);
        splash.pbar.setValue(splash.pbar.getValue() + 10 * val);
        String newmessage = (index + 1 < stages.length) ? ("Initializing " + stages[++index].toLowerCase()) : "Finalizing";
        splash.pbar.setString(newmessage + "...");
        if (SwingUtilities.isEventDispatchThread()) {
            splash.jPanel1.paintImmediately(0, 0, splash.jPanel1.getWidth(), splash.jPanel1.getHeight());
        }
    }

    public static void append(String message) {
        append(message, 1);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        pbar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Welcome to Tonga!");
        setAlwaysOnTop(true);
        setLocation(new java.awt.Point(0, 0));
        setUndecorated(true);
        setType(java.awt.Window.Type.POPUP);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        pbar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        pbar.setMaximum(1000);
        pbar.setIndeterminate(true);
        pbar.setString("Loading Tonga...");
        pbar.setStringPainted(true);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pbar, javax.swing.GroupLayout.PREFERRED_SIZE, 423, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pbar, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    public javax.swing.JProgressBar pbar;
    // End of variables declaration//GEN-END:variables

}
