package mainPackage;

import mainPackage.protocols.*;
import mainPackage.filters.*;
import mainPackage.counters.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.desktop.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.embed.swing.JFXPanel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.DefaultTableModel;
import mainPackage.JRangeSlider.RangeSliderUI;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelCreator.ControlType;
import mainPackage.Tonga.OS;
import mainPackage.TongaAnnotator.AnnotationType;
import mainPackage.utils.COL;

/**
 *
 * @author Victoria
 */
public class TongaFrame extends JFrame {

    /*
    --module-path "C:\Users\Victoria\AppData\Roaming\NetBeans\Libraries\javafx\lib" --add-modules "javafx.base,javafx.graphics,javafx.swing"
    /Volumes/ALEXANDRA/Scripts/Java/Projects/Tonga/legacy/Mac/lib/mac
     */
    ArrayList<Image> mainIcons;
    Protocol currentProtocol;
    Filter currentFilter;
    Counter currentCounter;
    JFXPanel panelBig, panelSmall;
    JLayeredPane panelBigLayer;
    JPanel actionPanel;
    JRangeSlider histoRange;
    public TongaResultTable resultTable;
    public TongaAnnotationTable annotationTable;
    public Map<String, Filter> filterHistory;
    boolean accelDisabled, historyAdjusting;
    Integer resultHash;
    static Splash splashDialog;
    static InfoDialog infoDialog;
    static Wizard wizardDialog;
    static SendForm feedbackDialog;
    static TableViewer tableWindow;

    public TongaFrame() {
        loadIconKit();
        initMacSupport();
        try {
            SwingUtilities.invokeAndWait(() -> {
                splashScreen();
            });
            SwingUtilities.invokeAndWait(() -> {
                initComponents();
                initExtraComponents();
                initFilterList();
                createDialogs();
                createPanels();
                constructFilterPopupMenu();
                constructToolTipSystem();
                constructMenuBar();
                createCloseHandler();
                adjustWindow();
            });
        } catch (InterruptedException ex) {
            Tonga.catchError(ex, "UI creation failed.");
        } catch (InvocationTargetException ex) {
            Tonga.catchError(ex.getCause(), "UI creation failed.");
        }
    }

    private void adjustWindow() {
        int[] winSize = getWindowSizeRecommendation();
        lSplitPane.setDividerLocation((int) ((winSize[0] - 600) * 0.7) + 200);
        setSize(winSize[0], winSize[1]);
        Splash.append("Window");
    }

    void display() {
        setLocationRelativeTo(null);
        setVisible(true);
        splashDialog.setVisible(false);
    }

    private void splashScreen() {
        splashDialog = new Splash();
        splashDialog.setLocationRelativeTo(null);
        splashDialog.setVisible(true);
    }

    private void createDialogs() {
        infoDialog = new InfoDialog();
        initializeDialog(infoDialog);
        Splash.append("Info dialog");
        wizardDialog = new Wizard();
        Splash.append("Wizard", 10);
        initializeDialog(wizardDialog);
        Splash.append("Wizard dialog");
        feedbackDialog = new SendForm();
        Splash.append("Form components", 5);
        initializeDialog(feedbackDialog);
        Splash.append("Form dialog");
    }

    private void initializeDialog(JFrame d) {
        d.setLocationRelativeTo(null);
        d.setIconImages(mainIcons);
        closeListener(d);
    }

    void launchersEnabled(boolean yesorno) {
        btnRunSingle.setEnabled(yesorno);
        btnRunAll.setEnabled(yesorno);
        btnRunSingle2.setEnabled(yesorno);
        btnRunAll2.setEnabled(yesorno);
        exportAsCSV.setEnabled(yesorno);
        openExcel.setEnabled(yesorno);
        histoAdjApplySingle.setEnabled(yesorno);
        histoAdjApplyAll.setEnabled(yesorno);
        histoAdjAutoSingle.setEnabled(yesorno);
        histoAdjAutoAll.setEnabled(yesorno);
        exportAnnot.setEnabled(yesorno);
        importAnnot.setEnabled(yesorno);
        menuFile.setEnabled(yesorno);
        menuFilters.setEnabled(yesorno);
        menuProtocols.setEnabled(yesorno);
        menuCounting.setEnabled(yesorno);
        menuDebug.setEnabled(yesorno);
    }

    private void loadIconKit() {
        ArrayList<Image> icons = new ArrayList<>();
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon1.png")));
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon2.png")));
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon3.png")));
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon4.png")));
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon5.png")));
        icons.add(Toolkit.getDefaultToolkit().createImage(
                getClass().getResource("/resourcePackage/icon6.png")));
        mainIcons = icons;
        Tonga.log.info("Icons initialized successfully");
    }

