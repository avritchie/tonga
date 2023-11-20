package mainPackage;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JTextField;

public class Key {

    static boolean keyCtrl = false, keyShift = false, keyAlt = false;
    static Set<Integer> pressedKeys = new TreeSet<>();

    public static void event(KeyEvent ke) {
        Tonga.log.trace("KeyCode: {} ; KeyID: {}", ke.getKeyCode(), ke.getID());
        Component fo = Tonga.frame().getFocusOwner();
        if (!Tonga.frame().isEnabled() && ke.getKeyCode() != KeyEvent.VK_ESCAPE) {
        } else if (fo == null) {
            Tonga.frame().accelEnable(false);
        } else if (fo.getClass().equals(JTextField.class)) {
            Tonga.frame().accelEnable(false);
            if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                if (fo.getClass().equals(JTextField.class)) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
            }
        } else if (Tonga.frame().accelDisabled) {
            Tonga.frame().accelEnable(true);
        } else {
            handleStrokes(ke);
        }
        Cheats.checkCode(ke);
    }

    private static void handleStrokes(KeyEvent ke) {
        keyShift = ke.isShiftDown();
        keyCtrl = ke.isControlDown();
        keyAlt = ke.isAltDown();
        int keyCode = ke.getKeyCode();
        if (ke.getID() == KeyEvent.KEY_PRESSED) {
            if (!pressedKeys.contains(keyCode)) {
                pressedKeys.add(keyCode);
                if (!keyCtrl) {
                    switch (keyCode) {
                        case KeyEvent.VK_S:
                            Tonga.stackMode(keyShift);
                            break;
                    }
                    if (!keyShift) {
                        if (!keyAlt) {
                            switch (keyCode) {
                                case KeyEvent.VK_ESCAPE:
                                    /*if (Tonga.query().isVisible()) {
                                    Tonga.frame().closeDialog(Tonga.query());
                                    }*/
                                    if (TongaAnnotator.annotationPending()) {
                                        TongaAnnotator.cancelAnnotation();
                                    }
                                    if (Tonga.loader().threadTask != null
                                            && Tonga.loader().threadTask.isAlive()) {
                                        Tonga.log.info("Thread {} abortion request.", Tonga.loader().threadTask.getName());
                                        Tonga.loader().abort();
                                    }
                                    break;
                                case KeyEvent.VK_ENTER:
                                    if (TongaAnnotator.annotating()) {
                                        TongaAnnotator.finalizePendingAnnotation();
                                    }
                                    break;
                                case KeyEvent.VK_TAB:
                                    Tonga.switchLayer();
                                    break;
                                case KeyEvent.VK_G:
                                    Tonga.ghostLayer();
                                    break;
                                case KeyEvent.VK_D:
                                    debug();
                                    break;
                            }
                        }
                    }
                } else {
                    switch (keyCode) {
                        case KeyEvent.VK_F:
                            Tonga.frame().executeFilter(keyShift);
                            break;
                        /*case KeyEvent.VK_Z:
                            int[] prevLayer = Tonga.previousLayer;
                            Tonga.removeLayer();
                            Tonga.switchLayer(prevLayer);
                            break;*/
                    }
                }
                switch (keyCode) {
                    case KeyEvent.VK_ALT:
                        if (TongaAnnotator.annotationPending()) {
                            TongaRender.redraw();
                        }
                        break;
                }
            }
            if (keyAlt && !keyCtrl && !keyShift) {
                switch (keyCode) {
                    case KeyEvent.VK_UP:
                        moveEvent(true);
                        break;
                    case KeyEvent.VK_DOWN:
                        moveEvent(false);
                        break;
                }
            }
        }
        if (ke.getID() == KeyEvent.KEY_RELEASED) {
            pressedKeys.remove(keyCode);
            switch (keyCode) {
                case KeyEvent.VK_DELETE:
                    deleteEvent();
                    break;
                case KeyEvent.VK_F2:
                    renameEvent();
                    break;
                case KeyEvent.VK_TAB:
                    Tonga.switchLayer();
                    break;
                case KeyEvent.VK_ALT:
                    if (TongaAnnotator.annotationPending()) {
                        TongaRender.redraw();
                    }
                    break;
            }
        }
    }

    protected static void deleteEvent() {
        if (Tonga.frame().imagesList.hasFocus()) {
            Tonga.removeImage();
        } else if (Tonga.frame().layersList.hasFocus()) {
            if (keyShift) {
                Tonga.removeLayers();
            } else {
                Tonga.removeLayer();
            }
        } else if (Tonga.frame().resultTableComponent.hasFocus()) {
            UndoRedo.start();
            Tonga.frame().resultTable.removeSelectedRows();
            UndoRedo.end();
        } else if (Tonga.frame().annoTableComponent.hasFocus()) {
            UndoRedo.start();
            Tonga.frame().annotationTable.removeSelectedRows();
            UndoRedo.end();
        }
    }

    protected static void renameEvent() {
        if (Tonga.frame().layersList.hasFocus()) {
            Tonga.renameLayers();
        } else if (Tonga.frame().imagesList.hasFocus()) {
            Tonga.renameImage();
        }
    }

    protected static void moveEvent(boolean direction) {
        if (Tonga.frame().layersList.hasFocus()) {
            Tonga.moveOrder(false, direction);
        } else if (Tonga.frame().imagesList.hasFocus()) {
            Tonga.moveOrder(true, direction);
        }
    }

    private static void debug() {
        if (Tonga.debug()) {
            Tonga.log.debug("Debug function executed");
            Tonga.refreshCanvases();
        }
    }

    private static class Cheats {

        static int[] code = {
            KeyEvent.VK_UP, KeyEvent.VK_UP,
            KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_B, KeyEvent.VK_A, KeyEvent.VK_ENTER};
        static int cc = 0;

        private static void checkCode(KeyEvent ke) {
            if (ke.getID() == KeyEvent.KEY_PRESSED) {
                if (ke.getKeyCode() == code[cc]) {
                    cc++;
                    if (cc == code.length) {
                        cc = 0;
                        Tonga.debugMode();
                    }
                } else {
                    cc = 0;
                }
            }
        }
    }
}
