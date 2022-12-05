package mainPackage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

public class Tonga {

    static TongaFrame mainFrame;
    static Loader loader;
    public static Logger log;
    private static Logger standardLogger;
    private static Logger debugLogger;
    private static Logger traceLogger;
    private static boolean happyBoot;
    private static boolean sadBoot;
    static Color tongaBlue, tongaLBlue;
    static int[] currentLayer, previousLayer;
    static int screenWidth, screenHeight;
    static ArrayList<TongaImage> picList;
    static DefaultListModel imageListModel, layerListModel;
    static boolean taskIsRunning;
    static int iterationCounter;
    static int cpuThreads;
    static int javaFxVersion;
    static int javaREVersion;
    static String tongaVersion;
    static OS currentOS;
    static UIDefaults specialFeels;

    public static void main(String args[]) {
        boot(args);
    }

    protected static void boot(String[] args) {
        initValues();
        initErrors();
        initLogger();
        initLooks();
        mainFrame = new TongaFrame();
        loader = new Loader();
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
            Tonga.log.info("Tonga: successful start");
        }
        handleArguments(args);
    }

    public static TongaFrame frame() {
        return mainFrame;
    }

    public static Loader loader() {
        return loader;
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
        tongaBlue = new Color(120, 120, 240);
        tongaLBlue = new Color(241, 241, 254);
        picList = new ArrayList<>();
        currentOS = currentOS();
        tongaVersion = readVersion();
        cpuThreads = Runtime.getRuntime().availableProcessors();
        screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        taskIsRunning = false;
        iterationCounter = 0;
    }

    private static void initLooks() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            Object ogFocus = UIManager.get("nimbusFocus");
            UIManager.put("nimbusFocus", Color.RED);
            Object[] painters = new Object[6];
            painters[0] = UIManager.get("Button[Focused+MouseOver].backgroundPainter");
            painters[1] = UIManager.get("Button[Focused+Pressed].backgroundPainter");
            painters[2] = UIManager.get("Button[Focused].backgroundPainter");
            painters[3] = UIManager.get("Button[Default+Focused+MouseOver].backgroundPainter");
            painters[4] = UIManager.get("Button[Default+Focused+Pressed].backgroundPainter");
            painters[5] = UIManager.get("Button[Default+Focused].backgroundPainter");
            UIManager.getLookAndFeel().uninitialize();
            UIManager.put("nimbusFocus", ogFocus);
            specialFeels = new UIDefaults();
            specialFeels.put("Button[Focused+MouseOver].backgroundPainter", painters[0]);
            specialFeels.put("Button[Focused+Pressed].backgroundPainter", painters[1]);
            specialFeels.put("Button[Focused].backgroundPainter", painters[2]);
            specialFeels.put("Button[Default+Focused+MouseOver].backgroundPainter", painters[3]);
            specialFeels.put("Button[Default+Focused+Pressed].backgroundPainter", painters[4]);
            specialFeels.put("Button[Default+Focused].backgroundPainter", painters[5]);
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("nimbusOrange", tongaBlue);
            Tonga.log.info("Looks initialized successfully");
        } catch (UnsupportedLookAndFeelException ex) {
            catchError(ex, "GUI initialization failed.");
        }
    }

    private static void initErrors() {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            String msg = "Uncaught exception.";
            if (e instanceof VerifyError) {
                msg = "Build broken. Please recompile from source.";
            }
            if (taskIsRunning) {
                loader().majorFail();
                catchError(e, msg);
            } else {
                catchError(e, msg);
            }
        });
    }

    private static void initLogger() {
        try {
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
            Tonga.log.info("TONGA v{}", tongaVersion);
            Tonga.log.info("Logging initialized successfully");
        } catch (Exception ex) {
            catchError(ex, "Logging setup failed.");
        }
    }

    public static boolean debug() {
        return Tonga.log.isDebugEnabled();
    }

    protected static void debugMode() {
        if (Tonga.debug()) {
            frame().menuDebug.setVisible(false);
            disableDebugging();
        } else {
            frame().menuDebug.setVisible(true);
            enableDebugLogging();
        }
    }

    protected static void enableDebugLogging() {
        log = debugLogger;
        Tonga.log.debug("Debug mode enabled.");
    }

    protected static void enableDebugTracing() {
        log = traceLogger;
        Tonga.log.trace("Tracing mode enabled.");
    }

    protected static void disableDebugging() {
        log = standardLogger;
        Tonga.log.info("Debug mode disabled.");
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
        Tonga.log.info("Selectors initialized successfully");
    }

    private static void initListeners() {
        JList imageJList = mainFrame.imagesList;
        JList layerJList = mainFrame.layersList;
        JPanel dndPanel = mainFrame.actionPanel;
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
        new fileDragAndDrop() {
            @Override
            public void action(List<File> files) {
                IO.importImage(files);
            }
        }.initDnD(dndPanel, "Import a new image with multiple layers");
        new fileDragAndDrop() {
            @Override
            public void action(List<File> files) {
                IO.importImages(files);
            }
        }.initDnD(imageJList, "Import new images with one layer each");
        layerJList.setDropTarget(new DropTarget() {
            @Override
            public void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_LINK);
                    List<File> files = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (imageListModel.size() != 0) {
                        if (files.size() % imageListModel.size() == 0 && getImageIndexes().length == 1) {
                            IO.importLayers(files, true);
                            evt.dropComplete(true);
                        } else {
                            IO.importLayers(files, false);
                            evt.dropComplete(true);
                        }
                    } else {
                        IO.importMultichannel(files);
                        evt.dropComplete(true);
                        //evt.rejectDrop();
                        //setStatus("Cannot import layers because there are no images. Please drag and drop into the main panel.");
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
                    dtde.acceptDrag(DnDConstants.ACTION_LINK);
                    setStatus("Import multiple new images with multiple layers each.");
                    //dtde.rejectDrag();
                    //setStatus("Cannot import layers because there are no images. Please drag and drop into the main panel.");
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
                    TongaRender.copyFromCache();
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
                    setStatus(ti.description());
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
                    setStatus(tl.description());
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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((KeyEvent ke) -> {
            Key.event(ke);
            return false;
        });
        Tonga.log.info("Listeners initialized successfully");
    }

    static void moveOrder(boolean images, boolean direction) {
        // images true = imageList, false = layerList
        // direction true = up, false = down
        int[] is = images ? getImageIndexes() : getLayerIndexes();
        List collection = images ? picList : getImage().layerList;
        if ((direction && is[0] > 0) || (!direction && is[is.length - 1] < collection.size() - 1)) {
            int[] nis = new int[is.length];
            if (direction) {
                for (int i = 0; i < is.length; i++) {
                    Collections.swap(collection, is[i], is[i] - 1);
                    nis[i] = is[i] - 1;
                }
            } else {
                for (int i = is.length - 1; i >= 0; i--) {
                    Collections.swap(collection, is[i], is[i] + 1);
                    nis[i] = is[i] + 1;
                }
            }
            if (images) {
                selectImage(nis);
                refreshImageList();
            } else {
                selectLayer(nis);
                refreshLayerList();
            }
        } else {
            setStatus("Can't move the " + (direction ? ("first one up") : ("last one down")));
        }
    }

    public abstract static class fileDragAndDrop {

        public abstract void action(List<File> files);

        public void initDnD(Component comp, String message) {
            comp.setDropTarget(new DropTarget() {
                @Override
                public void drop(DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_LINK);
                        List<File> files = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        action(files);
                        evt.dropComplete(true);
                    } catch (UnsupportedFlavorException | IOException ex) {
                        catchError(ex, "Drag and drop failure.");
                    }
                }

                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    setStatus(message);
                    dtde.acceptDrag(DnDConstants.ACTION_LINK);
                }
            });
        }

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

    static void selectImagesAll() {
        mainFrame.imagesList.setSelectionInterval(0, mainFrame.imagesList.getModel().getSize() - 1);
    }

    static void selectLayersAll() {
        mainFrame.layersList.setSelectionInterval(0, mainFrame.layersList.getModel().getSize() - 1);
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

    public static void injectNewLayer(MappedImage pic, String name) {
        injectNewLayer(pic, name, getImageIndex());
    }

    public static void injectNewLayer(String pointer, String name) {
        injectNewLayer(pointer, name, getImageIndex());
    }

    public static void injectNewLayer(File file, String name, int imageIndex) throws Exception {
        MappedImage img = IO.getImageFromFile(file);
        injectNewLayer(new TongaLayer(img, name), imageIndex);
    }

    public static void injectNewLayer(MappedImage pic, String name, int imageIndex) {
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
        return !(picList.isEmpty() || mainFrame.imagesList.getSelectedIndex() == -1 || mainFrame.layersList.getSelectedIndex() == -1 || getImage() == null);
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
        int postList = imageListModel.size();
        if (preList != postList) {
            selectImage();
            if (postList == 0) {
                frame().clearSavePath();
            }
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

    public static TongaLayer[] imageLayersFromIndexList() {
        return getImagelayersFromIndexList(Tonga.getImageIndex(), Tonga.getLayerIndexes());
    }

    public static TongaLayer[] imageLayersFromIndexList(int imageIndex) {
        return getImagelayersFromIndexList(imageIndex, Tonga.getImage(imageIndex).activeLayers);
    }

    public static TongaLayer[] imageLayersFromIndexList(int imageIndex, int[] layerIndexes) {
        if (layerIndexBoundsViolation(imageIndex, layerIndexes)) {
            catchError("One or more of the images has less layers than what is currently selected, and thus can not be processed.");
        }
        return getImagelayersFromIndexList(imageIndex, layerIndexes);
    }

    private static TongaLayer[] getImagelayersFromIndexList(int imageIndex, int[] layerIndexes) {
        List<TongaLayer> allLayers = Tonga.getLayerList(imageIndex);
        TongaLayer[] selectedLayers = new TongaLayer[layerIndexes.length];
        for (int i = 0; i < layerIndexes.length; i++) {
            selectedLayers[i] = allLayers.get(layerIndexes[i]);
        }
        return selectedLayers;
    }

    public static ImageData[] layersAsImageDataArray(TongaLayer[] layers) {
        return Arrays.stream(layers).map(i -> new ImageData(i)).toArray(ImageData[]::new);
    }

    public static ImageData[] layersAs8BitImageDataArray(TongaLayer[] layers) {
        return Arrays.stream(layers).map(i -> new ImageData(i)).peek(img -> img.set8BitPixels()).toArray(ImageData[]::new);
    }

    public static String[] layersAsPointerArray(TongaLayer[] layers) {
        return Arrays.stream(layers).map(i -> i.path).toArray(String[]::new);
    }

    public static int[] imageAsSelectedLayerArray(TongaImage img) {
        //array of index numbers of the selected layers 
        if (img.stack) {
            return img.layerList.stream().filter(tl -> !tl.isGhost).mapToInt(tl -> img.layerList.indexOf(tl)).toArray();
        } else {
            return img.activeLayers;
        }
    }

    public static int[] imageAsBinarySelectedLayerArray(TongaImage img) {
        //array of 0s and 1s (not selected and selected, respectively)
        int[] prior = new int[img.layerList.size()];
        if (img.stack) {
            prior = img.layerList.stream().mapToInt(l -> l.isGhost ? 0 : 1).toArray();
        } else {
            for (int i = 0; i < img.activeLayers.length; i++) {
                prior[img.activeLayers[i]] = 1;
            }
        }
        return prior;
    }

    public static boolean layerStructureMatches(int firstIndex, int secondIndex, int[] indexes) {
        TongaImage firstCompare = getImageList().get(firstIndex);
        TongaImage secondCompare = getImageList().get(secondIndex);
        TongaLayer firstLayer, secondLayer;
        try {
            //unable to compare because the image to compare to does not have as many layers
            if (layerIndexBoundsViolation(secondIndex, indexes)) {
                return false;
            }
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

    private static boolean layerIndexBoundsViolation(int image, int[] indexes) {
        //unable to compare because the image to compare to does not have as many layers
        return indexes[indexes.length - 1] >= Tonga.getLayerList(image).size();
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
            UndoRedo.end();
        }
        //query().popup(getLayerIndexes(), getImageIndexes());
    }

    public static void renameImage() {
        String newName = (String) JOptionPane.showInputDialog(mainFrame, "New name:", "Rename", JOptionPane.QUESTION_MESSAGE, null, null, getImage().imageName);
        if (newName != null) {
            UndoRedo.start();
            getImage().imageName = newName;
            refreshImageList();
            UndoRedo.end();
        }
        //query().popup(getImage());
    }

    public static void setImageScaling() {
        Length exl = getImage().imageScaling;
        String inputText = "<html>Scaling and unit (per pixel):</html>";
        JTextField scaleNum = new JTextField();
        if (exl != null) {
            scaleNum.setText(getImage().imageScaling.value().toString());
        }
        scaleNum.setAlignmentX(Component.LEFT_ALIGNMENT);
        scaleNum.setPreferredSize(new Dimension(100, 30));
        scaleNum.requestFocusInWindow();
        scaleNum.selectAll();
        String[] units = {"nm", "µm", "mm", "cm"};
        JComboBox scaleNam = new JComboBox(units);
        scaleNam.setSelectedIndex(1);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.add(scaleNum);
        radioPanel.add(scaleNam);
        Object[] p = {inputText, radioPanel};
        Object[] butt = {"Change", "Cancel"};
        int r = JOptionPane.showOptionDialog(mainFrame, p, "Set scaling", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, butt, "Change");
        if (r == JOptionPane.OK_OPTION) {
            Length nlen = null;
            try {
                if (!scaleNum.getText().isEmpty()) {
                    Double scale = Double.parseDouble(scaleNum.getText().replace(",", "."));
                    Unit<Length> unit = null;
                    switch (scaleNam.getSelectedIndex()) {
                        case 0:
                            unit = UNITS.NANOMETER;
                            break;
                        case 1:
                            unit = UNITS.MICROMETER;
                            break;
                        case 2:
                            unit = UNITS.MILLIMETER;
                            break;
                        case 3:
                            unit = UNITS.CENTIMETER;
                            break;
                    }
                    nlen = new Length(scale, unit);
                }
                int[] images = getImageIndexes();
                for (int i = 0; i < images.length; i++) {
                    TongaImage im = getImageList().get(images[i]);
                    im.imageScaling = nlen;
                }
                if (nlen == null) {
                    Tonga.setStatus("Scaling removed");
                } else {
                    Tonga.setStatus("Scaling changed to " + nlen.value().doubleValue() + " " + nlen.unit().getSymbol() + " / px");
                }
            } catch (NumberFormatException ex) {
                Tonga.setStatus("Please input a valid (decimal) number");
            }
        }
    }

    private static boolean dialog(String title, String text, Object[] butts, boolean doNotShowAgain, boolean defaultOption) {
        int hash = title.hashCode();
        boolean neverShow = Settings.getNeverShow(hash);
        String neverText = "Never " + (butts.length == 1 ? "show" : "ask") + " this again";
        int dialogStyle = butts.length == 1 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.QUESTION_MESSAGE;
        text = "<html><body><p style='width: 300px;'>" + text + "</p><br></body></html>";
        if (neverShow == true) {
            return defaultOption;
        } else {
            JCheckBox cb = new JCheckBox(neverText);
            Object p;
            if (doNotShowAgain) {
                p = new Object[]{text, cb};
            } else {
                p = new Object[]{text};
            }
            int r = JOptionPane.showOptionDialog(mainFrame, p, title,
                    JOptionPane.DEFAULT_OPTION, dialogStyle,
                    null, butts, butts[defaultOption ? 0 : 1]);
            boolean selection = r == 0;
            if (cb.isSelected()) {
                Tonga.log.info("Disabled the \"{}\" question ({})", title, hash);
                Settings.setNeverShow(hash, selection);
            }
            return selection;
        }
    }

    public static boolean askYesNo(String title, String text, boolean doNotShowAgain, boolean defaultOption) {
        Object[] butt = {"Yes", "No"};
        return dialog(title, text, butt, doNotShowAgain, defaultOption);
    }

    public static void notify(String title, String text, boolean doNotShowAgain) {
        Object[] butt = {"Ok"};
        dialog(title, text, butt, doNotShowAgain, true);
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
            if (args[0].equals("-debug")) {
                debugMode();
            }
            Tonga.log.debug("Launch arguments: {}", Arrays.toString(args));
            List<File> fargs = Arrays.stream(args).filter(a -> !a.startsWith("-")).map(a -> new File(a)).collect(Collectors.toList());
            if (fargs.size() > 0) {
                IO.importImage(fargs);
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
                TongaRender.copyFromCache();
            } else {
                updateLayerList();
                TongaRender.copyFromCache();
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
        if (!Settings.settingHWAcceleration()) {
            TongaRender.setRenderImage();
        }
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
        MappingManager.freeMemory();
        refreshCanvases();
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
        if (string == null) {
            Tonga.log.warn("Attempted to set null as the output path");
            return "";
        } else {
            if (currentOS == OS.MAC) {
                return string.replace("\\", "/");
            } else {
                return string;
            }
        }
    }

    static int fullLayerIndexCount() {
        if (getImage().stack) {
            return (int) getLayerList().stream().filter(tl -> !tl.isGhost).count();
        } else {
            return getLayerIndexes().length;
        }
    }

    static int selectedLayerIndex() {
        if (getImage().stack) {
            return getLayerList().indexOf(getLayerList().stream().filter(tl -> !tl.isGhost).findFirst().get());
        } else {
            return getLayerIndex();
        }
    }

    private static String readVersion() {
        ResourceBundle rb = ResourceBundle.getBundle("version");
        String maj = rb.getString("majorversion");
        String min = rb.getString("minorversion");
        String pat = rb.getString("patchversion");
        String bui = rb.getString("buildnumber").replaceAll("\u00a0", "");
        return maj + "." + min + "." + pat + "." + bui;
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
                //remove everything cached
                MappingManager.unmapAll();
                //remove any other files
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
        Tonga.log.info(fail ? "Tonga: exit with errors" : "Tonga: successful exit");
        System.exit(0);
    }

    public static void catchError(String msg) {
        Tonga.log.error("Error: " + msg);
        setStatus("<font color=\"red\">" + msg + "</font>");
    }

    public static void catchError(Throwable ex) {
        catchError(ex, null);
    }

    public static void catchError(Throwable ex, String msg) {
        if (!happyBoot) {
            sadBoot = true;
        }
        if (taskIsRunning) {
            loader().minorFail();
            if (loader().hasAborted() && Thread.currentThread().isInterrupted()) {
                Tonga.log.debug("Ignored " + ex.getClass().getSimpleName() + " during protocol shutdown");
                return;
            }
        }
        if (msg != null) {
            Tonga.log.error(msg);
        }
        Tonga.log.error("Exception: " + ExceptionUtils.getStackTrace(ex));
        if (mainFrame == null || !mainFrame.isVisible()) {
            boolean fatal = false;
            String message;
            if (ex instanceof NoClassDefFoundError) {
                fatal = true;
                message = "Tonga did not start correctly because an external class could not be found.\n"
                        + "Please make sure you are not trying to launch the JAR file directly.";
            } else {
                message = "Tonga did not " + (happyBoot ? "exit" : "start") + " correctly because an unexpected "
                        + ex.getClass().getSimpleName() + " occured." + (msg == null ? "" : "\n" + msg) + ".\nSome functions may not work correctly.";
            }
            int r;
            Object[] butt = msg.contains("Logging") ? new String[]{"Exit"} : fatal ? new String[]{"Exit", "Details"} : new String[]{"OK", "Details"};
            frame().splashDialog.setVisible(false);
            r = JOptionPane.showOptionDialog(null, message, "Error",
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, butt, 0);
            if (r == 1) {
                IO.openLogs();
            }
            if (fatal) {
                System.exit(1);
            }
        } else {
            setStatus("<font color=\"red\">Unexpected " + ex.getClass().getSimpleName() + " occured.</font> " + (msg != null ? msg + " " : "") + "See the log for details (Tonga -> Logs from the menu bar).");
            if (ex instanceof OutOfMemoryError && !Settings.settingMemoryMapping()) {
                notify("Memory", "Tonga has ran out of RAM memory. "
                        + "The task you are trying to execute requires more memory than what your computer has available.<br><br>"
                        + "You can enable memory mapping from the settings to reduce RAM usage and prevent many occasions of this error.<br>"
                        + "This setting can be found from the <b>General</b> tab -> <b>memory mapping</b>", true);
            }
        }
    }
}
