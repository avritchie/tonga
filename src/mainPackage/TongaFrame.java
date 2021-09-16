package mainPackage;

import mainPackage.protocols.*;
import mainPackage.filters.*;
import mainPackage.counters.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.embed.swing.JFXPanel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import mainPackage.CachedImage.CacheBuffer;
import mainPackage.JRangeSlider.RangeSliderUI;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelCreator.ControlType;

/**
 *
 * @author Victoria
 */
public class TongaFrame extends javax.swing.JFrame {

    /*
    --module-path "C:\Users\Victoria\AppData\Roaming\NetBeans\Libraries\javafx\lib" --add-modules "javafx.base,javafx.graphics,javafx.swing"
     */
    ArrayList<Image> mainIcons;
    Protocol currentProtocol;
    Filter currentFilter;
    Counter currentCounter;
    JFXPanel panelBig, panelSmall;
    JRangeSlider histoRange;
    public Map<String, Filter> filterHistory;
    boolean accelDisabled, historyAdjusting;
    Integer resultHash;
    Loader loaderDialog;
    InfoDialog infoDialog;
    Wizard wizardDialog;

    public TongaFrame() {
        loadIconKit();
        initComponents();
        initExtraComponents();
        initFilterList();
        createDialogs();
        createPanels();
        constructFilterPopupMenu();
        constructToolTipSystem();
        constructMenuBar();
        createCloseHandler();
    }

    void display() {
        int pw = Math.max(1200, Math.min(getPreferredSize().width, Tonga.screenWidth - 200));
        int ph = Math.max(800, Math.min(getPreferredSize().height, Tonga.screenHeight - 100));
        setSize(pw, ph);
        setLocationRelativeTo(null);
        setVisible(true);
        loaderDialog.setVisible(false);
        loaderDialog.setTitle("Processing...");
    }

    private void createDialogs() {
        infoDialog = new InfoDialog(readVersion());
        infoDialog.setLocationRelativeTo(null);
        infoDialog.setIconImages(mainIcons);
        closeListener(infoDialog);
        wizardDialog = new Wizard();
        wizardDialog.setLocationRelativeTo(null);
        wizardDialog.setIconImages(mainIcons);
        closeListener(wizardDialog);
        loaderDialog = new Loader(progressBar);
        loaderDialog.setLocationRelativeTo(null);
        loaderDialog.setIconImages(mainIcons);
        loaderDialog.setVisible(true);
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
        menuFile.setEnabled(yesorno);
        menuFilters.setEnabled(yesorno);
        menuProtocols.setEnabled(yesorno);
        menuCounting.setEnabled(yesorno);
        menuDebug.setEnabled(yesorno);
    }

    private static JFXPanel createJFXPanel(JPanel panel) {
        JFXPanel fx = new JFXPanel();
        panel.setLayout(new BorderLayout());
        panel.add(fx, BorderLayout.CENTER);
        fx.setSize(panel.getSize());
        return fx;
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
    }

