package mainPackage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import mainPackage.CachedImage.CacheBuffer;
import mainPackage.UndoRedo.Action;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

public class Tonga {

    static TongaFrame mainFrame;
    public static Logger log;
    private static Logger standardLogger;
    private static Logger debugLogger;
    private static Logger traceLogger;
    private static boolean happyBoot;
    private static boolean sadBoot;
    static int[] currentLayer, previousLayer;
    static int screenWidth, screenHeight;
    static ArrayList<TongaImage> picList;
    static ArrayList<CacheBuffer> cachedData;
    static DefaultListModel imageListModel, layerListModel;
    static boolean taskIsRunning;
    static int iterationCounter;
    static int cpuThreads;
    static int javaFxVersion;
    static int javaREVersion;
    static OS currentOS;

    public static void main(String args[]) {
        boot(args);
    }

    protected static void boot(String[] args) {
        initValues();
        initErrors();
        initLogger();
        initLooks();
        mainFrame = new TongaFrame();
        versionInfo();
        StackImporter.boot();
        Settings.boot();
        Histogram.boot();
        TongaRender.boot();
        initListSelectors();
        initListeners();
        mainFrame.display();
        happyBoot = true;
        if (!sadBoot) {
            Tonga.log.info("Tonga: succesful start");
        }
        handleArguments(args);
    }

    public static TongaFrame frame() {
        return mainFrame;
    }

    public static Loader loader() {
        return mainFrame.loaderDialog;
    }

    public static void iteration() {
        iteration(1);
    }

    public static void iteration(int i) {
        iterationCounter += i;
    }

    private static void initValues() {
        happyBoot = false;
        sadBoot = false;
        picList = new ArrayList<>();
        cachedData = new ArrayList<>();
        currentOS = currentOS();
        cpuThreads = Runtime.getRuntime().availableProcessors();
        screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        taskIsRunning = false;
        iterationCounter = 0;
    }

