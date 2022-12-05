package mainPackage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;

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
        for (int i = 0; i <= pc.interaction.length / 2; i += 2) {
            boolean status;
            status = ((JToggleButton) pc.comp).isSelected();
            status = pc.interaction[i + 1] == 1 ? status : !status;
            controls.get(pc.interaction[i]).comp.setEnabled(status);
        }
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
        String fp;
        String sp = ct;
        if (ct.equals("Browse...")) {
            sp = Tonga.frame().filePathField.getText();
        }
        fp = IO.getFolder(sp);
        if (fp != null) {
            butt.putClientProperty("Nimbus.Overrides", null);
            butt.setText(fp);
            butt.setToolTipText(fp);
        }
    }
}