    protected void launchProtocol(Supplier<Protocol> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("Open images before launching protocols");
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
            updateMainLabel("Open images before launching protocols");
        } else if (currentProtocol == null) {
            updateMainLabel("Select a protocol before running it");
        } else {
            currentProtocol.getParams();
            if (currentProtocol.checkEqualSize()) {
                Thread thread = new Thread(() -> {
                    try {
                        currentProtocol.runProtocol(all);
                    } catch (Exception ex) {
                        Tonga.loader().fail = true;
                        Tonga.catchError(ex, "The protocol crashed.");
                    }
                });
                Tonga.bootThread(thread, protocolName.getText(), false);
            } else {
                updateMainLabel("Please make sure all the selected layers are the same size.");
            }
        }
    }

    private void instantFilter(Supplier<Filter> method, boolean all, Object... parameters) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("No image to process");
        } else {
            Filter instaFilter = method.get();
            Thread thread = new Thread(() -> {
                instaFilter.param.setFilterParameters(instaFilter.parameterData, parameters);
                try {
                    Filter.publish(all ? instaFilter.runAll() : instaFilter.runSingle(), instaFilter.getName());
                } catch (Exception ex) {
                    Tonga.loader().fail = true;
                    Tonga.catchError(ex, "The filter crashed.");
                }
            });
            Tonga.bootThread(thread, instaFilter.getName(), false);
        }
    }

    private void launchFilter(Supplier<Filter> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("Open images before launching filters");
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
            updateMainLabel("Open images before launching filters");
        } else if (currentFilter == null) {
            updateMainLabel("Select a filter before running it");
        } else {
            String filterName = filterCombo.getSelectedItem().toString();
            updateHistory(filterName);
            currentFilter.getParams();
            if (true) {
                Thread thread = new Thread(() -> {
                    try {
                        Filter.publish(all ? currentFilter.runAll() : currentFilter.runSingle(), currentFilter.getName());
                    } catch (Exception ex) {
                        Tonga.loader().fail = true;
                        Tonga.catchError(ex, "The filter crashed.");
                    }
                });
                Tonga.bootThread(thread, currentFilter.getName(), false);
            }
        }
    }

    private void executeHistoFilter(Supplier<Filter> method, boolean all) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("Open images before adjusting values");
        } else {
            if (true) {
                Filter filter = method.get();
                Thread thread = new Thread(() -> {
                    if (filter.parameterData == Filter.limits) {
                        filter.param.setFilterParameters(filter.parameterData,
                                histoRange.getUpperValue(), histoRange.getValue());
                    }
                    try {
                        Filter.publish(all ? filter.runAll() : filter.runSingle(), filter.getName());
                    } catch (Exception ex) {
                        Tonga.loader().fail = true;
                        Tonga.catchError(ex, "The scaler crashed.");
                    }
                });
                Tonga.bootThread(thread, filter.getName(), false);
            }
        }
    }

    protected void executeCounter(boolean all) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("Open images before launching counters");
        } else if (currentCounter == null) {
            updateMainLabel("Select a counter before running it");
        } else {
            Thread thread = new Thread(() -> {
                try {
                    Counter.publish(all ? currentCounter.runAll() : currentCounter.runSingle());
                } catch (Exception ex) {
                    Tonga.loader().fail = true;
                    Tonga.catchError(ex, "The counter crashed.");
                }
            });
            Tonga.bootThread(thread, currentCounter.counterName, false);
        }
    }

    protected void launchCounter(Supplier<Counter> method, ActionEvent evt) {
        if (!Tonga.thereIsImage()) {
            updateMainLabel("Open images before launching counters");
        } else {
            currentProtocol = null;
            currentCounter = method.get();
            tabbedPane.setSelectedIndex(0);
            updatePanel(new PanelCreator().getPanel(), protocolSettingsPanel);
            protocolName.setText(currentCounter.counterName);
        }
    }

    public void updateSavePath(File file) {
        if (filePathField.getText().isEmpty()) {
            String out = Tonga.formatPath(file.getParent() + "\\output");
            String out2 = out;
            int ind = 0;
            while (new File(out2).exists()) {
                out2 = out + ind;
                ind++;
            }
            filePathField.setText(out2);
        }
    }

    public void updateMainLabel(String str) {
        bleftLabel.setText("<html>" + str + "</html>");
    }

    public void updateZoomLabel(int xx, int yy, double mainFactor, double zoomFactor) {
        brightLabel.setText(
                (!Tonga.thereIsImage() ? ("-.-") : (xx + "." + yy)) + " | "
                + Math.round(1000 * mainFactor) / 10 + "% | "
                + Math.round(1000 * zoomFactor) / 10 + "%");
    }

    public static void colourSelect(Object control) {
        Color cc = JColorChooser.showDialog(null, "Choose a color", ((JButton) control).getBackground());
        if (cc != null) {
            ((JButton) control).setBackground(cc);
        }
    }

    public void enableDisableControls(ArrayList<TongaImage> picList) {
        boolean disabled = picList.isEmpty();
        boxSettingNoRAM.setEnabled(disabled);
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

    public static final void handleComponents(Container cont) {
        Component[] components = cont.getComponents();
        for (Component component : components) {
            Class c = component.getClass();
            if (c.equals(JToggleButton.class)) {
                if (((JToggleButton) component).isSelected()) {
                    ((JToggleButton) component).setBackground(Color.getHSBColor((float) 0.3, (float) 0.85, (float) 0.7));
                    ((JToggleButton) component).setText("YES");
                } else {
                    ((JToggleButton) component).setBackground(Color.getHSBColor(0, (float) 0.95, (float) 0.675));
                    ((JToggleButton) component).setText("NO");
                }
                ((JToggleButton) component).addActionListener((java.awt.event.ActionEvent evt) -> {
                    JToggleButton source = (JToggleButton) evt.getSource();
                    if (source.isSelected()) {
                        source.setBackground(Color.getHSBColor((float) 0.3, (float) 0.85, (float) 0.7));
                        source.setText("YES");
                    } else {
                        source.setBackground(Color.getHSBColor(0, (float) 0.95, (float) 0.675));
                        source.setText("NO");
                    }
                });
                ((JToggleButton) component).addItemListener((java.awt.event.ItemEvent evt) -> {
                    JToggleButton source = (JToggleButton) evt.getSource();
                    if (source.isSelected()) {
                        source.setBackground(Color.getHSBColor((float) 0.3, (float) 0.85, (float) 0.7));
                        source.setText("YES");
                    } else {
                        source.setBackground(Color.getHSBColor(0, (float) 0.95, (float) 0.675));
                        source.setText("NO");
                    }
                });
            }
            if (c.equals(JButton.class)) {
                if (((JButton) component).getText().isEmpty()) {
                    ((JButton) component).addActionListener((ActionEvent evt) -> {
                        JButton source = (JButton) evt.getSource();
                        TongaFrame.colourSelect(source);
                    });
                }
            }
            if (c.equals(JSpinner.class)) {
                ((JSpinner) component).addMouseWheelListener((MouseWheelEvent mwe) -> {
                    JSpinner source = (JSpinner) mwe.getSource();
                    if (source.isEnabled()) {
                        source.setValue(Integer.parseInt((source.getValue().toString())) - mwe.getWheelRotation());
                    }
                });
            }
            if (c.equals(JSlider.class)) {
                ((JSlider) component).addMouseWheelListener((MouseWheelEvent mwe) -> {
                    JSlider source = (JSlider) mwe.getSource();
                    if (source.isEnabled()) {
                        source.setValue(source.getValue() - mwe.getWheelRotation());
                    }
                });
            }
            if (c.equals(JRangeSlider.class)) {
                ((JRangeSlider) component).addMouseWheelListener((MouseWheelEvent mwe) -> {
                    JRangeSlider source = (JRangeSlider) mwe.getSource();
                    if (source.isEnabled()) {
                        int x = mwe.getX();
                        int lx = ((RangeSliderUI) source.getUI()).getLowerX() - x;
                        int ux = ((RangeSliderUI) source.getUI()).getUpperX() - x;
                        boolean upper = (Math.abs(lx) > Math.abs(ux));
                        if (lx == ux) {
                            upper = x > ux;
                        }
                        if (upper) {
                            source.setUpperValue(source.getUpperValue() - mwe.getWheelRotation());
                        } else {
                            source.setValue(source.getValue() - mwe.getWheelRotation());
                        }
                    }
                });
            }
        }
    }

    private void constructFilterPopupMenu() {
        for (Component c : menuFilters.getMenuComponents()) {
            if (c.getClass().equals(Separator.class)) {
                filterMenu.add(new JSeparator());
            } else {
                filterMenu.add(deepCloneMenu((JMenu) c));
            }
        }
    }

    private void constructToolTipSystem() {
        ToolTipManager.sharedInstance().setEnabled(false);
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            deepToolTipMenu(menuBar.getMenu(i));
        }
        panelToolTips(generalPanel);
        panelToolTips(filterPanel);
        addToolTipListener(stackToggle);
    }

    private void initExtraComponents() {
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
        handleComponents(histoSliderPanel);
        //menuDebug.setVisible(false);
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

    private void addToolTipListener(Component c) {
        c.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                updateMainLabel(((JComponent) c).getToolTipText());
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
                i.setText(((JMenuItem) sc).getText());
                // MAKE SURE ALL ENTRIES HAVE ACTION LISTENERS
                i.addActionListener(((JMenuItem) sc).getActionListeners()[0]);
                tt = ((JMenuItem) sc).getToolTipText();
                if (tt != null && !tt.isEmpty()) {
                    i.setToolTipText(tt);
                    addToolTipListener(i);
                }
                nm.add(i);
            }
        }
        return nm;
    }

    private static void updateLabel(JLabel label, JSlider slider) {
        label.setText("(" + slider.getValue() + ")");
    }

    private void clearResultTable() {
        resultTable.setModel(new DefaultTableModel());
        resultTable.repaint();
    }

    private void refresh() {
        if (Tonga.frame() != null && Tonga.frame().isVisible()) {
            Tonga.refreshCanvases();
        }
    }

    private void constructMenuBar() {
        int menus = menuBar.getMenuCount();
        for (int i = 0; i < menus; i++) {
            JMenu m = menuBar.getMenu(i);
            m.addMenuListener(new javax.swing.event.MenuListener() {
                public void menuCanceled(javax.swing.event.MenuEvent evt) {
                }

                public void menuDeselected(javax.swing.event.MenuEvent evt) {
                }

                @Override
                public void menuSelected(javax.swing.event.MenuEvent evt) {
                    accelEnable(true);
                    setUndoRedoMenu();
                }
            });
        }
    }

    void setUndoRedoMenu() {
        menuFileUndo.setEnabled(UndoRedo.undoList != null);
        menuFileRedo.setEnabled(UndoRedo.redoList != null);
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

    private String readVersion() {
        ResourceBundle rb = ResourceBundle.getBundle("version");
        String num;
        num = rb.getString("buildnumber");
        return num;
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
                cleanAndShutDown();
            }
        });
    }

    private void cleanAndShutDown() {
        if (resultTable.getModel().getRowCount() > 0 && (resultHash == null || resultTable.getModel().hashCode() != resultHash)
                && !Tonga.askYesNo("Save before exit", "You have unsaved results in the results table. Do you want to exit without saving them?", true, true)) {
            return;
        }
        this.setVisible(false);
        try {
            ArrayList<CacheBuffer> remainingCache = new ArrayList<>(Tonga.cachedData);
            //System.out.println(remainingCache.toString());
            remainingCache.forEach(f -> {
                f.freeCache();
            });
            File dir = new File(System.getProperty("java.io.tmpdir") + "/Tonga");
            for (File file : dir.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } catch (Exception ex) {
            Tonga.catchError(ex, "Cache freeing failed.");
        }
        Settings.saveConfigFiles();
        System.out.println("Tonga: succesful exit");
        System.exit(0);
    }

    private void createPanels() {
        panelBig = createJFXPanel(imageBig);
        panelSmall = createJFXPanel(imageZoom);
    }

    protected void resultHash() {
        resultHash = resultTable.getModel().hashCode();
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
        contLayMerge = new javax.swing.JMenuItem();
        contLayDeleteThis = new javax.swing.JMenuItem();
        contLayDeleteAll = new javax.swing.JMenuItem();
        contextImageMenu = new javax.swing.JPopupMenu();
        contImgRename = new javax.swing.JMenuItem();
        contImgDelete = new javax.swing.JMenuItem();
        contextResultMenu = new javax.swing.JPopupMenu();
        contResClear = new javax.swing.JMenuItem();
        filterMenu = new javax.swing.JPopupMenu();
        frame = new javax.swing.JPanel();
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
        leftPanel = new javax.swing.JPanel();
        imageZoom = new javax.swing.JPanel();
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
        protocolPanel3 = new javax.swing.JPanel();
        boxSettingAlphaBG = new javax.swing.JCheckBox();
        layerBackColor = new javax.swing.JButton();
        stackCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        protocolPanel4 = new javax.swing.JPanel();
        boxSettingNoRAM = new javax.swing.JCheckBox();
        boxSettingResultsAppend = new javax.swing.JCheckBox();
        boxSettingMultiThreading = new javax.swing.JCheckBox();
        protocolPanel1 = new javax.swing.JPanel();
        autoscaleLabel = new javax.swing.JLabel();
        autoscaleCombo = new javax.swing.JComboBox<>();
        boxSettingAutoscale1 = new javax.swing.JCheckBox();
        protocolPanel5 = new javax.swing.JPanel();
        filePathField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        boxSettingOpenAfter = new javax.swing.JCheckBox();
        resultsPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        exportAsCSV = new javax.swing.JButton();
        openExcel = new javax.swing.JButton();
        histogramPanel = new javax.swing.JPanel();
        histoImg = new javax.swing.JPanel();
        histoLabel = new javax.swing.JLabel();
        histoAdjAutoSingle = new javax.swing.JButton();
        histoAdjApplySingle = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JSeparator();
        histoAdjApplyAll = new javax.swing.JButton();
        histoAdjAutoAll = new javax.swing.JButton();
        histoSliderPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        jSeparator1 = new javax.swing.JSeparator();
        brightLabel = new javax.swing.JLabel();
        bleftLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuFileUndo = new javax.swing.JMenuItem();
        menuFileRedo = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        menuImport = new javax.swing.JMenu();
        menuFileImportStacks = new javax.swing.JMenuItem();
        menuFileImportImages = new javax.swing.JMenuItem();
        menuFileImportImage = new javax.swing.JMenuItem();
        menuFileImportLayers = new javax.swing.JMenuItem();
        menuFileImportLayersThis = new javax.swing.JMenuItem();
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
        menuAbout = new javax.swing.JMenuItem();
        menuTongaExit = new javax.swing.JMenuItem();
        menuFilters = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenu6 = new javax.swing.JMenu();
        menuGamma = new javax.swing.JMenuItem();
        jMenuItem56 = new javax.swing.JMenuItem();
        jMenuItem70 = new javax.swing.JMenuItem();
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
        jMenuItem16 = new javax.swing.JMenuItem();
        jMenu14 = new javax.swing.JMenu();
        jMenuItem64 = new javax.swing.JMenuItem();
        jMenuItem59 = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        menuBoxBlur = new javax.swing.JMenuItem();
        jMenuItem49 = new javax.swing.JMenuItem();
        jMenu15 = new javax.swing.JMenu();
        jMenuItem29 = new javax.swing.JMenuItem();
        jMenuItem27 = new javax.swing.JMenuItem();
        jMenuItem26 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        jMenuItem51 = new javax.swing.JMenuItem();
        jMenuItem52 = new javax.swing.JMenuItem();
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
        jMenuItem40 = new javax.swing.JMenuItem();
        menuFilterSmallSize = new javax.swing.JMenuItem();
        menuFilterSmallDimension = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem67 = new javax.swing.JMenuItem();
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
        jMenuItem25 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItem69 = new javax.swing.JMenuItem();
        jMenuItem73 = new javax.swing.JMenuItem();
        jMenuItem75 = new javax.swing.JMenuItem();
        jMenuItem58 = new javax.swing.JMenuItem();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenuItem63 = new javax.swing.JMenuItem();
        jMenu17 = new javax.swing.JMenu();
        jMenuItem46 = new javax.swing.JMenuItem();
        jMenuItem54 = new javax.swing.JMenuItem();
        menuSubstract = new javax.swing.JMenuItem();
        menuProtocols = new javax.swing.JMenu();
        jMenuItem55 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenu2 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        menuProtocolCKMask = new javax.swing.JMenuItem();
        jMenuItem48 = new javax.swing.JMenuItem();
        jMenuItem39 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem68 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem22 = new javax.swing.JMenuItem();
        menuProtocolNucleusSep = new javax.swing.JMenuItem();
        jMenu18 = new javax.swing.JMenu();
        jMenuItem44 = new javax.swing.JMenuItem();
        jMenuItem37 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem18 = new javax.swing.JMenuItem();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenuItem21 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem78 = new javax.swing.JMenuItem();
        jMenuItem71 = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        jMenu9 = new javax.swing.JMenu();
        jMenuItem19 = new javax.swing.JMenuItem();
        jMenuItem33 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItem42 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem72 = new javax.swing.JMenuItem();
        jMenuItem74 = new javax.swing.JMenuItem();
        menuCounting = new javax.swing.JMenu();
        jMenu1 = new javax.swing.JMenu();
        menuCountRGB = new javax.swing.JMenuItem();
        jMenuItem65 = new javax.swing.JMenuItem();
        jMenuItem28 = new javax.swing.JMenuItem();
        menuDebug = new javax.swing.JMenu();
        debugMemory = new javax.swing.JMenuItem();
        debugSysInfo = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        debugParameter = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        debugExecuteClass = new javax.swing.JMenuItem();
        jMenuItem38 = new javax.swing.JMenuItem();
        debugTestFilter = new javax.swing.JMenuItem();
        debugTestProtocols = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem36 = new javax.swing.JMenuItem();
        jMenuItem76 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();

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

        contLayMerge.setText("Merge into one");
        contLayMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contLayMergeActionPerformed(evt);
            }
        });
        contextLayerMenu.add(contLayMerge);

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

        contImgRename.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        contImgRename.setText("Rename");
        contImgRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgRenameActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgRename);

        contImgDelete.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        contImgDelete.setText("Delete");
        contImgDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contImgDeleteActionPerformed(evt);
            }
        });
        contextImageMenu.add(contImgDelete);

        contResClear.setText("Clear");
        contResClear.setToolTipText("");
        contResClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contResClearActionPerformed(evt);
            }
        });
        contextResultMenu.add(contResClear);

        filterMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
                filterMenuPopupMenuCanceled(evt);
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

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
        lSplitPane.setDividerLocation(778);
        lSplitPane.setDividerSize(6);
        lSplitPane.setResizeWeight(0.5);
        lSplitPane.setMinimumSize(new java.awt.Dimension(600, 400));
        lSplitPane.setPreferredSize(new java.awt.Dimension(1200, 800));

        imageBig.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        imageBig.setMinimumSize(new java.awt.Dimension(400, 400));
        imageBig.setName(""); // NOI18N
        imageBig.setPreferredSize(new java.awt.Dimension(600, 600));

        javax.swing.GroupLayout imageBigLayout = new javax.swing.GroupLayout(imageBig);
        imageBig.setLayout(imageBigLayout);
        imageBigLayout.setHorizontalGroup(
            imageBigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 774, Short.MAX_VALUE)
        );
        imageBigLayout.setVerticalGroup(
            imageBigLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 855, Short.MAX_VALUE)
        );

        lSplitPane.setLeftComponent(imageBig);

        leftPanel.setPreferredSize(new java.awt.Dimension(400, 600));

        imageZoom.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        imageZoom.setMaximumSize(new java.awt.Dimension(800, 800));
        imageZoom.setMinimumSize(new java.awt.Dimension(200, 200));
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
            .addGap(0, 0, Short.MAX_VALUE)
        );
        imageZoomLayout.setVerticalGroup(
            imageZoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 409, Short.MAX_VALUE)
        );

        tabbedPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        tabbedPane.setMinimumSize(new java.awt.Dimension(100, 300));
        tabbedPane.setPreferredSize(new java.awt.Dimension(400, 400));

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
            .addGap(0, 301, Short.MAX_VALUE)
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
                    .addComponent(protocolSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
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
                .addComponent(protocolSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
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
            .addGap(0, 417, Short.MAX_VALUE)
        );
        filterSettingsPanelLayout.setVerticalGroup(
            filterSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 292, Short.MAX_VALUE)
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
                    .addComponent(filterSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
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
                    .addComponent(browseFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterNameSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(filterSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterSettingsSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(filterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRunSingle2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRunAll2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabbedPane.addTab("Filter", filterPanel);

        protocolPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Layer settings"));
        protocolPanel3.setPreferredSize(new java.awt.Dimension(370, 252));

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

        stackCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Add", "Multiply", "Exclude", "Difference", "Lighten", "Darken", "Overlay" }));
        stackCombo.setPreferredSize(new java.awt.Dimension(100, 20));
        stackCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stackComboActionPerformed(evt);
            }
        });

        jLabel2.setText("Stack mode");
        jLabel2.setToolTipText("");

        javax.swing.GroupLayout protocolPanel3Layout = new javax.swing.GroupLayout(protocolPanel3);
        protocolPanel3.setLayout(protocolPanel3Layout);
        protocolPanel3Layout.setHorizontalGroup(
            protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel3Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolPanel3Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(boxSettingAlphaBG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(layerBackColor, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stackCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        protocolPanel3Layout.setVerticalGroup(
            protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, protocolPanel3Layout.createSequentialGroup()
                .addGroup(protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(layerBackColor, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(boxSettingAlphaBG, javax.swing.GroupLayout.PREFERRED_SIZE, 20, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stackCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30))
        );

        protocolPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("General settings"));
        protocolPanel4.setPreferredSize(new java.awt.Dimension(370, 252));

        boxSettingNoRAM.setText("work on disk");
        boxSettingNoRAM.setToolTipText("Only a file location pointer is imported - images are imported upon running a filter/protocol and the result is saved on disk.");
        boxSettingNoRAM.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingNoRAM.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingNoRAM.setPreferredSize(new java.awt.Dimension(120, 23));
        boxSettingNoRAM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxSettingNoRAMActionPerformed(evt);
            }
        });

        boxSettingResultsAppend.setText("append results");
        boxSettingResultsAppend.setToolTipText("Append new results to the previous results instead of clearing the list automatically");
        boxSettingResultsAppend.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingResultsAppend.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingResultsAppend.setPreferredSize(new java.awt.Dimension(120, 23));

        boxSettingMultiThreading.setSelected(true);
        boxSettingMultiThreading.setText("multithreading");
        boxSettingMultiThreading.setToolTipText("Use multiple threads to execute protocols. Depending on the computer this can provide a large speed boost when processing multiple images.");
        boxSettingMultiThreading.setMaximumSize(new java.awt.Dimension(200, 23));
        boxSettingMultiThreading.setMinimumSize(new java.awt.Dimension(80, 23));
        boxSettingMultiThreading.setPreferredSize(new java.awt.Dimension(120, 23));

        javax.swing.GroupLayout protocolPanel4Layout = new javax.swing.GroupLayout(protocolPanel4);
        protocolPanel4.setLayout(protocolPanel4Layout);
        protocolPanel4Layout.setHorizontalGroup(
            protocolPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(boxSettingMultiThreading, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(boxSettingNoRAM, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(boxSettingResultsAppend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        protocolPanel4Layout.setVerticalGroup(
            protocolPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(protocolPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxSettingNoRAM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingResultsAppend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingMultiThreading, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        protocolPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Autoscaling settings"));
        protocolPanel1.setPreferredSize(new java.awt.Dimension(370, 252));

        autoscaleLabel.setText("Autoscaling");
        autoscaleLabel.setToolTipText("Autoscale >8-bit images automatically based on the histogram. Please note that this is only a visual change.");

        autoscaleCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Image sets", "Every image" }));
        autoscaleCombo.setSelectedIndex(1);
        autoscaleCombo.setToolTipText("Autoscaling can be done for every image separately, or for a set of images (=channels) jointly to maintain visual comparability");
        autoscaleCombo.setPreferredSize(new java.awt.Dimension(100, 20));
        autoscaleCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoscaleComboActionPerformed(evt);
            }
        });

        boxSettingAutoscale1.setText("aggressive");
        boxSettingAutoscale1.setToolTipText("The autoscaling has adaptation to ignore single bright dots etc.");

        javax.swing.GroupLayout protocolPanel1Layout = new javax.swing.GroupLayout(protocolPanel1);
        protocolPanel1.setLayout(protocolPanel1Layout);
        protocolPanel1Layout.setHorizontalGroup(
            protocolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(autoscaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(autoscaleCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(boxSettingAutoscale1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        protocolPanel1Layout.setVerticalGroup(
            protocolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel1Layout.createSequentialGroup()
                .addGroup(protocolPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(autoscaleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(autoscaleCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxSettingAutoscale1))
                .addGap(0, 20, Short.MAX_VALUE))
        );

        protocolPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("File output"));
        protocolPanel5.setPreferredSize(new java.awt.Dimension(370, 252));

        browseButton.setText("Browse...");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        boxSettingOpenAfter.setSelected(true);
        boxSettingOpenAfter.setText("open the folder after export");

        javax.swing.GroupLayout protocolPanel5Layout = new javax.swing.GroupLayout(protocolPanel5);
        protocolPanel5.setLayout(protocolPanel5Layout);
        protocolPanel5Layout.setHorizontalGroup(
            protocolPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(boxSettingOpenAfter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(protocolPanel5Layout.createSequentialGroup()
                        .addComponent(filePathField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton)))
                .addContainerGap())
        );
        protocolPanel5Layout.setVerticalGroup(
            protocolPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolPanel5Layout.createSequentialGroup()
                .addGroup(protocolPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filePathField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(boxSettingOpenAfter)
                .addContainerGap(59, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout generalPanelLayout = new javax.swing.GroupLayout(generalPanel);
        generalPanel.setLayout(generalPanelLayout);
        generalPanelLayout.setHorizontalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(protocolPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                    .addComponent(protocolPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                    .addComponent(protocolPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                    .addComponent(protocolPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))
                .addContainerGap())
        );
        generalPanelLayout.setVerticalGroup(
            generalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(protocolPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(protocolPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane.addTab("General", generalPanel);

        resultTable.setAutoCreateRowSorter(true);
        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                resultTableMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(resultTable);

        exportAsCSV.setText("Export as CSV");
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

        javax.swing.GroupLayout resultsPanelLayout = new javax.swing.GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(exportAsCSV, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openExcel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(exportAsCSV, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(openExcel, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))
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
            .addComponent(histoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
        );
        histoImgLayout.setVerticalGroup(
            histoImgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(histoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
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
            .addGap(0, 421, Short.MAX_VALUE)
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
                        .addComponent(histoAdjApplySingle, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(histoAdjApplyAll, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE))
                    .addComponent(jSeparator9, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(histogramPanelLayout.createSequentialGroup()
                        .addComponent(histoAdjAutoSingle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(histoAdjAutoAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(histoImg, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                    .addComponent(histoSliderPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))
                .addContainerGap())
        );
        histogramPanelLayout.setVerticalGroup(
            histogramPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(histogramPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(histoImg, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(imageZoom, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addComponent(imageZoom, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 435, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        tabbedPane.getAccessibleContext().setAccessibleName("");

        lSplitPane.setRightComponent(leftPanel);

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
        menuFileUndo.setText("Undo");
        menuFileUndo.setToolTipText("Revert the latest action");
        menuFileUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileUndoActionPerformed(evt);
            }
        });
        menuFile.add(menuFileUndo);

        menuFileRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menuFileRedo.setText("Redo");
        menuFileRedo.setToolTipText("Revert the latest revert");
        menuFileRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileRedoActionPerformed(evt);
            }
        });
        menuFile.add(menuFileRedo);
        menuFile.add(jSeparator3);

        menuImport.setText("Import images");

        menuFileImportStacks.setText("Stack image(s)");
        menuFileImportStacks.setToolTipText("Import stack image files which have a fixed layer structure and might contain multiple images and layers.");
        menuFileImportStacks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileImportStacksActionPerformed(evt);
            }
        });
        menuImport.add(menuFileImportStacks);

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

        menuFilters.setText("Filters");

        jMenu12.setText("Basic");
        jMenu12.setToolTipText("Perform basic operations");

        jMenu6.setText("Correction");

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

        jMenuItem70.setText("Combined correction");
        jMenuItem70.setToolTipText("Perform a combination of illumination correction, gamma correction, and brightness autoscaling to get an evenly illuminated image.");
        jMenuItem70.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem70ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem70);

        jMenu12.add(jMenu6);

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

        jMenuItem30.setText("Extreme color");
        jMenuItem30.setToolTipText("Make every pixel have maximum colour");
        jMenuItem30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem30ActionPerformed(evt);
            }
        });
        jMenu26.add(jMenuItem30);

        jMenuItem32.setText("Extreme color & brightness");
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

        menuAlphaColor.setText("Covert colour to alpha");
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

        jMenuItem35.setText("Pop colour to a new layer");
        jMenuItem35.setToolTipText("Extract the strength of a certain colour to a new grayscale image");
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

        menuLoCut.setText("Scale darkness to range");
        menuLoCut.setToolTipText("Scale the image to a new range, stretching dark areas");
        menuLoCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoCutActionPerformed(evt);
            }
        });
        jMenu23.add(menuLoCut);

        menuHiCut.setText("Scale brightness to range");
        menuHiCut.setToolTipText("Scale the image to a new range, stretching bright areas");
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
        menuLoCutNoise.setToolTipText("Make almost black areas black");
        menuLoCutNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLoCutNoiseActionPerformed(evt);
            }
        });
        jMenu23.add(menuLoCutNoise);

        menuHiCutPeak.setText("Limit the highest values");
        menuHiCutPeak.setToolTipText("Make brightest areas less bright");
        menuHiCutPeak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHiCutPeakActionPerformed(evt);
            }
        });
        jMenu23.add(menuHiCutPeak);

        jMenu27.add(jMenu23);

        jMenuItem34.setText("Scale brightness to range");
        jMenuItem34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem34ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem34);

        jMenuItem41.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem41.setText("Scale brightness to maximum");
        jMenuItem41.setToolTipText("Same as above, but ignore certain percentage of the darkest and brightest values to remove noise and bright dots");
        jMenuItem41.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem41ActionPerformed(evt);
            }
        });
        jMenu27.add(jMenuItem41);

        jMenuItem60.setText("Scale brightness to maximum with adaptation");
        jMenuItem60.setToolTipText("Same as above, but ignore some of the darkest and brightest values to remove noise and bright dots");
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
        jMenuItem59.setToolTipText("Precise gaussian blurring without approximation (please note this is <font color=\"red\">slow</font>)");
        jMenuItem59.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem59ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem59);
        jMenu14.add(jSeparator6);

        menuBoxBlur.setText("Box blur");
        menuBoxBlur.setToolTipText("Precise box blurring without approximation (please note this is <font color=\"red\">slow</font>)");
        menuBoxBlur.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuBoxBlurActionPerformed(evt);
            }
        });
        jMenu14.add(menuBoxBlur);

        jMenuItem49.setText("Blur brightness");
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

        jMenuItem27.setText("Global Human Eye Threshold");
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
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem13);
        jMenu15.add(jSeparator13);

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

        menuDoG.setText("Difference of gaussians");
        menuDoG.setToolTipText("Two gaussian passes are substracted from each other to produce a detection for objects");
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
        jMenuItem53.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem53ActionPerformed(evt);
            }
        });
        jMenu20.add(jMenuItem53);

        menuDotRemove.setText("Connected dots");
        menuDotRemove.setToolTipText("Remove small dots");
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

        jMenuItem40.setText("Inner holes on object edges");
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

        jMenuItem77.setText("Objects touching the edges");
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

        jMenuItem25.setText("Distance to the edges");
        jMenuItem25.setToolTipText("Calculate the distances to the edges with the watershed-algorithm");
        jMenuItem25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem25ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem25);
        jMenu10.add(jSeparator5);

        jMenuItem69.setText("Find edges");
        jMenuItem69.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem69ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem69);

        jMenuItem73.setText("Find extended edges");
        jMenuItem73.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem73ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem73);

        jMenuItem75.setText("Find object overlap");
        jMenuItem75.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem75ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem75);

        jMenuItem58.setText("Find inner areas");
        jMenuItem58.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem58ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem58);

        jMenuItem15.setText("Find separated objects");
        jMenuItem15.setToolTipText("Identify objects which have been separated compared to an unseparated image");
        jMenuItem15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem15ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem15);

        jMenuItem63.setText("Find common objects");
        jMenuItem63.setToolTipText("Identify objects which are present on both of two images");
        jMenuItem63.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem63ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem63);

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

        menuSubstract.setText("Substract image from another");
        menuSubstract.setToolTipText("Substract the pixel values of an image from another image");
        menuSubstract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSubstractActionPerformed(evt);
            }
        });
        jMenu17.add(menuSubstract);

        menuFilters.add(jMenu17);

        menuBar.add(menuFilters);

        menuProtocols.setForeground(new java.awt.Color(0, 0, 255));
        menuProtocols.setText("Protocols");

        jMenuItem55.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem55.setForeground(new java.awt.Color(0, 0, 255));
        jMenuItem55.setText("Launch the wizard! ✨");
        jMenuItem55.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem55ActionPerformed(evt);
            }
        });
        menuProtocols.add(jMenuItem55);
        menuProtocols.add(jSeparator2);

        jMenu2.setText("Deprecated");

        jMenu4.setText("Experiment image analysis");

        menuProtocolCKMask.setText("Cytokeratin staining");
        menuProtocolCKMask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuProtocolCKMaskActionPerformed(evt);
            }
        });
        jMenu4.add(menuProtocolCKMask);

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

        jMenu3.setText("For tissues");
        menuProtocols.add(jMenu3);

        jMenu7.setText("For nuclei");

        jMenuItem18.setText("Count the number of nuclei");
        jMenuItem18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem18ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem18);

        jMenuItem14.setText("Count the ratio of positive nuclei");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem14);

        jMenuItem21.setText("Measure staining intensity");
        jMenuItem21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem21ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem21);

        jMenuItem11.setText("Measure double-staining intensity");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem11);

        jMenuItem1.setText("Measure surrounding intensity");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem1);
        jMenu7.add(jSeparator16);

        jMenuItem8.setText("Measure background staining");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem8);

        jMenuItem78.setText("Remove dead and dividing cells");
        jMenuItem78.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem78ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem78);

        jMenuItem71.setText("Estimate nucleus size");
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

        menuBar.add(menuProtocols);

        menuCounting.setText("Counting");

        jMenu1.setText("Old");

        menuCountRGB.setText("Count RGBCMYK");
        menuCountRGB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCountRGBActionPerformed(evt);
            }
        });
        jMenu1.add(menuCountRGB);

        jMenuItem65.setText("Count Black-Red stains");
        jMenuItem65.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem65ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem65);

        jMenuItem28.setText("Histogram distributions");
        jMenuItem28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem28ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem28);

        menuCounting.add(jMenu1);

        menuBar.add(menuCounting);

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

        jMenuItem6.setText("Debug function");
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

        debugTestProtocols.setText("Run test protocols");

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

        menuDebug.add(debugTestProtocols);

        menuBar.add(menuDebug);

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
                .addGap(18, 18, 18)
                .addComponent(brightLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        bleftLabel.getAccessibleContext().setAccessibleName("<html>Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!Welcome to Tonga!</html>");

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
        File[] files = IO.getFile(System.getProperty("user.dir"));
        if (files != null) {
            IO.importImages(Arrays.asList(files));
        }
    }//GEN-LAST:event_menuFileImportImagesActionPerformed

    private void menuFileImportLayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileImportLayersActionPerformed
        File[] files = IO.getFile(System.getProperty("user.dir"));
        if (files != null) {
            IO.importLayers(Arrays.asList(files), true);
        }
    }//GEN-LAST:event_menuFileImportLayersActionPerformed

    private void menuExportAllLayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportAllLayersActionPerformed
        IO.exportLayer(true);
    }//GEN-LAST:event_menuExportAllLayersActionPerformed

    private void menuTongaExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTongaExitActionPerformed
        cleanAndShutDown();
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
        File file = IO.exportTable(true);
        System.out.println("File exported to: " + file.getAbsolutePath());
        try {
            switch (Tonga.currentOS()) {
                case WINDOWS:
                    Runtime.getRuntime().exec("cmd /c start excel \"" + file.getAbsolutePath() + "\"");
                    break;
                case MAC:
                    new ProcessBuilder("open", "-a", "Microsoft Excel", file.getAbsolutePath()).start();
                    //exec("open -a \"Microsoft Excel\" \"" + file.getAbsolutePath() + "\"");
                    //exec("open /Applications/Microsoft Excel.app");
                    break;
                case UNKNOWN:
                    Tonga.catchError(new UnsupportedOperationException(), "No excel launcher for this OS.");
                    break;
            }
        } catch (IOException ex) {
            Tonga.catchError(ex, "Excel can not be started.");
        }
    }//GEN-LAST:event_openExcelActionPerformed

    private void layerBackColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layerBackColorActionPerformed
        TongaFrame.colourSelect(evt.getSource());
        refresh();
    }//GEN-LAST:event_layerBackColorActionPerformed

    private void menuAlphaColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAlphaColorActionPerformed
        launchFilter(Filters::alpha, evt);
    }//GEN-LAST:event_menuAlphaColorActionPerformed

    private void layersListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_layersListMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
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
        if (evt.getButton() == MouseEvent.BUTTON3) {
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
        launchFilter(ConnectEdges::run, evt);
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
        launchFilter(ShapenEdges::run, evt);
    }//GEN-LAST:event_menuSharpCornerActionPerformed

    private void jMenuItem28ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem28ActionPerformed
        //executeCounter(Counters.analHisto(), Quantitizer::runSingle);
        launchCounter(Counters::analHisto, evt);
    }//GEN-LAST:event_jMenuItem28ActionPerformed

    private void menuFillAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFillAreaActionPerformed
        launchFilter(FiltersPass::fillInnerAreas, evt);
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
        Tonga.freeMemory();
        long memn = Runtime.getRuntime().totalMemory();
        System.out.println("Initial memory: " + (mem / 1000000) + " MB");
        System.out.println("Current memory: " + (memn / 1000000) + " MB");
        System.out.println("Freed memory: " + ((mem - memn) / 1000000) + " MB");
    }//GEN-LAST:event_debugMemoryActionPerformed

    private void jMenuItem24ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem24ActionPerformed
        launchFilter(FilterCrisps::run, evt);
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
        launchFilter(Smoother::run, evt);
    }//GEN-LAST:event_menuSmoothenActionPerformed

    private void menuSubstractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSubstractActionPerformed
        launchProtocol(SubtractLayers::new, evt);
    }//GEN-LAST:event_menuSubstractActionPerformed

    private void menuProtocolCopyMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuProtocolCopyMaskActionPerformed
        launchProtocol(CopyAreas::new, evt);
    }//GEN-LAST:event_menuProtocolCopyMaskActionPerformed

    private void jMenuItem22ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem22ActionPerformed
        launchProtocol(MatrigelOrganoids::new, evt);
    }//GEN-LAST:event_jMenuItem22ActionPerformed

    private void menuProtocolCKMaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuProtocolCKMaskActionPerformed
        launchProtocol(Cytokeratin::new, evt);
    }//GEN-LAST:event_menuProtocolCKMaskActionPerformed

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
        filePathField.setText(IO.getFolder(filePathField.getText()));
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
        launchFilter(FiltersRender::renderStack, evt);
    }//GEN-LAST:event_jMenuItem46ActionPerformed

    private void menuFilterSmallSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFilterSmallSizeActionPerformed
        launchFilter(FiltersPass::filterObjectSize, evt);
    }//GEN-LAST:event_menuFilterSmallSizeActionPerformed

    private void menuFilterSmallDimensionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFilterSmallDimensionActionPerformed
        launchFilter(FiltersPass::filterObjectDimension, evt);
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

    private void resultTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseReleased
        if (evt.getButton() == MouseEvent.BUTTON3) {
            contextResultMenu.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_resultTableMouseReleased

    private void contResClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contResClearActionPerformed
        clearResultTable();
    }//GEN-LAST:event_contResClearActionPerformed

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
        File[] files = IO.getFile(System.getProperty("user.dir"));
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
        File[] files = IO.getFile(System.getProperty("user.dir"));
        if (files != null) {
            IO.importLayers(Arrays.asList(files), false);
        }
    }//GEN-LAST:event_menuFileImportLayersThisActionPerformed

    private void menuFileUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileUndoActionPerformed
        UndoRedo.undo();
    }//GEN-LAST:event_menuFileUndoActionPerformed

    private void menuFileRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileRedoActionPerformed
        UndoRedo.redo();
    }//GEN-LAST:event_menuFileRedoActionPerformed

    private void debugTestFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugTestFilterActionPerformed
        Test.run(Tonga.getLayer().layerImage);
    }//GEN-LAST:event_debugTestFilterActionPerformed

    private void jMenuItem56ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem56ActionPerformed
        launchFilter(Filters::illuminationCorrection, evt);
    }//GEN-LAST:event_jMenuItem56ActionPerformed

    private void jMenuItem55ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem55ActionPerformed
        if (Tonga.thereIsImage()) {
            popupDialog(wizardDialog);
        } else {
            updateMainLabel("Import the images to analyze first before opening the wizard");
        }
    }//GEN-LAST:event_jMenuItem55ActionPerformed

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

    private void boxSettingNoRAMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxSettingNoRAMActionPerformed
        UndoRedo.clear();
        refresh();
    }//GEN-LAST:event_boxSettingNoRAMActionPerformed

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
        File[] files = IO.getFile(System.getProperty("user.dir"));
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
        instantFilter(FiltersRender::renderStack, false);
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
        launchFilter(FiltersPass::fillInnerAreasSizeShape, evt);
    }//GEN-LAST:event_jMenuItem43ActionPerformed

    private void jMenuItem58ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem58ActionPerformed
        launchFilter(FiltersPass::innerAreas, evt);
    }//GEN-LAST:event_jMenuItem58ActionPerformed

    private void jMenuItem67ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem67ActionPerformed
        launchFilter(FiltersPass::filterObjectSizeShape, evt);
    }//GEN-LAST:event_jMenuItem67ActionPerformed

    private void jMenuItem69ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem69ActionPerformed
        launchFilter(FiltersPass::getEdgeMask, evt);
    }//GEN-LAST:event_jMenuItem69ActionPerformed

    private void debugSysInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugSysInfoActionPerformed
        System.out.println("This CPU has " + Runtime.getRuntime().availableProcessors() + " threads available.");
        switch (Tonga.currentOS) {
            case WINDOWS:
                System.out.println("The current operating system is Windows.");
                break;
            case MAC:
                System.out.println("The current operating system is Mac OS.");
                break;
            case UNKNOWN:
                System.out.println("Unknown operating system: " + System.getProperty("os.name"));
                break;
        }
        long mem = Runtime.getRuntime().totalMemory();
        long memm = Runtime.getRuntime().maxMemory();
        System.out.println("Max RAM memory available: " + (memm / 1000000) + " MB, currently " + (mem / 1000000) + " MB (" + ((int) ((double) mem / memm * 100)) + "%) used.");
        System.out.println("Application data path is: " + Tonga.getAppDataPath());
    }//GEN-LAST:event_debugSysInfoActionPerformed

    private void jMenuItem70ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem70ActionPerformed
        launchFilter(FiltersPass::evenIllumination, evt);
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
        launchFilter(FiltersPass::filterObjectSizeDimension, evt);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        TongaRender.resetHash();
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
        launchFilter(FiltersPass::getExtendedMask, evt);
    }//GEN-LAST:event_jMenuItem73ActionPerformed

    private void jMenuItem75ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem75ActionPerformed
        launchFilter(FiltersPass::getRadiusOverlap, evt);
    }//GEN-LAST:event_jMenuItem75ActionPerformed

    private void jMenuItem76ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem76ActionPerformed
        launchProtocol(__ObjectSegmentStrict::new, evt);
    }//GEN-LAST:event_jMenuItem76ActionPerformed

    private void jMenuItem77ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem77ActionPerformed
        launchFilter(FiltersPass::filterEdgeTouchers, evt);
    }//GEN-LAST:event_jMenuItem77ActionPerformed

    private void jMenuItem78ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem78ActionPerformed
        launchProtocol(__DeadDividing::new, evt);
    }//GEN-LAST:event_jMenuItem78ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JComboBox<String> autoscaleCombo;
    protected javax.swing.JLabel autoscaleLabel;
    protected javax.swing.JLabel bleftLabel;
    public javax.swing.JCheckBox boxSettingAlphaBG;
    protected javax.swing.JCheckBox boxSettingAutoscale1;
    protected javax.swing.JCheckBox boxSettingMultiThreading;
    protected javax.swing.JCheckBox boxSettingNoRAM;
    protected javax.swing.JCheckBox boxSettingOpenAfter;
    protected javax.swing.JCheckBox boxSettingResultsAppend;
    protected javax.swing.JLabel brightLabel;
    protected javax.swing.JButton browseButton;
    protected javax.swing.JButton browseFilter;
    protected javax.swing.JButton btnRunAll;
    protected javax.swing.JButton btnRunAll2;
    protected javax.swing.JButton btnRunSingle;
    protected javax.swing.JButton btnRunSingle2;
    protected javax.swing.JMenuItem contImgDelete;
    protected javax.swing.JMenuItem contImgRename;
    protected javax.swing.JMenuItem contLayDeleteAll;
    protected javax.swing.JMenuItem contLayDeleteThis;
    protected javax.swing.JMenuItem contLayMerge;
    protected javax.swing.JMenuItem contLayRename;
    protected javax.swing.JMenuItem contLaySelect;
    protected javax.swing.JMenuItem contResClear;
    protected javax.swing.JPopupMenu contextImageMenu;
    protected javax.swing.JPopupMenu contextLayerMenu;
    protected javax.swing.JPopupMenu contextResultMenu;
    protected javax.swing.JMenuItem debugExecuteClass;
    protected javax.swing.JMenuItem debugMemory;
    protected javax.swing.JMenuItem debugParameter;
    protected javax.swing.JMenuItem debugSysInfo;
    protected javax.swing.JMenuItem debugTestFilter;
    protected javax.swing.JMenu debugTestProtocols;
    protected javax.swing.JButton exportAsCSV;
    public javax.swing.JTextField filePathField;
    protected javax.swing.JComboBox<String> filterCombo;
    protected javax.swing.JPopupMenu filterMenu;
    protected javax.swing.JSeparator filterNameSeparator;
    protected javax.swing.JPanel filterPanel;
    protected javax.swing.JPanel filterSettingsPanel;
    protected javax.swing.JSeparator filterSettingsSeparator;
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
    protected javax.swing.JMenu jMenu2;
    protected javax.swing.JMenu jMenu20;
    protected javax.swing.JMenu jMenu21;
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
    protected javax.swing.JMenuItem jMenuItem11;
    protected javax.swing.JMenuItem jMenuItem12;
    protected javax.swing.JMenuItem jMenuItem13;
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
    protected javax.swing.JMenuItem jMenuItem8;
    protected javax.swing.JMenuItem jMenuItem9;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JScrollPane jScrollPane1;
    protected javax.swing.JScrollPane jScrollPane2;
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
    public javax.swing.JList<String> layersList;
    protected javax.swing.JPanel leftPanel;
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
    protected javax.swing.JMenuItem menuFileImportStacks;
    protected javax.swing.JMenuItem menuFileRedo;
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
    protected javax.swing.JMenuItem menuProtocolCKMask;
    protected javax.swing.JMenuItem menuProtocolCopyMask;
    protected javax.swing.JMenuItem menuProtocolNucleusSep;
    protected javax.swing.JMenu menuProtocols;
    protected javax.swing.JMenuItem menuSharpCorner;
    protected javax.swing.JMenuItem menuShrinkEdge;
    protected javax.swing.JMenuItem menuSmoothen;
    protected javax.swing.JMenuItem menuSpreadEdge;
    protected javax.swing.JMenuItem menuSubstract;
    protected javax.swing.JMenuItem menuTongaExit;
    protected javax.swing.JButton openExcel;
    protected javax.swing.JProgressBar progressBar;
    protected javax.swing.JPanel protocolHeadPanel;
    public javax.swing.JLabel protocolName;
    protected javax.swing.JSeparator protocolNameSeparator;
    protected javax.swing.JPanel protocolPanel;
    protected javax.swing.JPanel protocolPanel1;
    protected javax.swing.JPanel protocolPanel3;
    protected javax.swing.JPanel protocolPanel4;
    protected javax.swing.JPanel protocolPanel5;
    protected javax.swing.JPanel protocolSettingsPanel;
    protected javax.swing.JSeparator protocolSettingsSeparator;
    protected javax.swing.JSplitPane rSplitPane;
    public javax.swing.JTable resultTable;
    protected javax.swing.JPanel resultsPanel;
    protected javax.swing.JComboBox<String> stackCombo;
    protected javax.swing.JToggleButton stackToggle;
    public javax.swing.JTabbedPane tabbedPane;
    // End of variables declaration//GEN-END:variables

}
