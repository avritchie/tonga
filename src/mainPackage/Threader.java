package mainPackage;

import java.util.ArrayList;
import java.util.List;

public abstract class Threader {

    public abstract void action(int imageId);

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

    public abstract class ImageExecutor implements Runnable {

        int index;

        public ImageExecutor(int id) {
            this.index = id;
        }

        @Override
        public abstract void run();
    }

    public static String getCaller(int id) {
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[id].getMethodName();
    }
}
