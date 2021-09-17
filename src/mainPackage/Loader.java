package mainPackage;

import java.awt.Taskbar;
import java.awt.Taskbar.*;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author Victoria
 */
public class Loader extends javax.swing.JFrame {

    Thread threadMaster;
    Thread threadTask;
    Thread[] threadArray;
    Object tbar;
    JProgressBar fbar;
    double stepsTotal;
    double stepsNow;
    long timeStart;
    long timeEnd;
    long syst;
    boolean abort;
    boolean fail;
    boolean taskbarSupport;
    boolean dontAppend;

    public Loader(JProgressBar pb) {
        initComponents();
        fbar = pb;
        taskbarSupport = true;
        dontAppend = false;
        try {
            tbar = Taskbar.getTaskbar();
            resetProgress();
        } catch (NoClassDefFoundError | RuntimeException ex) {
            Tonga.log.info("The current JRE does not support the taskbar.");
            taskbarSupport = false;
        }
    }

    public void setIterations(int i) {
        Tonga.log.debug("Iterations set to " + i);
        // note: only use this function inside a thread(!)
        stepsTotal = i;
        stepsNow = 0;
    }

    public Thread getTask() {
        return threadTask;
    }

    public double getProgress() {
        return stepsNow / stepsTotal;
    }

    public double getStepsNow() {
        return stepsNow;
    }

    public void allocateLoader(Thread thread, String name, boolean intermediate) {
        loaderStart(intermediate);
        threadTask = thread;
        threadTask.setName(name);
        threadTask.start();
        threadMaster = new Thread(() -> {
            try {
                Tonga.log.info("{} was started and allocated.", threadTask.getName());
                threadTask.join();
            } catch (InterruptedException ex) {
                Tonga.catchError(ex, "Loader thread interrupted. You should never see this.");
            } finally {
                loaderFinish();
                if (abort) {
                    Tonga.setStatus("Operation aborted by user");
                    Tonga.log.info("{} was aborted by the user.", threadTask.getName());
                    abort = false;
                } else if (fail) {
                    Tonga.setStatus("<font color=\"red\">" + threadTask.getName() + " crashed unexpectedly.</font> See the console for details.");
                    Tonga.log.info("{} crashed unexpectedly.", threadTask.getName());
                    fail = false;
                } else if (!threadTask.getName().equals("An unknown task")) {
                    Tonga.setStatus("Completed " + threadTask.getName() + " succesfully in " + (timeEnd - timeStart) / 10000000 / 100. + "s");
                    Tonga.log.info("{} was completed succesfully in {}s.", threadTask.getName(), (timeEnd - timeStart) / 10000000 / 100.);
                }
            }
        });
        threadMaster.setName("TaskWatcher");
        threadMaster.start();
    }

    private void updateProgress(double max, double done) {
        long sysc = System.nanoTime();
        if (sysc > syst + 1000000 || done + 0.01 > max) {
            int stage = (int) (1000.0 / max * done);
            if (stage > 7) {
                fbar.setIndeterminate(false);
            }
            //fbar.setString((stage / 10) + "%");
            fbar.setValue(stage);
            //pbar.setValue(stage);
            if (taskbarSupport) {
                ((Taskbar) tbar).setWindowProgressValue(Tonga.frame(), (int) (done / max * 100));
            }
            syst = sysc;
        }
    }

    public void loaderProgress(int progress, int max) {
        updateProgress(max, progress);
    }

    public void stopAppending() {
        dontAppend = true;
    }

    public void continueAppending() {
        dontAppend = false;
    }

    public void appendToNext() {
        if (dontAppend) {
            return;
        }
        if (Math.round(100 * stepsNow) / 100 == Math.round(stepsNow)) {
            stepsNow += 1;
        } else {
            stepsNow = Math.ceil(stepsNow);
        }
        updateProgress(stepsTotal, stepsNow);
    }

    public void appendProgress(double d) {
        if (dontAppend) {
            return;
        }
        stepsNow += d;
        updateProgress(stepsTotal, stepsNow);
    }

    public void appendProgress(int d) {
        if (dontAppend) {
            return;
        }
        stepsNow += 1. / d;
        updateProgress(stepsTotal, stepsNow);
    }

    private void resetProgress() throws RuntimeException {
        fbar.setIndeterminate(false);
        fbar.setValue(0);
        if (taskbarSupport) {
            ((Taskbar) tbar).setWindowProgressValue(Tonga.frame(), 0);
            ((Taskbar) tbar).setWindowProgressState(Tonga.frame(), State.OFF);
        }
    }

    private void loaderStart(boolean intermediate) {
        Tonga.threadActionStart();
        timeStart = System.nanoTime();
        stepsTotal = 1;
        stepsNow = 0;
        syst = System.nanoTime();
        fbar.setValue(0);
        fbar.setIndeterminate(true);
        if (taskbarSupport) {
            ((Taskbar) tbar).setWindowProgressState(Tonga.frame(), intermediate ? State.INDETERMINATE : State.NORMAL);
            ((Taskbar) tbar).setWindowProgressValue(Tonga.frame(), 0);
        }
    }

    public void maxProgress() {
        updateProgress(stepsTotal, stepsTotal);
    }

    public void setMultiThread(Thread[] threads) {
        threadArray = threads;
    }

    void abort() {
        threadTask.interrupt();
        abort = true;
    }

    private void loaderFinish() {
        Tonga.threadActionEnd();
        timeEnd = System.nanoTime();
        resetProgress();
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

        pbar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        pbar.setForeground(new java.awt.Color(0, 0, 0));
        pbar.setMaximum(1000);
        pbar.setIndeterminate(true);
        pbar.setString("LOADING");
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
