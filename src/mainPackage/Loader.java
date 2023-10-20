package mainPackage;

import java.awt.Taskbar;
import java.awt.Taskbar.*;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author Victoria
 */
public class Loader {

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
    private boolean taskAborted;
    private boolean majorFailure;
    private boolean minorFailure;
    private boolean routineTask;
    private boolean taskbarSupport;
    private boolean dontAppend;

    public Loader() {
        fbar = Tonga.frame().progressBar;
        taskbarSupport = true;
        dontAppend = false;
        try {
            tbar = Taskbar.getTaskbar();
            resetProgress();
        } catch (NoClassDefFoundError | RuntimeException ex) {
            Tonga.log.info("The current JRE does not support the taskbar.");
            taskbarSupport = false;
        }
        Splash.append("Loader", 2);
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

    public void allocateLoader(Thread thread, String name, boolean intermediate, boolean routine) {
        loaderStart(intermediate);
        routineTask = routine;
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
                if (taskAborted) {
                    Tonga.setStatus("Operation aborted by user");
                    Tonga.log.info("{} was aborted by the user.", threadTask.getName());
                } else if (majorFailure) {
                    Tonga.setStatus("<font color=\"red\">" + threadTask.getName() + " crashed unexpectedly.</font> See the log for details (Tonga -> Logs from the menu bar).");
                    Tonga.log.info("{} crashed unexpectedly.", threadTask.getName());
                } else if (!routineTask) {
                    Tonga.setStatus("Completed " + threadTask.getName() + (minorFailure ? " <font color=\"red\">with errors</font> in " : " successfully in ") + (timeEnd - timeStart) / 10000000 / 100. + "s");
                    Tonga.log.info("{} was completed {} in {}s.", threadTask.getName(), minorFailure ? "with errors" : "successfully", (timeEnd - timeStart) / 10000000 / 100.);
                }
                resetStatus();
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

    public void resetProgress() throws RuntimeException {
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

    public void abort() {
        threadTask.interrupt();
        taskAborted = true;
    }

    public void majorFail() {
        majorFailure = true;
    }

    public void minorFail() {
        minorFailure = true;
    }

    public boolean hasFailed() {
        return majorFailure;
    }

    public boolean hasAborted() {
        return taskAborted;
    }

    private void resetStatus() {
        taskAborted = false;
        majorFailure = false;
        minorFailure = false;
    }

    private void loaderFinish() {
        timeEnd = System.nanoTime();
        SwingUtilities.invokeLater(() -> {
            Tonga.threadActionEnd();
            resetProgress();
        });
    }
}
