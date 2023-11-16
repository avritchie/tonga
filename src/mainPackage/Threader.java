package mainPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Threader {

    public abstract void action(int imageId);

    public abstract boolean evaluate(int i, int j);

    public void runThreaded(List<Integer> procImgs, String name) {
        int threads = Math.min(procImgs.size(), Tonga.getCpuThreads() / 2 + 1);
        Thread[] coreThreads = new Thread[threads];
        List<Integer>[] ctInd = new List[threads];
        for (int i = 0; i < procImgs.size(); i++) {
            int ctI = (int) ((threads / (double) procImgs.size()) * i);
            if (ctInd[ctI] == null) {
                ctInd[ctI] = new ArrayList<>();
            }
            ctInd[ctI].add(procImgs.get(i));
        }
        for (int i = 0; i < threads; i++) {
            coreThreads[i] = new Thread((new ImageExecutor(i) {
                @Override
                public void run() {
                    ctInd[index].forEach(imageId -> {
                        action(imageId);
                    });
                }
            }));
            coreThreads[i].setName("Core" + i + name);
            coreThreads[i].start();
        }
        for (int i = 0; i < threads; i++) {
            try {
                coreThreads[i].join();
            } catch (InterruptedException ex) {
                Tonga.log.info("Multithreaded task abortion request for the task {}.", i, name);
                for (int t = 0; t < threads; t++) {
                    if (coreThreads[t].isAlive()) {
                        coreThreads[t].interrupt();
                        try {
                            coreThreads[t].join();
                        } catch (InterruptedException ex1) {
                            Tonga.catchError(ex, "Waiter thread interrupted.");
                        }
                        Tonga.log.debug("Abortion request sent to thread {}.", t);
                    } else {
                        Tonga.log.debug("Thread {} has already finished.", t);
                    }
                }
            }
        }
    }

    public void runMirax(int xloop, int yloop, byte[] md) {
        int threads = Math.min(xloop * yloop, Tonga.getCpuThreads() / 2 + 1);
        Semaphore loopSem = new Semaphore(threads);
        Tonga.log.info("Process the mirax file with {} threads.", threads);
        for (int xt = 0; xt < xloop; xt++) {
            for (int yt = 0; yt < yloop; yt++) {
                if (evaluate(xt, yt)) {
                    try {
                        loopSem.acquire();
                        int id = loopSem.availablePermits();
                        Thread mthread = new Thread((new MiraxExecutor(xt, yt, md) {
                            @Override
                            public void run() {
                                action(tiley * xloop + tilex);
                                loopSem.release();
                            }
                        }));
                        mthread.setName("MiraxCore" + (threads - id - 1));
                        mthread.start();
                    } catch (InterruptedException ex) {
                        Tonga.log.info("Mirax task abortion request.");
                    }
                }
            }
        }
        try {
            loopSem.acquire(threads);
        } catch (InterruptedException ex) {
            Tonga.log.info("Mirax task abortion request.");
        }
    }

    public abstract class ImageExecutor implements Runnable {

        int index;

        public ImageExecutor(int id) {
            this.index = id;
        }

        @Override
        public abstract void run();
    }

    public static abstract class MiraxExecutor implements Runnable {

        int tilex, tiley;
        byte[] data;

        public MiraxExecutor(int tx, int ty, byte[] mainData) {
            this.tilex = tx;
            this.tiley = ty;
            this.data = mainData;
        }

        @Override
        public abstract void run();
    }

    public static String getCaller(int id) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[id].getMethodName();
    }
}
