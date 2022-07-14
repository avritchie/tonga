package mainPackage;

import java.util.ArrayList;
import mainPackage.counters.TableData;

public class UndoRedo {

    static TongaStructure comparableStructure;
    static ArrayList<TongaAction> undoList;
    static ArrayList<TongaAction> redoList;

    static void start() {
        comparableStructure = new TongaStructure();
    }

    static void end() {
        redoList = null;
        undoList = TongaStructure.getActionList(comparableStructure);
        Tonga.frame().setUndoRedoMenu();
    }

    static void undoAction() {
        if (undoList == null) {
            if (redoList != null) {
                redo();
            }
        } else {
            undo();
        }
    }

    private static void undo() {
        if (undoList == null) {
            return;
        }
        TongaStructure is = new TongaStructure();
        TongaAction.executeActionList(undoList);
        redoList = TongaStructure.getActionList(is);
        undoList = null;
        Tonga.frame().setUndoRedoMenu();
    }

    private static void redo() {
        if (redoList == null) {
            return;
        }
        TongaStructure is = new TongaStructure();
        TongaAction.executeActionList(redoList);
        undoList = TongaStructure.getActionList(is);
        redoList = null;
        Tonga.frame().setUndoRedoMenu();
    }

    static void clear() {
        undoList = null;
        redoList = null;
        Tonga.frame().setUndoRedoMenu();
    }

    public static class TongaStructure {

        int size;
        int[] imgSel;
        int[] layerSel;
        ArrayList<TongaStructure> subList;
        ArrayList<String> nameList;
        ArrayList<Object> referenceList;
        TableData results;

        public TongaStructure() {
            size = Tonga.picList.size();
            imgSel = Tonga.getImageIndexes();
            layerSel = Tonga.getLayerIndexes();
            subList = new ArrayList<>();
            nameList = new ArrayList<>();
            referenceList = new ArrayList<>();
            results = TongaStructure.getResults();
            Tonga.picList.forEach((TongaImage ti) -> {
                nameList.add(ti.imageName);
                referenceList.add(ti);
                subList.add(new TongaStructure(ti));
            });
        }

        public TongaStructure(TongaImage ti) {
            size = ti.layerList.size();
            layerSel = ti.activeLayers;
            subList = null;
            nameList = new ArrayList<>();
            referenceList = new ArrayList<>();
            ti.layerList.forEach((TongaLayer tl) -> {
                nameList.add(tl.layerName);
                referenceList.add(tl);
            });
        }

        private static TableData getResults() {
            TableData td = TongaTable.td;
            if (td == null) {
                return null;
            } else {
                return td.copy();
            }
        }

