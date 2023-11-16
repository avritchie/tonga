package mainPackage;

import java.io.File;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static mainPackage.Tonga.picList;

public class MappingManager {

    private static List<WeakReference<MappedBuffer>> mbs = new ArrayList<>();
    private static Cleaner wipe = Cleaner.create();

    public static void manage(MappedBuffer mb) {
        wipe.register(mb, new Unmapper(mb.getMapping()));
        WeakReference wr = new WeakReference(mb);
        mb.getMapping().refer(wr);
        mbs.add(wr);
        Tonga.log.trace("Registered {} for cleaning", mb.getMapping());
    }

    public static void trash() {
        Runtime.getRuntime().gc();
        Tonga.log.trace("Invoked garbage collection");
    }

    static void unmapAll() {
        mbs.forEach(r -> {
            MappedBuffer mb = r.get();
            if (mb != null) {
                mb.getMapping().unmap();
            }
        });
    }

    private static class Unmapper implements Runnable {

        private final Mapping mapping;

        private Unmapper(Mapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public void run() {
            mapping.unmap();
            mbs.remove(mapping.getRefer());
        }
    }

    protected static void freeMemory() {
        //no need to do this anymore
        //freeUnsusedCache();
        trash();
    }

    @Deprecated
    private static void freeUnsusedCache() {
        Thread remover = new Thread(() -> {
            //list all files in cache
            File[] fl = new File(Tonga.getTempPath()).listFiles();
            ArrayList<File> al = new ArrayList<>();
            al.addAll(Arrays.asList(fl));
            //remove directories
            Iterator<File> it = al.iterator();
            while (it.hasNext()) {
                if (it.next().isDirectory()) {
                    it.remove();
                }
            }
            //remove all files which are still in use
            if (!Settings.settingBatchProcessing()) {
                picList.forEach(p -> {
                    p.getLayerStream().forEach(i -> {
                        if (i.layerImage.isMapped()) {
                            al.remove(((MappedBuffer) i.layerImage.getBuffer()).getMapping().getFile());
                        }
                    });
                });
            }
            if (UndoRedo.redoList != null) {
                UndoRedo.redoList.forEach(r -> {
                    if (r.type == UndoRedo.Action.ADD) {
                        if (r.container.getClass() == TongaImage.class) {
                            ((TongaImage) r.container).getLayerStream().forEach(i -> {
                                if (i.layerImage.isMapped()) {
                                    al.remove(((MappedBuffer) i.layerImage.getBuffer()).getMapping().getFile());
                                }
                            });
                        }
                        if (r.container.getClass() == TongaLayer.class) {
                            if (((TongaLayer) r.container).layerImage.isMapped()) {
                                al.remove(((MappedBuffer) ((TongaLayer) r.container).layerImage.getBuffer()).getMapping().getFile());
                            }
                        }
                    }
                });
            }
            if (UndoRedo.undoList != null) {
                UndoRedo.undoList.forEach(r -> {
                    if (r.type == UndoRedo.Action.ADD) {
                        if (r.container.getClass() == TongaImage.class) {
                            ((TongaImage) r.container).getLayerStream().forEach(i -> {
                                if (i.layerImage.isMapped()) {
                                    al.remove(((MappedBuffer) i.layerImage.getBuffer()).getMapping().getFile());
                                }
                            });
                        }
                        if (r.container.getClass() == TongaLayer.class) {
                            if (((TongaLayer) r.container).layerImage.isMapped()) {
                                al.remove(((MappedBuffer) ((TongaLayer) r.container).layerImage.getBuffer()).getMapping().getFile());
                            }
                        }
                    }
                });
            }
            //remove everything cached that was not removed
            al.forEach(f -> {
                Tonga.log.warn("This file should not exist: {}", f.getName());
            });
            /*ArrayList<MappedBuffer> remainingCache = new ArrayList<>(mappedData);
            remainingCache.forEach(f -> {
                if (al.contains(f.getMapping().getFile())) {
                    al.remove(f.getMapping().getFile());
                    f.freeCache();
                }
            });*/
            trash();
            Tonga.log.debug("Cache cleaned");
        });
        remover.setName("CacheCleaner");
        remover.start();
    }
}
