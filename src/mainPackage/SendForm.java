package mainPackage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 *
 * @author Victoria
 */
public class SendForm extends javax.swing.JFrame {

    public SendForm() {
        initComponents();
    }

    @Override
    public void setVisible(boolean yes) {
        super.setVisible(yes);
        if (yes) {
            reset();
        }
    }

    private void reset() {
        enableControls(true);
        progressBar.setIndeterminate(false);
        textArea.setText("");
        logBox.setSelected(false);
    }

    private void enableControls(boolean yes) {
        textArea.setEnabled(yes);
        logBox.setEnabled(yes);
        goForItButton.setEnabled(yes);
        nahButton.setEnabled(yes);
    }

    private void sendFeedback() {
        if (!textArea.getText().isEmpty()) {
            try {
                Method sendMethod = Class.forName("mainPackage.SheetsAPI").getMethod("sendFeedback",
                        Object[].class, String.class, String.class);
                progressBar.setIndeterminate(true);
                enableControls(false);
                Thread thread = new Thread(() -> {
                    String text = textArea.getText();
                    String logs = getLogsAsString();
                    Object[] fields = new String[]{
                        getTime(),
                        Tonga.tongaVersion,
                        Tonga.currentOS.name(), "Hover to read", (logs != null ? "Hover to read" : "Not sent")};
                    try {
                        Boolean success = (Boolean) sendMethod.invoke(null, fields, text, logs);
                        if (success) {
                            Tonga.setStatus("Feedback" + (logs != null ? " with logs " : " ") + "was sent successfully");
                            Tonga.frame().closeDialog(this);
                        } else {
                            Tonga.catchError("The feedback request did not complete successfully. The message may not have been sent.");
                        }
                    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Tonga.catchError(ex, "The feedback could not be sent.");
                    }
                    progressBar.setIndeterminate(false);
                    enableControls(true);
                });
                thread.setName("Feedback");
                thread.start();
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                Tonga.catchError(ex, "The implementation of this feature is missing from this software package. The feedback could not be sent.");
            }
        }
    }

    private String getLogsAsString() {
        String logs = null;
        try {
            if (logBox.isSelected()) {
                Tonga.frame().logSysInfo();
                logs = new String(Files.readAllBytes(Paths.get(Tonga.getAppDataPath() + "tonga.log")));
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "The log file can not be accessed. The logs were not sent.");
        }
        return logs;
    }

    private String getTime() {
        LocalDateTime d = LocalDateTime.now();
        return d.getDayOfMonth() + "." + d.getMonthValue() + "." + d.getYear() + "::" + d.getHour() + "." + d.getMinute() + "." + d.getSecond();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel4 = new javax.swing.JLabel();
        goForItButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        progressBar = new javax.swing.JProgressBar();
        nahButton = new javax.swing.JButton();
        logBox = new javax.swing.JCheckBox();

        setTitle("Feedback");
        setLocation(new java.awt.Point(0, 0));
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        setResizable(false);
        setType(java.awt.Window.Type.POPUP);

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jLabel4.setText("Send feedback to the authors");

        goForItButton.setText("Send!");
        goForItButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goForItButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("<html><div>Did you encounter a bug or some other issue? Or do you have some other feedback or suggestions? Contact the authors by writing a message below. This will be sent to the authors anonymously.</div><br><div>If desribing a bug, try to be exact on what you were doing before the bug occured. You can also send us the log file to help us understand what went wrong. No personal information is included, with the exception of file/path names.</div></html>");
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        textArea.setColumns(20);
        textArea.setLineWrap(true);
        textArea.setRows(5);
        jScrollPane1.setViewportView(textArea);

        nahButton.setText("Cancel");
        nahButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nahButtonActionPerformed(evt);
            }
        });

        logBox.setText("Include the log file");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(goForItButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nahButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(logBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nahButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(goForItButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void goForItButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goForItButtonActionPerformed
        sendFeedback();
    }//GEN-LAST:event_goForItButtonActionPerformed

    private void nahButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nahButtonActionPerformed
        Tonga.frame().closeDialog(this);
    }//GEN-LAST:event_nahButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton goForItButton;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JCheckBox logBox;
    private javax.swing.JButton nahButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables

}