        public static ArrayList<TongaAction> getActionList(TongaStructure cs) {
            ArrayList<TongaAction> al = new ArrayList<>();
            boolean rename = false;
            if (cs.size != Tonga.picList.size()) {
                if (cs.size > Tonga.picList.size()) {
                    for (int i = 0, j = 0; i < cs.size; i++) {
                        if (i - j >= Tonga.picList.size() || cs.referenceList.get(i) != Tonga.picList.get(i - j)) {
                            al.add(new TongaAction(Action.ADD, cs.referenceList.get(i), i));
                            j++;
                        }
                    }
                }
                if (cs.size < Tonga.picList.size()) {
                    for (int i = 0, j = 0; i < Tonga.picList.size(); i++) {
                        if (i - j >= cs.size || cs.referenceList.get(i - j) != Tonga.picList.get(i)) {
                            al.add(new TongaAction(Action.DELETE, Tonga.picList.get(i), i));
                            j++;
                        }
                    }
                }
            } else {
                for (int r = 0; r < cs.size; r++) {
                    if (!cs.nameList.get(r).equals(Tonga.picList.get(r).imageName)) {
                        al.add(new TongaAction(Action.RENAME, cs.nameList.get(r), r));
                        rename = true;
                    }
                }
            }
            if (!rename) {
                for (int r = 0; r < cs.size; r++) {
                    ArrayList<TongaLayer> cl = ((TongaImage) cs.referenceList.get(r)).layerList;
                    TongaStructure pl = cs.subList.get(r);
                    if (pl.size != cl.size()) {
                        if (pl.size > cl.size()) {
                            for (int i = 0, j = 0; i < pl.size; i++) {
                                if (i - j >= cl.size() || pl.referenceList.get(i) != cl.get(i - j)) {
                                    al.add(new TongaAction(Action.ADD, pl.referenceList.get(i), r, i));
                                    j++;
                                }
                            }
                        }
                        if (pl.size < cl.size()) {
                            for (int i = 0, j = 0; i < cl.size(); i++) {
                                if (i - j >= pl.referenceList.size() || pl.referenceList.get(i - j) != cl.get(i)) {
                                    al.add(new TongaAction(Action.DELETE, cl.get(i), r, i));
                                    j++;
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < pl.size; i++) {
                            if (!pl.nameList.get(i).equals(cl.get(i).layerName)) {
                                al.add(new TongaAction(Action.RENAME, pl.nameList.get(i), r, i));
                            }
                        }
                    }
                }
                for (int i = 0, j = 0; i < cs.size; i++) {
                    al.add(new TongaAction(Action.SELECT, cs.subList.get(j).layerSel, cs.imgSel.length > 0 ? j : -99, -1));
                    j++;
                }
                if (cs.results != TongaTable.td && (cs.results == null || !cs.results.equals(TongaTable.td))) {
                    al.add(new TongaAction(Action.TABLE, cs.results));
                }
            }
            al.add(new TongaAction(Action.SELECT, cs.imgSel, cs.imgSel.length > 0 ? -1 : -99, -1));
            return al.isEmpty() ? null : al;
        }

    }

    public enum Action {
        RENAME,
        ADD,
        DELETE,
        SELECT,
        TABLE
    }

    public static class TongaAction {

        int imageId;
        int position;
        Object container;
        Action type;

        public TongaAction(Action type, Object data) {
            this(type, data, -1, -1);
        }

        public TongaAction(Action type, Object image, int position) {
            this(type, image, -1, position);
        }

        public TongaAction(Action type, Object image, int imgid, int position) {
            this.type = type;
            this.container = image;
            this.imageId = imgid;
            this.position = position;
        }

        protected static void executeActionList(ArrayList<TongaAction> al) {
            //reverse order for deleting
            if (al.get(0).type == Action.DELETE) {
                for (int i = al.size() - 1; i >= 0; i--) {
                    processAction(al, i, false);
                }
            } else {
                for (int i = 0; i < al.size(); i++) {
                    processAction(al, i, false);
                }
            }
            //only set the positions after refreshing the lists
            //otherwise will point to a non-existent location
            Tonga.relistElements("Last action reverted");
            for (int i = 0; i < al.size(); i++) {
                processAction(al, i, true);
            }
            Tonga.refreshCanvases();
        }

        private static void processAction(ArrayList<TongaAction> al, int i, boolean processSelections) {
            TongaAction ac = al.get(i);
            // process selections separately
            if (processSelections && ac.type == Action.SELECT) {
                switch (ac.imageId) {
                    case -99:
                        // empty image, do nothing
                        break;
                    case -1:
                        Tonga.selectImage((int[]) ac.container);
                        break;
                    default:
                        if (Tonga.getImageIndex() == ac.imageId) {
                            Tonga.selectLayer((int[]) ac.container);
                        } else {
                            Tonga.getImage(ac.imageId).activeLayers = ((int[]) ac.container);
                        }
                        break;
                }
            } //process the rest
            else if (!processSelections) {
                if (ac.type == Action.TABLE) {
                    if (ac.container == null) {
                        TongaTable.clearData();
                    } else {
                        TongaTable.overwriteData((TableData) ac.container);
                    }
                } else if (ac.imageId == -1) {
                    switch (ac.type) {
                        case DELETE:
                            Tonga.picList.remove(ac.position);
                            break;
                        case ADD:
                            Tonga.picList.add(ac.position, (TongaImage) ac.container);
                            break;
                        case RENAME:
                            Tonga.getImage(ac.position).imageName = (String) ac.container;
                            break;
                    }
                } else {
                    switch (ac.type) {
                        case DELETE:
                            Tonga.getImage(ac.imageId).layerList.remove(ac.position);
                            break;
                        case ADD:
                            Tonga.getImage(ac.imageId).layerList.add(ac.position, (TongaLayer) ac.container);
                            break;
                        case RENAME:
                            Tonga.getImage(ac.imageId).layerList.get(ac.position).layerName = (String) ac.container;
                            break;
                    }
                }
            }
        }
    }
}