    private static void initLooks() {
        try {
            for (UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("nimbusOrange", new Color(120, 120, 240));
                    break;
                }
            }
            Tonga.log.info("Looks initialized succesfully");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            catchError(ex, "GUI initialization failed.");
        }
    }

    private static void initErrors() {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            if (taskIsRunning) {
                loader().majorFail();
                catchError(e);
            } else {
                catchError(e, "Uncaught exception.");
            }
        });
    }

    private static void initLogger() {
        LoggerContext logger = (LoggerContext) LoggerFactory.getILoggerFactory();
        FileAppender fap = new FileAppender();
        fap.setContext(logger);
        fap.setName("oslog");
        fap.setFile(getAppDataPath() + "tonga.log");
        fap.setImmediateFlush(true);
        fap.setAppend(false);
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setContext(logger);
        ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%level] %msg%n");
        ple.start();
        fap.setEncoder(ple);
        fap.start();
        standardLogger = logger.getLogger("tonga.logger");
        standardLogger.addAppender(fap);
        debugLogger = logger.getLogger("tonga.logger.debug");
        debugLogger.addAppender(fap);
        traceLogger = logger.getLogger("tonga.logger.trace");
        traceLogger.addAppender(fap);
        log = standardLogger;
        Tonga.log.info("Logging initialized succesfully");
    }

    protected static void debugMode() {
        frame().menuDebug.setVisible(true);
        enableDebugLogging();
    }

    protected static void enableDebugLogging() {
        log = debugLogger;
        Tonga.log.debug("Debug mode enabled.");
    }

    protected static void enableDebugTracing() {
        log = traceLogger;
        Tonga.log.trace("Tracing mode enabled.");
    }

    private static void initListSelectors() {
        java.awt.Color selectColor = new java.awt.Color(57, 105, 138);
        DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                TongaImage image = (TongaImage) value;
                setText(image.imageName);
                if (isSelected) {
                    if (picList.indexOf(image) == getImageIndex()) {
                        setBackground(selectColor);
                    } else {
                        setBackground(selectColor.brighter());
                    }
                    setForeground(java.awt.Color.WHITE);
                } else {
                    setForeground(java.awt.Color.BLACK);
                    setBackground(java.awt.Color.WHITE);
                }
                return c;
            }
        };
        DefaultListCellRenderer layerListRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                TongaLayer layer = (TongaLayer) value;
                boolean st = getImage().stack;
                setText(layer.layerName);
                if (!st) {
                    if (isSelected) {
                        if (layer.isGhost) {
                            setBackground(selectColor.brighter());
                        } else {
                            setBackground(selectColor);
                        }
                        setForeground(java.awt.Color.WHITE);
                    } else {
                        setForeground(java.awt.Color.BLACK);
                        setBackground(java.awt.Color.WHITE);
                    }
                } else {
                    if (layer.isGhost) {
                        setBackground(selectColor.brighter());
                    } else {
                        setBackground(selectColor);
                    }
                    setForeground(java.awt.Color.WHITE);
                }
                return c;
            }
        };
        imageListModel = (DefaultListModel) mainFrame.imagesList.getModel();
        mainFrame.imagesList.setCellRenderer(listRenderer);
        layerListModel = (DefaultListModel) mainFrame.layersList.getModel();
        mainFrame.layersList.setCellRenderer(layerListRenderer);
        Tonga.log.info("Selectors initialized succesfully");
    }

    private static void initListeners() {
        JList imageJList = mainFrame.imagesList;
        JList layerJList = mainFrame.layersList;
        // remove default key listeners
        KeyListener[] list;
        list = imageJList.getKeyListeners();
        for (KeyListener kl : list) {
            imageJList.removeKeyListener(kl);
        }
        list = layerJList.getKeyListeners();
        for (KeyListener kl : list) {
            layerJList.removeKeyListener(kl);
        }
        // add d&d and others
        imageJList.setDropTarget(new DropTarget() {
            @Override
            public void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_LINK);
                    List<File> files = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    IO.importImages(files);
                    evt.dropComplete(true);
                } catch (UnsupportedFlavorException | IOException ex) {
                    catchError(ex, "Drag and drop failure.");
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                setStatus("Import new images with one layer each");
                dtde.acceptDrag(DnDConstants.ACTION_LINK);
            }
        });
        layerJList.setDropTarget(new DropTarget() {
            @Override
            public void drop(DropTargetDropEvent evt) {
                try {
                    if (imageListModel.size() != 0) {
                        evt.acceptDrop(DnDConstants.ACTION_LINK);
                        List<File> files = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (files.size() % imageListModel.size() == 0 && getImageIndexes().length == 1) {
                            IO.importLayers(files, true);
                            evt.dropComplete(true);
                        } else {
                            IO.importLayers(files, false);
                            evt.dropComplete(true);
                        }
                    } else {
                        evt.rejectDrop();
                        setStatus("Cannot import layers because there are no images. Please drag and drop into the main panel.");
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    catchError(ex, "Drag and drop failure.");
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (imageListModel.size() != 0) {
                    dtde.acceptDrag(DnDConstants.ACTION_LINK);
                    setStatus("Import new layer(s) to existing images");
                } else {
                    dtde.rejectDrag();
                    setStatus("Cannot import layers because there are no images. Please drag and drop into the main panel.");
                }
            }
        });
        imageJList.addListSelectionListener((ListSelectionEvent evt) -> {
            if (!evt.getValueIsAdjusting()) {
                frame().enableDisableControls(picList);
                if (getImageIndex() != -1) {
                    updateLayerList();
                    TongaImage img = picList.get(getImageIndex());
                    if (img.activeLayers[0] == -1) {
                        img.activeLayers = new int[]{0};
                    } else if (img.activeLayers[0] >= img.layerList.size()) {
                        img.activeLayers = new int[]{img.layerList.size() - 1};
                    }
                    updateRenders();
                    selectLayer(img.activeLayers);
                }
            }
        });
        imageJList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JList l = (JList) e.getSource();
                ListModel m = l.getModel();
                int i = l.locationToIndex(e.getPoint());
                if (i > -1) {
                    TongaImage ti = (TongaImage) (m.getElementAt(i));
                    setStatus(ti.imageName + "  |  " + ti.layerList.size() + " layers");
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
        layerJList.addListSelectionListener((ListSelectionEvent evt) -> {
            if (!evt.getValueIsAdjusting()) {
                int[] vals = layerJList.getSelectedIndices();
                if (vals.length > 0) {
                    picList.get(getImageIndex()).activeLayers = vals;
                    previousLayer = currentLayer;
                    currentLayer = getLayerIndexes();
                    if (!taskIsRunning) {
                        refreshCanvases();
                    }
                }
            }
        });
        layerJList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JList l = (JList) e.getSource();
                ListModel m = l.getModel();
                int i = l.locationToIndex(e.getPoint());
                if (i > -1) {
                    TongaLayer tl = (TongaLayer) (m.getElementAt(i));
                    if (tl.isPointer) {
                        setStatus(tl.layerName + "  |  " + tl.path);
                    } else {
                        setStatus(tl.layerName + "  |  " + tl.layerImage.bits + "-bit  |  " + tl.width + "x"
                                + tl.height + " px  |  " + String.format("%.2f", tl.layerImage.size / 1048576.).replace(",", ".") + " MB");
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
        layerJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (thereIsImage()) {
                    if (getImage().stack) {
                        if (Key.keyShift) {
                            getLayerList().forEach(l -> {
                                l.isGhost = !l.isGhost;
                            });
                            layerJList.repaint();
                            redraw();
                            IO.waitForJFXRunLater();
                            redraw();
                        } else if (e.getButton() == MouseEvent.BUTTON1) {
                            getLayer().isGhost = !getLayer().isGhost;
                            layerJList.repaint();
                            redraw();
                            IO.waitForJFXRunLater();
                            redraw();
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        setLayerSelectionToAllImages();
                    }
                }
            }
        });
        Platform.runLater(() -> {
            mainFrame.panelBig.getScene().setOnDragOver((DragEvent event) -> {
                event.acceptTransferModes(TransferMode.ANY);
                setStatus("Import new image with multiple layers");
            });
            mainFrame.panelBig.getScene().setOnDragDropped((DragEvent event) -> {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    Tonga.log.debug("Import event triggered");
                    IO.importImage(db.getFiles());
                } else {
                    Tonga.log.debug("The dragboard is empty.");
                }
            });
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent ke) -> {
            Key.event(ke);
            return false;
        });
        Tonga.log.info("Listeners initialized succesfully");
    }

    public static void selectImage() {
        selectImage(picList.size() - 1);
    }

    static void selectImage(int i) {
        mainFrame.imagesList.setSelectedIndex(i);
    }

    static void selectImage(int[] i) {
        mainFrame.imagesList.setSelectedIndices(i);
    }

    public static void selectLayer() {
        if (picList.isEmpty()) {
            return;
        }
        int imgSize = getLayerList(getImageIndex()).size();
        if (imgSize == 0) {
            selectImage();
        }
        selectLayer(imgSize - 1);
    }

    static void selectLayer(int i) {
        mainFrame.layersList.setSelectedIndex(i);
    }

    static void selectLayer(TongaImage i) {
        int[] t = new int[i.layerList.size()];
        for (int j = 0; j < t.length; j++) {
            t[j] = j;
        }
        mainFrame.layersList.setSelectedIndices(t);
    }

    static void selectLayer(int[] i) {
        int[] tempLayer = currentLayer;
        mainFrame.layersList.setSelectedIndices(i);
        previousLayer = tempLayer;
        currentLayer = i;
    }

    public static void injectNewLayer(TongaLayer layer) {
        injectNewLayer(layer, getImageIndex());
    }

    public static void injectNewLayer(CachedImage pic, String name) {
        injectNewLayer(pic, name, getImageIndex());
    }

    public static void injectNewLayer(String pointer, String name) {
        injectNewLayer(pointer, name, getImageIndex());
    }

    public static void injectNewLayer(File file, String name, int imageIndex) throws Exception {
        CachedImage img = IO.getImageFromFile(file);
        injectNewLayer(new TongaLayer(img, name), imageIndex);
    }

    public static void injectNewLayer(CachedImage pic, String name, int imageIndex) {
        injectNewLayer(new TongaLayer(pic, name), imageIndex);
    }

    public static void injectNewLayer(String pointer, String name, int imageIndex) {
        injectNewLayer(new TongaLayer(pointer, name), imageIndex);
    }

    public static void injectNewLayer(TongaLayer layer, int imageIndex) {
        getLayerList(imageIndex).add(layer);
        picList.get(imageIndex).activeLayers = new int[]{getLayerList(imageIndex).size() - 1};
    }

    public static ArrayList<TongaLayer> getLayerList() {
        return picList.isEmpty() ? null : picList.get(getImageIndex()).layerList;
    }

    public static ArrayList<TongaLayer> getLayerList(int i) {
        return picList.get(i).layerList;
    }

    public static ArrayList<TongaImage> getImageList() {
        return picList;
    }

    public static int getImageIndex() {
        return mainFrame.imagesList.getSelectedIndex();
    }

    public static TongaImage getImage() {
        return getImage(getImageIndex());
    }

    public static TongaImage getImage(int index) {
        return index == -1 || picList.isEmpty() ? null : picList.get(index);
    }

    public static int[] getImageIndexes() {
        return mainFrame.imagesList.getSelectedIndices();
    }

    public static int getLayerIndex() {
        return mainFrame.layersList.getSelectedIndex();
    }

    public static TongaLayer getLayer() {
        int i = getLayerIndex();
        return i == -1 ? null : getImage().layerList.get(i);
    }

    public static ArrayList<TongaLayer> getLayers() {
        ArrayList<TongaLayer> layers = new ArrayList<>();
        ArrayList<TongaLayer> img = getImage().layerList;
        int[] incs = getLayerIndexes();
        for (int i = 0; i < incs.length; i++) {
            layers.add(img.get(incs[i]));
        }
        return layers;
    }

    public static int[] getLayerIndexes() {
        return mainFrame.layersList.getSelectedIndices();
    }

    public static boolean thereIsImage() {
        return !(picList.isEmpty() || mainFrame.imagesList.getSelectedIndex() == -1);
    }

    public static void updateImageList() {
        imageListModel.clear();
        picList.forEach((pics) -> {
            imageListModel.addElement(pics);
        });
    }

    public static void updateLayerList() {
        layerListModel.clear();
        if (!picList.isEmpty()) {
            mainFrame.stackToggle.setSelected(getImage().stack);
            getLayerList().forEach((name) -> {
                layerListModel.addElement(name);
            });
        }
        if (mainFrame.currentProtocol != null) {
            mainFrame.currentProtocol.updateComponents();
        }
    }

    protected static void refreshChanges(File path, String string) {
        SwingUtilities.invokeLater(() -> {
            if (path != null) {
                mainFrame.updateSavePath(path);
            }
            refreshChanges(string);
        });
    }

    protected static void refreshChanges(String string) {
        int preList = imageListModel.size();
        refreshElements(string);
        if (preList != imageListModel.size()) {
            selectImage();
        }
        refreshCanvases();
    }

    public static void setLayerSelectionToAllImages() {
        int i = getLayerIndex();
        int c = 0;
        String s = picList.get(getImageIndex()).layerList.get(i).layerName;
        for (TongaImage t : picList) {
            if (!(t.layerList.size() < (i + 1))) {
                if (t.layerList.get(i).layerName.equals(s)) {
                    t.activeLayers = new int[]{i};
                    c++;
                }
            }
        }
        setStatus("Layer selected in " + c + "/" + totalImages() + " images");
    }

    public static void removeLayer(int[] i) {
        UndoRedo.start();
        TongaImage t = picList.get(getImageIndex());
        if (i.length == t.layerList.size()) {
            picList.remove(t);
            selectImage();
            refreshChanges("Removed the image");
        } else {
            Arrays.stream(i).boxed().sorted(Collections.reverseOrder()).mapToInt(x -> (int) x).forEach(n -> {
                t.layerList.remove(n);
                t.activeLayers = IntStream.of(t.activeLayers).filter(x -> x < t.layerList.size()).map(x -> x >= n ? x - 1 : x).distinct().toArray();
            });
            selectLayer();
            refreshChanges("Removed the layer(s)");
        }
        SwingUtilities.invokeLater(() -> {
            UndoRedo.end();
        });
    }

    public static void removeLayer() {
        if (getLayerIndexes().length > 0) {
            removeLayer(getLayerIndexes());
        }
    }

    public static void removeLayers() {
        UndoRedo.start();
        int[] i = getLayerIndexes();
        int c = 0;
        int m = picList.size();
        Iterator<TongaImage> picIter = picList.iterator();
        while (picIter.hasNext()) {
            TongaImage t = picIter.next();
            if (i[i.length - 1] < t.layerList.size()) {
                if (layerStructureMatches(getImageIndex(), picList.indexOf(t), i)) {
                    c++;
                    for (int j = i.length - 1; j >= 0; j--) {
                        t.layerList.remove(i[j]);
                    }
                    if (t.layerList.isEmpty()) {
                        picIter.remove();
                    } else {
                        t.activeLayers = new int[]{t.layerList.size() - 1};
                    }
                }
            }
        }
        refreshChanges("Removed the layer(s) from " + c + "/" + m + " images");
        UndoRedo.end();
    }

    public static void removeImage() {
        if (!picList.isEmpty()) {
            removeImage(getImageIndexes());
        }
    }

    public static void removeImage(int[] i) {
        UndoRedo.start();
        Arrays.stream(i).boxed().sorted(Collections.reverseOrder()).mapToInt(x -> (int) x).forEach(n -> {
            picList.remove(n);
        });
        if (!picList.isEmpty()) {
            selectImage();
        }
        refreshChanges("Removed the image(s)");
        UndoRedo.end();
    }

    private static int totalImages() {
        return picList.size();
    }

    public static boolean layerStructureMatches(int firstIndex, int secondIndex, int[] indexes) {
        TongaImage firstCompare = getImageList().get(firstIndex);
        TongaImage secondCompare = getImageList().get(secondIndex);
        TongaLayer firstLayer, secondLayer;
        try {
            for (int i = 0; i < indexes.length; i++) {
                firstLayer = firstCompare.layerList.get(i);
                secondLayer = secondCompare.layerList.get(i);
                if (!firstLayer.layerName.equals(secondLayer.layerName)) {
                    return false;
                }
            }
        } catch (Exception ex) {
            catchError(ex, "Comparison error.");
            return false;
        }
        return true;
    }

    public static void setStatus(String string) {
        SwingUtilities.invokeLater(() -> {
            mainFrame.updateMainLabel(string);
        });
    }

    public static void renameLayers() {
        String newName = (String) JOptionPane.showInputDialog(mainFrame, "New name:", "Rename", JOptionPane.QUESTION_MESSAGE, null, null, getLayer().layerName);
        if (newName != null) {
            UndoRedo.start();
            int[] layers = getLayerIndexes();
            int[] images = getImageIndexes();
            for (int i = 0; i < images.length; i++) {
                TongaImage im = getImageList().get(images[i]);
                for (int j = 0; j < layers.length; j++) {
                    im.layerList.get(layers[j]).layerName = newName;
                }
            }
            refreshLayerList();
        }
        //query().popup(getLayerIndexes(), getImageIndexes());
    }

    public static void renameImage() {
        String newName = (String) JOptionPane.showInputDialog(mainFrame, "New name:", "Rename", JOptionPane.QUESTION_MESSAGE, null, null, getImage().imageName);
        if (newName != null) {
            getImage().imageName = newName;
            refreshImageList();
        }
        UndoRedo.end();
        //query().popup(getImage());
    }

    public static boolean askYesNo(String title, String text, boolean doNotShowAgain, boolean defaultOption) {
        int hash = title.hashCode();
        boolean neverShow = Settings.getNeverShow(hash);
        text = "<html><body><p style='width: 300px;'>" + text + "</p><br></body></html>";
        if (neverShow == true) {
            return defaultOption;
        } else {
            JCheckBox cb = new JCheckBox("Never ask this again");
            Object p;
            if (doNotShowAgain) {
                p = new Object[]{text, cb};
            } else {
                p = new Object[]{text};
            }
            Object[] butt = {"Yes", "No"};
            int r = JOptionPane.showOptionDialog(mainFrame, p, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, butt, butt[defaultOption ? 0 : 1]);
            boolean selection = r == 0;
            if (cb.isSelected()) {
                Tonga.log.info("Disabled the \"{}\" question.", title);
                Settings.setNeverShow(hash, selection);
            }
            return selection;
        }
    }

    public static String getAppDataPath() {
        switch (currentOS) {
            case WINDOWS:
                return System.getProperty("user.home") + "\\AppData\\Local\\Tonga\\";
            case MAC:
                return System.getProperty("user.home") + "/Library/Application Support/Tonga/";
            default:
                return System.getProperty("user.home") + "/Tonga/";
        }
    }

    public static String getTempPath() {
        String path = Tonga.formatPath(System.getProperty("java.io.tmpdir") + "Tonga\\");
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return path;
    }

    protected static void bootThread(Thread thread, boolean intermediate, boolean routine) {
        bootThread(thread, "An unknown task", intermediate, routine);
    }

    protected static void bootThread(Thread thread, String name, boolean intermediate, boolean routine) {
        if (loader().threadTask == null || !loader().threadTask.isAlive()) {
            loader().allocateLoader(thread, name, intermediate, routine);
        } else {
            Tonga.log.warn("Boot thread {} despite previous task not finished.", name);
        }
    }

    static void switchLayer() {
        if (getLayerList() != null) {
            switchLayer(previousLayer);
        }
    }

    private static void switchLayer(int[] target) {
        if (target != null && ((getLayerList().size() - 1) >= target[0])) {
            selectLayer(target);
            redraw();
        }
    }

    static void ghostLayer() {
        if (getImage().stack) {
            return;
        }
        Arrays.stream(getLayerIndexes()).mapToObj(i -> getLayerList().get(i)).forEach(t -> {
            t.isGhost = !t.isGhost;
        });
        refreshLayerList();
        redraw();
    }

    public static boolean stackMode(boolean forAll) {
        boolean st;
        TongaImage img = getImage();
        if (picList.isEmpty() || img == null) {
            mainFrame.stackToggle.setSelected(false);
            return false;
        }
        img.stack = !img.stack;
        st = img.stack;
        if (st) {
            if (getLayerIndexes().length > 1) {
                for (int i = 0; i < getLayerList().size(); i++) {
                    getLayerList().get(i).isGhost = !mainFrame.layersList.isSelectedIndex(i);
                }
            }
        }
        if (!taskIsRunning) {
            if (st) {
                selectLayer(img);
            } else {
                selectLayer(getLayerList().size() - 1);
            }
            mainFrame.layersList.repaint();
            mainFrame.stackToggle.setSelected(st);
            refreshCanvases();
        }
        if (forAll) {
            getImageList().forEach(i -> {
                i.stack = st;
            });
        }
        return st;
    }

    private static void handleArguments(String[] args) {
        if (args.length > 0) {
            Tonga.log.debug("Launch arguments: {}", Arrays.toString(args));
            if (args[0].equals("-debug")) {
                debugMode();
            } else {
                IO.importImage(Arrays.stream(args).map(a -> new File(a)).collect(Collectors.toList()));
            }
        }
    }

    private static void redraw() {
        TongaRender.resetHash();
        TongaRender.redraw();
    }

    public static void publishLayerList() {
        SwingUtilities.invokeLater(() -> {
            if (loader().hasFailed()) {
                refreshLayerList();
                TongaRender.updateRenders();
            } else {
                updateLayerList();
                TongaRender.updateRenders();
                selectLayer();
            }
        });
    }

    public static void relistElements(String string) {
        //relist = only load the new elements
        //does not set positions, use only before setting them manually
        updateImageList();
        if (picList.isEmpty()) {
            updateLayerList();
        }
        setStatus(string);
    }

    public static void refreshElements(String string) {
        //refresh = load the new elements and select a layer/image
        //dont use if setting selections manually afterwards
        refreshImageList();
        if (picList.isEmpty()) {
            refreshLayerList();
        }
        setStatus(string);
    }

    public static void refreshCanvases() {
        Histogram.update();
        redraw();
    }

    public static void refreshLayerList() {
        int[] sels = getLayerIndexes();
        updateLayerList();
        selectLayer(sels);
    }

    public static void refreshImageList() {
        int[] preSel = getImageIndexes();
        updateImageList();
        selectImage(preSel);
    }

    static void threadActionStart() {
        taskIsRunning = true;
        iterationCounter = 0;
        mainFrame.launchersEnabled(false);
        UndoRedo.start();
    }

    static void threadActionEnd() {
        taskIsRunning = false;
        if (iterationCounter > 0) {
            Tonga.log.debug("Task finished with {} iterations", iterationCounter);
        }
        mainFrame.launchersEnabled(true);
        UndoRedo.end();
        freeMemory();
        refreshCanvases();
    }

    protected static void freeMemory() {
        freeUnsusedCache();
        Runtime.getRuntime().gc();
    }

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
                    p.layerList.forEach(i -> {
                        al.remove(i.layerImage.getBuffer().getFile());
                    });
                });
            }
            if (UndoRedo.redoList != null) {
                UndoRedo.redoList.forEach(r -> {
                    if (r.type == Action.ADD) {
                        if (r.container.getClass() == TongaImage.class) {
                            ((TongaImage) r.container).layerList.forEach(i -> {
                                al.remove(i.layerImage.getBuffer().getFile());
                            });
                        }
                        if (r.container.getClass() == TongaLayer.class) {
                            al.remove(((TongaLayer) r.container).layerImage.getBuffer().getFile());
                        }
                    }
                });
            }
            if (UndoRedo.undoList != null) {
                UndoRedo.undoList.forEach(r -> {
                    if (r.type == Action.ADD) {
                        if (r.container.getClass() == TongaImage.class) {
                            ((TongaImage) r.container).layerList.forEach(i -> {
                                al.remove(i.layerImage.getBuffer().getFile());
                            });
                        }
                        if (r.container.getClass() == TongaLayer.class) {
                            al.remove(((TongaLayer) r.container).layerImage.getBuffer().getFile());
                        }
                    }
                });
            }
            //remove everything cached that was not removed
            ArrayList<CacheBuffer> remainingCache = new ArrayList<>(cachedData);
            remainingCache.forEach(f -> {
                if (al.contains(f.getFile())) {
                    al.remove(f.getFile());
                    f.freeCache();
                }
            });
            //remove everything else
            al.forEach(f -> {
                f.delete();
            });
            Runtime.getRuntime().gc();
            Tonga.log.debug("Cache cleaned");
        });
        remover.setName("CacheCleaner");
        remover.start();
    }

    private static void versionInfo() {
        //only do this after the jfxpanel has been initialized in TongaFrame, otherwise won't work
        String jfxvs = System.getProperty("javafx.version");
        javaFxVersion = Integer.parseInt(jfxvs.contains(".") ? jfxvs.substring(0, jfxvs.indexOf(".")) : jfxvs);
        if (javaFxVersion < 12) {
            Tonga.log.info("Please update the JavaFX to version 12 or higher. You now have " + javaFxVersion + " (" + jfxvs + ")");
        }
        String jrevs = System.getProperty("java.version");
        javaREVersion = Integer.parseInt(jrevs.contains(".") ? jrevs.substring(0, jrevs.indexOf(".")) : jrevs);
        javaREVersion = javaREVersion == 1 ? Integer.parseInt(jrevs.substring(jrevs.indexOf(".") + 1, jrevs.indexOf(".") + 2)) : javaREVersion;
        if (javaREVersion < 9) {
            Tonga.log.info("Please update Java to at least version 9. You now have " + javaREVersion + " (" + jrevs + ")");
        }
    }

    public static String formatPath(String string) {
        if (currentOS == OS.MAC) {
            return string.replace("\\", "/");
        } else {
            return string;
        }
    }

    static int fullLayerIndexCount() {
        if (getImage().stack) {
            return (int) getLayerList().stream().filter(tl -> !tl.isGhost).count();
        } else {
            return getLayerIndexes().length;
        }
    }

    public enum OS {
        WINDOWS, MAC, UNKNOWN;
    }

    protected static OS currentOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OS.WINDOWS;
        } else if (os.contains("mac")) {
            return OS.MAC;
        } else {
            return OS.UNKNOWN;
        }
    }

    public static int getCpuThreads() {
        return cpuThreads;
    }

    private static void updateRenders() {
        TongaRender.updateRenders(getImage());
    }

    protected static void cleanAndShutDown() {
        if (mainFrame.resultTable.getModel().getRowCount() > 0 && (mainFrame.resultHash == null
                || mainFrame.resultTable.getModel().hashCode() != mainFrame.resultHash)
                && !askYesNo("Save before exit", "You have unsaved results in the results table. Do you want to exit without saving them?", true, true)) {
            return;
        }
        mainFrame.setVisible(false);
        boolean fail = false;
        try {
            try {
                ArrayList<CacheBuffer> remainingCache = new ArrayList<>(cachedData);
                remainingCache.forEach(f -> {
                    f.freeCache();
                });
                File dir = new File(getTempPath());
                for (File file : dir.listFiles()) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            } catch (Exception ex) {
                catchError(ex, "Cache freeing failed.");
                throw ex;
            }
            try {
                Settings.saveConfigFiles();
            } catch (IOException ex) {
                throw ex;
            }
        } catch (Exception ex) {
            fail = true;
        }
        Tonga.log.info(fail ? "Tonga: exit with errors" : "Tonga: succesful exit");
        System.exit(0);
    }

    public static void catchError(Throwable ex) {
        catchError(ex, null);
    }

    public static void catchError(Throwable ex, String msg) {
        if (msg != null) {
            Tonga.log.error(msg);
        }
        if (!happyBoot) {
            sadBoot = true;
        }
        if (taskIsRunning) {
            loader().minorFail();
        }
        //ex.printStackTrace(System.out);
        Tonga.log.error("Exception : " + ExceptionUtils.getStackTrace(ex));
        if (mainFrame == null || !mainFrame.isVisible()) {
            if (ex instanceof NoClassDefFoundError) {
                JOptionPane.showMessageDialog(null, "Tonga did not start correctly because an external class could not be found.\n"
                        + "Please make sure you are not trying to launch the JAR file directly.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Tonga did not " + (happyBoot ? "exit" : "start") + " correctly because an unexpected "
                        + ex.getClass().getSimpleName() + " occured.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        } else {
            setStatus("<font color=\"red\">Unexpected " + ex.getClass().getSimpleName() + " occured.</font> " + (msg != null ? msg + " " : "") + "See the log for details.");
        }
    }
}
