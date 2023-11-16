package mainPackage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import static mainPackage.PanelCreator.ControlType.ANNOTATION;
import static mainPackage.PanelCreator.ControlType.ANNOTATION_GROUP;
import static mainPackage.PanelCreator.ControlType.ANNOTATION_TYPE;
import static mainPackage.PanelCreator.ControlType.FOLDER;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.Tonga.picList;
import mainPackage.TongaAnnotator.AnnotationType;
import mainPackage.utils.COL;

public class PanelUtils {

    public static final void initPanelListeners(Container cont) {
        Component[] components = cont.getComponents();
        for (Component component : components) {
            Class c = component.getClass();
            if (c.equals(JToggleButton.class)) {
                toggleCosmetics((JToggleButton) component);
                ((JToggleButton) component).addActionListener((java.awt.event.ActionEvent evt) -> {
                    toggleCosmetics((JToggleButton) evt.getSource());
                });
                ((JToggleButton) component).addItemListener((java.awt.event.ItemEvent evt) -> {
                    toggleCosmetics((JToggleButton) evt.getSource());
                });
            }
            if (c.equals(JButton.class)) {
                //colour button is without text
                if (((JButton) component).getText().isEmpty()) {
                    ((JButton) component).addActionListener((ActionEvent evt) -> {
                        JButton source = (JButton) evt.getSource();
                        colourSelect(source);
                    });
                    //folder button has text
                } else {
                    ((JButton) component).addActionListener((ActionEvent evt) -> {
                        JButton source = (JButton) evt.getSource();
                        pathSelect(source);
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
                        int lx = ((JRangeSlider.RangeSliderUI) source.getUI()).getLowerX() - x;
                        int ux = ((JRangeSlider.RangeSliderUI) source.getUI()).getUpperX() - x;
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

    static void initControlListeners(ArrayList<PanelControl> controls) {
        for (PanelControl pc : controls) {
            if (pc.type == PanelCreator.ControlType.TOGGLE && pc.interaction != null) {
                ((JToggleButton) pc.comp).addActionListener((ActionEvent evt) -> {
                    toggleEnable(pc, controls);
                });
                toggleEnable(pc, controls);
            }
        }
    }

    private static void toggleEnable(PanelControl pc, ArrayList<PanelControl> controls) {
        for (int i = 0; i < pc.interaction.length; i += 2) {
            boolean status;
            status = ((JToggleButton) pc.comp).isSelected();
            // to enable based on another toggle button, set index as 3-digit number:
            // fe. 131 checks if the 3rd element is selected and enables it if it is
            if (pc.interaction[i + 1] > 100) {
                boolean estatus = ((JToggleButton) (controls.get((pc.interaction[i + 1] - 100) / 10).comp)).isSelected();
                estatus = pc.interaction[i + 1] % 2 == 1 ? estatus : !estatus;
                status = status ? estatus : false;
            } else {
                status = pc.interaction[i + 1] == 1 ? status : !status;
            }
            controls.get(pc.interaction[i]).comp.setEnabled(status);
        }
    }

    public static void updateColorButtonColour(JButton button, JComboBox combo) {
        Color c;
        if (Tonga.thereIsImage() && !Settings.settingBatchProcessing()) {
            MappedImage img = Tonga.getLayerList(Tonga.getImageIndex()).get(combo.getSelectedIndex()).layerImage;
            c = COL.layerCornerColour(img);
        } else {
            c = Color.BLACK;
        }
        button.setBackground(c);
    }

    public static void updateComboLayerList(JComboBox<String> combo) {
        ArrayList<TongaLayer> layers = Tonga.getLayerList();
        int lsize = layers == null ? 0 : layers.size();
        TongaLayer[] list = new TongaLayer[lsize];
        for (int j = 0; j < lsize; j++) {
            list[j] = layers.get(j);
        }
        combo.setModel(new DefaultComboBoxModel(list));
    }

    public static void updateComboAnnotationList(JComboBox<String> combo, Object[] allowedTypes) {
        TongaAnnotation[] list;
        if (Tonga.getImage() == null) {
            list = new TongaAnnotation[0];
        } else {
            List<TongaAnnotation> annos = Tonga.getAnnotations();
            List<TongaAnnotation> fannos;
            if (allowedTypes != null) {
                List<AnnotationType> atl = Arrays.asList((AnnotationType[]) allowedTypes);
                fannos = annos.stream().filter(a -> atl.contains(a.getType())).toList();
            } else {
                fannos = annos;
            }
            int lsize = fannos.size();
            list = new TongaAnnotation[lsize];
            for (int j = 0; j < lsize; j++) {
                list[j] = fannos.get(j);
            }
            combo.putClientProperty("Nimbus.Overrides", null);
        }
        combo.setModel(new DefaultComboBoxModel(list));
    }

    public static void updateComboAnnotationTypeList(JComboBox<String> combo) {
        Object[] list;
        if (Tonga.getImage() == null) {
            list = new Object[]{"All"};
        } else {
            List<AnnotationType> atl = Tonga.getAnnotations().stream().map(a -> a.getType()).distinct().toList();
            list = new Object[atl.size() + 1];
            list[0] = "All";
            for (int a = 0; a < atl.size(); a++) {
                list[a + 1] = atl.get(a);
            }
        }
        combo.setModel(new DefaultComboBoxModel(list));
    }

    public static void updateComboAnnotationGroupList(JComboBox<String> combo) {
        String[] list;
        if (Tonga.getImage() == null) {
            list = new String[]{"All"};
        } else {
            List<Integer> atl = Tonga.getAnnotations().stream().map(a -> a.getGroup()).distinct().toList();
            list = new String[atl.size() + 1];
            list[0] = "All";
            for (int a = 0; a < atl.size(); a++) {
                list[a + 1] = atl.get(a).toString();
            }
        }
        combo.setModel(new DefaultComboBoxModel(list));
    }

    public static void comboSelector(JComboBox<String> c, int defval) {
        int i = defval;
        if (i >= 0 && i < c.getItemCount()) {
            c.setSelectedIndex(i);
        } else {
            c.setSelectedIndex(c.getItemCount() - 1);
        }
    }

    public static void comboSelector(JComboBox<Object> c, Object defitem) {
        boolean exists = false;
        for (int i = 0; i < c.getItemCount(); i++) {
            Object item = c.getItemAt(i);
            if (item.equals(defitem)) {
                exists = true;
                break;
            }
        }
        if (exists) {
            c.setSelectedItem(defitem);
        } else {
            c.setSelectedIndex(0);
        }
    }

    public static void updateComponents(PanelCreator pc) {
        pc.getControls().forEach(c -> {
            if (c.type == LAYER || c.type == ANNOTATION || c.type == ANNOTATION_TYPE || c.type == ANNOTATION_GROUP) {
                JComboBox co = (JComboBox) c.comp;
                int dv = co.getSelectedIndex();
                Object di = co.getSelectedItem();
                switch (c.type) {
                    case LAYER:
                        PanelUtils.updateComboLayerList(co);
                        PanelUtils.comboSelector(co, dv);
                        break;
                    case ANNOTATION:
                        PanelUtils.updateComboAnnotationList(co, c.data);
                        PanelUtils.comboSelector(co, dv);
                        break;
                    case ANNOTATION_TYPE:
                        PanelUtils.updateComboAnnotationTypeList(co);
                        PanelUtils.comboSelector(co, di);
                        break;
                    case ANNOTATION_GROUP:
                        PanelUtils.updateComboAnnotationGroupList(co);
                        PanelUtils.comboSelector(co, di);
                        break;
                }
            }
            if (c.type == FOLDER) {
                JButton jb = (JButton) c.comp;
                if (!picList.isEmpty()) {
                    TongaLayer tl = Tonga.getImage().getLayer(0);
                    String fp = tl.layerImage.source;
                    if ((jb.getText().equals("Browse...") || jb.getForeground().equals(new Color(0, 128, 0))) && fp != null) {
                        if (fp.toLowerCase().endsWith(".mrxs")) {
                            pathSelect(jb, IO.folderPath(fp));
                        }
                    }
                } else if (jb.getForeground().equals(new Color(0, 128, 0))) {
                    pathSelect(jb, null);
                }
            }
        });
    }

    private static void toggleCosmetics(JToggleButton source) {
        if (source.isSelected()) {
            source.setBackground(Color.getHSBColor((float) 0.3, (float) 0.85, (float) 0.7));
            source.setText("YES");
        } else {
            source.setBackground(Color.getHSBColor(0, (float) 0.95, (float) 0.675));
            source.setText("NO");
        }
    }

    public static void colourSelect(Component control) {
        Color cc = JColorChooser.showDialog(null, "Choose a color", ((JButton) control).getBackground());
        if (cc != null) {
            ((JButton) control).setBackground(cc);
        }
    }

    private static void pathSelect(Component control) {
        JButton butt = (JButton) control;
        String ct = butt.getText();
        String sp = ct;
        if (ct.equals("Browse...")) {
            sp = Tonga.frame().filePathField.getText();
        }
        String fp = IO.getFolder(sp);
        pathSelect(control, fp);
    }

    private static void pathSelect(Component control, String fp) {
        JButton butt = (JButton) control;
        butt.putClientProperty("Nimbus.Overrides", null);
        if (fp != null) {
            butt.setText(fp);
            butt.setToolTipText(fp);
            TongaLayer tl = Tonga.getImage().getLayer(0);
            String ip = tl.layerImage.source;
            if (ip != null && fp.toLowerCase().contains(ip.toLowerCase().replaceAll(".mrxs", ""))) {
                butt.setForeground(new Color(0, 128, 0));
            } else {
                butt.setForeground(null);
            }
        } else {
            butt.setText("Browse...");
            butt.setToolTipText(null);
            butt.setForeground(null);
        }
    }
}