    protected void launchProtocol(Supplier<Protocol> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching protocols");
        } else {
            currentCounter = null;
            currentProtocol = method.get();
            currentProtocol.loadComponents();
            tabbedPane.setSelectedIndex(0);
            updatePanel(currentProtocol.panelCreator.getPanel(), protocolSettingsPanel);
            protocolName.setText(currentProtocol.getName());
        }
    }

    private void executeProtocol(boolean all) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching protocols");
        } else if (currentProtocol == null) {
            Tonga.setStatus("Select a protocol before running it");
        } else {
            currentProtocol.getParams();
            if (validateProtocolExecution()) {
                Thread thread = new Thread(() -> {
                    try {
                        currentProtocol.runProtocol(all);
                    } catch (Exception ex) {
                        Tonga.loader().majorFail();
                        Tonga.catchError(ex, "The protocol crashed.");
                    }
                });
                Tonga.bootThread(thread, protocolName.getText(), false, false);
            }
        }
    }

    private boolean validateProtocolExecution() {
        if (!currentProtocol.checkEqualSize()) {
            Tonga.setStatus("Please make sure all the selected layers are the same size.");
            return false;
        }
        for (PanelControl pc : currentProtocol.panelCreator.getControls()) {
            if (pc.type == ControlType.FOLDER) {
                JButton jb = (JButton) pc.comp;
                if (jb.getText().equals("Browse...")) {
                    jb.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
                    jb.putClientProperty("Nimbus.Overrides", Tonga.specialFeels);
                    jb.requestFocus();
                    Tonga.setStatus("The protocol can not be executed without a folder target.");
                    return false;
                }
            }
            if (pc.type == ControlType.ANNOTATION) {
                JComboBox jc = (JComboBox) pc.comp;
                if (jc.getSelectedIndex() == -1) {
                    jc.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
                    jc.putClientProperty("Nimbus.Overrides", Tonga.specialFeels);
                    jc.requestFocus();
                    Tonga.setStatus("The protocol can not be executed without an annotation.");
                    return false;
                }
            }
        }
        return true;
    }

    private void instantFilter(Supplier<Filter> method, boolean all, Object... parameters) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("No image to process");
        } else {
            Filter instaFilter = method.get();
            Thread thread = new Thread(() -> {
                instaFilter.param.setFilterParameters(instaFilter.parameterData, parameters);
                try {
                    Filter.publish(all ? instaFilter.runAll() : instaFilter.runSingle(), instaFilter.getName());
                } catch (Exception ex) {
                    Tonga.loader().majorFail();
                    Tonga.catchError(ex, "The filter crashed.");
                }
            });
            Tonga.bootThread(thread, instaFilter.getName(), false, true);
        }
    }

    private void launchFilter(Supplier<Filter> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching filters");
        } else {
            String filterName = ((JMenuItem) evt.getSource()).getText();
            currentFilter = method.get();
            currentFilter.loadComponents();
            updateHistory(filterName);
            tabbedPane.setSelectedIndex(1);
            updatePanel(currentFilter.panelCreator.getPanel(), filterSettingsPanel);
        }
    }

    protected void executeFilter(boolean all) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching filters");
        } else if (currentFilter == null) {
            Tonga.setStatus("Select a filter before running it");
        } else {
            String filterName = filterCombo.getSelectedItem().toString();
            updateHistory(filterName);
            currentFilter.loadParams();
            if (true) {
                Thread thread = new Thread(() -> {
                    try {
                        Filter.publish(all ? currentFilter.runAll() : currentFilter.runSingle(), currentFilter.getName());
                    } catch (Exception ex) {
                        Tonga.loader().majorFail();
                        Tonga.catchError(ex, "The filter crashed.");
                    }
                });
                Tonga.bootThread(thread, currentFilter.getName(), false, false);
            }
        }
    }

    private void executeHistoFilter(Supplier<Filter> method, boolean all) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before adjusting values");
        } else {
            if (true) {
                Filter filter = method.get();
                Thread thread = new Thread(() -> {
                    if (filter.parameterData == Filter.limits) {
                        filter.param.setFilterParameters(filter.parameterData,
                                histoRange.getValue(), histoRange.getUpperValue());
                    }
                    try {
                        Filter.publish(all ? filter.runAll() : filter.runSingle(), filter.getName());
                    } catch (Exception ex) {
                        Tonga.loader().majorFail();
                        Tonga.catchError(ex, "The scaler crashed.");
                    }
                });
                Tonga.bootThread(thread, filter.getName(), false, true);
            }
        }
    }

    protected void executeCounter(boolean all) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching counters");
        } else if (currentCounter == null) {
            Tonga.setStatus("Select a counter before running it");
        } else {
            currentCounter.loadParams();
            Thread thread = new Thread(() -> {
                try {
                    Counter.publish(all ? currentCounter.runAll() : currentCounter.runSingle());
                } catch (Exception ex) {
                    Tonga.loader().majorFail();
                    Tonga.catchError(ex, "The counter crashed.");
                }
            });
            Tonga.bootThread(thread, currentCounter.counterName, false, false);
        }
    }

    protected void launchCounter(Supplier<Counter> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            Tonga.setStatus("Open images before launching counters");
        } else {
            currentProtocol = null;
            currentCounter = method.get();
            currentCounter.loadComponents();
            tabbedPane.setSelectedIndex(0);
            updatePanel(currentCounter.panelCreator.getPanel(), protocolSettingsPanel);
            protocolName.setText(currentCounter.counterName);
        }
    }

    public void updatePanels() {
        if (currentProtocol != null) {
            PanelUtils.updateComponents(currentProtocol.panelCreator);
        }
        if (currentCounter != null) {
            PanelUtils.updateComponents(currentCounter.panelCreator);
        }
        if (currentFilter != null) {
            PanelUtils.updateComponents(currentFilter.panelCreator);
        }
    }

    public void updateSavePath(File file) {
        if (filePathField.getText().isEmpty()) {
            Tonga.log.debug("Set the output path from: {}", file.toString());
            File parent = file.getParentFile();
            if (parent != null && parent.isDirectory()) {
                String out = parent.getAbsolutePath();
                if (Settings.settingSubfolders()) {
                    out += "\\output";
                    String outo = out;
                    int ind = 0;
                    while (new File(Tonga.formatPath(out)).exists()) {
                        out = outo + ind;
                        ind++;
                    }
                }
                filePathField.setText(Tonga.formatPath(out));
            } else {
                Tonga.log.debug("The location is invalid: {}", parent.toString());
            }
        }
    }

    public void clearSavePath() {
        filePathField.setText("");
    }

    public void updateMainLabel(String str) {
        bleftLabel.setText("<html>" + str + "</html>");
    }

    public void updateZoomLabel(int xx, int yy, int brightness, String hex, double mainFactor, double zoomFactor) {
        brightLabel.setText(
                (!Tonga.thereIsImage() ? ("-.-") : (xx + "." + yy)) + " | "
                + (!Tonga.thereIsImage() ? ("- (-") : (brightness + " (" + hex)) + ") | "
                + Math.round(1000 * mainFactor) / 10 + "% | "
                + Math.round(1000 * zoomFactor) / 10 + "%");
    }

    public void enableDisableControls(ArrayList<TongaImage> picList) {
        boolean disabled = picList.isEmpty();
        boxSettingBatch.setEnabled(disabled);
    }

    static void sliderToolTip(MouseEvent evt) {
        JSlider slider = (JSlider) evt.getSource();
        PopupFactory popupFactory = PopupFactory.getSharedInstance();
        ToolTipManager.sharedInstance().setInitialDelay(0);
        JToolTip toolTip = slider.createToolTip();
        slider.setToolTipText(Integer.toString(slider.getValue()));
        Popup popup = popupFactory.getPopup(slider, toolTip, evt.getXOnScreen(), evt.getXOnScreen());
        popup.show();
    }

    private void updateHistory(String filterName) {
        historyAdjusting = true;
        filterHistory.put(filterName, currentFilter);
        filterCombo.removeAllItems();
        filterHistory.keySet().stream().collect(Collectors.toCollection(LinkedList::new))
                .descendingIterator().forEachRemaining(k -> {
                    filterCombo.addItem(k);
                });
        filterCombo.setSelectedIndex(0);
        historyAdjusting = false;
    }

    private void constructFilterPopupMenu() {
        for (Component c : menuFilters.getMenuComponents()) {
            if (c.getClass().equals(Separator.class)) {
                filterMenu.add(new JSeparator());
            } else {
                filterMenu.add(deepCloneMenu((JMenu) c));
            }
        }
        Splash.append("Popup", 2);
    }

    private void constructToolTipSystem() {
        ToolTipManager.sharedInstance().setEnabled(false);
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            deepToolTipMenu(menuBar.getMenu(i));
        }
        panelToolTips(generalPanel);
        panelToolTips(filterPanel);
        addToolTipListener(stackToggle);
        addToolTipListener(maxTabButton);
        addToolTipListener(annotColour);
        addToolTipListener(annotVisible);
        addToolTipListener(annotMove);
        addToolTipListener(annotErase);
        addToolTipListener(annotClearAll);
        addToolTipListenerSub(annotRadius, annotRadius.getComponents()[2].getComponentAt(0, 0));
        addToolTipListenerSub(annotGroup, annotGroup.getComponents()[2].getComponentAt(0, 0));
        addTableHeaderListener(resultTable);
        addTableHeaderListener(annotationTable);
    }

    private void initTables() {
        resultTable = new TongaResultTable(resultTableComponent, 3);
        annotationTable = new TongaAnnotationTable(annoTableComponent, 5);
    }

    private void initExtraComponents() {
        Splash.append("Frame packing", 15);
        histoRange = new JRangeSlider();
        histoRange.setMinimum(0);
        histoRange.setMaximum(255);
        histoRange.setValue(histoRange.getMinimum());
        histoRange.setUpperValue(histoRange.getMaximum());
        GroupLayout histoSliderPanelLayout = new GroupLayout(histoSliderPanel);
        histoSliderPanel.setLayout(histoSliderPanelLayout);
        histoSliderPanelLayout.setHorizontalGroup(histoSliderPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(histoRange, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        histoSliderPanelLayout.setVerticalGroup(histoSliderPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(histoRange, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
        histoRange.addChangeListener((ChangeEvent e) -> {
            Histogram.update();
        });
        PanelUtils.initPanelListeners(histoSliderPanel);
        menuDebug.setVisible(false);
        Splash.append("Extra component", 3);
    }

    private void initMacSupport() {
        if (Tonga.currentOS() == OS.MAC) {
            //System.setProperty("apple.laf.useScreenMenuBar", "true");
            Desktop d = Desktop.getDesktop();
            d.setQuitHandler((QuitEvent e, QuitResponse response) -> {
                Tonga.cleanAndShutDown();
            });
            d.setAboutHandler((AboutEvent e) -> {
                popupDialog(infoDialog);
            });
            try {
                Taskbar.getTaskbar().setIconImage(mainIcons.get(5));
            } catch (NoClassDefFoundError | RuntimeException ex) {
                Tonga.log.info("The current OS or JRE does not support the dock.");
            }
            Tonga.log.info("MacOS components initialized successfully");
        }
    }

    void logSysInfo() {
        Tonga.log.info("The JRE version is {} and the JFX version is {}", Tonga.javaREVersion, Tonga.javaFxVersion);
        Tonga.log.info("This CPU has {} threads available.", Runtime.getRuntime().availableProcessors());
        Tonga.log.info("The display has {}% resolution scaling.", TongaRender.getDisplayScaling() * 100);
        switch (Tonga.currentOS) {
            case WINDOWS:
                Tonga.log.info("The current operating system is Windows.");
                break;
            case MAC:
                Tonga.log.info("The current operating system is Mac OS.");
                break;
            case UNKNOWN:
                Tonga.log.info("Unknown operating system: " + System.getProperty("os.name"));
                break;
        }
        Tonga.log.info("The local storage path is {}", Tonga.getAppDataPath());
        Tonga.log.info("The cache storage path is {}", Tonga.getTempPath());
        long mem = Runtime.getRuntime().totalMemory();
        long memm = Runtime.getRuntime().maxMemory();
        Tonga.log.info("Max RAM memory available: {} MB, currently {} MB ({}%) used.", (memm / 1000000), (mem / 1000000), ((int) ((double) mem / memm * 100)));
    }

    int[] getWindowSizeRecommendation() {
        int pw = Math.max(1200, Math.min(getPreferredSize().width, Tonga.screenWidth - 200));
        int ph = Math.max(800, Math.min(getPreferredSize().height, Tonga.screenHeight - 100));
        return new int[]{pw, ph};
    }

    private void panelToolTips(JPanel panel) {
        for (Component c : panel.getComponents()) {
            String tt = ((JComponent) c).getToolTipText();
            if (tt != null && !tt.isEmpty()) {
                addToolTipListener(c);
            }
            if (c.getClass().equals(JPanel.class)) {
                panelToolTips((JPanel) c);
            }
        }
    }

    private void deepToolTipMenu(JMenu m) {
        for (Component c : m.getMenuComponents()) {
            Class cc = c.getClass();
            if (cc.equals(JMenuItem.class) || cc.equals(JMenu.class)) {
                String tt = ((JComponent) c).getToolTipText();
                if (tt != null && !tt.isEmpty()) {
                    addToolTipListener(c);
                }
                if (cc.equals(JMenu.class)) {
                    deepToolTipMenu((JMenu) c);
                }
            }
        }
    }

    private void addTableHeaderListener(TongaTable t) {
        t.getTableComponent().getTableHeader().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                t.hover(t.getTableComponent().columnAtPoint(evt.getPoint()));
            }
        });
    }

    private void addToolTipListener(Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                Tonga.setStatus(((JComponent) c).getToolTipText());
            }
        });
    }

    private void addToolTipListenerSub(Component c, Component subc) {
        subc.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                Tonga.setStatus(((JComponent) c).getToolTipText());
            }
        });
    }

    @Deprecated
    private void addColorListener(Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (c.getForeground().equals(Tonga.tongaBlue)) {
                    c.setForeground(Tonga.tongaLBlue);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                if (c.getForeground().equals(Tonga.tongaLBlue)) {
                    c.setForeground(Tonga.tongaBlue);
                }
            }
        });
    }

    private JMenu deepCloneMenu(JMenu m) {
        JMenu nm = new JMenu();
        String tt = m.getToolTipText();
        if (tt != null && !tt.isEmpty()) {
            nm.setToolTipText(tt);
            addToolTipListener(nm);
        }
        nm.setText(m.getText());
        for (Component sc : m.getMenuComponents()) {
            if (sc.getClass().equals(JMenu.class)) {
                nm.add(deepCloneMenu((JMenu) sc));
            } else if (sc.getClass().equals(Separator.class)) {
                nm.add(new JSeparator());
            } else {
                JMenuItem i = new JMenuItem();
                try {
                    i.setText(((JMenuItem) sc).getText());
                    // MAKE SURE ALL ENTRIES HAVE ACTION LISTENERS
                    i.addActionListener(((JMenuItem) sc).getActionListeners()[0]);
                    tt = ((JMenuItem) sc).getToolTipText();
                    if (tt != null && !tt.isEmpty()) {
                        i.setToolTipText(tt);
                        addToolTipListener(i);
                    }
                    nm.add(i);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Tonga.catchError(ex, "Failed to load menu actions for \"" + i.getText() + "\" because the action list was empty.\n"
                            + "Make sure that a listener has been assigned for this entry");
                }
            }
        }
        return nm;
    }

    private void getFilepath() {
        String fp = IO.getFolder(filePathField.getText());
        if (fp != null) {
            filePathField.setText(fp);
        }
    }

    private void refresh() {
        if (Tonga.frame() != null && Tonga.frame().isVisible()) {
            Tonga.refreshCanvases();
        }
    }

    private void constructMenuBar() {
        MenuListener accelListener = new MenuListener() {
            @Override
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }

            @Override
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }

            @Override
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                accelEnable(true);
                setUndoRedoMenu();
            }
        };
        int menus = menuBar.getMenuCount();
        for (int i = 0; i < menus; i++) {
            JMenu m = menuBar.getMenu(i);
            m.addMenuListener(accelListener);
        }
        menuWizard.addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }

            @Override
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
                menuWizard.setForeground(Tonga.tongaBlue);
            }

            @Override
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                menuWizard.setForeground(Tonga.tongaLBlue);
            }
        });
    }

    void setUndoRedoMenu() {
        menuFileUndo.setEnabled(UndoRedo.undoList != null || UndoRedo.redoList != null);
        menuFileUndo.setText(UndoRedo.redoList != null ? "Redo" : "Undo");
    }

    void accelEnable(boolean b) {
        int menus = menuBar.getMenuCount();
        for (int i = 0; i < menus; i++) {
            JMenu m = menuBar.getMenu(i);
            menuDisable(m, b);
        }
        accelDisabled = !b;
    }

    private void menuDisable(JMenu m, boolean b) {
        for (Component sc : m.getMenuComponents()) {
            if (sc.getClass().equals(JMenu.class)) {
                menuDisable((JMenu) sc, b);
            } else if (sc.getClass().equals(JMenuItem.class)) {
                ((JMenuItem) sc).setEnabled(b);
            }
        }
    }

    private void colorMenuSelect(java.awt.Color col) {
        if (col == UIManager.get("MenuItem.foreground")) {
            ((JButton) annotColour).setBackground(col);
            TongaAnnotator.setAnnotationColor(null);
        } else {
            ((JButton) annotColour).setBackground(col);
            TongaAnnotator.setAnnotationColor(COL.awt2FX(col));
        }
    }

    public void annotButtonToggle() {
        annotDot.setSelected(TongaAnnotator.annotating() && (TongaAnnotator.getAnnoType() == AnnotationType.DOT
                || TongaAnnotator.getAnnoType() == AnnotationType.RADIUS));
        annotLine.setSelected(TongaAnnotator.annotating() && (TongaAnnotator.getAnnoType() == AnnotationType.LINE
                || TongaAnnotator.getAnnoType() == AnnotationType.POLYLINE
                || TongaAnnotator.getAnnoType() == AnnotationType.PLANE));
        annotShape.setSelected(TongaAnnotator.annotating() && (TongaAnnotator.getAnnoType() == AnnotationType.RECTANGLE
                || TongaAnnotator.getAnnoType() == AnnotationType.CIRCLE
                || TongaAnnotator.getAnnoType() == AnnotationType.OVAL
                || TongaAnnotator.getAnnoType() == AnnotationType.POLYGON));
        annotMove.setSelected(!TongaAnnotator.annotating());
        annotErase.setSelected(TongaAnnotator.annotating() && TongaAnnotator.getAnnoType() == AnnotationType.ERASER);
    }

    protected boolean annotationTabOpen() {
        return Tonga.frame().tabbedPane.getSelectedIndex() == 5;
    }

    private void maxButtEnable() {
        maxTabButton.setEnabled(tabbedPane.getSelectedIndex() == 3 || tabbedPane.getSelectedIndex() == 5);
    }

    private JScrollPane returnActiveTableScrollPanel() {
        int tabIndex = tabbedPane.getSelectedIndex();
        if (tabIndex == 5) {
            return annoScrollPane;
        } else if (tabIndex == 3) {
            return resultScrollPane;
        }
        Tonga.log.warn("Requested an active table scroll panel but a table was not open.");
        return null;
    }

    private JTable returnActiveTable() {
        int tabIndex = tabbedPane.getSelectedIndex();
        if (tabIndex == 5) {
            return annoTableComponent;
        } else if (tabIndex == 3) {
            return resultTableComponent;
        }
        Tonga.log.warn("Requested an active table but a table was not open.");
        return null;
    }

    private void initFilterList() {
        currentProtocol = null;
        currentFilter = null;
        filterHistory = new LinkedHashMap<String, Filter>(10, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Filter> eldest) {
                return this.size() > 10;
            }
        };
    }

    public void closeDialog(Window dialog) {
        Tonga.frame().setEnabled(true);
        dialog.setVisible(false);
    }

    public void popupDialog(Window dialog) {
        dialog.setVisible(true);
        Tonga.frame().setEnabled(false);
    }

    private void closeListener(Window dialog) {
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Tonga.frame().closeDialog(dialog);
            }
        });
    }

    private void updatePanel(JPanel newPanel, JPanel targetPanel) {
        targetPanel.removeAll();
        targetPanel.add(newPanel);
        newPanel.setLocation(2, 2);
        newPanel.setSize(targetPanel.getWidth() - 4, targetPanel.getHeight() - 4);
        targetPanel.validate();
        targetPanel.repaint();
    }

    private void createCloseHandler() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                Tonga.cleanAndShutDown();
            }
        });
        Splash.append("Window handler");
    }

    private void createPanels() {
        // layering for main panel
        panelBigLayer = new JLayeredPane();
        panelBigLayer.setBounds(0, 0, imageBig.getWidth(), imageBig.getHeight());
        imageBig.setLayout(new BorderLayout());
        imageBig.add(panelBigLayer, BorderLayout.CENTER);
        // dnd overlay
        actionPanel = new JPanel();
        actionPanel.setBounds(0, 0, imageBig.getWidth(), imageBig.getHeight());
        actionPanel.setOpaque(false);
        // main panel
        panelBig = new JFXPanel();
        panelBigLayer.add(actionPanel, 1);
        panelBigLayer.add(panelBig, 2);
        panelBig.setSize(panelBigLayer.getSize());
        // zoom panel
        panelSmall = new JFXPanel();
        imageZoom.setLayout(new BorderLayout());
        imageZoom.add(panelSmall, BorderLayout.CENTER);
        panelSmall.setSize(imageZoom.getSize());
        Splash.append("Image panel", 28);
    }

    protected void resultHash() {
        resultHash = resultTableComponent.getModel().hashCode();
    }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contextLayerMenu = new javax.swing.JPopupMenu();
        contLayRename = new javax.swing.JMenuItem();
        contLaySelect = new javax.swing.JMenuItem();
        contLaySelectAll = new javax.swing.JMenuItem();
        contLayDeleteThis = new javax.swing.JMenuItem();
        contLayDeleteAll = new javax.swing.JMenuItem();
        contLayMoveUp = new javax.swing.JMenuItem();
        contLayMoveDown = new javax.swing.JMenuItem();
        contLaySep = new javax.swing.JPopupMenu.Separator();
        contLayMerge = new javax.swing.JMenuItem();
        contLayColor = new javax.swing.JMenu();
        contLayColorRed = new javax.swing.JMenuItem();
        contLayColorGreen = new javax.swing.JMenuItem();
        contLayColorBlue = new javax.swing.JMenuItem();
        contLayColorGray = new javax.swing.JMenuItem();
        contLayColorCustom = new javax.swing.JMenuItem();
        contLayExtract = new javax.swing.JMenu();
        contLayExtractRed = new javax.swing.JMenuItem();
        contLayExtractGreen = new javax.swing.JMenuItem();
        contLayExtractBlue = new javax.swing.JMenuItem();
        contLayExtractAlpha = new javax.swing.JMenuItem();
        contLayExtractAll = new javax.swing.JMenuItem();
        contextImageMenu = new javax.swing.JPopupMenu();
        contImgRename = new javax.swing.JMenuItem();
        contImgScale = new javax.swing.JMenuItem();
        contImgSelectAll = new javax.swing.JMenuItem();
        contImgMoveUp = new javax.swing.JMenuItem();
        contImgMoveDown = new javax.swing.JMenuItem();
        contImgDelete = new javax.swing.JMenuItem();
        contImgClear = new javax.swing.JMenuItem();
        contextTableMenu = new javax.swing.JPopupMenu();
        contResClear = new javax.swing.JMenuItem();
        contResDelRow = new javax.swing.JMenuItem();
        jSeparator24 = new javax.swing.JPopupMenu.Separator();
        contResTranspose = new javax.swing.JMenuItem();
        contResSeparator = new javax.swing.JPopupMenu.Separator();
        contResExport = new javax.swing.JMenuItem();
        contResExcel = new javax.swing.JMenuItem();
        contextAnnotationMenu = new javax.swing.JPopupMenu();
        contAnnoClear = new javax.swing.JMenuItem();
        contAnnoDelRow = new javax.swing.JMenuItem();
        contAnnoSeparator = new javax.swing.JPopupMenu.Separator();
        contAnnoExport = new javax.swing.JMenuItem();
        contAnnoExcel = new javax.swing.JMenuItem();
        filterMenu = new javax.swing.JPopupMenu();
        shapeMenu = new javax.swing.JPopupMenu();
        shapeRectangle = new javax.swing.JMenuItem();
        shapeCircle = new javax.swing.JMenuItem();
        shapeOval = new javax.swing.JMenuItem();
        shapePoly = new javax.swing.JMenuItem();
        lineMenu = new javax.swing.JPopupMenu();
        lineLine = new javax.swing.JMenuItem();
        linePolyline = new javax.swing.JMenuItem();
        lineThick = new javax.swing.JMenuItem();
        lineAngle = new javax.swing.JMenuItem();
        colorMenu = new javax.swing.JPopupMenu();
        colorDefault = new javax.swing.JMenuItem();
        colorRed = new javax.swing.JMenuItem();
        colorGreen = new javax.swing.JMenuItem();
        colorBlue = new javax.swing.JMenuItem();
        colorYellow = new javax.swing.JMenuItem();
        colorCyan = new javax.swing.JMenuItem();
        colorMagenta = new javax.swing.JMenuItem();
        colorPink = new javax.swing.JMenuItem();
        colorOrange = new javax.swing.JMenuItem();
        colorPurple = new javax.swing.JMenuItem();
        colorLime = new javax.swing.JMenuItem();
        dotMenu = new javax.swing.JPopupMenu();
        dotDot = new javax.swing.JMenuItem();
        dotRadius = new javax.swing.JMenuItem();
        Splash.append("Menu control",10);
        frame = new javax.swing.JPanel();
        Splash.append("Panel control");
        rSplitPane = new javax.swing.JSplitPane();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList<>();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        imagesList = new javax.swing.JList<>();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        layersList = new javax.swing.JList<>();
        jSeparator4 = new javax.swing.JSeparator();
        stackToggle = new javax.swing.JToggleButton();
        lSplitPane = new javax.swing.JSplitPane();
        imageBig = new javax.swing.JPanel();
        vSplitPane = new javax.swing.JSplitPane();
        imageZoom = new javax.swing.JPanel();
        layerPane = new javax.swing.JLayeredPane();
        floatingPane = new javax.swing.JPanel();
        Splash.append("Panel layout",5);
        maxTabButton = new javax.swing.JButton();
        tabbedPane = new javax.swing.JTabbedPane();
        protocolPanel = new javax.swing.JPanel();
        protocolHeadPanel = new javax.swing.JPanel();
        protocolNameSeparator = new javax.swing.JSeparator();
        protocolName = new javax.swing.JLabel();
        protocolSettingsPanel = new javax.swing.JPanel();
        protocolSettingsSeparator = new javax.swing.JSeparator();
        btnRunSingle = new javax.swing.JButton();
        btnRunAll = new javax.swing.JButton();
        filterPanel = new javax.swing.JPanel();
        filterSettingsPanel = new javax.swing.JPanel();
        btnRunSingle2 = new javax.swing.JButton();
        btnRunAll2 = new javax.swing.JButton();
        filterSettingsSeparator = new javax.swing.JSeparator();
        filterNameSeparator = new javax.swing.JSeparator();
        filterCombo = new javax.swing.JComboBox<>();
        browseFilter = new javax.swing.JButton();
        generalPanel = new javax.swing.JPanel();
        settingPanelLayer = new javax.swing.JPanel();
        boxSettingAlphaBG = new javax.swing.JCheckBox();
        layerBackColor = new javax.swing.JButton();
        stackCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        settingPanelGeneral = new javax.swing.JPanel();
        boxSettingMultiThreading = new javax.swing.JCheckBox();
        boxSettingMMapping = new javax.swing.JCheckBox();
        boxSettingHWRendering = new javax.swing.JCheckBox();
        boxSettingBatch = new javax.swing.JCheckBox();
        boxSettingResultsAppend = new javax.swing.JCheckBox();
        settingPanelScale = new javax.swing.JPanel();
        autoscaleLabel = new javax.swing.JLabel();
        autoscaleCombo = new javax.swing.JComboBox<>();
        boxSettingAutoscale1 = new javax.swing.JCheckBox();
        settingPanelFile = new javax.swing.JPanel();
        filePathField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        boxSettingOpenAfter = new javax.swing.JCheckBox();
        boxSettingSubfolder = new javax.swing.JCheckBox();
        resultsPanel = new javax.swing.JPanel();
        exportAsCSV = new javax.swing.JButton();
        openExcel = new javax.swing.JButton();
        resultScrollPane = new javax.swing.JScrollPane();
        resultTableComponent = new javax.swing.JTable();
        launchExcel = new javax.swing.JButton();
        exportTSV = new javax.swing.JButton();
        histogramPanel = new javax.swing.JPanel();
        histoImg = new javax.swing.JPanel();
        histoLabel = new javax.swing.JLabel();
        histoAdjAutoSingle = new javax.swing.JButton();
        histoAdjApplySingle = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JSeparator();
        histoAdjApplyAll = new javax.swing.JButton();
        histoAdjAutoAll = new javax.swing.JButton();
        histoSliderPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        exportAnnot = new javax.swing.JButton();
        importAnnot = new javax.swing.JButton();
        annoScrollPane = new javax.swing.JScrollPane();
        annoTableComponent = new javax.swing.JTable();
        annotClearAll = new javax.swing.JButton();
        jSeparator20 = new javax.swing.JSeparator();
        annotDot = new javax.swing.JToggleButton();
        annotLine = new javax.swing.JToggleButton();
        annotShape = new javax.swing.JToggleButton();
        jSeparator21 = new javax.swing.JSeparator();
        annotErase = new javax.swing.JToggleButton();
        annotMove = new javax.swing.JToggleButton();
        annotColour = new javax.swing.JButton();
        annotRadius = new javax.swing.JSpinner();
        annotVisible = new javax.swing.JToggleButton();
        annotGroup = new javax.swing.JSpinner();
        progressBar = new javax.swing.JProgressBar();
        jSeparator1 = new javax.swing.JSeparator();
        brightLabel = new javax.swing.JLabel();
        bleftLabel = new javax.swing.JLabel();
        Splash.append("Interactive control",10);
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuFileUndo = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        menuImport = new javax.swing.JMenu();
        menuFileImportImages = new javax.swing.JMenuItem();
        menuFileImportImage = new javax.swing.JMenuItem();
        menuFileImportLayers = new javax.swing.JMenuItem();
        menuFileImportLayersThis = new javax.swing.JMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        menuFileImportStacks = new javax.swing.JMenuItem();
        menuFileImportMultichannel = new javax.swing.JMenuItem();
        menuExport = new javax.swing.JMenu();
        menuExportSingle = new javax.swing.JMenu();
        menuExportSingleLayer = new javax.swing.JMenuItem();
        menuExportSingleImage = new javax.swing.JMenuItem();
        menuExportSingleStack = new javax.swing.JMenuItem();
        menuExportAll = new javax.swing.JMenu();
        menuExportAllLayers = new javax.swing.JMenuItem();
        menuExportAllImages = new javax.swing.JMenuItem();
        menuExportAllStacks = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        jMenuItem83 = new javax.swing.JMenuItem();
        jMenuItem88 = new javax.swing.JMenuItem();
        jMenuItem80 = new javax.swing.JMenuItem();
        menuAbout = new javax.swing.JMenuItem();
        menuTongaExit = new javax.swing.JMenuItem();
        menuWizard = new javax.swing.JMenu();
        menuFilters = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenu24 = new javax.swing.JMenu();
        menuMultiplyPxls = new javax.swing.JMenuItem();
        jMenuItem66 = new javax.swing.JMenuItem();
        jMenu26 = new javax.swing.JMenu();
        jMenuItem30 = new javax.swing.JMenuItem();
        jMenuItem32 = new javax.swing.JMenuItem();
        jMenuItem31 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        menuAlphaBright = new javax.swing.JMenuItem();
        jMenuItem57 = new javax.swing.JMenuItem();
        menuAlphaColor = new javax.swing.JMenuItem();
        menuOpaque = new javax.swing.JMenuItem();
        jMenu13 = new javax.swing.JMenu();
        menuMapLuminescence = new javax.swing.JMenuItem();
        menuGrayscale = new javax.swing.JMenuItem();
        menuLightness = new javax.swing.JMenuItem();
        jMenuItem45 = new javax.swing.JMenuItem();
        menuMapHue = new javax.swing.JMenuItem();
        jMenuItem23 = new javax.swing.JMenuItem();
        menuInterpolate = new javax.swing.JMenuItem();
        jMenuItem100 = new javax.swing.JMenuItem();
        jMenuItem110 = new javax.swing.JMenuItem();
        jMenuItem96 = new javax.swing.JMenuItem();
        jMenuItem20 = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        jMenuItem62 = new javax.swing.JMenuItem();
        menuProtocolCopyMask = new javax.swing.JMenuItem();
        jMenuItem35 = new javax.swing.JMenuItem();
        jMenuItem47 = new javax.swing.JMenuItem();
        jMenu27 = new javax.swing.JMenu();
        jMenu23 = new javax.swing.JMenu();
        menuLoCut = new javax.swing.JMenuItem();
        menuHiCut = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        menuLoCutAuto = new javax.swing.JMenuItem();
        menuHiCutAuto = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        menuLoCutNoise = new javax.swing.JMenuItem();
        menuHiCutPeak = new javax.swing.JMenuItem();
        jMenuItem34 = new javax.swing.JMenuItem();
        jMenuItem41 = new javax.swing.JMenuItem();
        jMenuItem60 = new javax.swing.JMenuItem();
        jMenuItem61 = new javax.swing.JMenuItem();
        jMenuItem92 = new javax.swing.JMenuItem();
        jMenuItem94 = new javax.swing.JMenuItem();
        jMenuItem16 = new javax.swing.JMenuItem();
        jMenu14 = new javax.swing.JMenu();
        jMenuItem64 = new javax.swing.JMenuItem();
        jMenuItem59 = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        menuBoxBlur = new javax.swing.JMenuItem();
        jMenuItem91 = new javax.swing.JMenuItem();
        jMenuItem49 = new javax.swing.JMenuItem();
        jMenu15 = new javax.swing.JMenu();
        jMenuItem29 = new javax.swing.JMenuItem();
        jMenuItem27 = new javax.swing.JMenuItem();
        jMenuItem26 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenuItem99 = new javax.swing.JMenuItem();
        jMenuItem105 = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        jMenuItem89 = new javax.swing.JMenuItem();
        jMenuItem51 = new javax.swing.JMenuItem();
        jMenuItem52 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        menuGamma = new javax.swing.JMenuItem();
        jMenuItem56 = new javax.swing.JMenuItem();
        jMenuItem87 = new javax.swing.JMenuItem();
        jMenuItem102 = new javax.swing.JMenuItem();
        jMenuItem108 = new javax.swing.JMenuItem();
        jMenuItem90 = new javax.swing.JMenuItem();
        jMenuItem97 = new javax.swing.JMenuItem();
        jMenuItem101 = new javax.swing.JMenuItem();
        jMenuItem70 = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        jMenuItem93 = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem106 = new javax.swing.JMenuItem();
        jMenuItem95 = new javax.swing.JMenuItem();
        jMenu11 = new javax.swing.JMenu();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem50 = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        menuDoG = new javax.swing.JMenuItem();
        menuDoB = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        jMenu10 = new javax.swing.JMenu();
        jMenu20 = new javax.swing.JMenu();
        jMenuItem53 = new javax.swing.JMenuItem();
        menuDotRemove = new javax.swing.JMenuItem();
        menuFillArea = new javax.swing.JMenuItem();
        jMenuItem43 = new javax.swing.JMenuItem();
        jMenuItem86 = new javax.swing.JMenuItem();
        jMenuItem40 = new javax.swing.JMenuItem();
        menuFilterSmallSize = new javax.swing.JMenuItem();
        menuFilterSmallDimension = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem67 = new javax.swing.JMenuItem();
        jMenuItem85 = new javax.swing.JMenuItem();
        jMenuItem77 = new javax.swing.JMenuItem();
        jMenu21 = new javax.swing.JMenu();
        menuSpreadEdge = new javax.swing.JMenuItem();
        menuShrinkEdge = new javax.swing.JMenuItem();
        menuSharpCorner = new javax.swing.JMenuItem();
        menuFillGap = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem24 = new javax.swing.JMenuItem();
        menuSmoothen = new javax.swing.JMenuItem();
        jMenuItem17 = new javax.swing.JMenuItem();
        jMenu19 = new javax.swing.JMenu();
        jMenuItem69 = new javax.swing.JMenuItem();
        jMenuItem25 = new javax.swing.JMenuItem();
        jMenuItem73 = new javax.swing.JMenuItem();
        jMenuItem58 = new javax.swing.JMenuItem();
        jMenuItem75 = new javax.swing.JMenuItem();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenuItem63 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItem84 = new javax.swing.JMenuItem();
        jMenu17 = new javax.swing.JMenu();
        jMenuItem46 = new javax.swing.JMenuItem();
        jMenuItem54 = new javax.swing.JMenuItem();
        menuProtocols = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem98 = new javax.swing.JMenuItem();
        jMenuItem107 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem18 = new javax.swing.JMenuItem();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenuItem21 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem71 = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        jMenu9 = new javax.swing.JMenu();
        jMenuItem19 = new javax.swing.JMenuItem();
        jMenuItem78 = new javax.swing.JMenuItem();
        jMenuItem33 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItem42 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem72 = new javax.swing.JMenuItem();
        jMenuItem74 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenu2 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        menuProtocolIFMask = new javax.swing.JMenuItem();
        jMenuItem48 = new javax.swing.JMenuItem();
        jMenuItem39 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem68 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem22 = new javax.swing.JMenuItem();
        jMenuItem111 = new javax.swing.JMenuItem();
        menuProtocolNucleusSep = new javax.swing.JMenuItem();
        jMenu18 = new javax.swing.JMenu();
        jMenuItem44 = new javax.swing.JMenuItem();
        jMenuItem37 = new javax.swing.JMenuItem();
        menuCounting = new javax.swing.JMenu();
        jMenu22 = new javax.swing.JMenu();
        menuCountRGB = new javax.swing.JMenuItem();
        jMenuItem104 = new javax.swing.JMenuItem();
        jMenuItem103 = new javax.swing.JMenuItem();
        jMenuItem65 = new javax.swing.JMenuItem();
        jMenuItem55 = new javax.swing.JMenuItem();
        jMenuItem28 = new javax.swing.JMenuItem();
        jMenuItem139 = new javax.swing.JMenuItem();
        jMenuItem137 = new javax.swing.JMenuItem();
        jMenuItem112 = new javax.swing.JMenuItem();
        jMenuItem138 = new javax.swing.JMenuItem();
        menuDebug = new javax.swing.JMenu();
        debugMemory = new javax.swing.JMenuItem();
        debugSysInfo = new javax.swing.JMenuItem();
        jMenuItem79 = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        jMenuItem6 = new javax.swing.JMenuItem();
        debugParameter = new javax.swing.JMenuItem();
        jMenuItem82 = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        debugExecuteClass = new javax.swing.JMenuItem();
        jMenuItem38 = new javax.swing.JMenuItem();
        debugTestFilter = new javax.swing.JMenuItem();
        debugTestProtocol = new javax.swing.JMenuItem();
        debugTestProtocols = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem36 = new javax.swing.JMenuItem();
        jMenuItem76 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenuItem81 = new javax.swing.JMenuItem();

        contLayRename.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        contLayRename.setText("Rename");
        contLayRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayRenameActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayRename);

        contLaySelect.setText("Select in all images");
        contLaySelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLaySelectActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLaySelect);

        contLaySelectAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        contLaySelectAll.setText("Select all");
        contLaySelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLaySelectAllActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLaySelectAll);

        contLayDeleteThis.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        contLayDeleteThis.setText("Delete the layer(s) in this image");
        contLayDeleteThis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayDeleteThisActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayDeleteThis);

        contLayDeleteAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        contLayDeleteAll.setText("Delete the layer(s) in all images");
        contLayDeleteAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayDeleteAllActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayDeleteAll);

        contLayMoveUp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_DOWN_MASK));
        contLayMoveUp.setText("Move up");
        contLayMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayMoveUpActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayMoveUp);

        contLayMoveDown.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_DOWN_MASK));
        contLayMoveDown.setText("Move down");
        contLayMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayMoveDownActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayMoveDown);
        contextLayerMenu.add(contLaySep);

        contLayMerge.setText("Merge into one");
        contLayMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayMergeActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayMerge);

        contLayColor.setText("Change colour");

        contLayColorRed.setText("Red");
        contLayColorRed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayColorRedActionPerformed(evt);
            }
        });
        contLayColor.add(contLayColorRed);

        contLayColorGreen.setText("Green");
        contLayColorGreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayColorGreenActionPerformed(evt);
            }
        });
        contLayColor.add(contLayColorGreen);

        contLayColorBlue.setText("Blue");
        contLayColorBlue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayColorBlueActionPerformed(evt);
            }
        });
        contLayColor.add(contLayColorBlue);

        contLayColorGray.setText("Grayscale");
        contLayColorGray.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayColorGrayActionPerformed(evt);
            }
        });
        contLayColor.add(contLayColorGray);

        contLayColorCustom.setText("Custom");
        contLayColorCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayColorCustomActionPerformed(evt);
            }
        });
        contLayColor.add(contLayColorCustom);

        contextLayerMenu.add(contLayColor);

        contLayExtract.setText("Extract colour");

        contLayExtractRed.setText("Red");
        contLayExtractRed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayExtractRedActionPerformed(evt);
            }
        });
        contLayExtract.add(contLayExtractRed);

        contLayExtractGreen.setText("Green");
        contLayExtractGreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayExtractGreenActionPerformed(evt);
            }
        });
        contLayExtract.add(contLayExtractGreen);

        contLayExtractBlue.setText("Blue");
        contLayExtractBlue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayExtractBlueActionPerformed(evt);
            }
        });
        contLayExtract.add(contLayExtractBlue);

        contLayExtractAlpha.setText("Alpha");
        contLayExtractAlpha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayExtractAlphaActionPerformed(evt);
            }
        });
        contLayExtract.add(contLayExtractAlpha);

        contLayExtractAll.setText("RGB");
        contLayExtractAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayExtractAllActionPerformed(evt);
            }
        });
        contLayExtract.add(contLayExtractAll);

        contextLayerMenu.add(contLayExtract);

        contImgRename.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        contImgRename.setText("Rename");
        contImgRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgRenameActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgRename);

        contImgScale.setText("Set scaling");
        contImgScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgScaleActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgScale);

        contImgSelectAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        contImgSelectAll.setText("Select all");
        contImgSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgSelectAllActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgSelectAll);

        contImgMoveUp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.ALT_DOWN_MASK));
        contImgMoveUp.setText("Move up");
        contImgMoveUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgMoveUpActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgMoveUp);

        contImgMoveDown.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.ALT_DOWN_MASK));
        contImgMoveDown.setText("Move down");
        contImgMoveDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgMoveDownActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgMoveDown);

        contImgDelete.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        contImgDelete.setText("Delete");
        contImgDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgDeleteActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgDelete);

        contImgClear.setText("Clear all");
        contImgClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgClearActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgClear);

        contResClear.setText("Clear all");
        contResClear.setToolTipText("");
        contResClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResClearActionPerformed(evt);
            }
        });
        contextTableMenu.add(contResClear);

        contResDelRow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        contResDelRow.setText("Delete selected row(s)");
        contResDelRow.setToolTipText("");
        contResDelRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResDelRowActionPerformed(evt);
            }
        });
        contextTableMenu.add(contResDelRow);
        contextTableMenu.add(jSeparator24);

        contResTranspose.setText("Transpose by image");
        contResTranspose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResTransposeActionPerformed(evt);
            }
        });
        contextTableMenu.add(contResTranspose);
        contextTableMenu.add(contResSeparator);

        contResExport.setText("Export as TSV");
        contResExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResExportActionPerformed(evt);
            }
        });
        contextTableMenu.add(contResExport);

        contResExcel.setText("Open in Excel");
        contResExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResExcelActionPerformed(evt);
            }
        });
        contextTableMenu.add(contResExcel);

        Splash.append("Context layout",5);

        contAnnoClear.setText("Clear all");
        contAnnoClear.setToolTipText("");
        contAnnoClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contAnnoClearActionPerformed(evt);
            }
        });
        contextAnnotationMenu.add(contAnnoClear);

        contAnnoDelRow.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        contAnnoDelRow.setText("Delete selected row(s)");
        contAnnoDelRow.setToolTipText("");
        contAnnoDelRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contAnnoDelRowActionPerformed(evt);
            }
        });
        contextAnnotationMenu.add(contAnnoDelRow);
        contextAnnotationMenu.add(contAnnoSeparator);

        contAnnoExport.setText("Export as TSV");
        contAnnoExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contAnnoExportActionPerformed(evt);
            }
        });
        contextAnnotationMenu.add(contAnnoExport);

        contAnnoExcel.setText("Open in Excel");
        contAnnoExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contAnnoExcelActionPerformed(evt);
            }
        });
        contextAnnotationMenu.add(contAnnoExcel);

        Splash.append("Context layout",5);

        filterMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
                filterMenuPopupMenuCanceled(evt);
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        shapeRectangle.setText("Rectangle");
        shapeRectangle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shapeRectangleActionPerformed(evt);
            }
        });
        shapeMenu.add(shapeRectangle);

        shapeCircle.setText("Circle");
        shapeCircle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shapeCircleActionPerformed(evt);
            }
        });
        shapeMenu.add(shapeCircle);

        shapeOval.setText("Oval");
        shapeOval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shapeOvalActionPerformed(evt);
            }
        });
        shapeMenu.add(shapeOval);

        shapePoly.setText("Polygon");
        shapePoly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shapePolyActionPerformed(evt);
            }
        });
        shapeMenu.add(shapePoly);

        lineLine.setText("Line");
        lineLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lineLineActionPerformed(evt);
            }
        });
        lineMenu.add(lineLine);

        linePolyline.setText("Polyline");
        linePolyline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linePolylineActionPerformed(evt);
            }
        });
        lineMenu.add(linePolyline);

        lineThick.setText("Polyline radius");
        lineThick.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lineThickActionPerformed(evt);
            }
        });
        lineMenu.add(lineThick);

        lineAngle.setText("Angle");
        lineAngle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lineAngleActionPerformed(evt);
            }
        });
        lineMenu.add(lineAngle);

        colorDefault.setText("Default");
        colorDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorDefault);

        colorRed.setForeground(new java.awt.Color(255, 51, 51));
        colorRed.setText("Red");
        colorRed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorRed);

        colorGreen.setForeground(new java.awt.Color(0, 204, 0));
        colorGreen.setText("Green");
        colorGreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorGreen);

        colorBlue.setForeground(new java.awt.Color(0, 51, 255));
        colorBlue.setText("Blue");
        colorBlue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorBlue);

        colorYellow.setForeground(new java.awt.Color(255, 255, 51));
        colorYellow.setText("Yellow");
        colorYellow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorYellow);

        colorCyan.setForeground(new java.awt.Color(51, 255, 255));
        colorCyan.setText("Cyan");
        colorCyan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorCyan);

        colorMagenta.setForeground(new java.awt.Color(255, 51, 255));
        colorMagenta.setText("Fuchsia");
        colorMagenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorMagenta);

        colorPink.setForeground(new java.awt.Color(255, 204, 255));
        colorPink.setText("Pink");
        colorPink.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorPink);

        colorOrange.setForeground(new java.awt.Color(255, 153, 51));
        colorOrange.setText("Orange");
        colorOrange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorOrange);

        colorPurple.setForeground(new java.awt.Color(153, 51, 255));
        colorPurple.setText("Purple");
        colorPurple.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorPurple);

        colorLime.setForeground(new java.awt.Color(204, 255, 0));
        colorLime.setText("Lime");
        colorLime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorMenuActionPerformed(evt);
            }
        });
        colorMenu.add(colorLime);

        dotDot.setText("Point");
        dotDot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dotDotActionPerformed(evt);
            }
        });
        dotMenu.add(dotDot);

        dotRadius.setText("Point radius");
        dotRadius.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dotRadiusActionPerformed(evt);
            }
        });
        dotMenu.add(dotRadius);

        Splash.append("Popup layout",2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Tonga");
        setIconImages(mainIcons);
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(100, 100));

        frame.setPreferredSize(new java.awt.Dimension(1435, 814));

        rSplitPane.setBorder(null);
        rSplitPane.setDividerLocation(450);
        rSplitPane.setDividerSize(6);
        rSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        rSplitPane.setResizeWeight(0.75);
        rSplitPane.setMinimumSize(new java.awt.Dimension(100, 500));
        rSplitPane.setPreferredSize(new java.awt.Dimension(200, 500));

        jList4.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(jList4);

        rSplitPane.setRightComponent(jScrollPane4);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Images"));
        jPanel1.setMinimumSize(new java.awt.Dimension(100, 200));
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 400));

        jScrollPane1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        imagesList.setModel(new DefaultListModel());
        imagesList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                imagesListMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(imagesList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );

        rSplitPane.setLeftComponent(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Layers"));
        jPanel2.setMinimumSize(new java.awt.Dimension(100, 200));
        jPanel2.setPreferredSize(new java.awt.Dimension(100, 200));

        jScrollPane3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        jScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setMinimumSize(new java.awt.Dimension(21, 50));
        jScrollPane3.setPreferredSize(new java.awt.Dimension(33, 50));

        layersList.setModel(new DefaultListModel());
        layersList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                layersListMouseReleased(evt);
            }
        });
        jScrollPane3.setViewportView(layersList);

        stackToggle.setText("Stack");
        stackToggle.setToolTipText("Toggle the stack mode on/off (S). Right click to apply to all images (Shift+S).");
        stackToggle.setPreferredSize(new java.awt.Dimension(100, 23));
        stackToggle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                stackToggleMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addComponent(stackToggle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jSeparator4, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                .addGap(1, 1, 1)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(stackToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        rSplitPane.setBottomComponent(jPanel2);

        lSplitPane.setBorder(null);
        lSplitPane.setDividerLocation(780);
        lSplitPane.setDividerSize(6);
        lSplitPane.setResizeWeight(0.7);
        lSplitPane.setMinimumSize(new java.awt.Dimension(600, 400));

        imageBig.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        imageBig.setMinimumSize(new java.awt.Dimension(400, 400));
        imageBig.setName(""); // NOI18N
        imageBig.setPreferredSize(new java.awt.Dimension(600, 600));
        imageBig.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                imageBigComponentResized(evt);
            }
        });

        javax.swing.GroupLayout imageBigLayout = new javax.swing.GroupLayout(imageBig);
        imageBig.setLayout(imageBigLayout);
        imageBigLayout.setHorizontalGroup(
            imageBigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 776, Short.MAX_VALUE)
        );
        imageBigLayout.setVerticalGroup(
            imageBigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 855, Short.MAX_VALUE)
        );

        lSplitPane.setLeftComponent(imageBig);

        vSplitPane.setBorder(null);
        vSplitPane.setDividerLocation(405);
        vSplitPane.setDividerSize(6);
        vSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        vSplitPane.setResizeWeight(0.6);

        imageZoom.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        imageZoom.setMaximumSize(new java.awt.Dimension(800, 800));
        imageZoom.setMinimumSize(new java.awt.Dimension(200, 0));
        imageZoom.setPreferredSize(new java.awt.Dimension(400, 400));
        imageZoom.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                imageZoomComponentResized(evt);
            }
        });

        javax.swing.GroupLayout imageZoomLayout = new javax.swing.GroupLayout(imageZoom);
        imageZoom.setLayout(imageZoomLayout);
        imageZoomLayout.setHorizontalGroup(
            imageZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 396, Short.MAX_VALUE)
        );
        imageZoomLayout.setVerticalGroup(
            imageZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 401, Short.MAX_VALUE)
        );

        vSplitPane.setLeftComponent(imageZoom);

        floatingPane.setOpaque(false);

        maxTabButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/window.png"))); // NOI18N
        maxTabButton.setToolTipText("Open the result table in a new big window");
        maxTabButton.setIconTextGap(0);
        maxTabButton.setMargin(new java.awt.Insets(0, 0, -1, 0));
        maxTabButton.setMaximumSize(new java.awt.Dimension(24, 23));
        maxTabButton.setMinimumSize(new java.awt.Dimension(24, 23));
        maxTabButton.setPreferredSize(new java.awt.Dimension(24, 23));
        maxTabButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxTabButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout floatingPaneLayout = new javax.swing.GroupLayout(floatingPane);
        floatingPane.setLayout(floatingPaneLayout);
        floatingPaneLayout.setHorizontalGroup(
            floatingPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, floatingPaneLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(maxTabButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3))
        );
        floatingPaneLayout.setVerticalGroup(
            floatingPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(floatingPaneLayout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(maxTabButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tabbedPane.setMinimumSize(new java.awt.Dimension(100, 400));
        tabbedPane.setPreferredSize(new java.awt.Dimension(400, 400));
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        protocolName.setText("Sample protocol");

        protocolSettingsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        protocolSettingsPanel.setPreferredSize(new java.awt.Dimension(370, 265));
        protocolSettingsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                protocolSettingsPanelComponentResized(evt);
            }
        });

        javax.swing.GroupLayout protocolSettingsPanelLayout = new javax.swing.GroupLayout(protocolSettingsPanel);
        protocolSettingsPanel.setLayout(protocolSettingsPanelLayout);
        protocolSettingsPanelLayout.setHorizontalGroup(
            protocolSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        protocolSettingsPanelLayout.setVerticalGroup(
            protocolSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 313, Short.MAX_VALUE)
        );

        btnRunSingle.setText("Run for single");
        btnRunSingle.setMaximumSize(new java.awt.Dimension(100, 23));
        btnRunSingle.setMinimumSize(new java.awt.Dimension(100, 23));
        btnRunSingle.setPreferredSize(new java.awt.Dimension(100, 23));
        btnRunSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunSingleActionPerformed(evt);
            }
        });

        btnRunAll.setText("Run for all");
        btnRunAll.setMaximumSize(new java.awt.Dimension(100, 23));
        btnRunAll.setMinimumSize(new java.awt.Dimension(100, 23));
        btnRunAll.setPreferredSize(new java.awt.Dimension(100, 23));
        btnRunAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout protocolHeadPanelLayout = new javax.swing.GroupLayout(protocolHeadPanel);
        protocolHeadPanel.setLayout(protocolHeadPanelLayout);
        protocolHeadPanelLayout.setHorizontalGroup(
            protocolHeadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolHeadPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolHeadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocolName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(protocolNameSeparator)
                    .addComponent(protocolSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(protocolSettingsSeparator, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, protocolHeadPanelLayout.createSequentialGroup()
                        .addComponent(btnRunSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRunAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        protocolHeadPanelLayout.setVerticalGroup(
            protocolHeadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, protocolHeadPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(protocolName, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolNameSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(protocolSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolSettingsSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(protocolHeadPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRunSingle, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRunAll, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout protocolPanelLayout = new javax.swing.GroupLayout(protocolPanel);
        protocolPanel.setLayout(protocolPanelLayout);
        protocolPanelLayout.setHorizontalGroup(
            protocolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocolHeadPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        protocolPanelLayout.setVerticalGroup(
            protocolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(protocolHeadPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        tabbedPane.addTab("Protocol", protocolPanel);

        filterSettingsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        filterSettingsPanel.setPreferredSize(new java.awt.Dimension(370, 256));
        filterSettingsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                filterSettingsPanelComponentResized(evt);
            }
        });

        javax.swing.GroupLayout filterSettingsPanelLayout = new javax.swing.GroupLayout(filterSettingsPanel);
        filterSettingsPanel.setLayout(filterSettingsPanelLayout);
        filterSettingsPanelLayout.setHorizontalGroup(
            filterSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 367, Short.MAX_VALUE)
        );
        filterSettingsPanelLayout.setVerticalGroup(
            filterSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 255, Short.MAX_VALUE)
        );

        btnRunSingle2.setText("Run for single");
        btnRunSingle2.setMaximumSize(new java.awt.Dimension(100, 23));
        btnRunSingle2.setMinimumSize(new java.awt.Dimension(100, 23));
        btnRunSingle2.setPreferredSize(new java.awt.Dimension(100, 23));
        btnRunSingle2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunSingle2ActionPerformed(evt);
            }
        });

        btnRunAll2.setText("Run for all");
        btnRunAll2.setMaximumSize(new java.awt.Dimension(100, 23));
        btnRunAll2.setMinimumSize(new java.awt.Dimension(100, 23));
        btnRunAll2.setPreferredSize(new java.awt.Dimension(100, 23));
        btnRunAll2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunAll2ActionPerformed(evt);
            }
        });

        filterCombo.setToolTipText("These are the most recently used filters");
        filterCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                filterComboItemStateChanged(evt);
            }
        });

        browseFilter.setText("...");
        browseFilter.setToolTipText("You can access all the filters also here");
        browseFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseFilterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout filterPanelLayout = new javax.swing.GroupLayout(filterPanel);
        filterPanel.setLayout(filterPanelLayout);
        filterPanelLayout.setHorizontalGroup(
            filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(filterSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(filterSettingsSeparator, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, filterPanelLayout.createSequentialGroup()
                        .addComponent(btnRunSingle2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRunAll2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(filterNameSeparator)
                    .addGroup(filterPanelLayout.createSequentialGroup()
                        .addComponent(browseFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        filterPanelLayout.setVerticalGroup(
            filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(filterCombo)
                    .addComponent(browseFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterNameSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(filterSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 307, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterSettingsSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRunSingle2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRunAll2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabbedPane.addTab("Filter", filterPanel);

        settingPanelLayer.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer settings"));
        settingPanelLayer.setPreferredSize(new java.awt.Dimension(370, 252));

        boxSettingAlphaBG.setText("alpha background");
        boxSettingAlphaBG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxSettingAlphaBGActionPerformed(evt);
            }
        });

        layerBackColor.setBackground(new java.awt.Color(0, 0, 0));
        layerBackColor.setPreferredSize(new java.awt.Dimension(100, 30));
        layerBackColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layerBackColorActionPerformed(evt);
            }
        });

        stackCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Sum", "Difference", "Multiply", "Maximum", "Minimum" }));
        stackCombo.setPreferredSize(new java.awt.Dimension(100, 20));
        stackCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stackComboActionPerformed(evt);
            }
        });

        jLabel2.setText("Stack mode");
        jLabel2.setToolTipText("");

        javax.swing.GroupLayout settingPanelLayerLayout = new javax.swing.GroupLayout(settingPanelLayer);
        settingPanelLayer.setLayout(settingPanelLayerLayout);
        settingPanelLayerLayout.setHorizontalGroup(
            settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingPanelLayerLayout.createSequentialGroup()
                .addGroup(settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingPanelLayerLayout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE))
                    .addGroup(settingPanelLayerLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(boxSettingAlphaBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(layerBackColor, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stackCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        settingPanelLayerLayout.setVerticalGroup(
            settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingPanelLayerLayout.createSequentialGroup()
                .addGroup(settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(layerBackColor, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(boxSettingAlphaBG, javax.swing.GroupLayout.PREFERRED_SIZE, 20, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingPanelLayerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stackCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        settingPanelGeneral.setBorder(javax.swing.BorderFactory.createTitledBorder("General settings"));
        settingPanelGeneral.setPreferredSize(new java.awt.Dimension(370, 252));

        boxSettingMultiThreading.setSelected(true);
        boxSettingMultiThreading.setText("multithreading");
        boxSettingMultiThreading.setToolTipText("Use multiple threads to execute protocols. Depending on the computer this can provide a large speed boost when processing multiple images.");
        boxSettingMultiThreading.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingMultiThreading.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingMultiThreading.setPreferredSize(new java.awt.Dimension(120, 23));

        boxSettingMMapping.setSelected(true);
        boxSettingMMapping.setText("memory mapping");
        boxSettingMMapping.setToolTipText("Use memory mapping to prevent running out of RAM.");
        boxSettingMMapping.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingMMapping.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingMMapping.setPreferredSize(new java.awt.Dimension(120, 23));

        boxSettingHWRendering.setSelected(true);
        boxSettingHWRendering.setText("hardware rendering");
        boxSettingHWRendering.setToolTipText("Use hardware acceleration to render images faster. Switch off in case of incompatilibity issues.");
        boxSettingHWRendering.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingHWRendering.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingHWRendering.setPreferredSize(new java.awt.Dimension(120, 23));
        boxSettingHWRendering.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxSettingHWRenderingActionPerformed(evt);
            }
        });

        boxSettingBatch.setText("work on disk");
        boxSettingBatch.setToolTipText("Only a file location pointer is imported - images are imported upon running a filter/protocol and the result is saved on disk.");
        boxSettingBatch.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingBatch.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingBatch.setPreferredSize(new java.awt.Dimension(120, 23));
        boxSettingBatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxSettingBatchActionPerformed(evt);
            }
        });

        boxSettingResultsAppend.setText("append results");
        boxSettingResultsAppend.setToolTipText("Append new results to the previous results instead of clearing the list automatically");
        boxSettingResultsAppend.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingResultsAppend.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingResultsAppend.setPreferredSize(new java.awt.Dimension(120, 23));

        javax.swing.GroupLayout settingPanelGeneralLayout = new javax.swing.GroupLayout(settingPanelGeneral);
        settingPanelGeneral.setLayout(settingPanelGeneralLayout);
        settingPanelGeneralLayout.setHorizontalGroup(
            settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingPanelGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(boxSettingMMapping, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxSettingMultiThreading, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxSettingHWRendering, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(25, 25, 25)
                .addGroup(settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(boxSettingBatch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxSettingResultsAppend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        settingPanelGeneralLayout.setVerticalGroup(
            settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingPanelGeneralLayout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxSettingBatch, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingMultiThreading, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxSettingMMapping, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingResultsAppend, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(boxSettingHWRendering, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );

        settingPanelScale.setBorder(javax.swing.BorderFactory.createTitledBorder("Autoscaling settings"));
        settingPanelScale.setPreferredSize(new java.awt.Dimension(370, 252));

        autoscaleLabel.setText("Autoscaling");
        autoscaleLabel.setToolTipText("Autoscale >8-bit images automatically based on the histogram. Please note that this is only a visual change.");

        autoscaleCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "File channel", "Every channel", "Every image" }));
        autoscaleCombo.setSelectedIndex(2);
        autoscaleCombo.setToolTipText("Autoscaling can be done for every image separately, or channelwise (either per file or per a set of imported images) to maintain visual comparability");
        autoscaleCombo.setPreferredSize(new java.awt.Dimension(100, 20));
        autoscaleCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscaleComboActionPerformed(evt);
            }
        });

        boxSettingAutoscale1.setText("adaptive");
        boxSettingAutoscale1.setToolTipText("The autoscaling has adaptation to ignore single bright dots etc.");

        javax.swing.GroupLayout settingPanelScaleLayout = new javax.swing.GroupLayout(settingPanelScale);
        settingPanelScale.setLayout(settingPanelScaleLayout);
        settingPanelScaleLayout.setHorizontalGroup(
            settingPanelScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingPanelScaleLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(autoscaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(autoscaleCombo, 0, 178, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(boxSettingAutoscale1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        settingPanelScaleLayout.setVerticalGroup(
            settingPanelScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingPanelScaleLayout.createSequentialGroup()
                .addGroup(settingPanelScaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(autoscaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(autoscaleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingAutoscale1))
                .addGap(0, 22, Short.MAX_VALUE))
        );

        settingPanelFile.setBorder(javax.swing.BorderFactory.createTitledBorder("File output"));
        settingPanelFile.setPreferredSize(new java.awt.Dimension(370, 252));

        browseButton.setText("Browse...");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        boxSettingOpenAfter.setSelected(true);
        boxSettingOpenAfter.setText("open the folder after export");

        boxSettingSubfolder.setSelected(true);
        boxSettingSubfolder.setText("auto-create subfolders");

        javax.swing.GroupLayout settingPanelFileLayout = new javax.swing.GroupLayout(settingPanelFile);
        settingPanelFile.setLayout(settingPanelFileLayout);
        settingPanelFileLayout.setHorizontalGroup(
            settingPanelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingPanelFileLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingPanelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(settingPanelFileLayout.createSequentialGroup()
                        .addComponent(filePathField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton))
                    .addGroup(settingPanelFileLayout.createSequentialGroup()
                        .addComponent(boxSettingOpenAfter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(32, 32, 32)
                        .addComponent(boxSettingSubfolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        settingPanelFileLayout.setVerticalGroup(
            settingPanelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingPanelFileLayout.createSequentialGroup()
                .addGroup(settingPanelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filePathField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addGap(7, 7, 7)
                .addGroup(settingPanelFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxSettingOpenAfter)
                    .addComponent(boxSettingSubfolder))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(settingPanelLayer, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(settingPanelGeneral, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(settingPanelScale, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(settingPanelFile, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE))
                .addContainerGap())
        );
        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingPanelLayer, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingPanelGeneral, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingPanelScale, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(settingPanelFile, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        settingPanelLayer.getAccessibleContext().setAccessibleName("");

        tabbedPane.addTab("General", generalPanel);

        exportAsCSV.setText("Export as TSV");
        exportAsCSV.setMaximumSize(new java.awt.Dimension(100, 23));
        exportAsCSV.setMinimumSize(new java.awt.Dimension(100, 23));
        exportAsCSV.setPreferredSize(new java.awt.Dimension(100, 23));
        exportAsCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAsCSVActionPerformed(evt);
            }
        });

        openExcel.setText("Launch Excel");
        openExcel.setMaximumSize(new java.awt.Dimension(100, 23));
        openExcel.setMinimumSize(new java.awt.Dimension(100, 23));
        openExcel.setPreferredSize(new java.awt.Dimension(100, 23));
        openExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExcelActionPerformed(evt);
            }
        });

        resultTableComponent.setAutoCreateRowSorter(true);
        resultTableComponent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        resultTableComponent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                resultTableComponentMouseReleased(evt);
            }
        });
        resultScrollPane.setViewportView(resultTableComponent);

            }
        });

        javax.swing.GroupLayout resultsPanelLayout = new javax.swing.GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                    .addGroup(resultsPanelLayout.createSequentialGroup()
                        .addComponent(exportTSV, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(launchExcel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(launchExcel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportTSV, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabbedPane.addTab("Results", resultsPanel);

        histoImg.setBackground(new java.awt.Color(153, 153, 153));
        histoImg.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        histoImg.setPreferredSize(new java.awt.Dimension(371, 239));

        histoLabel.setBackground(new java.awt.Color(255, 255, 255));
        histoLabel.setAlignmentY(0.0F);
        histoLabel.setIconTextGap(0);
        histoLabel.setPreferredSize(new java.awt.Dimension(371, 239));

        javax.swing.GroupLayout histoImgLayout = new javax.swing.GroupLayout(histoImg);
        histoImg.setLayout(histoImgLayout);
        histoImgLayout.setHorizontalGroup(
            histoImgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(histoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
        );
        histoImgLayout.setVerticalGroup(
            histoImgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(histoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
        );

        histoAdjAutoSingle.setText("Auto-adjust");
        histoAdjAutoSingle.setMaximumSize(new java.awt.Dimension(100, 23));
        histoAdjAutoSingle.setMinimumSize(new java.awt.Dimension(100, 23));
        histoAdjAutoSingle.setPreferredSize(new java.awt.Dimension(100, 23));
        histoAdjAutoSingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                histoAdjAutoSingleActionPerformed(evt);
            }
        });

        histoAdjApplySingle.setText("Apply");
        histoAdjApplySingle.setPreferredSize(new java.awt.Dimension(100, 23));
        histoAdjApplySingle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                histoAdjApplySingleActionPerformed(evt);
            }
        });

        histoAdjApplyAll.setText("Apply to all");
        histoAdjApplyAll.setPreferredSize(new java.awt.Dimension(100, 23));
        histoAdjApplyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                histoAdjApplyAllActionPerformed(evt);
            }
        });

        histoAdjAutoAll.setText("Auto-adjust all");
        histoAdjAutoAll.setMaximumSize(new java.awt.Dimension(100, 23));
        histoAdjAutoAll.setMinimumSize(new java.awt.Dimension(100, 23));
        histoAdjAutoAll.setPreferredSize(new java.awt.Dimension(100, 23));
        histoAdjAutoAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                histoAdjAutoAllActionPerformed(evt);
            }
        });

        histoSliderPanel.setPreferredSize(new java.awt.Dimension(200, 25));

        javax.swing.GroupLayout histoSliderPanelLayout = new javax.swing.GroupLayout(histoSliderPanel);
        histoSliderPanel.setLayout(histoSliderPanelLayout);
        histoSliderPanelLayout.setHorizontalGroup(
            histoSliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 371, Short.MAX_VALUE)
        );
        histoSliderPanelLayout.setVerticalGroup(
            histoSliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 25, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout histogramPanelLayout = new javax.swing.GroupLayout(histogramPanel);
        histogramPanel.setLayout(histogramPanelLayout);
        histogramPanelLayout.setHorizontalGroup(
            histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(histogramPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(histogramPanelLayout.createSequentialGroup()
                        .addComponent(histoAdjApplySingle, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(histoAdjApplyAll, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
                    .addComponent(jSeparator9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(histogramPanelLayout.createSequentialGroup()
                        .addComponent(histoAdjAutoSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(histoAdjAutoAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(histoImg, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                    .addComponent(histoSliderPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE))
                .addContainerGap())
        );
        histogramPanelLayout.setVerticalGroup(
            histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(histogramPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(histoImg, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(histoSliderPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addGroup(histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(histoAdjApplySingle, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(histoAdjApplyAll, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(histoAdjAutoSingle, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(histoAdjAutoAll, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabbedPane.addTab("Histogram", histogramPanel);

        exportAnnot.setText("Export annotations");
        exportAnnot.setMaximumSize(new java.awt.Dimension(100, 23));
        exportAnnot.setMinimumSize(new java.awt.Dimension(100, 23));
        exportAnnot.setPreferredSize(new java.awt.Dimension(100, 23));
        exportAnnot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAnnotActionPerformed(evt);
            }
        });

        importAnnot.setText("Import annotations");
        importAnnot.setMaximumSize(new java.awt.Dimension(100, 23));
        importAnnot.setMinimumSize(new java.awt.Dimension(100, 23));
        importAnnot.setPreferredSize(new java.awt.Dimension(100, 23));
        importAnnot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importAnnotActionPerformed(evt);
            }
        });

        annoTableComponent.setAutoCreateRowSorter(true);
        annoTableComponent.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        annoTableComponent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                annoTableComponentMouseReleased(evt);
            }
        });
        annoScrollPane.setViewportView(annoTableComponent);

        annotClearAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/cross.png"))); // NOI18N
        annotClearAll.setToolTipText("Delete all annotations");
        annotClearAll.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotClearAll.setMaximumSize(new java.awt.Dimension(32, 32));
        annotClearAll.setMinimumSize(new java.awt.Dimension(32, 32));
        annotClearAll.setPreferredSize(new java.awt.Dimension(32, 32));
        annotClearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotClearAllActionPerformed(evt);
            }
        });

        jSeparator20.setOrientation(javax.swing.SwingConstants.VERTICAL);

        annotDot.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/dots.png"))); // NOI18N
        annotDot.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotDot.setMaximumSize(new java.awt.Dimension(32, 32));
        annotDot.setMinimumSize(new java.awt.Dimension(32, 32));
        annotDot.setPreferredSize(new java.awt.Dimension(32, 32));
        annotDot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotDotActionPerformed(evt);
            }
        });

        annotLine.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/lines.png"))); // NOI18N
        annotLine.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotLine.setMaximumSize(new java.awt.Dimension(32, 32));
        annotLine.setMinimumSize(new java.awt.Dimension(32, 32));
        annotLine.setPreferredSize(new java.awt.Dimension(32, 32));
        annotLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotLineActionPerformed(evt);
            }
        });

        annotShape.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/shapes.png"))); // NOI18N
        annotShape.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotShape.setMaximumSize(new java.awt.Dimension(32, 32));
        annotShape.setMinimumSize(new java.awt.Dimension(32, 32));
        annotShape.setPreferredSize(new java.awt.Dimension(32, 32));
        annotShape.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotShapeActionPerformed(evt);
            }
        });

        jSeparator21.setOrientation(javax.swing.SwingConstants.VERTICAL);

        annotErase.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/delete.png"))); // NOI18N
        annotErase.setToolTipText("Click an annotation to delete");
        annotErase.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotErase.setMaximumSize(new java.awt.Dimension(32, 32));
        annotErase.setMinimumSize(new java.awt.Dimension(32, 32));
        annotErase.setPreferredSize(new java.awt.Dimension(32, 32));
        annotErase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotEraseActionPerformed(evt);
            }
        });

        annotMove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/arrow.png"))); // NOI18N
        annotMove.setSelected(true);
        annotMove.setToolTipText("Select annotations");
        annotMove.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotMove.setMaximumSize(new java.awt.Dimension(32, 32));
        annotMove.setMinimumSize(new java.awt.Dimension(32, 32));
        annotMove.setPreferredSize(new java.awt.Dimension(32, 32));
        annotMove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotMoveActionPerformed(evt);
            }
        });

        annotColour.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/color.png"))); // NOI18N
        annotColour.setToolTipText("Annotation color");
        annotColour.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotColour.setMaximumSize(new java.awt.Dimension(32, 32));
        annotColour.setMinimumSize(new java.awt.Dimension(32, 32));
        annotColour.setPreferredSize(new java.awt.Dimension(32, 32));
        annotColour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotColourActionPerformed(evt);
            }
        });

        annotRadius.setToolTipText("Annotation radius (for dot/polyline radius)");
        annotRadius.setMinimumSize(new java.awt.Dimension(30, 22));
        annotRadius.setPreferredSize(new java.awt.Dimension(30, 32));
        annotRadius.setValue(Integer.valueOf(20));
        annotRadius.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                annotRadiusStateChanged(evt);
            }
        });
        annotRadius.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                annotRadiusMouseWheelMoved(evt);
            }
        });

        annotVisible.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resourcePackage/tool/eye.png"))); // NOI18N
        annotVisible.setSelected(true);
        annotVisible.setToolTipText("Toggle annotation visibility");
        annotVisible.setMargin(new java.awt.Insets(-7, -9, -7, -9));
        annotVisible.setMaximumSize(new java.awt.Dimension(32, 32));
        annotVisible.setMinimumSize(new java.awt.Dimension(32, 32));
        annotVisible.setPreferredSize(new java.awt.Dimension(32, 32));
        annotVisible.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotVisibleActionPerformed(evt);
            }
        });

        annotGroup.setToolTipText("Annotation group");
        annotGroup.setMinimumSize(new java.awt.Dimension(25, 22));
        annotGroup.setPreferredSize(new java.awt.Dimension(25, 32));
        annotGroup.setValue(Integer.valueOf(1));
        annotGroup.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                annotGroupStateChanged(evt);
            }
        });
        annotGroup.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                annotGroupMouseWheelMoved(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(annotDot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(annotLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(annotShape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annotGroup, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annotRadius, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annotColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annotVisible, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(annotMove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(annotErase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(annotClearAll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(annoScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(exportAnnot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importAnnot, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator20, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(annotClearAll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotErase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotMove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotVisible, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator21, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(annotDot, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotShape, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(annotColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(annotRadius, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(annotGroup, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(annoScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(importAnnot, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportAnnot, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6))
        );

        tabbedPane.addTab("Annotations", jPanel3);

        layerPane.setLayer(floatingPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
        layerPane.setLayer(tabbedPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layerPaneLayout = new javax.swing.GroupLayout(layerPane);
        layerPane.setLayout(layerPaneLayout);
        layerPaneLayout.setHorizontalGroup(
            layerPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layerPaneLayout.createSequentialGroup()
                .addGap(0, 350, Short.MAX_VALUE)
                .addComponent(floatingPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layerPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layerPaneLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        layerPaneLayout.setVerticalGroup(
            layerPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layerPaneLayout.createSequentialGroup()
                .addComponent(floatingPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 348, Short.MAX_VALUE))
            .addGroup(layerPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layerPaneLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        tabbedPane.getAccessibleContext().setAccessibleName("");

        vSplitPane.setRightComponent(layerPane);

        lSplitPane.setRightComponent(vSplitPane);

        javax.swing.GroupLayout frameLayout = new javax.swing.GroupLayout(frame);
        frame.setLayout(frameLayout);
        frameLayout.setHorizontalGroup(
            frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(frameLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(rSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1234, Short.MAX_VALUE)
                .addGap(6, 6, 6))
        );
        frameLayout.setVerticalGroup(
            frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(rSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(frameLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(lSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
                .addGap(2, 2, 2))
        );

        Splash.append("Control layout",10);

        progressBar.setMaximum(1000);

        brightLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        brightLabel.setText(" ");
        brightLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        brightLabel.setMaximumSize(new java.awt.Dimension(396, 16));
        brightLabel.setMinimumSize(new java.awt.Dimension(396, 16));
        brightLabel.setPreferredSize(new java.awt.Dimension(396, 16));

        bleftLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        bleftLabel.setText("<html>Welcome to Tonga!</html>");
        bleftLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        bleftLabel.setMinimumSize(new java.awt.Dimension(43, 16));
        bleftLabel.setPreferredSize(new java.awt.Dimension(966, 16));

        menuFile.setText("Tonga");

        menuFileUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFileUndo.setText("Undo/redo");
        menuFileUndo.setToolTipText("Revert the latest action");
        menuFileUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileUndoActionPerformed(evt);
            }
        });
        menuFile.add(menuFileUndo);
        menuFile.add(jSeparator3);

        menuImport.setText("Import images");

        menuFileImportImages.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFileImportImages.setText("As new images");
        menuFileImportImages.setToolTipText("Import new images with one layer each. Each file will become a new image with only one layer.");
        menuFileImportImages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportImagesActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportImages);

        menuFileImportImage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFileImportImage.setText("As an image with layers");
        menuFileImportImage.setToolTipText("Import new image with multiple layers. One image will be created, and each file will become a layer of this image.");
        menuFileImportImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportImageActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportImage);

        menuFileImportLayers.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFileImportLayers.setText("Add layers to all images");
        menuFileImportLayers.setToolTipText("Import new layer(s) to all images. Please make sure that the number of files is divisible with the number of images open.");
        menuFileImportLayers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportLayersActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportLayers);

        menuFileImportLayersThis.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        menuFileImportLayersThis.setText("Add layers to this/these image(s)");
        menuFileImportLayersThis.setToolTipText("Import new layer(s) to the images selected in the Images-list. Please make sure that the number of files is divisible with the number of selected images.");
        menuFileImportLayersThis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportLayersThisActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportLayersThis);
        menuImport.add(jSeparator18);

        menuFileImportStacks.setText("Stack image(s)");
        menuFileImportStacks.setToolTipText("Import stack image files which have a fixed layer structure and might contain multiple images and layers.");
        menuFileImportStacks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportStacksActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportStacks);

        menuFileImportMultichannel.setText("A multichannel image set");
        menuFileImportMultichannel.setToolTipText("Import an image set with multiple channels, where each channel is a separate file. Multiple new images with x amount of layers can be imported at once.");
        menuFileImportMultichannel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportMultichannelActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportMultichannel);

        menuFile.add(menuImport);

        menuExport.setText("Export images");

        menuExportSingle.setText("Current image");

        menuExportSingleLayer.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuExportSingleLayer.setText("Selected layer only");
        menuExportSingleLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportSingleLayerActionPerformed(evt);
            }
        });
        menuExportSingle.add(menuExportSingleLayer);

        menuExportSingleImage.setText("All layers");
        menuExportSingleImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportSingleImageActionPerformed(evt);
            }
        });
        menuExportSingle.add(menuExportSingleImage);

        menuExportSingleStack.setText("As a stack image");
        menuExportSingleStack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportSingleStackActionPerformed(evt);
            }
        });
        menuExportSingle.add(menuExportSingleStack);

        menuExport.add(menuExportSingle);

        menuExportAll.setText("All images");

        menuExportAllLayers.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuExportAllLayers.setText("Selected layer only");
        menuExportAllLayers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportAllLayersActionPerformed(evt);
            }
        });
        menuExportAll.add(menuExportAllLayers);

        menuExportAllImages.setText("All layers");
        menuExportAllImages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportAllImagesActionPerformed(evt);
            }
        });
        menuExportAll.add(menuExportAllImages);

        menuExportAllStacks.setText("As a stack image");
        menuExportAllStacks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportAllStacksActionPerformed(evt);
            }
        });
        menuExportAll.add(menuExportAllStacks);

        menuExport.add(menuExportAll);

        menuFile.add(menuExport);
        menuFile.add(jSeparator8);

        jMenuItem83.setText("Manual");
        jMenuItem83.setToolTipText("View the usage guide");
        jMenuItem83.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem83ActionPerformed(evt);
            }
        });
        menuFile.add(jMenuItem83);

        jMenuItem88.setText("Feedback");
        jMenuItem88.setToolTipText("Write feedback to the authors");
        jMenuItem88.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem88ActionPerformed(evt);
            }
        });
        menuFile.add(jMenuItem88);

        jMenuItem80.setText("Logs");
        jMenuItem80.setToolTipText("Open the log file");
        jMenuItem80.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem80ActionPerformed(evt);
            }
        });
        menuFile.add(jMenuItem80);

        menuAbout.setText("About");
        menuAbout.setToolTipText("Author and version information");
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        menuFile.add(menuAbout);

        menuTongaExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_DOWN_MASK));
        menuTongaExit.setText("Exit");
        menuTongaExit.setToolTipText("Exit Tonga");
        menuTongaExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuTongaExitActionPerformed(evt);
            }
        });
        menuFile.add(menuTongaExit);

        menuBar.add(menuFile);
        Splash.append("Tonga menu",8);

        menuWizard.setForeground(Tonga.tongaBlue);
        menuWizard.setText("Wizard");
        menuWizard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                menuWizardMouseClicked(evt);
            }
        });
        menuBar.add(menuWizard);

        menuFilters.setText("Filters");

        jMenu12.setText("Basic");
        jMenu12.setToolTipText("Perform basic operations");

        jMenu24.setText("Multiply");

        menuMultiplyPxls.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        menuMultiplyPxls.setText("Multiply pixel values");
        menuMultiplyPxls.setToolTipText("Multiply raw pixel values of the image");
        menuMultiplyPxls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMultiplyPxlsActionPerformed(evt);
            }
        });
        jMenu24.add(menuMultiplyPxls);

        jMenuItem66.setText("Multiply colour and brightness");
        jMenuItem66.setToolTipText("Multiply colour and brightness of the image with a factor");
        jMenuItem66.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem66ActionPerformed(evt);
            }
        });
        jMenu24.add(jMenuItem66);

        jMenu12.add(jMenu24);

        jMenu26.setText("Extremize");

        jMenuItem30.setText("Extreme colour");
        jMenuItem30.setToolTipText("Make every pixel have maximum colour");
        jMenuItem30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem30ActionPerformed(evt);
            }
        });
        jMenu26.add(jMenuItem30);

        jMenuItem32.setText("Extreme colour & brightness");
        jMenuItem32.setToolTipText("Make every pixel completely bright and maximum colour");
        jMenuItem32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem32ActionPerformed(evt);
            }
        });
        jMenu26.add(jMenuItem32);

        jMenuItem31.setText("Extreme brightness");
        jMenuItem31.setToolTipText("Make every pixel completely bright");
        jMenuItem31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem31ActionPerformed(evt);
            }
        });
        jMenu26.add(jMenuItem31);

        jMenu12.add(jMenu26);

        jMenu5.setText("Alpha");
        jMenu5.setToolTipText("");

        menuAlphaBright.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        menuAlphaBright.setText("Darkness to alpha");
        menuAlphaBright.setToolTipText("Transform the darkness into the alpha, treating black as transparent and white as opaque");
        menuAlphaBright.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAlphaBrightActionPerformed(evt);
            }
        });
        jMenu5.add(menuAlphaBright);

        jMenuItem57.setText("Brightness to alpha");
        jMenuItem57.setToolTipText("Transform the brightness into the alpha, treating white as transparent and black as opaque");
        jMenuItem57.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem57ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem57);

        menuAlphaColor.setText("Convert colour to alpha");
        menuAlphaColor.setToolTipText("Transform the selected colour into the alpha channel");
        menuAlphaColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAlphaColorActionPerformed(evt);
            }
        });
        jMenu5.add(menuAlphaColor);

        menuOpaque.setText("Make opaque");
        menuOpaque.setToolTipText("Remove the alpha channel completely");
        menuOpaque.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpaqueActionPerformed(evt);
            }
        });
        jMenu5.add(menuOpaque);

        jMenu12.add(jMenu5);

        jMenu13.setText("Grayscale");
        jMenu13.setToolTipText("");

        menuMapLuminescence.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        menuMapLuminescence.setText("Brightness");
        menuMapLuminescence.setToolTipText("Convert the image to grayscale based on the brightest colour of a pixel");
        menuMapLuminescence.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMapLuminescenceActionPerformed(evt);
            }
        });
        jMenu13.add(menuMapLuminescence);

        menuGrayscale.setText("Luminance");
        menuGrayscale.setToolTipText("Convert the image to grayscale based on the relative luminance of all the colours");
        menuGrayscale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuGrayscaleActionPerformed(evt);
            }
        });
        jMenu13.add(menuGrayscale);

        menuLightness.setText("Lightness");
        menuLightness.setToolTipText("Convert the image to grayscale based on the combination of colour brightnesses of a pixel");
        menuLightness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLightnessActionPerformed(evt);
            }
        });
        jMenu13.add(menuLightness);

        jMenuItem45.setText("Saturation");
        jMenuItem45.setToolTipText("Convert the image to grayscale based on the colour saturation");
        jMenuItem45.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem45ActionPerformed(evt);
            }
        });
        jMenu13.add(jMenuItem45);

        menuMapHue.setText("Hue");
        menuMapHue.setToolTipText("Convert the image to grayscale based on the hue of the colour of a pixel");
        menuMapHue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMapHueActionPerformed(evt);
            }
        });
        jMenu13.add(menuMapHue);

        jMenu12.add(jMenu13);

        jMenuItem23.setText("Colorize");
        jMenuItem23.setToolTipText("Convert the image to a color by using the luminance");
        jMenuItem23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem23ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem23);

        menuInterpolate.setText("Reduce");
        menuInterpolate.setToolTipText("Reduce the depth of the brightness");
        menuInterpolate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuInterpolateActionPerformed(evt);
            }
        });
        jMenu12.add(menuInterpolate);

        jMenuItem100.setText("Average");
        jMenuItem100.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem100ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem100);

        jMenuItem110.setText("Colour sum");
        jMenuItem110.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem110ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem110);

        jMenuItem96.setText("Integral");
        jMenuItem96.setToolTipText("Create an integral image (sum-area table)");
        jMenuItem96.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem96ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem96);

        jMenuItem20.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem20.setText("Invert");
        jMenuItem20.setToolTipText("Invert the image values");
        jMenuItem20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem20ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem20);
        jMenu12.add(jSeparator7);

        jMenuItem62.setText("Replace a colour with another");
        jMenuItem62.setToolTipText("Pick a colour from the image and replace it with another colour");
        jMenuItem62.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem62ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem62);

        menuProtocolCopyMask.setText("Copy areas to another");
        menuProtocolCopyMask.setToolTipText("Pick a colour from the image and merge it to another image");
        menuProtocolCopyMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuProtocolCopyMaskActionPerformed(evt);
            }
        });
        jMenu12.add(menuProtocolCopyMask);

        jMenuItem35.setText("Extract a colour to a new layer");
        jMenuItem35.setToolTipText("Extract a certain colour from an image and place it into a new layer (as grayscale). The more similar a pixel colour will be to the selected colour, the brighter it will appear in the new layer.");
        jMenuItem35.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem35ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem35);

        jMenuItem47.setText("Separate ARGB into channels");
        jMenuItem47.setToolTipText("Separate the alpha, red, green, and blue channels");
        jMenuItem47.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem47ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem47);

        menuFilters.add(jMenu12);

        jMenu27.setText("Scaling");
        jMenu27.setToolTipText("Stretch the image values to a certain range");

        jMenu23.setText("High/low-speficic scaling");
        jMenu23.setToolTipText("");

        menuLoCut.setText("Limit between a range by scaling darkness");
        menuLoCut.setToolTipText("Set a minimum darkness and maximum brightness limit. Dark values will be scaled smoothly, bright values will be discarded without scaling.");
        menuLoCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoCutActionPerformed(evt);
            }
        });
        jMenu23.add(menuLoCut);

        menuHiCut.setText("Limit between a range by scaling brightness");
        menuHiCut.setToolTipText("Set a minimum darkness and maximum brightness limit. Bright values will be scaled smoothly, dark values will be discarded without scaling.");
        menuHiCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHiCutActionPerformed(evt);
            }
        });
        jMenu23.add(menuHiCut);
        jMenu23.add(jSeparator12);

        menuLoCutAuto.setText("Scale darkness to the highest peak");
        menuLoCutAuto.setToolTipText("Calculate the highest peak and make that the new darkest value");
        menuLoCutAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoCutAutoActionPerformed(evt);
            }
        });
        jMenu23.add(menuLoCutAuto);

        menuHiCutAuto.setText("Scale brightness to the highest peak");
        menuHiCutAuto.setToolTipText("Calculate the highest peak and make that the new brightest value");
        menuHiCutAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHiCutAutoActionPerformed(evt);
            }
        });
        jMenu23.add(menuHiCutAuto);
        jMenu23.add(jSeparator11);

        menuLoCutNoise.setText("Limit the darkest values");
        menuLoCutNoise.setToolTipText("Make almost black areas black by removing % of the darkest values");
        menuLoCutNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoCutNoiseActionPerformed(evt);
            }
        });
        jMenu23.add(menuLoCutNoise);

        menuHiCutPeak.setText("Limit the highest values");
        menuHiCutPeak.setToolTipText("Make brightest areas less bright by removing % of the brightest values");
        menuHiCutPeak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHiCutPeakActionPerformed(evt);
            }
        });
        jMenu23.add(menuHiCutPeak);

        jMenu27.add(jMenu23);

        jMenuItem34.setText("Scale brightness to range");
        jMenuItem34.setToolTipText("Set a minimum darkness and maximum brightness limit, and scale the pixel values to this range.");
        jMenuItem34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem34ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem34);

        jMenuItem41.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem41.setText("Scale brightness to maximum");
        jMenuItem41.setToolTipText("Same as above, but detect the minimum and maximum automatically, so that no data is lost.");
        jMenuItem41.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem41ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem41);

        jMenuItem60.setText("Scale brightness to maximum with adaptation");
        jMenuItem60.setToolTipText("Same as above, but ignore certain percentage of the darkest and brightest values to remove noise and bright dots");
        jMenuItem60.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem60ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem60);

        jMenuItem61.setText("Scale brightness to maximum with pixel limit");
        jMenuItem61.setToolTipText("Same as above, but use the absolute number of outlier pixels instead of percentage");
        jMenuItem61.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem61ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem61);

        jMenuItem92.setText("Scale to darkest values");
        jMenuItem92.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem92ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem92);

        jMenuItem94.setText("Balanced brightness scaling");
        jMenuItem94.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem94ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem94);

        jMenuItem16.setText("Scale colors to maximum");
        jMenuItem16.setToolTipText("Scale the colour values to maximize the saturation in the image");
        jMenuItem16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem16ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem16);

        menuFilters.add(jMenu27);

        jMenu14.setText("Blurring");
        jMenu14.setToolTipText("Apply blurring");

        jMenuItem64.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem64.setText("Gaussian blur (approximation)");
        jMenuItem64.setToolTipText("Gaussian blurring approximated by multiple passes of box blur");
        jMenuItem64.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem64ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem64);

        jMenuItem59.setText("Gaussian blur (precise)");
        jMenuItem59.setToolTipText("Precise Gaussian blurring without approximation (please note this is <font color=\"red\">slow</font>)");
        jMenuItem59.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem59ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem59);
        jMenu14.add(jSeparator6);

        menuBoxBlur.setText("Box blur");
        menuBoxBlur.setToolTipText("Implementation of the box blur in a given radius");
        menuBoxBlur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuBoxBlurActionPerformed(evt);
            }
        });
        jMenu14.add(menuBoxBlur);

        jMenuItem91.setText("Conditional blur");
        jMenuItem91.setToolTipText("Radial blurring using only pixels other than the defined background colour");
        jMenuItem91.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem91ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem91);

        jMenuItem49.setText("Brightness blur");
        jMenuItem49.setToolTipText("Mean value of the brightness on neighbouring pixels");
        jMenuItem49.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem49ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem49);

        menuFilters.add(jMenu14);

        jMenu15.setText("Thresholding");
        jMenu15.setToolTipText("Binarize the image");

        jMenuItem29.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem29.setText("Global Threshold");
        jMenuItem29.setToolTipText("Global thresholding for the image brightness");
        jMenuItem29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem29ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem29);

        jMenuItem27.setText("Global Relative Threshold");
        jMenuItem27.setToolTipText("Global thresholding for the relative luminance (what the human eye sees)");
        jMenuItem27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem27ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem27);

        jMenuItem26.setText("Global Lightness Threshold");
        jMenuItem26.setToolTipText("Global thresholding for the average of brightness and darkness");
        jMenuItem26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem26ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem26);

        jMenuItem13.setText("Global Double Threshold");
        jMenuItem13.setToolTipText("Global thresholding for the image brightness, with three levels instead of two");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem13);

        jMenuItem99.setText("Global Masked Threshold");
        jMenuItem99.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem99ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem99);

        jMenuItem105.setText("Global RGB Threshold");
        jMenuItem105.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem105ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem105);
        jMenu15.add(jSeparator13);

        jMenuItem89.setText("Local Area Threshold");
        jMenuItem89.setToolTipText("Local illumination-sensitive thresholding for a specific area");
        jMenuItem89.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem89ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem89);

        jMenuItem51.setText("Local Threshold");
        jMenuItem51.setToolTipText("Local thresholding for the image brightness with mean");
        jMenuItem51.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem51ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem51);

        jMenuItem52.setText("Local Niblack Threshold");
        jMenuItem52.setToolTipText("Local thresholding for the image brightness with mean and standard deviation");
        jMenuItem52.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem52ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem52);

        menuFilters.add(jMenu15);

        jMenu6.setText("Correcting");

        menuGamma.setText("Gamma correction");
        menuGamma.setToolTipText("Gamma correction for the image");
        menuGamma.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuGammaActionPerformed(evt);
            }
        });
        jMenu6.add(menuGamma);

        jMenuItem56.setText("Illumination correction");
        jMenuItem56.setToolTipText("Correct uneven background lighting");
        jMenuItem56.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem56ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem56);

        jMenuItem87.setText("Background correction subtract");
        jMenuItem87.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem87ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem87);

        jMenuItem102.setText("Background correction scale");
        jMenuItem102.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem102ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem102);

        jMenuItem108.setText("Background correction gradient");
        jMenuItem108.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem108ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem108);

        jMenuItem90.setText("Fixation correction nonlinear");
        jMenuItem90.setToolTipText("Correct uneven staining for multichannel images due to uneven fixation/deparaffinization etc. using a nonlinear DAPI normalization");
        jMenuItem90.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem90ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem90);

        jMenuItem97.setText("Fixation correction linear");
        jMenuItem97.setToolTipText("Correct uneven staining for multichannel images due to uneven fixation/deparaffinization etc. using linear normalization");
        jMenuItem97.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem97ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem97);

        jMenuItem101.setText("Overlap correction");
        jMenuItem101.setToolTipText("Correct bright staining artefacts due to tissue overlap on a multichannel fluorescent image <font color=\"red\">after correcting for background staining and uneven staining</font>");
        jMenuItem101.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem101ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem101);

        jMenuItem70.setText("Halo correction");
        jMenuItem70.setToolTipText("Correct undesired halo effects in fluorescent cell images");
        jMenuItem70.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem70ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem70);
        jMenu6.add(jSeparator19);

        jMenuItem93.setText("Manual correction");
        jMenuItem93.setToolTipText("Correct image by applying a custom correction map");
        jMenuItem93.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem93ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem93);

        menuFilters.add(jMenu6);

        jMenu1.setText("Segmenting");

        jMenuItem106.setText("Serial segmentation");
        jMenuItem106.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem106ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem106);

        jMenuItem95.setText("Two-step segmentation");
        jMenuItem95.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem95ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem95);

        menuFilters.add(jMenu1);

        jMenu11.setText("Edge detection");
        jMenu11.setToolTipText("Methods for edge detection");

        jMenuItem10.setText("Maximum difference");
        jMenuItem10.setToolTipText("Maximum difference in the neighbouring pixels");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu11.add(jMenuItem10);

        jMenuItem50.setText("Standard deviation");
        jMenuItem50.setToolTipText("Standard deviation of the brightness in the neighbouring pixels");
        jMenuItem50.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem50ActionPerformed(evt);
            }
        });
        jMenu11.add(jMenuItem50);
        jMenu11.add(jSeparator14);

        menuDoG.setText("Difference of Gaussians");
        menuDoG.setToolTipText("Two Gaussian blurs are substracted from each other to produce a detection for objects");
        menuDoG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDoGActionPerformed(evt);
            }
        });
        jMenu11.add(menuDoG);

        menuDoB.setText("Difference of boxes");
        menuDoB.setToolTipText("Two passes of box blur are substracted from each other to produce a detection for objects");
        menuDoB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDoBActionPerformed(evt);
            }
        });
        jMenu11.add(menuDoB);

        menuFilters.add(jMenu11);
        menuFilters.add(jSeparator10);

        jMenu10.setText("Binary images");
        jMenu10.setToolTipText("Operations for binarized images");

        jMenu20.setText("Remove");

        jMenuItem53.setText("Solitary dots");
        jMenuItem53.setToolTipText("Remove single pixels which are surrounded by background on all sides");
        jMenuItem53.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem53ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem53);

        menuDotRemove.setText("Connected dots");
        menuDotRemove.setToolTipText("Remove pixels which are surrounded by background pixels on both sides vertically or horizontally");
        menuDotRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDotRemoveActionPerformed(evt);
            }
        });
        jMenu20.add(menuDotRemove);

        menuFillArea.setText("Inner holes (all)");
        menuFillArea.setToolTipText("Remove holes inside the objects");
        menuFillArea.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFillAreaActionPerformed(evt);
            }
        });
        jMenu20.add(menuFillArea);

        jMenuItem43.setText("Inner holes (by shape and size)");
        jMenuItem43.setToolTipText("Remove holes inside the objects by defining a minimum size and minimum roundness");
        jMenuItem43.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem43ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem43);

        jMenuItem86.setText("Inner holes (by intensity)");
        jMenuItem86.setToolTipText("Remove inner holes if the hole area intensity on another image is above a limit");
        jMenuItem86.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem86ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem86);

        jMenuItem40.setText("Inner holes on object edges");
        jMenuItem40.setToolTipText("Remove small circular holes breaking the object edges, while not being completely inside the objects");
        jMenuItem40.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem40ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem40);

        menuFilterSmallSize.setText("Small objects (by area size)");
        menuFilterSmallSize.setToolTipText("Remove small objects by defining a minimum size");
        menuFilterSmallSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFilterSmallSizeActionPerformed(evt);
            }
        });
        jMenu20.add(menuFilterSmallSize);

        menuFilterSmallDimension.setText("Small objects (by dimensions)");
        menuFilterSmallDimension.setToolTipText("Remove small objects by defining minimum dimensions");
        menuFilterSmallDimension.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFilterSmallDimensionActionPerformed(evt);
            }
        });
        jMenu20.add(menuFilterSmallDimension);

        jMenuItem5.setText("Small objects (by size and dimensions)");
        jMenuItem5.setToolTipText("Combination of the two options above");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem5);

        jMenuItem67.setText("Small objects (by shape and size)");
        jMenuItem67.setToolTipText("Remove small objects by defining a minimum size and minimum roundness");
        jMenuItem67.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem67ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem67);

        jMenuItem85.setText("Dim objects (by intensity)");
        jMenuItem85.setToolTipText("Remove objects if the object area intensity on another image is below a limit");
        jMenuItem85.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem85ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem85);

        jMenuItem77.setText("Objects touching the edges");
        jMenuItem77.setToolTipText("Remove objects if they are touching the image edges");
        jMenuItem77.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem77ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem77);

        jMenu10.add(jMenu20);

        jMenu21.setText("Morphology");

        menuSpreadEdge.setText("Dilate");
        menuSpreadEdge.setToolTipText("Extend the edges of the objects");
        menuSpreadEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSpreadEdgeActionPerformed(evt);
            }
        });
        jMenu21.add(menuSpreadEdge);

        menuShrinkEdge.setText("Erode");
        menuShrinkEdge.setToolTipText("Reduce the edges of the objects");
        menuShrinkEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuShrinkEdgeActionPerformed(evt);
            }
        });
        jMenu21.add(menuShrinkEdge);

        menuSharpCorner.setText("Sharpen");
        menuSharpCorner.setToolTipText("Sharpen edges of the objects");
        menuSharpCorner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSharpCornerActionPerformed(evt);
            }
        });
        jMenu21.add(menuSharpCorner);

        menuFillGap.setText("Connect edges");
        menuFillGap.setToolTipText("Connect closeby pixels");
        menuFillGap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFillGapActionPerformed(evt);
            }
        });
        jMenu21.add(menuFillGap);

        jMenuItem4.setText("Connect long shapes");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu21.add(jMenuItem4);

        jMenuItem24.setText("Smoothen edges");
        jMenuItem24.setToolTipText("Smoothen edges of the objects by removing sharp pixels");
        jMenuItem24.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem24ActionPerformed(evt);
            }
        });
        jMenu21.add(jMenuItem24);

        menuSmoothen.setText("Smoothen corners");
        menuSmoothen.setToolTipText("Smoothen edges of the objects by removing sharp corners");
        menuSmoothen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSmoothenActionPerformed(evt);
            }
        });
        jMenu21.add(menuSmoothen);

        jMenuItem17.setText("Smoothen iteratively");
        jMenuItem17.setToolTipText("Perform several smoothing operations iteratively");
        jMenuItem17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem17ActionPerformed(evt);
            }
        });
        jMenu21.add(jMenuItem17);

        jMenu10.add(jMenu21);

        jMenu19.setText("Find");

        jMenuItem69.setText("Edges");
        jMenuItem69.setToolTipText("Highlight the object edges (outer and inner) with colour");
        jMenuItem69.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem69ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem69);

        jMenuItem25.setText("Distance to the edges");
        jMenuItem25.setToolTipText("Create a distance map by calculating the distance to the closest edge for every pixel");
        jMenuItem25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem25ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem25);

        jMenuItem73.setText("Extended edges");
        jMenuItem73.setToolTipText("Dilate the objects on a certain radius and render this dilated area");
        jMenuItem73.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem73ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem73);

        jMenuItem58.setText("Inner areas");
        jMenuItem58.setToolTipText("Only get the areas located completely inside other objects");
        jMenuItem58.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem58ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem58);

        jMenuItem75.setText("Object overlap");
        jMenuItem75.setToolTipText("Find the overlapping area between two objects within a certain radius from their edges");
        jMenuItem75.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem75ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem75);

        jMenuItem15.setText("Separated objects");
        jMenuItem15.setToolTipText("Identify objects which have been separated compared to an unseparated image");
        jMenuItem15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem15ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem15);

        jMenuItem63.setText("Common objects");
        jMenuItem63.setToolTipText("Identify objects which are present on both of two images");
        jMenuItem63.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem63ActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItem63);

        jMenu10.add(jMenu19);
        jMenu10.add(jSeparator5);

        jMenuItem84.setText("Segment");
        jMenuItem84.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem84ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem84);

        menuFilters.add(jMenu10);

        jMenu17.setText("Merge images");
        jMenu17.setToolTipText("Perform operations with multiple images");

        jMenuItem46.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem46.setText("Render a stack");
        jMenuItem46.setToolTipText("Merge the currently selected channels with the currently selected stack mode");
        jMenuItem46.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem46ActionPerformed(evt);
            }
        });
        jMenu17.add(jMenuItem46);

        jMenuItem54.setText("Blend image with another");
        jMenuItem54.setToolTipText("Render two images on top of each other in a certain way");
        jMenuItem54.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem54ActionPerformed(evt);
            }
        });
        jMenu17.add(jMenuItem54);

        menuFilters.add(jMenu17);

        menuBar.add(menuFilters);
        Splash.append("Filters menu",8);

        menuProtocols.setText("Protocols");

        jMenu3.setText("For tissues");

        jMenuItem98.setText("IF tissue intensities");
        jMenuItem98.setToolTipText("Perform a multichannel normalization for background staining, uneven illumination, and staining artefacts all at once, on fluorescent tissue images");
        jMenuItem98.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem98ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem98);


        menuProtocols.add(jMenu3);

        jMenu7.setText("For nuclei");

        jMenuItem18.setText("Count the number of nuclei");
        jMenuItem18.setToolTipText("Nuclei from a fluorescent nuclear dye image are detected, and their number is counted");
        jMenuItem18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem18ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem18);

        jMenuItem14.setText("Count the ratio of positive nuclei");
        jMenuItem14.setToolTipText("Nuclei from a fluorescent nuclear dye image are detected and counted, and the ratio of nuclei positive for another dye is measured");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem14);

        jMenuItem21.setText("Measure staining intensity");
        jMenuItem21.setToolTipText("Nuclei from a fluorescent nuclear dye image are detected, and the intensity of another dye on the nucleus area is measured");
        jMenuItem21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem21ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem21);

        jMenuItem11.setText("Measure double-staining intensity");
        jMenuItem11.setToolTipText("Nuclei from a fluorescent nuclear dye image are detected, and the intensity of another dye on the nucleus area which is also positive for a third dye is measured");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem11);

        jMenuItem1.setText("Measure surrounding intensity");
        jMenuItem1.setToolTipText("Nuclei from a fluorescent nuclear dye image are detected, and the intensity of another dye on the area around the nucleus on a certain radius is measured");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem1);
        jMenu7.add(jSeparator16);

        jMenuItem8.setText("Measure background staining");
        jMenuItem8.setToolTipText("Background area is detected and its average intensity measured");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem8);

        jMenuItem71.setText("Estimate the average nucleus size");
        jMenuItem71.setToolTipText("Estimate the average size of nuclei in the image as pixel diameter");
        jMenuItem71.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem71ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem71);

        menuProtocols.add(jMenu7);

        jMenu8.setText("For masks");

        jMenu9.setText("Count");

        jMenuItem19.setText("Count objects");
        jMenuItem19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem19ActionPerformed(evt);
            }
        });
        jMenu9.add(jMenuItem19);

        jMenuItem78.setText("Count dead and dividing cells");
        jMenuItem78.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem78ActionPerformed(evt);
            }
        });
        jMenu9.add(jMenuItem78);

        jMenuItem33.setText("Segment and count objects");
        jMenuItem33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem33ActionPerformed(evt);
            }
        });
        jMenu9.add(jMenuItem33);

        jMenu8.add(jMenu9);

        jMenu16.setText("Measure");

        jMenuItem42.setText("Channel intensity on the object area");
        jMenuItem42.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem42ActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItem42);

        jMenuItem12.setText("Channel double-intensity on the object area");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem12ActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItem12);

        jMenuItem72.setText("Channel intensity on the object surroundings");
        jMenuItem72.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem72ActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItem72);

        jMenuItem74.setText("Colour intensity of the object itself");
        jMenuItem74.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem74ActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItem74);

        jMenu8.add(jMenu16);

        menuProtocols.add(jMenu8);
        menuProtocols.add(jSeparator2);

        jMenu2.setText("Untested");
        jMenu2.setToolTipText("Potentially unstable protocols in development");

        jMenu4.setText("Experiment image analysis");

        menuProtocolIFMask.setText("IF staining");
        menuProtocolIFMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuProtocolIFMaskActionPerformed(evt);
            }
        });
        jMenu4.add(menuProtocolIFMask);

        jMenuItem48.setText("Oligodendrocyte branching");
        jMenuItem48.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem48ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem48);

        jMenuItem39.setText("Tissue separation & stain calculation");
        jMenuItem39.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem39ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem39);

        jMenuItem2.setText("HE tissue separation");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem2);

        jMenuItem68.setText("In Situ -hybridisation");
        jMenuItem68.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem68ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem68);

        jMenuItem9.setText("Intestinal crypt cells");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem9);

        jMenuItem22.setText("Matrigel organoids");
        jMenuItem22.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem22ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem22);

        jMenuItem111.setText("Organoids");
        jMenuItem111.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem111ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem111);

        menuProtocolNucleusSep.setText("Nucleus (+EdU) recognition");
        menuProtocolNucleusSep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuProtocolNucleusSepActionPerformed(evt);
            }
        });
        jMenu4.add(menuProtocolNucleusSep);

        jMenu2.add(jMenu4);

        jMenu18.setText("Object regcognition and counting");

        jMenuItem44.setText("Count areas around objects");
        jMenuItem44.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem44ActionPerformed(evt);
            }
        });
        jMenu18.add(jMenuItem44);

        jMenuItem37.setText("Count colored areas");
        jMenuItem37.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem37ActionPerformed(evt);
            }
        });
        jMenu18.add(jMenuItem37);

        jMenu2.add(jMenu18);

        menuProtocols.add(jMenu2);

        menuBar.add(menuProtocols);
        Splash.append("Protocol menu",3);

        menuCounting.setText("Counting");

        jMenu22.setText("Pixel counters");

        menuCountRGB.setText("Count the pixels of basic colours");
        menuCountRGB.setToolTipText("Count the number of pixels for each basic colour (Red, Green, Blue, Cyan, Magenta, Yellow, Black, White)");
        menuCountRGB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCountRGBActionPerformed(evt);
            }
        });
        jMenu22.add(menuCountRGB);

        jMenuItem104.setText("Count RGB channel intensities on area");
        jMenuItem104.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem104ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem104);

        jMenuItem103.setText("Count RGB binarized channel unity on area");
        jMenuItem103.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem103ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem103);

        jMenuItem65.setText("Count red channel intensity on area");
        jMenuItem65.setToolTipText("Count the non-black area and the intensity of the red channel in it");
        jMenuItem65.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem65ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem65);

        jMenuItem55.setText("Count background intensity on area");
        jMenuItem55.setToolTipText("Count the background (non-white) area and the intensity in it");
        jMenuItem55.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem55ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem55);
        jMenu22.add(jSeparator25);

        jMenuItem28.setText("Histogram distributions");
        jMenuItem28.setToolTipText("Count the number of pixels for each channel for a histogram");
        jMenuItem28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem28ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem28);

        jMenuItem139.setText("RGBYCM distributions");
        jMenuItem139.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem139ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem139);

        jMenuItem137.setText("Intensity distributions");
        jMenuItem137.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem137ActionPerformed(evt);
            }
        });
        jMenu22.add(jMenuItem137);

        menuCounting.add(jMenu22);

        jMenuItem138.setText("Transpose by image");
        jMenuItem138.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem138ActionPerformed(evt);
            }
        });
        menuCounting.add(jMenuItem138);

        menuBar.add(menuCounting);
        Splash.append("Counter menu");

        menuDebug.setText("Debug");

        debugMemory.setText("Free unused memory");
        debugMemory.setToolTipText("???");
        debugMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugMemoryActionPerformed(evt);
            }
        });
        menuDebug.add(debugMemory);

        debugSysInfo.setText("Show system information");
        debugSysInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugSysInfoActionPerformed(evt);
            }
        });
        menuDebug.add(debugSysInfo);

        jMenuItem79.setText("Enable detailed tracing");
        jMenuItem79.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem79ActionPerformed(evt);
            }
        });
        menuDebug.add(jMenuItem79);
        menuDebug.add(jSeparator17);

        jMenuItem6.setText("Execute a debug function");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        menuDebug.add(jMenuItem6);

        debugParameter.setText("Create a sample parameter panel");
        debugParameter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugParameterActionPerformed(evt);
            }
        });
        menuDebug.add(debugParameter);

        jMenuItem82.setText("Create a fake results table");
        jMenuItem82.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem82ActionPerformed(evt);
            }
        });
        menuDebug.add(jMenuItem82);
        menuDebug.add(jSeparator15);

        debugExecuteClass.setText("Execute protocol by name");
        debugExecuteClass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugExecuteClassActionPerformed(evt);
            }
        });
        menuDebug.add(debugExecuteClass);

        jMenuItem38.setText("Execute filter by name");
        jMenuItem38.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem38ActionPerformed(evt);
            }
        });
        menuDebug.add(jMenuItem38);

        debugTestFilter.setText("Run a test filter");
        debugTestFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugTestFilterActionPerformed(evt);
            }
        });
        menuDebug.add(debugTestFilter);

        debugTestProtocol.setText("Run a test protocol");
        debugTestProtocol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugTestProtocolActionPerformed(evt);
            }
        });
        menuDebug.add(debugTestProtocol);

        debugTestProtocols.setText("Run debug protocols");

        jMenuItem3.setText("Create nucleus mask");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        debugTestProtocols.add(jMenuItem3);

        jMenuItem36.setText("Segment cells at mask");
        jMenuItem36.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem36ActionPerformed(evt);
            }
        });
        debugTestProtocols.add(jMenuItem36);

        jMenuItem76.setText("Segment cells at mask (strict)");
        jMenuItem76.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem76ActionPerformed(evt);
            }
        });
        debugTestProtocols.add(jMenuItem76);

        jMenuItem7.setText("Construct the final mask");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        debugTestProtocols.add(jMenuItem7);

        jCheckBoxMenuItem1.setText("Override nucleus size estimation");
        debugTestProtocols.add(jCheckBoxMenuItem1);

        jMenuItem81.setText("Difference reflection");
        jMenuItem81.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem81ActionPerformed(evt);
            }
        });
        debugTestProtocols.add(jMenuItem81);

        menuDebug.add(debugTestProtocols);

        menuBar.add(menuDebug);
        Splash.append("Debug menu");

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(frame, javax.swing.GroupLayout.DEFAULT_SIZE, 1450, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(6, 6, 6))
            .addComponent(jSeparator1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(bleftLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(brightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(frame, javax.swing.GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(brightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bleftLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6))
        );

        Splash.append("Main frame");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuMapLuminescenceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMapLuminescenceActionPerformed
        launchFilter(Filters::bwBrightness, evt);
    }//GEN-LAST:event_menuMapLuminescenceActionPerformed

    private void btnRunSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunSingleActionPerformed
        if (currentCounter != null && currentProtocol == null) {
            executeCounter(false);
        } else {
            executeProtocol(false);
        }
    }//GEN-LAST:event_btnRunSingleActionPerformed

    private void imageZoomComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_imageZoomComponentResized
        refresh();
    }//GEN-LAST:event_imageZoomComponentResized

    private void btnRunAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunAllActionPerformed
        if (currentCounter != null && currentProtocol == null) {
            executeCounter(true);
        } else {
            executeProtocol(true);
        }
    }//GEN-LAST:event_btnRunAllActionPerformed

    private void menuFileImportImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportImagesActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importImages(Arrays.asList(files));
        }
    }//GEN-LAST:event_menuFileImportImagesActionPerformed

    private void menuFileImportLayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportLayersActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importLayers(Arrays.asList(files), true);
        }
    }//GEN-LAST:event_menuFileImportLayersActionPerformed

    private void menuExportAllLayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportAllLayersActionPerformed
        IO.exportLayer(true);
    }//GEN-LAST:event_menuExportAllLayersActionPerformed

    private void menuTongaExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTongaExitActionPerformed
        Tonga.cleanAndShutDown();
    }//GEN-LAST:event_menuTongaExitActionPerformed

    private void protocolSettingsPanelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_protocolSettingsPanelComponentResized
        Component[] child = protocolSettingsPanel.getComponents();

        if (child.length > 0 && child[0].getClass() == JPanel.class) {
            child[0].setSize(protocolSettingsPanel.getWidth() - 4, protocolSettingsPanel.getHeight() - 4);
        }
    }//GEN-LAST:event_protocolSettingsPanelComponentResized

    private void menuExportSingleLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportSingleLayerActionPerformed
        IO.exportLayer(false);
    }//GEN-LAST:event_menuExportSingleLayerActionPerformed

    private void contLayRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayRenameActionPerformed
        Tonga.renameLayers();
    }//GEN-LAST:event_contLayRenameActionPerformed

    private void contLaySelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLaySelectActionPerformed
        Tonga.setLayerSelectionToAllImages();
    }//GEN-LAST:event_contLaySelectActionPerformed

    private void contLayDeleteAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayDeleteAllActionPerformed
        Tonga.removeLayers();
    }//GEN-LAST:event_contLayDeleteAllActionPerformed

    private void exportAsCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAsCSVActionPerformed
        IO.exportTable(false);
    }//GEN-LAST:event_exportAsCSVActionPerformed

    private void openExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExcelActionPerformed
        IO.exportTable(true);
    }//GEN-LAST:event_openExcelActionPerformed

    private void layerBackColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layerBackColorActionPerformed
        PanelUtils.colourSelect((Component) evt.getSource());
        refresh();
    }//GEN-LAST:event_layerBackColorActionPerformed

    private void menuAlphaColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAlphaColorActionPerformed
        launchFilter(Filters::alpha, evt);
    }//GEN-LAST:event_menuAlphaColorActionPerformed

    private void layersListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layersListMouseReleased
        if (Tonga.thereIsImage() && evt.getButton() == MouseEvent.BUTTON3) {
            if (Tonga.fullLayerIndexCount() == 1) {
                contLayMerge.setText("Make a copy");
            } else {
                contLayMerge.setText("Merge into one");
            }
            contextLayerMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_layersListMouseReleased

    private void contImgRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgRenameActionPerformed
        Tonga.renameImage();
    }//GEN-LAST:event_contImgRenameActionPerformed

    private void imagesListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_imagesListMouseReleased
        if (Tonga.thereIsImage() && evt.getButton() == MouseEvent.BUTTON3) {
            contextImageMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_imagesListMouseReleased

    private void contLayDeleteThisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayDeleteThisActionPerformed
        Tonga.removeLayer();
    }//GEN-LAST:event_contLayDeleteThisActionPerformed

    private void menuCountRGBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCountRGBActionPerformed
        launchCounter(Counters::countRGBCMYK, evt);
    }//GEN-LAST:event_menuCountRGBActionPerformed

    private void menuExportSingleImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportSingleImageActionPerformed
        IO.exportLayers(false);
    }//GEN-LAST:event_menuExportSingleImageActionPerformed

    private void menuExportAllImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportAllImagesActionPerformed
        IO.exportLayers(true);
    }//GEN-LAST:event_menuExportAllImagesActionPerformed

    private void contImgDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgDeleteActionPerformed
        Tonga.removeImage();
    }//GEN-LAST:event_contImgDeleteActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        launchFilter(Filters::maximumDiffEdge, evt);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void menuInterpolateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuInterpolateActionPerformed
        launchFilter(Filters::reduceBrightDepth, evt);
    }//GEN-LAST:event_menuInterpolateActionPerformed

    private void jMenuItem16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem16ActionPerformed
        launchFilter(Filters::saturate, evt);
    }//GEN-LAST:event_jMenuItem16ActionPerformed

    private void menuFillGapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFillGapActionPerformed
        launchFilter(Filters::connectEdges, evt);
    }//GEN-LAST:event_menuFillGapActionPerformed

    private void jMenuItem20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem20ActionPerformed
        launchFilter(Filters::invert, evt);
    }//GEN-LAST:event_jMenuItem20ActionPerformed

    private void menuSpreadEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSpreadEdgeActionPerformed
        launchFilter(FiltersPass::edgeDilate, evt);
    }//GEN-LAST:event_menuSpreadEdgeActionPerformed

    private void menuGammaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuGammaActionPerformed
        launchFilter(Filters::gamma, evt);
    }//GEN-LAST:event_menuGammaActionPerformed

    private void menuLoCutNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLoCutNoiseActionPerformed
        launchFilter(Filters::crapCleaner, evt);
    }//GEN-LAST:event_menuLoCutNoiseActionPerformed

    private void jMenuItem25ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem25ActionPerformed
        launchFilter(Filters::distanceTransform, evt);
    }//GEN-LAST:event_jMenuItem25ActionPerformed

    private void menuShrinkEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuShrinkEdgeActionPerformed
        launchFilter(FiltersPass::edgeErode, evt);
    }//GEN-LAST:event_menuShrinkEdgeActionPerformed

    private void menuSharpCornerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSharpCornerActionPerformed
        launchFilter(Filters::sharpenEdges, evt);
    }//GEN-LAST:event_menuSharpCornerActionPerformed

    private void jMenuItem28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem28ActionPerformed
        //executeCounter(Counters.analHisto(), Quantitizer::runSingle);
        launchCounter(Counters::analHisto, evt);
    }//GEN-LAST:event_jMenuItem28ActionPerformed

    private void menuFillAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFillAreaActionPerformed
        launchFilter(FiltersSet::fillInnerAreas, evt);
    }//GEN-LAST:event_menuFillAreaActionPerformed

    private void jMenuItem30ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem30ActionPerformed
        launchFilter(Filters::hueExtreme, evt);
    }//GEN-LAST:event_jMenuItem30ActionPerformed

    private void jMenuItem32ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem32ActionPerformed
        launchFilter(Filters::hueBrightExtreme, evt);
    }//GEN-LAST:event_jMenuItem32ActionPerformed

    private void jMenuItem31ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem31ActionPerformed
        launchFilter(Filters::brightExtreme, evt);
    }//GEN-LAST:event_jMenuItem31ActionPerformed

    private void jMenuItem35ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem35ActionPerformed
        launchFilter(Filters::popColour, evt);
    }//GEN-LAST:event_jMenuItem35ActionPerformed

    private void menuMapHueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMapHueActionPerformed
        launchFilter(Filters::bwHue, evt);
    }//GEN-LAST:event_menuMapHueActionPerformed

    private void menuGrayscaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuGrayscaleActionPerformed
        launchFilter(Filters::bwLuminance, evt);
    }//GEN-LAST:event_menuGrayscaleActionPerformed

    private void menuDoBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDoBActionPerformed
        launchFilter(Filters::dob, evt);
    }//GEN-LAST:event_menuDoBActionPerformed

    private void menuHiCutPeakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHiCutPeakActionPerformed
        launchFilter(Filters::hiCleaner, evt);
    }//GEN-LAST:event_menuHiCutPeakActionPerformed

    private void menuDotRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDotRemoveActionPerformed
        launchFilter(Filters::dotConnectRemove, evt);
    }//GEN-LAST:event_menuDotRemoveActionPerformed

    private void menuDoGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDoGActionPerformed
        launchFilter(Filters::dog, evt);
    }//GEN-LAST:event_menuDoGActionPerformed

    private void btnRunSingle2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunSingle2ActionPerformed
        executeFilter(false);
    }//GEN-LAST:event_btnRunSingle2ActionPerformed

    private void btnRunAll2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunAll2ActionPerformed
        executeFilter(true);
    }//GEN-LAST:event_btnRunAll2ActionPerformed

    private void menuBoxBlurActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuBoxBlurActionPerformed
        launchFilter(Filters::box, evt);
    }//GEN-LAST:event_menuBoxBlurActionPerformed

    private void debugMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugMemoryActionPerformed
        long mem = Runtime.getRuntime().totalMemory();
        MappingManager.freeMemory();
        long memn = Runtime.getRuntime().totalMemory();
        Tonga.log.debug("Initial memory: {} MB", (mem / 1000000));
        Tonga.log.debug("Current memory: {} MB", (memn / 1000000));
        Tonga.log.debug("Freed memory: {} MB", ((mem - memn) / 1000000));
    }//GEN-LAST:event_debugMemoryActionPerformed

    private void jMenuItem24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem24ActionPerformed
        launchFilter(Filters::evenEdges, evt);
    }//GEN-LAST:event_jMenuItem24ActionPerformed

    private void jMenuItem26ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem26ActionPerformed
        launchFilter(Filters::thresholdLight, evt);
    }//GEN-LAST:event_jMenuItem26ActionPerformed

    private void jMenuItem27ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem27ActionPerformed
        launchFilter(Filters::thresholdBiol, evt);
    }//GEN-LAST:event_jMenuItem27ActionPerformed

    private void jMenuItem29ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem29ActionPerformed
        launchFilter(Filters::thresholdBright, evt);
    }//GEN-LAST:event_jMenuItem29ActionPerformed

    private void menuSmoothenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSmoothenActionPerformed
        launchFilter(Filters::smoothenCorners, evt);
    }//GEN-LAST:event_menuSmoothenActionPerformed

    private void menuProtocolCopyMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuProtocolCopyMaskActionPerformed
        launchProtocol(CopyAreas::new, evt);
    }//GEN-LAST:event_menuProtocolCopyMaskActionPerformed

    private void jMenuItem22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem22ActionPerformed
        launchProtocol(MatrigelOrganoids::new, evt);
    }//GEN-LAST:event_jMenuItem22ActionPerformed

    private void menuProtocolIFMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuProtocolIFMaskActionPerformed
        launchProtocol(IFTissueStaining::new, evt);
    }//GEN-LAST:event_menuProtocolIFMaskActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        launchProtocol(CryptCells::new, evt);
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void menuProtocolNucleusSepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuProtocolNucleusSepActionPerformed
        launchProtocol(NucleusEdUCounter::new, evt);
    }//GEN-LAST:event_menuProtocolNucleusSepActionPerformed

    private void jMenuItem37ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem37ActionPerformed
        launchProtocol(CountColoredObjects::new, evt);
    }//GEN-LAST:event_jMenuItem37ActionPerformed

    private void jMenuItem44ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem44ActionPerformed
        launchProtocol(AreaAroundObject::new, evt);
    }//GEN-LAST:event_jMenuItem44ActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        getFilepath();
    }//GEN-LAST:event_browseButtonActionPerformed

    private void menuAlphaBrightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAlphaBrightActionPerformed
        launchFilter(Filters::alphaDark, evt);
    }//GEN-LAST:event_menuAlphaBrightActionPerformed

    private void menuExportSingleStackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportSingleStackActionPerformed
        IO.exportStack(false);
    }//GEN-LAST:event_menuExportSingleStackActionPerformed

    private void menuExportAllStacksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportAllStacksActionPerformed
        IO.exportStack(true);
    }//GEN-LAST:event_menuExportAllStacksActionPerformed

    private void browseFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseFilterActionPerformed
        filterMenu.show((Component) evt.getSource(), ((JButton) evt.getSource()).getX(), ((JButton) evt.getSource()).getY());
    }//GEN-LAST:event_browseFilterActionPerformed

    private void filterMenuPopupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_filterMenuPopupMenuCanceled

    }//GEN-LAST:event_filterMenuPopupMenuCanceled

    private void jMenuItem46ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem46ActionPerformed
        launchFilter(FiltersRender::blendStackCurrent, evt);
    }//GEN-LAST:event_jMenuItem46ActionPerformed

    private void menuFilterSmallSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFilterSmallSizeActionPerformed
        launchFilter(FiltersSet::filterObjectSize, evt);
    }//GEN-LAST:event_menuFilterSmallSizeActionPerformed

    private void menuFilterSmallDimensionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFilterSmallDimensionActionPerformed
        launchFilter(FiltersSet::filterObjectDimension, evt);
    }//GEN-LAST:event_menuFilterSmallDimensionActionPerformed

    private void jMenuItem47ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem47ActionPerformed
        launchProtocol(BreakImageRGB::new, evt);
    }//GEN-LAST:event_jMenuItem47ActionPerformed

    private void stackToggleMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stackToggleMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON1) {
            Tonga.stackMode(false);
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            Tonga.stackMode(true);
        }
    }//GEN-LAST:event_stackToggleMouseReleased

    private void jMenuItem48ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem48ActionPerformed
        launchProtocol(OligoDendroBranch::new, evt);
    }//GEN-LAST:event_jMenuItem48ActionPerformed

    private void jMenuItem49ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem49ActionPerformed
        launchFilter(Filters::meanBrightness, evt);
    }//GEN-LAST:event_jMenuItem49ActionPerformed

    private void jMenuItem50ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem50ActionPerformed
        launchFilter(Filters::stdBrightness, evt);
    }//GEN-LAST:event_jMenuItem50ActionPerformed

    private void jMenuItem51ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem51ActionPerformed
        launchFilter(Filters::localThreshold, evt);
    }//GEN-LAST:event_jMenuItem51ActionPerformed

    private void jMenuItem52ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem52ActionPerformed
        launchFilter(Filters::niblack, evt);
    }//GEN-LAST:event_jMenuItem52ActionPerformed

    private void menuLightnessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLightnessActionPerformed
        launchFilter(Filters::bwLightness, evt);
    }//GEN-LAST:event_menuLightnessActionPerformed

    private void jMenuItem54ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem54ActionPerformed
        launchFilter(FiltersRender::blendStack, evt);
    }//GEN-LAST:event_jMenuItem54ActionPerformed

    private void menuLoCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLoCutActionPerformed
        launchFilter(Filters::lcFilter, evt);
    }//GEN-LAST:event_menuLoCutActionPerformed

    private void menuHiCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHiCutActionPerformed
        launchFilter(Filters::hcFilter, evt);
    }//GEN-LAST:event_menuHiCutActionPerformed

    private void menuMultiplyPxlsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMultiplyPxlsActionPerformed
        launchFilter(Filters::multiply, evt);
    }//GEN-LAST:event_menuMultiplyPxlsActionPerformed

    private void histoAdjAutoSingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_histoAdjAutoSingleActionPerformed
        executeHistoFilter(Filters::autoscale, false);
    }//GEN-LAST:event_histoAdjAutoSingleActionPerformed

    private void histoAdjApplySingleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_histoAdjApplySingleActionPerformed
        executeHistoFilter(Filters::cutFilter, false);
    }//GEN-LAST:event_histoAdjApplySingleActionPerformed

    private void histoAdjApplyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_histoAdjApplyAllActionPerformed
        executeHistoFilter(Filters::cutFilter, true);
    }//GEN-LAST:event_histoAdjApplyAllActionPerformed

    private void histoAdjAutoAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_histoAdjAutoAllActionPerformed
        executeHistoFilter(Filters::autoscale, true);
    }//GEN-LAST:event_histoAdjAutoAllActionPerformed

    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
        popupDialog(infoDialog);
    }//GEN-LAST:event_menuAboutActionPerformed

    private void filterComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_filterComboItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED && !historyAdjusting) {
            int sel = filterCombo.getSelectedIndex();
            if (sel >= 0 && filterCombo.getItemCount() > 0) {
                currentFilter = filterHistory.get(filterCombo.getItemAt(sel));
                updatePanel(currentFilter.panelCreator.getPanel(), filterSettingsPanel);
            }
        }
    }//GEN-LAST:event_filterComboItemStateChanged

    private void jMenuItem60ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem60ActionPerformed
        launchFilter(Filters::autoscaleWithAdapt, evt);
    }//GEN-LAST:event_jMenuItem60ActionPerformed

    private void menuLoCutAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLoCutAutoActionPerformed
        launchFilter(Filters::cutFilterAutoLow, evt);
    }//GEN-LAST:event_menuLoCutAutoActionPerformed

    private void menuHiCutAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHiCutAutoActionPerformed
        launchFilter(Filters::cutFilterAutoHigh, evt);
    }//GEN-LAST:event_menuHiCutAutoActionPerformed

    private void jMenuItem63ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem63ActionPerformed
        launchProtocol(ObjectsCommon::new, evt);
    }//GEN-LAST:event_jMenuItem63ActionPerformed

    private void jMenuItem65ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem65ActionPerformed
        launchCounter(Counters::countRBStain, evt);
    }//GEN-LAST:event_jMenuItem65ActionPerformed

    private void jMenuItem68ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem68ActionPerformed
        launchProtocol(InSituRNA::new, evt);
    }//GEN-LAST:event_jMenuItem68ActionPerformed

    private void jMenuItem39ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem39ActionPerformed
        launchProtocol(StainTissueSeparation::new, evt);
    }//GEN-LAST:event_jMenuItem39ActionPerformed

    private void jMenuItem45ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem45ActionPerformed
        launchFilter(Filters::bwSaturation, evt);
    }//GEN-LAST:event_jMenuItem45ActionPerformed

    private void boxSettingAlphaBGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxSettingAlphaBGActionPerformed
        refresh();
    }//GEN-LAST:event_boxSettingAlphaBGActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        launchProtocol(__NucleusPrimaryMask::new, evt);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem23ActionPerformed
        launchFilter(Filters::colorize, evt);
    }//GEN-LAST:event_jMenuItem23ActionPerformed

    private void stackComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stackComboActionPerformed
        Settings.setBlendMode();
        refresh();
    }//GEN-LAST:event_stackComboActionPerformed

    private void jMenuItem36ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem36ActionPerformed
        launchProtocol(__ObjectSegment::new, evt);
    }//GEN-LAST:event_jMenuItem36ActionPerformed

    private void resultTableComponentMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableComponentMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            contextTableMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_resultTableComponentMouseReleased

    private void menuOpaqueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpaqueActionPerformed
        launchFilter(Filters::opaque, evt);
    }//GEN-LAST:event_menuOpaqueActionPerformed

    private void jMenuItem59ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem59ActionPerformed
        launchFilter(Filters::gaussPerfect, evt);
    }//GEN-LAST:event_jMenuItem59ActionPerformed

    private void jMenuItem64ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem64ActionPerformed
        launchFilter(Filters::gaussApprox, evt);
    }//GEN-LAST:event_jMenuItem64ActionPerformed

    private void menuFileImportImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportImageActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importImage(Arrays.asList(files));
        }
    }//GEN-LAST:event_menuFileImportImageActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        launchProtocol(__NucleusFinalMask::new, evt);
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem17ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem17ActionPerformed
        launchFilter(FiltersPass::gaussSmoothing, evt);
    }//GEN-LAST:event_jMenuItem17ActionPerformed

    private void jMenuItem21ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem21ActionPerformed
        launchProtocol(_NucleusCounterIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem21ActionPerformed

    private void jMenuItem42ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem42ActionPerformed
        launchProtocol(_AreaStainIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem42ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        launchProtocol(HETissueSeparation::new, evt);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void menuFileImportLayersThisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportLayersThisActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importLayers(Arrays.asList(files), false);
        }
    }//GEN-LAST:event_menuFileImportLayersThisActionPerformed

    private void menuFileUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileUndoActionPerformed
        UndoRedo.undoAction();
    }//GEN-LAST:event_menuFileUndoActionPerformed

    private void debugTestFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugTestFilterActionPerformed
        instantFilter(TestFilter::test, false);
    }//GEN-LAST:event_debugTestFilterActionPerformed

    private void jMenuItem56ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem56ActionPerformed
        launchFilter(Filters::illuminationCorrection, evt);
    }//GEN-LAST:event_jMenuItem56ActionPerformed

    private void filterSettingsPanelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_filterSettingsPanelComponentResized
        Component[] child = filterSettingsPanel.getComponents();

        if (child.length > 0 && child[0].getClass() == JPanel.class) {
            child[0].setSize(filterSettingsPanel.getWidth() - 4, filterSettingsPanel.getHeight() - 4);
        }
    }//GEN-LAST:event_filterSettingsPanelComponentResized

    private void jMenuItem57ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem57ActionPerformed
        launchFilter(Filters::alphaBright, evt);
    }//GEN-LAST:event_jMenuItem57ActionPerformed

    private void jMenuItem41ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem41ActionPerformed
        launchFilter(Filters::autoscale, evt);
    }//GEN-LAST:event_jMenuItem41ActionPerformed

    private void jMenuItem61ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem61ActionPerformed
        launchFilter(Filters::autoscaleWithPixelAdapt, evt);
    }//GEN-LAST:event_jMenuItem61ActionPerformed

    private void autoscaleComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoscaleComboActionPerformed
        Settings.setAutoscale();
    }//GEN-LAST:event_autoscaleComboActionPerformed

    private void boxSettingBatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxSettingBatchActionPerformed
        UndoRedo.clear();
        refresh();
    }//GEN-LAST:event_boxSettingBatchActionPerformed

    private void jMenuItem62ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem62ActionPerformed
        launchFilter(Filters::swapColour, evt);
    }//GEN-LAST:event_jMenuItem62ActionPerformed

    private void jMenuItem66ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem66ActionPerformed
        launchFilter(Filters::multiplyColBright, evt);
    }//GEN-LAST:event_jMenuItem66ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        launchFilter(Filters::doubleThreshold, evt);
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void menuFileImportStacksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportStacksActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importStacks(Arrays.asList(files));
        }
    }//GEN-LAST:event_menuFileImportStacksActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        launchProtocol(_NucleusCounterPositivity::new, evt);
    }//GEN-LAST:event_jMenuItem14ActionPerformed

    private void debugExecuteClassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugExecuteClassActionPerformed
        String cname = null;
        try {
            cname = JOptionPane.showInputDialog("Class name:");
            Class clazz = Class.forName("mainPackage.protocols." + cname);
            Protocol prot = (Protocol) clazz.getDeclaredConstructor().newInstance();
            Supplier<Protocol> supp = () -> prot;
            launchProtocol(supp, evt);
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException
                | NoSuchMethodException
                | SecurityException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            Tonga.catchError(ex, "Couldnt invoke the method \"" + cname + "\".");
        }
    }//GEN-LAST:event_debugExecuteClassActionPerformed

    private void jMenuItem19ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem19ActionPerformed
        launchProtocol(_ObjectCount::new, evt);
    }//GEN-LAST:event_jMenuItem19ActionPerformed

    private void jMenuItem33ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem33ActionPerformed
        launchProtocol(_ObjectSegmentCount::new, evt);
    }//GEN-LAST:event_jMenuItem33ActionPerformed

    private void jMenuItem34ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem34ActionPerformed
        launchFilter(Filters::cutFilter, evt);
    }//GEN-LAST:event_jMenuItem34ActionPerformed

    private void debugParameterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugParameterActionPerformed
        ArrayList<ControlReference> ctrls = new ArrayList<>();
        ctrls.add(new ControlReference(ControlType.COMBO, new String[]{"1", "2"}, "This is a combo", 1));
        ctrls.add(new ControlReference(ControlType.COMBO, new String[]{"1", "2"}, "This is a combo", 1));
        ctrls.add(new ControlReference(ControlType.SELECT, new String[]{"1", "2"}, "This is a combo", 1));
        ctrls.add(new ControlReference(ControlType.COMBO, new String[]{"1", "2"}, "This is a combo", 1));
        //ctrls.add(new ControlReference(ControlType.TOGGLE, "This is a toggle", 1));
        //ctrls.add(new ControlReference(ControlType.RANGE, new Integer[]{20, 500}, "Range%Range"));
        ctrls.add(new ControlReference(ControlType.SLIDER, new Integer[]{20, 500}, "This is a slider", 50));
        //ctrls.add(new ControlReference(ControlType.SPINNER, "This is a spinner", 10));
        //ctrls.add(new ControlReference(ControlType.TOGGLE, "This is a toggle", 1));
        //ctrls.add(new ControlReference(ControlType.COLOR, "This is a color", 1));
        updatePanel(new PanelCreator(ctrls).getPanel(), filterSettingsPanel);
        tabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_debugParameterActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        launchProtocol(_BackgroundArea::new, evt);
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void contLayMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayMergeActionPerformed
        instantFilter(FiltersRender::blendStackCurrent, false);
    }//GEN-LAST:event_contLayMergeActionPerformed

    private void jMenuItem18ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem18ActionPerformed
        launchProtocol(_NucleusCounterNumber::new, evt);
    }//GEN-LAST:event_jMenuItem18ActionPerformed

    private void jMenuItem38ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem38ActionPerformed
        String[] cname = null;
        try {
            cname = JOptionPane.showInputDialog("Class/method name:").split("\\.");
            Class clazz = Class.forName("mainPackage.filters." + cname[0]);
            Filter filt = (Filter) clazz.getMethod(cname[1]).invoke(null);
            Supplier<Filter> supp = () -> filt;
            launchFilter(supp, evt);
        } catch (IllegalAccessException
                | ClassNotFoundException
                | NoSuchMethodException
                | SecurityException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            Tonga.catchError(ex, "Couldnt invoke the filter \"" + cname[1] + " from class " + cname[0] + "\".");
        }
    }//GEN-LAST:event_jMenuItem38ActionPerformed

    private void jMenuItem43ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem43ActionPerformed
        launchFilter(FiltersSet::fillInnerAreasSizeShape, evt);
    }//GEN-LAST:event_jMenuItem43ActionPerformed

    private void jMenuItem58ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem58ActionPerformed
        launchFilter(FiltersSet::innerAreas, evt);
    }//GEN-LAST:event_jMenuItem58ActionPerformed

    private void jMenuItem67ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem67ActionPerformed
        launchFilter(FiltersSet::filterObjectSizeShape, evt);
    }//GEN-LAST:event_jMenuItem67ActionPerformed

    private void jMenuItem69ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem69ActionPerformed
        launchFilter(FiltersSet::getEdgeMask, evt);
    }//GEN-LAST:event_jMenuItem69ActionPerformed

    private void debugSysInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugSysInfoActionPerformed
        logSysInfo();
    }//GEN-LAST:event_debugSysInfoActionPerformed

    private void jMenuItem70ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem70ActionPerformed
        launchFilter(FiltersPass::fuzzyCorrection, evt);
    }//GEN-LAST:event_jMenuItem70ActionPerformed

    private void jMenuItem71ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem71ActionPerformed
        launchProtocol(_EstimateNucleusSize::new, evt);
    }//GEN-LAST:event_jMenuItem71ActionPerformed

    private void jMenuItem72ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem72ActionPerformed
        launchProtocol(_AreaSurroundIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem72ActionPerformed

    private void jMenuItem74ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem74ActionPerformed
        launchProtocol(_ObjectIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem74ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        launchProtocol(_NucleusCounterSurroundIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        launchFilter(FiltersPass::connectLineObjects, evt);
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        launchFilter(FiltersSet::filterObjectSizeDimension, evt);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        //
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        launchProtocol(_NucleusCounterDoubleIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        launchProtocol(_AreaDoubleStainIntensity::new, evt);
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jMenuItem15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem15ActionPerformed
        launchProtocol(ObjectsSeparated::new, evt);
    }//GEN-LAST:event_jMenuItem15ActionPerformed

    private void jMenuItem40ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem40ActionPerformed
        launchFilter(FiltersPass::fillSmallEdgeHoles, evt);
    }//GEN-LAST:event_jMenuItem40ActionPerformed

    private void jMenuItem53ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem53ActionPerformed
        launchFilter(Filters::dotRemove, evt);
    }//GEN-LAST:event_jMenuItem53ActionPerformed

    private void jMenuItem73ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem73ActionPerformed
        launchFilter(FiltersSet::getExtendedMask, evt);
    }//GEN-LAST:event_jMenuItem73ActionPerformed

    private void jMenuItem75ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem75ActionPerformed
        launchFilter(FiltersSet::getRadiusOverlap, evt);
    }//GEN-LAST:event_jMenuItem75ActionPerformed

    private void jMenuItem76ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem76ActionPerformed
        launchProtocol(__ObjectSegmentStrict::new, evt);
    }//GEN-LAST:event_jMenuItem76ActionPerformed

    private void jMenuItem77ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem77ActionPerformed
        launchFilter(FiltersSet::filterEdgeTouchers, evt);
    }//GEN-LAST:event_jMenuItem77ActionPerformed

    private void jMenuItem78ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem78ActionPerformed
        launchProtocol(__DeadDividing::new, evt);
    }//GEN-LAST:event_jMenuItem78ActionPerformed

    private void jMenuItem79ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem79ActionPerformed
        Tonga.enableDebugTracing();
    }//GEN-LAST:event_jMenuItem79ActionPerformed

    private void jMenuItem80ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem80ActionPerformed
        IO.openLogs();
    }//GEN-LAST:event_jMenuItem80ActionPerformed

    private void jMenuItem81ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem81ActionPerformed
        launchProtocol(GradientReflect::new, evt);
    }//GEN-LAST:event_jMenuItem81ActionPerformed

    private void jMenuItem82ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem82ActionPerformed
        /*TableData tableData = new TableData(new String[]{"Image", "Nuclei", "Avg.size", "Avg.intensity"});
        for (int i = 0; i < 50; i++) {
            tableData.newRow(new Object[]{
                "CellImage" + i,
                (new Random().nextInt(50) + 50) + "",
                (Math.round((new Random().nextDouble() + 5) * 10000) / 100.) + "",
                (Math.round(((new Random().nextDouble() + 3) * 25) * 10000.) / 10000.) + "%"});
        }*/
        TableData tableData = new TableData(new String[]{"Image", "Nucleus", "Size", "Intensity"}, new String[]{"Image name", "Nucleus number", "Size in pixels", "???"});
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < new Random().nextInt(50) + 50; j++) {
                tableData.newRow(new Object[]{
                    "CellImage" + i,
                    "#" + j,
                    Math.round((new Random().nextDouble() + 3) * 157) + "",
                    (Math.round(((new Random().nextDouble() + 2) * 35) * 10000.) / 10000.) + "%"});
            }
        }
        resultTable.publishData(tableData);
    }//GEN-LAST:event_jMenuItem82ActionPerformed

    private void boxSettingHWRenderingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxSettingHWRenderingActionPerformed
        if (Tonga.thereIsImage()) {
            TongaRender.forceRenderRefresh();
            TongaRender.copyFromCache();
            refresh();
        }
    }//GEN-LAST:event_boxSettingHWRenderingActionPerformed

    private void imageBigComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_imageBigComponentResized
        if (panelBigLayer != null && panelBig != null && actionPanel != null) {
            int w = imageBig.getWidth(), h = imageBig.getHeight();
            /* must be -4 because the mainPanel has borders and that is not reducted
            automatically, since JLayeredPane does not follow any resizing rules */
            panelBigLayer.setSize(w - 4, h - 4);
            panelBig.setSize(w - 4, h - 4);
            actionPanel.setSize(w - 4, h - 4);
        }
    }//GEN-LAST:event_imageBigComponentResized

    private void jMenuItem84ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem84ActionPerformed
        launchProtocol(__ObjectSegment::new, evt);
    }//GEN-LAST:event_jMenuItem84ActionPerformed

    private void jMenuItem55ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem55ActionPerformed
        launchCounter(Counters::countBWBG, evt);
    }//GEN-LAST:event_jMenuItem55ActionPerformed

    private void jMenuItem85ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem85ActionPerformed
        launchProtocol(DimRemover::new, evt);
    }//GEN-LAST:event_jMenuItem85ActionPerformed

    private void jMenuItem86ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem86ActionPerformed
        launchProtocol(BrightRemover::new, evt);
    }//GEN-LAST:event_jMenuItem86ActionPerformed

    private void menuFileImportMultichannelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportMultichannelActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"), true);
        if (files != null) {
            IO.importMultichannel(Arrays.asList(files));
        }
    }//GEN-LAST:event_menuFileImportMultichannelActionPerformed

    private void contLaySelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLaySelectAllActionPerformed
        Tonga.selectLayersAll();
    }//GEN-LAST:event_contLaySelectAllActionPerformed

    private void contImgSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgSelectAllActionPerformed
        Tonga.selectImagesAll();
    }//GEN-LAST:event_contImgSelectAllActionPerformed

    private void contImgClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgClearActionPerformed
        Tonga.selectImagesAll();
        Tonga.removeImage();
    }//GEN-LAST:event_contImgClearActionPerformed

    private void jMenuItem83ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem83ActionPerformed
        IO.launchURL("https://github.com/avritchie/tonga/wiki/");
    }//GEN-LAST:event_jMenuItem83ActionPerformed

    private void jMenuItem88ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem88ActionPerformed
        popupDialog(feedbackDialog);
    }//GEN-LAST:event_jMenuItem88ActionPerformed

    private void menuWizardMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_menuWizardMouseClicked
        if (Tonga.thereIsImage()) {
            popupDialog(wizardDialog);
        } else {
            Tonga.setStatus("Import the images to analyze first before opening the wizard");
        }
    }//GEN-LAST:event_menuWizardMouseClicked

    private void maxTabButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxTabButtonActionPerformed
        JScrollPane scrollPane = returnActiveTableScrollPanel();
        JTable tableComponent = returnActiveTable();
        if (tableWindow == null) {
            tableWindow = new TableViewer();
        }
        if (tableComponent.getParent().getParent().equals(scrollPane)) {
            tableWindow.setTable(tableComponent, scrollPane, tabbedPane.getSelectedIndex());
        } else {
            tableWindow.requestFocus();
        }
    }//GEN-LAST:event_maxTabButtonActionPerformed

    private void tabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged
        maxButtEnable();
    }//GEN-LAST:event_tabbedPaneStateChanged

    private void contImgScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgScaleActionPerformed
        Tonga.setImageScaling();
    }//GEN-LAST:event_contImgScaleActionPerformed

    private void contLayColorRedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayColorRedActionPerformed
        instantFilter(Filters::colorize, false, 0xFFFF0000);
    }//GEN-LAST:event_contLayColorRedActionPerformed

    private void contLayColorGreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayColorGreenActionPerformed
        instantFilter(Filters::colorize, false, 0xFF00FF00);
    }//GEN-LAST:event_contLayColorGreenActionPerformed

    private void contLayColorBlueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayColorBlueActionPerformed
        instantFilter(Filters::colorize, false, 0xFF0000FF);
    }//GEN-LAST:event_contLayColorBlueActionPerformed

    private void contLayColorGrayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayColorGrayActionPerformed
        instantFilter(Filters::colorize, false, 0xFFFFFFFF);
    }//GEN-LAST:event_contLayColorGrayActionPerformed

    private void contLayColorCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayColorCustomActionPerformed
        launchFilter(Filters::colorize, evt);
    }//GEN-LAST:event_contLayColorCustomActionPerformed

    private void contResDelRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contResDelRowActionPerformed
        UndoRedo.start();
        resultTable.removeSelectedRows();
        UndoRedo.end();
    }//GEN-LAST:event_contResDelRowActionPerformed

    private void contResClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contResClearActionPerformed
        UndoRedo.start();
        resultTable.clearData();
        UndoRedo.end();
    }//GEN-LAST:event_contResClearActionPerformed

    private void debugTestProtocolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugTestProtocolActionPerformed
        launchProtocol(TestProtocol::new, evt);
    }//GEN-LAST:event_debugTestProtocolActionPerformed

    private void contLayMoveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayMoveUpActionPerformed
        Tonga.moveOrder(false, true);
    }//GEN-LAST:event_contLayMoveUpActionPerformed

    private void contLayMoveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayMoveDownActionPerformed
        Tonga.moveOrder(false, false);
    }//GEN-LAST:event_contLayMoveDownActionPerformed

    private void contImgMoveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgMoveUpActionPerformed
        Tonga.moveOrder(true, true);
    }//GEN-LAST:event_contImgMoveUpActionPerformed

    private void contImgMoveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contImgMoveDownActionPerformed
        Tonga.moveOrder(true, false);
    }//GEN-LAST:event_contImgMoveDownActionPerformed

    private void jMenuItem89ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem89ActionPerformed
        launchFilter(FiltersPass::adaptiveThreshold, evt);
    }//GEN-LAST:event_jMenuItem89ActionPerformed

    private void jMenuItem90ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem90ActionPerformed
        launchProtocol(DAPICorrect::new, evt);
    }//GEN-LAST:event_jMenuItem90ActionPerformed

    private void jMenuItem91ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem91ActionPerformed
        launchFilter(Filters::blurConditional, evt);
    }//GEN-LAST:event_jMenuItem91ActionPerformed

    private void jMenuItem92ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem92ActionPerformed
        launchFilter(Filters::scaleDark, evt);
    }//GEN-LAST:event_jMenuItem92ActionPerformed

    private void jMenuItem93ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem93ActionPerformed
        launchProtocol(ApplyIllumination::new, evt);
    }//GEN-LAST:event_jMenuItem93ActionPerformed

    private void contLayExtractRedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayExtractRedActionPerformed
        instantFilter(Filters::separateChannel, false, 0);
    }//GEN-LAST:event_contLayExtractRedActionPerformed

    private void contLayExtractGreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayExtractGreenActionPerformed
        instantFilter(Filters::separateChannel, false, 1);
    }//GEN-LAST:event_contLayExtractGreenActionPerformed

    private void contLayExtractBlueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayExtractBlueActionPerformed
        instantFilter(Filters::separateChannel, false, 2);
    }//GEN-LAST:event_contLayExtractBlueActionPerformed

    private void contLayExtractAlphaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayExtractAlphaActionPerformed
        instantFilter(Filters::separateChannel, false, 3);
    }//GEN-LAST:event_contLayExtractAlphaActionPerformed

    private void jMenuItem94ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem94ActionPerformed
        launchFilter(FiltersPass::balancedScaling, evt);
    }//GEN-LAST:event_jMenuItem94ActionPerformed

    private void jMenuItem95ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem95ActionPerformed
        launchFilter(FiltersPass::dogSementing, evt);
    }//GEN-LAST:event_jMenuItem95ActionPerformed

    private void jMenuItem96ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem96ActionPerformed
        launchFilter(Filters::integral, evt);
    }//GEN-LAST:event_jMenuItem96ActionPerformed

    private void jMenuItem97ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem97ActionPerformed
        launchProtocol(IFNormalize::new, evt);
    }//GEN-LAST:event_jMenuItem97ActionPerformed

    private void jMenuItem98ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem98ActionPerformed
        launchProtocol(IFCorrectAll::new, evt);
    }//GEN-LAST:event_jMenuItem98ActionPerformed

    private void jMenuItem99ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem99ActionPerformed
        launchProtocol(AverageMaskThreshold::new, evt);
    }//GEN-LAST:event_jMenuItem99ActionPerformed

    private void jMenuItem100ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem100ActionPerformed
        launchFilter(Filters::average, evt);
    }//GEN-LAST:event_jMenuItem100ActionPerformed

    private void jMenuItem101ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem101ActionPerformed
        launchFilter(Filters::tissueOverlapCorrection, evt);
    }//GEN-LAST:event_jMenuItem101ActionPerformed

    private void jMenuItem102ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem102ActionPerformed
        launchFilter(FiltersPass::backgroundStainingScaling, evt);
    }//GEN-LAST:event_jMenuItem102ActionPerformed

    private void jMenuItem104ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem104ActionPerformed
        launchCounter(Counters::countIFChannels, evt);
    }//GEN-LAST:event_jMenuItem104ActionPerformed

    private void jMenuItem103ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem103ActionPerformed
        launchCounter(Counters::countUnity, evt);
    }//GEN-LAST:event_jMenuItem103ActionPerformed

    private void jMenuItem105ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem105ActionPerformed
        launchFilter(Filters::thresholdRGB, evt);
    }//GEN-LAST:event_jMenuItem105ActionPerformed

    private void jMenuItem106ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem106ActionPerformed
        launchFilter(FiltersPass::multiLocalThreshold, evt);
    }//GEN-LAST:event_jMenuItem106ActionPerformed

    private void contLayExtractAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contLayExtractAllActionPerformed
        int lind = Tonga.getLayerIndex();
        instantFilter(Filters::separateChannel, false, 0);
        try {
            Tonga.loader.threadTask.join();
        } catch (InterruptedException ex) {
            Tonga.catchError(ex);
        } finally {
            Tonga.selectLayer(lind);
            instantFilter(Filters::separateChannel, false, 1);
            try {
                Tonga.loader.threadTask.join();
            } catch (InterruptedException ex) {
                Tonga.catchError(ex);
            } finally {
                Tonga.selectLayer(lind);
                instantFilter(Filters::separateChannel, false, 2);
            }
        }
    }//GEN-LAST:event_contLayExtractAllActionPerformed

    private void jMenuItem107ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem107ActionPerformed
        launchProtocol(IFMirax::new, evt);
    }//GEN-LAST:event_jMenuItem107ActionPerformed

    private void jMenuItem87ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem87ActionPerformed
        launchFilter(FiltersPass::backgroundStainingAvgSubtraction, evt);
    }//GEN-LAST:event_jMenuItem87ActionPerformed

    private void jMenuItem108ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem108ActionPerformed
        launchFilter(FiltersPass::maxDiffCorrect, evt);
    }//GEN-LAST:event_jMenuItem108ActionPerformed

    private void jMenuItem109ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem109ActionPerformed
        launchProtocol(RadialArea::new, evt);
    }//GEN-LAST:event_jMenuItem109ActionPerformed

    private void jMenuItem110ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem110ActionPerformed
        launchFilter(Filters::condSum, evt);
    }//GEN-LAST:event_jMenuItem110ActionPerformed

    private void jMenuItem111ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem111ActionPerformed
        launchProtocol(Spheroids::new, evt);
    }//GEN-LAST:event_jMenuItem111ActionPerformed

    private void exportAnnotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAnnotActionPerformed
        IO.exportAnnos(false);
    }//GEN-LAST:event_exportAnnotActionPerformed

    private void importAnnotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importAnnotActionPerformed
        IO.importAnnos();
    }//GEN-LAST:event_importAnnotActionPerformed

    private void annoTableComponentMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_annoTableComponentMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            contextAnnotationMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_annoTableComponentMouseReleased

    private void annotClearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotClearAllActionPerformed
        TongaAnnotator.deleteAll();
    }//GEN-LAST:event_annotClearAllActionPerformed

    private void annotDotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotDotActionPerformed
        annotButtonToggle();
        dotMenu.show((Component) evt.getSource(), 8, 8);
    }//GEN-LAST:event_annotDotActionPerformed

    private void annotLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotLineActionPerformed
        annotButtonToggle();
        lineMenu.show((Component) evt.getSource(), 8, 8);
    }//GEN-LAST:event_annotLineActionPerformed

    private void annotShapeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotShapeActionPerformed
        annotButtonToggle();
        shapeMenu.show((Component) evt.getSource(), 8, 8);
    }//GEN-LAST:event_annotShapeActionPerformed

    private void annotMoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotMoveActionPerformed
        TongaAnnotator.activateAnnotator(false);
    }//GEN-LAST:event_annotMoveActionPerformed

    private void annotEraseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotEraseActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.ERASER);
    }//GEN-LAST:event_annotEraseActionPerformed

    private void annotColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotColourActionPerformed
        annotButtonToggle();
        colorMenu.show((Component) evt.getSource(), 8, 8);
    }//GEN-LAST:event_annotColourActionPerformed

    private void shapeRectangleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shapeRectangleActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.RECTANGLE);
    }//GEN-LAST:event_shapeRectangleActionPerformed

    private void shapeCircleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shapeCircleActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.CIRCLE);
    }//GEN-LAST:event_shapeCircleActionPerformed

    private void shapeOvalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shapeOvalActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.OVAL);
    }//GEN-LAST:event_shapeOvalActionPerformed

    private void lineLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lineLineActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.LINE);
    }//GEN-LAST:event_lineLineActionPerformed

    private void linePolylineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linePolylineActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.POLYLINE);
    }//GEN-LAST:event_linePolylineActionPerformed

    private void lineThickActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lineThickActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.PLANE);
    }//GEN-LAST:event_lineThickActionPerformed

    private void colorMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorMenuActionPerformed
        colorMenuSelect(((JMenuItem) evt.getSource()).getForeground());
    }//GEN-LAST:event_colorMenuActionPerformed

    private void annotRadiusMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_annotRadiusMouseWheelMoved
        JSpinner source = (JSpinner) evt.getSource();
        int val = Integer.parseInt((source.getValue().toString())) - evt.getWheelRotation();
        source.setValue(val);
    }//GEN-LAST:event_annotRadiusMouseWheelMoved

    private void annotRadiusStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_annotRadiusStateChanged
        TongaAnnotator.setAnnotationSize((int) ((JSpinner) evt.getSource()).getValue());
    }//GEN-LAST:event_annotRadiusStateChanged

    private void launchExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchExcelActionPerformed
        IO.exportTable(returnActiveTable(), true);
    }//GEN-LAST:event_launchExcelActionPerformed

    private void jMenuItem112ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem112ActionPerformed
        launchCounter(AnnoCounters::planeIntensities, evt);
    }//GEN-LAST:event_jMenuItem112ActionPerformed

    private void shapePolyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shapePolyActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.POLYGON);
    }//GEN-LAST:event_shapePolyActionPerformed

    private void annotVisibleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotVisibleActionPerformed
        TongaAnnotator.visible(((javax.swing.JToggleButton) evt.getSource()).isSelected());
    }//GEN-LAST:event_annotVisibleActionPerformed

    private void dotDotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dotDotActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.DOT);
    }//GEN-LAST:event_dotDotActionPerformed

    private void dotRadiusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dotRadiusActionPerformed
        TongaAnnotator.setAnnotationType(AnnotationType.RADIUS);
    }//GEN-LAST:event_dotRadiusActionPerformed

    private void jMenuItem137ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem137ActionPerformed
        launchCounter(Counters::intHisto, evt);
    }//GEN-LAST:event_jMenuItem137ActionPerformed

    private void contResTransposeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contResTransposeActionPerformed
        resultTable.transposeByImage();
    }//GEN-LAST:event_contResTransposeActionPerformed

    private void jMenuItem139ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem139ActionPerformed
        launchCounter(Counters::analIFHisto, evt);
    }//GEN-LAST:event_jMenuItem139ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JScrollPane annoScrollPane;
    protected javax.swing.JTable annoTableComponent;
    protected javax.swing.JButton annotClearAll;
    protected javax.swing.JButton annotColour;
    protected javax.swing.JToggleButton annotDot;
    protected javax.swing.JToggleButton annotErase;
    protected javax.swing.JSpinner annotGroup;
    protected javax.swing.JToggleButton annotLine;
    protected javax.swing.JToggleButton annotMove;
    protected javax.swing.JSpinner annotRadius;
    protected javax.swing.JToggleButton annotShape;
    protected javax.swing.JToggleButton annotVisible;
    protected javax.swing.JComboBox<String> autoscaleCombo;
    protected javax.swing.JLabel autoscaleLabel;
    protected javax.swing.JLabel bleftLabel;
    public javax.swing.JCheckBox boxSettingAlphaBG;
    protected javax.swing.JCheckBox boxSettingAutoscale1;
    protected javax.swing.JCheckBox boxSettingBatch;
    protected javax.swing.JCheckBox boxSettingHWRendering;
    protected javax.swing.JCheckBox boxSettingMMapping;
    protected javax.swing.JCheckBox boxSettingMultiThreading;
    protected javax.swing.JCheckBox boxSettingOpenAfter;
    protected javax.swing.JCheckBox boxSettingResultsAppend;
    protected javax.swing.JCheckBox boxSettingSubfolder;
    protected javax.swing.JLabel brightLabel;
    protected javax.swing.JButton browseButton;
    protected javax.swing.JButton browseFilter;
    protected javax.swing.JButton btnRunAll;
    protected javax.swing.JButton btnRunAll2;
    protected javax.swing.JButton btnRunSingle;
    protected javax.swing.JButton btnRunSingle2;
    protected javax.swing.JMenuItem colorBlue;
    protected javax.swing.JMenuItem colorCyan;
    protected javax.swing.JMenuItem colorDefault;
    protected javax.swing.JMenuItem colorGreen;
    protected javax.swing.JMenuItem colorLime;
    protected javax.swing.JMenuItem colorMagenta;
    protected javax.swing.JPopupMenu colorMenu;
    protected javax.swing.JMenuItem colorOrange;
    protected javax.swing.JMenuItem colorPink;
    protected javax.swing.JMenuItem colorPurple;
    protected javax.swing.JMenuItem colorRed;
    protected javax.swing.JMenuItem colorYellow;
    protected javax.swing.JMenuItem contAnnoClear;
    protected javax.swing.JMenuItem contAnnoDelRow;
    protected javax.swing.JMenuItem contAnnoExcel;
    protected javax.swing.JMenuItem contAnnoExport;
    protected javax.swing.JPopupMenu.Separator contAnnoSeparator;
    protected javax.swing.JMenuItem contImgClear;
    protected javax.swing.JMenuItem contImgDelete;
    protected javax.swing.JMenuItem contImgMoveDown;
    protected javax.swing.JMenuItem contImgMoveUp;
    protected javax.swing.JMenuItem contImgRename;
    protected javax.swing.JMenuItem contImgScale;
    protected javax.swing.JMenuItem contImgSelectAll;
    protected javax.swing.JMenu contLayColor;
    protected javax.swing.JMenuItem contLayColorBlue;
    protected javax.swing.JMenuItem contLayColorCustom;
    protected javax.swing.JMenuItem contLayColorGray;
    protected javax.swing.JMenuItem contLayColorGreen;
    protected javax.swing.JMenuItem contLayColorRed;
    protected javax.swing.JMenuItem contLayDeleteAll;
    protected javax.swing.JMenuItem contLayDeleteThis;
    protected javax.swing.JMenu contLayExtract;
    protected javax.swing.JMenuItem contLayExtractAll;
    protected javax.swing.JMenuItem contLayExtractAlpha;
    protected javax.swing.JMenuItem contLayExtractBlue;
    protected javax.swing.JMenuItem contLayExtractGreen;
    protected javax.swing.JMenuItem contLayExtractRed;
    protected javax.swing.JMenuItem contLayMerge;
    protected javax.swing.JMenuItem contLayMoveDown;
    protected javax.swing.JMenuItem contLayMoveUp;
    protected javax.swing.JMenuItem contLayRename;
    protected javax.swing.JMenuItem contLaySelect;
    protected javax.swing.JMenuItem contLaySelectAll;
    protected javax.swing.JPopupMenu.Separator contLaySep;
    protected javax.swing.JMenuItem contResClear;
    protected javax.swing.JMenuItem contResDelRow;
    protected javax.swing.JMenuItem contResExcel;
    protected javax.swing.JMenuItem contResExport;
    protected javax.swing.JPopupMenu.Separator contResSeparator;
    protected javax.swing.JMenuItem contResTranspose;
    protected javax.swing.JPopupMenu contextAnnotationMenu;
    protected javax.swing.JPopupMenu contextImageMenu;
    protected javax.swing.JPopupMenu contextLayerMenu;
    protected javax.swing.JPopupMenu contextResultMenu;
    protected javax.swing.JMenuItem debugExecuteClass;
    protected javax.swing.JMenuItem debugMemory;
    protected javax.swing.JMenuItem debugParameter;
    protected javax.swing.JMenuItem debugSysInfo;
    protected javax.swing.JMenuItem debugTestFilter;
    protected javax.swing.JMenuItem debugTestProtocol;
    protected javax.swing.JMenu debugTestProtocols;
    protected javax.swing.JButton exportAsCSV;
    protected javax.swing.JMenuItem dotDot;
    protected javax.swing.JPopupMenu dotMenu;
    protected javax.swing.JMenuItem dotRadius;
    protected javax.swing.JButton exportAnnot;
    protected javax.swing.JButton exportTSV;
    public javax.swing.JTextField filePathField;
    protected javax.swing.JComboBox<String> filterCombo;
    protected javax.swing.JPopupMenu filterMenu;
    protected javax.swing.JSeparator filterNameSeparator;
    protected javax.swing.JPanel filterPanel;
    protected javax.swing.JPanel filterSettingsPanel;
    protected javax.swing.JSeparator filterSettingsSeparator;
    protected javax.swing.JPanel floatingPane;
    protected javax.swing.JPanel frame;
    protected javax.swing.JPanel generalPanel;
    protected javax.swing.JButton histoAdjApplyAll;
    protected javax.swing.JButton histoAdjApplySingle;
    protected javax.swing.JButton histoAdjAutoAll;
    protected javax.swing.JButton histoAdjAutoSingle;
    protected javax.swing.JPanel histoImg;
    protected javax.swing.JLabel histoLabel;
    protected javax.swing.JPanel histoSliderPanel;
    protected javax.swing.JPanel histogramPanel;
    protected javax.swing.JPanel imageBig;
    protected javax.swing.JPanel imageZoom;
    public javax.swing.JList<String> imagesList;
    protected javax.swing.JButton importAnnot;
    protected javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    protected javax.swing.JLabel jLabel2;
    protected javax.swing.JList<String> jList4;
    protected javax.swing.JMenu jMenu1;
    protected javax.swing.JMenu jMenu10;
    protected javax.swing.JMenu jMenu11;
    protected javax.swing.JMenu jMenu12;
    protected javax.swing.JMenu jMenu13;
    protected javax.swing.JMenu jMenu14;
    protected javax.swing.JMenu jMenu15;
    protected javax.swing.JMenu jMenu16;
    protected javax.swing.JMenu jMenu17;
    protected javax.swing.JMenu jMenu18;
    protected javax.swing.JMenu jMenu19;
    protected javax.swing.JMenu jMenu2;
    protected javax.swing.JMenu jMenu20;
    protected javax.swing.JMenu jMenu21;
    protected javax.swing.JMenu jMenu22;
    protected javax.swing.JMenu jMenu23;
    protected javax.swing.JMenu jMenu24;
    protected javax.swing.JMenu jMenu26;
    protected javax.swing.JMenu jMenu27;
    protected javax.swing.JMenu jMenu3;
    protected javax.swing.JMenu jMenu4;
    protected javax.swing.JMenu jMenu5;
    protected javax.swing.JMenu jMenu6;
    protected javax.swing.JMenu jMenu7;
    protected javax.swing.JMenu jMenu8;
    protected javax.swing.JMenu jMenu9;
    protected javax.swing.JMenuItem jMenuItem1;
    protected javax.swing.JMenuItem jMenuItem10;
    protected javax.swing.JMenuItem jMenuItem100;
    protected javax.swing.JMenuItem jMenuItem101;
    protected javax.swing.JMenuItem jMenuItem102;
    protected javax.swing.JMenuItem jMenuItem103;
    protected javax.swing.JMenuItem jMenuItem104;
    protected javax.swing.JMenuItem jMenuItem105;
    protected javax.swing.JMenuItem jMenuItem106;
    protected javax.swing.JMenuItem jMenuItem107;
    protected javax.swing.JMenuItem jMenuItem108;
    protected javax.swing.JMenuItem jMenuItem11;
    protected javax.swing.JMenuItem jMenuItem110;
    protected javax.swing.JMenuItem jMenuItem111;
    protected javax.swing.JMenuItem jMenuItem112;
    protected javax.swing.JMenuItem jMenuItem12;
    protected javax.swing.JMenuItem jMenuItem13;
    protected javax.swing.JMenuItem jMenuItem137;
    protected javax.swing.JMenuItem jMenuItem138;
    protected javax.swing.JMenuItem jMenuItem139;
    protected javax.swing.JMenuItem jMenuItem14;
    protected javax.swing.JMenuItem jMenuItem15;
    protected javax.swing.JMenuItem jMenuItem16;
    protected javax.swing.JMenuItem jMenuItem17;
    protected javax.swing.JMenuItem jMenuItem18;
    protected javax.swing.JMenuItem jMenuItem19;
    protected javax.swing.JMenuItem jMenuItem2;
    protected javax.swing.JMenuItem jMenuItem20;
    protected javax.swing.JMenuItem jMenuItem21;
    protected javax.swing.JMenuItem jMenuItem22;
    protected javax.swing.JMenuItem jMenuItem23;
    protected javax.swing.JMenuItem jMenuItem24;
    protected javax.swing.JMenuItem jMenuItem25;
    protected javax.swing.JMenuItem jMenuItem26;
    protected javax.swing.JMenuItem jMenuItem27;
    protected javax.swing.JMenuItem jMenuItem28;
    protected javax.swing.JMenuItem jMenuItem29;
    protected javax.swing.JMenuItem jMenuItem3;
    protected javax.swing.JMenuItem jMenuItem30;
    protected javax.swing.JMenuItem jMenuItem31;
    protected javax.swing.JMenuItem jMenuItem32;
    protected javax.swing.JMenuItem jMenuItem33;
    protected javax.swing.JMenuItem jMenuItem34;
    protected javax.swing.JMenuItem jMenuItem35;
    protected javax.swing.JMenuItem jMenuItem36;
    protected javax.swing.JMenuItem jMenuItem37;
    protected javax.swing.JMenuItem jMenuItem38;
    protected javax.swing.JMenuItem jMenuItem39;
    protected javax.swing.JMenuItem jMenuItem4;
    protected javax.swing.JMenuItem jMenuItem40;
    protected javax.swing.JMenuItem jMenuItem41;
    protected javax.swing.JMenuItem jMenuItem42;
    protected javax.swing.JMenuItem jMenuItem43;
    protected javax.swing.JMenuItem jMenuItem44;
    protected javax.swing.JMenuItem jMenuItem45;
    protected javax.swing.JMenuItem jMenuItem46;
    protected javax.swing.JMenuItem jMenuItem47;
    protected javax.swing.JMenuItem jMenuItem48;
    protected javax.swing.JMenuItem jMenuItem49;
    protected javax.swing.JMenuItem jMenuItem5;
    protected javax.swing.JMenuItem jMenuItem50;
    protected javax.swing.JMenuItem jMenuItem51;
    protected javax.swing.JMenuItem jMenuItem52;
    protected javax.swing.JMenuItem jMenuItem53;
    protected javax.swing.JMenuItem jMenuItem54;
    protected javax.swing.JMenuItem jMenuItem55;
    protected javax.swing.JMenuItem jMenuItem56;
    protected javax.swing.JMenuItem jMenuItem57;
    protected javax.swing.JMenuItem jMenuItem58;
    protected javax.swing.JMenuItem jMenuItem59;
    protected javax.swing.JMenuItem jMenuItem6;
    protected javax.swing.JMenuItem jMenuItem60;
    protected javax.swing.JMenuItem jMenuItem61;
    protected javax.swing.JMenuItem jMenuItem62;
    protected javax.swing.JMenuItem jMenuItem63;
    protected javax.swing.JMenuItem jMenuItem64;
    protected javax.swing.JMenuItem jMenuItem65;
    protected javax.swing.JMenuItem jMenuItem66;
    protected javax.swing.JMenuItem jMenuItem67;
    protected javax.swing.JMenuItem jMenuItem68;
    protected javax.swing.JMenuItem jMenuItem69;
    protected javax.swing.JMenuItem jMenuItem7;
    protected javax.swing.JMenuItem jMenuItem70;
    protected javax.swing.JMenuItem jMenuItem71;
    protected javax.swing.JMenuItem jMenuItem72;
    protected javax.swing.JMenuItem jMenuItem73;
    protected javax.swing.JMenuItem jMenuItem74;
    protected javax.swing.JMenuItem jMenuItem75;
    protected javax.swing.JMenuItem jMenuItem76;
    protected javax.swing.JMenuItem jMenuItem77;
    protected javax.swing.JMenuItem jMenuItem78;
    protected javax.swing.JMenuItem jMenuItem79;
    protected javax.swing.JMenuItem jMenuItem8;
    protected javax.swing.JMenuItem jMenuItem80;
    protected javax.swing.JMenuItem jMenuItem81;
    protected javax.swing.JMenuItem jMenuItem82;
    protected javax.swing.JMenuItem jMenuItem83;
    protected javax.swing.JMenuItem jMenuItem84;
    protected javax.swing.JMenuItem jMenuItem85;
    protected javax.swing.JMenuItem jMenuItem86;
    protected javax.swing.JMenuItem jMenuItem87;
    protected javax.swing.JMenuItem jMenuItem88;
    protected javax.swing.JMenuItem jMenuItem89;
    protected javax.swing.JMenuItem jMenuItem9;
    protected javax.swing.JMenuItem jMenuItem90;
    protected javax.swing.JMenuItem jMenuItem91;
    protected javax.swing.JMenuItem jMenuItem92;
    protected javax.swing.JMenuItem jMenuItem93;
    protected javax.swing.JMenuItem jMenuItem94;
    protected javax.swing.JMenuItem jMenuItem95;
    protected javax.swing.JMenuItem jMenuItem96;
    protected javax.swing.JMenuItem jMenuItem97;
    protected javax.swing.JMenuItem jMenuItem98;
    protected javax.swing.JMenuItem jMenuItem99;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JScrollPane jScrollPane1;
    protected javax.swing.JScrollPane jScrollPane3;
    protected javax.swing.JScrollPane jScrollPane4;
    protected javax.swing.JSeparator jSeparator1;
    protected javax.swing.JPopupMenu.Separator jSeparator10;
    protected javax.swing.JPopupMenu.Separator jSeparator11;
    protected javax.swing.JPopupMenu.Separator jSeparator12;
    protected javax.swing.JPopupMenu.Separator jSeparator13;
    protected javax.swing.JPopupMenu.Separator jSeparator14;
    protected javax.swing.JPopupMenu.Separator jSeparator15;
    protected javax.swing.JPopupMenu.Separator jSeparator16;
    protected javax.swing.JPopupMenu.Separator jSeparator17;
    protected javax.swing.JPopupMenu.Separator jSeparator18;
    protected javax.swing.JPopupMenu.Separator jSeparator2;
    protected javax.swing.JPopupMenu.Separator jSeparator3;
    protected javax.swing.JSeparator jSeparator4;
    protected javax.swing.JPopupMenu.Separator jSeparator5;
    protected javax.swing.JPopupMenu.Separator jSeparator6;
    protected javax.swing.JPopupMenu.Separator jSeparator7;
    protected javax.swing.JPopupMenu.Separator jSeparator8;
    protected javax.swing.JSeparator jSeparator9;
    protected javax.swing.JSplitPane lSplitPane;
    public javax.swing.JButton layerBackColor;
    protected javax.swing.JLayeredPane layerPane;
    public javax.swing.JList<String> layersList;
    protected javax.swing.JMenuItem lineAngle;
    protected javax.swing.JMenuItem lineLine;
    protected javax.swing.JPopupMenu lineMenu;
    protected javax.swing.JMenuItem linePolyline;
    protected javax.swing.JMenuItem lineThick;
    protected javax.swing.JButton maxTabButton;
    protected javax.swing.JMenuItem menuAbout;
    protected javax.swing.JMenuItem menuAlphaBright;
    protected javax.swing.JMenuItem menuAlphaColor;
    protected javax.swing.JMenuBar menuBar;
    protected javax.swing.JMenuItem menuBoxBlur;
    protected javax.swing.JMenuItem menuCountRGB;
    protected javax.swing.JMenu menuCounting;
    protected javax.swing.JMenu menuDebug;
    protected javax.swing.JMenuItem menuDoB;
    protected javax.swing.JMenuItem menuDoG;
    protected javax.swing.JMenuItem menuDotRemove;
    protected javax.swing.JMenu menuExport;
    protected javax.swing.JMenu menuExportAll;
    protected javax.swing.JMenuItem menuExportAllImages;
    protected javax.swing.JMenuItem menuExportAllLayers;
    protected javax.swing.JMenuItem menuExportAllStacks;
    protected javax.swing.JMenu menuExportSingle;
    protected javax.swing.JMenuItem menuExportSingleImage;
    protected javax.swing.JMenuItem menuExportSingleLayer;
    protected javax.swing.JMenuItem menuExportSingleStack;
    protected javax.swing.JMenu menuFile;
    protected javax.swing.JMenuItem menuFileImportImage;
    protected javax.swing.JMenuItem menuFileImportImages;
    protected javax.swing.JMenuItem menuFileImportLayers;
    protected javax.swing.JMenuItem menuFileImportLayersThis;
    protected javax.swing.JMenuItem menuFileImportMultichannel;
    protected javax.swing.JMenuItem menuFileImportStacks;
    protected javax.swing.JMenuItem menuFileUndo;
    protected javax.swing.JMenuItem menuFillArea;
    protected javax.swing.JMenuItem menuFillGap;
    protected javax.swing.JMenuItem menuFilterSmallDimension;
    protected javax.swing.JMenuItem menuFilterSmallSize;
    protected javax.swing.JMenu menuFilters;
    protected javax.swing.JMenuItem menuGamma;
    protected javax.swing.JMenuItem menuGrayscale;
    protected javax.swing.JMenuItem menuHiCut;
    protected javax.swing.JMenuItem menuHiCutAuto;
    protected javax.swing.JMenuItem menuHiCutPeak;
    protected javax.swing.JMenu menuImport;
    protected javax.swing.JMenuItem menuInterpolate;
    protected javax.swing.JMenuItem menuLightness;
    protected javax.swing.JMenuItem menuLoCut;
    protected javax.swing.JMenuItem menuLoCutAuto;
    protected javax.swing.JMenuItem menuLoCutNoise;
    protected javax.swing.JMenuItem menuMapHue;
    protected javax.swing.JMenuItem menuMapLuminescence;
    protected javax.swing.JMenuItem menuMultiplyPxls;
    protected javax.swing.JMenuItem menuOpaque;
    protected javax.swing.JMenuItem menuProtocolCopyMask;
    protected javax.swing.JMenuItem menuProtocolIFMask;
    protected javax.swing.JMenuItem menuProtocolNucleusSep;
    protected javax.swing.JMenu menuProtocols;
    protected javax.swing.JMenuItem menuSharpCorner;
    protected javax.swing.JMenuItem menuShrinkEdge;
    protected javax.swing.JMenuItem menuSmoothen;
    protected javax.swing.JMenuItem menuSpreadEdge;
    protected javax.swing.JMenuItem menuTongaExit;
    protected javax.swing.JMenu menuWizard;
    protected javax.swing.JButton openExcel;
    protected javax.swing.JProgressBar progressBar;
    protected javax.swing.JPanel protocolHeadPanel;
    public javax.swing.JLabel protocolName;
    protected javax.swing.JSeparator protocolNameSeparator;
    protected javax.swing.JPanel protocolPanel;
    protected javax.swing.JPanel protocolSettingsPanel;
    protected javax.swing.JSeparator protocolSettingsSeparator;
    protected javax.swing.JSplitPane rSplitPane;
    protected javax.swing.JScrollPane resultScrollPane;
    protected javax.swing.JTable resultTableComponent;
    protected javax.swing.JPanel resultsPanel;
    protected javax.swing.JPanel settingPanelFile;
    protected javax.swing.JPanel settingPanelGeneral;
    protected javax.swing.JPanel settingPanelLayer;
    protected javax.swing.JPanel settingPanelScale;
    protected javax.swing.JMenuItem shapeCircle;
    protected javax.swing.JPopupMenu shapeMenu;
    protected javax.swing.JMenuItem shapeOval;
    protected javax.swing.JMenuItem shapePoly;
    protected javax.swing.JMenuItem shapeRectangle;
    protected javax.swing.JComboBox<String> stackCombo;
    protected javax.swing.JToggleButton stackToggle;
    public javax.swing.JTabbedPane tabbedPane;
    protected javax.swing.JSplitPane vSplitPane;
    // End of variables declaration//GEN-END:variables

}
