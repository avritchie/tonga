package mainPackage;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JTextField;

public class Key {

    static boolean keyCtrl = false, keyShift = false, keyAlt = false;
    static Set<Integer> pressedKeys = new TreeSet<Integer>();

    public static void event(KeyEvent ke) {
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
        if (ke.getID() == KeyEvent.KEY_PRESSED) {
            if (!pressedKeys.contains(ke.getKeyCode())) {
                pressedKeys.add(ke.getKeyCode());
                if (!keyCtrl) {
                    switch (ke.getKeyCode()) {
                        case KeyEvent.VK_S:
                            Tonga.stackMode(keyShift);
                            break;
                    }
                    if (!keyShift) {
                        switch (ke.getKeyCode()) {
                            case KeyEvent.VK_ESCAPE:
                                /*if (Tonga.query().isVisible()) {
                                    Tonga.frame().closeDialog(Tonga.query());
                                }*/
                                if (Tonga.loader().threadTask != null
                                        && Tonga.loader().threadTask.isAlive()) {
                                    System.out.println("Thread aborted.");
                                    Tonga.loader().abort();
                                }
                                if (Tonga.loader().hasFocus()) {
                                    Tonga.loader().abort();
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
                } else if (keyCtrl) {
                    switch (ke.getKeyCode()) {
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
            }
        }
        if (ke.getID() == KeyEvent.KEY_RELEASED) {
            pressedKeys.remove(ke.getKeyCode());
            switch (ke.getKeyCode()) {
                case KeyEvent.VK_DELETE:
                    deleteEvent();
                    break;
                case KeyEvent.VK_F2:
                    renameEvent();
                    break;
                case KeyEvent.VK_TAB:
                    Tonga.switchLayer();
                    break;
            }
        }
    }

    protected static void deleteEvent() {
        if (Tonga.frame().imagesList.hasFocus()) {
            Tonga.removeImage();
        } else if (Tonga.frame().layersList.hasFocus()) {
            Tonga.removeLayer();
        }
    }

    protected static void renameEvent() {
        if (Tonga.frame().layersList.hasFocus()) {
            Tonga.renameLayers();
        } else if (Tonga.frame().imagesList.hasFocus()) {
            Tonga.renameImage();
        }
    }

    private static void debug() {
        System.out.println("Debug");
        Tonga.refreshCanvases();
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
                        Tonga.frame().menuDebug.setVisible(true);
                    }
                } else {
                    cc = 0;
                }
            }
        }
    }
}
