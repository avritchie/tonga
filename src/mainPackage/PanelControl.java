package mainPackage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.PanelCreator.ControlType;
import mainPackage.utils.COL;

public class PanelControl {

    private static DecimalFormat df = new DecimalFormat("#.##");

    public ControlType type; //what type of a control it is
    public Object[] data; //parameters, for combo String[]{options}, for slider Integer[]{bounds}
    public int[] interaction;
    /*with other components:
        for colour, control index to follow (must be LAYER) to change the colour
            only use this for PROTOCOLS because filters never have layers supplied, for them use "defsel = -2"
        for toggle, control indexes to disable/enable. always two values per index; the first is the control index
            and the second value tells if it should be enabled when YES (1) or enabled when NO (0)*/
    String desc; //description
    int defsel; //default selection
    public Component comp;
    JLabel label, label2;
    JTextField jtbox, jtbox2;

    public PanelControl(ControlReference ref) {
        this(ref.type, ref.data, ref.desc, ref.defsel, ref.interaction);
    }

    public PanelControl(ControlType type, Object[] data, String description, int defaultoption, int[] disables) {
        this.type = type;
        this.data = data;
        this.desc = description;
        this.defsel = defaultoption;
        this.interaction = disables;
        this.label = new JLabel();
        label.setText(desc);
        int defnum;
        switch (type) {
            case COMBO:
                JComboBox cc = new JComboBox<>();
                if (data != null) {
                    cc.setModel(new DefaultComboBoxModel<>((String[]) data));
                    cc.setSelectedIndex(defsel == -1 ? 0 : defsel);
                }
                cc.setPreferredSize(new Dimension(100, 25));
                label.setPreferredSize(new Dimension(200, 25));
                comp = cc;
                break;
            case ANNOTATION:
            case ANNOTATION_TYPE:
            case ANNOTATION_GROUP:
            case LAYER:
                JComboBox cy = new JComboBox<>();
                cy.setPreferredSize(new Dimension(100, 25));
                label.setPreferredSize(new Dimension(200, 25));
                comp = cy;
                break;
            case FOLDER:
                JButton cf = new JButton();
                cf.setPreferredSize(new Dimension(100, 25));
                label.setPreferredSize(new Dimension(200, 25));
                cf.setText("Browse...");
                comp = cf;
                break;
            case SELECT:
                JComboBox ce = new JComboBox<>();
                if (data != null) {
                    ce.setModel(new DefaultComboBoxModel<>((String[]) data));
                    ce.setSelectedIndex(defsel == -1 ? 0 : defsel);
                }
                ce.setPreferredSize(new Dimension(300, 25));
                label.setHorizontalAlignment(SwingConstants.LEADING);
                label.setPreferredSize(new Dimension(300, 20));
                comp = ce;
                break;
            case COLOUR:
                JButton cb = new JButton();
                cb.setBackground(defsel == -2 && Tonga.getLayer() != null
                        ? COL.layerCornerColour(Tonga.getLayer().layerImage)
                        : defsel == -1 ? new Color(0, 0, 0) : new Color(defsel));
                cb.setPreferredSize(new Dimension(100, 30));
                label.setPreferredSize(new Dimension(200, 30));
                comp = cb;
                break;
            case SPINNER:
                JSpinner cs = new JSpinner();
                cs.setValue(defsel == -1 ? 0 : defsel);
                cs.setPreferredSize(new Dimension(100, 25));
                label.setPreferredSize(new Dimension(200, 25));
                comp = cs;
                break;
            case TOGGLE:
                JToggleButton ct = new JToggleButton();
                ct.setPreferredSize(new Dimension(100, 30));
                label.setPreferredSize(new Dimension(200, 30));
                ct.setSelected(defsel == 1);
                comp = ct;
                break;
            case SLIDER:
                JSlider cl = new JSlider();
                if (data == null) {
                    cl.setMinimum(0);
                    cl.setMaximum(100);
                } else if (data.length == 2) {
                    cl.setMinimum(((Integer) data[0]));
                    cl.setMaximum(((Integer) data[1]));
                } else {
                    cl.setMinimum(0);
                    cl.setMaximum(((Integer) data[data.length - 1]));
                }
                defnum = defsel == -1 ? (cl.getMaximum() - cl.getMinimum()) / 2 : defsel;
                cl.setValue(defnum);
                cl.setPreferredSize(new Dimension(300, 25));
                jtbox = sliderTextBox(cl, defnum, PanelParams.sliderParams(data));
                label.setHorizontalAlignment(SwingConstants.LEADING);
                label.setPreferredSize(new Dimension(270, 20));
                comp = cl;
                break;
            case RANGE:
                JRangeSlider cr = new JRangeSlider();
                if (data == null) {
                    cr.setMinimum(0);
                    cr.setMaximum(100);
                } else {
                    cr.setMinimum(((Integer) data[0]));
                    cr.setMaximum(((Integer) data[1]));
                }
                cr.setValue(defsel == -1 ? cr.getMinimum() : (int) ((cr.getMaximum() - cr.getMinimum()) / 3.));
                cr.setUpperValue(defsel == -1 ? cr.getMaximum() : (int) ((cr.getMaximum() - cr.getMinimum()) / 1.5));
                cr.setPreferredSize(new Dimension(300, 25));
                jtbox = sliderTextBox(cr, cr.getMinimum(), PanelParams.sliderParams(data));
                jtbox2 = sliderTextBox(cr, cr.getMaximum(), PanelParams.sliderParams(data));
                label2 = new JLabel();
                label.setText(desc.split("%")[0]);
                label2.setText(desc.split("%")[1]);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label2.setHorizontalAlignment(SwingConstants.CENTER);
                label.setPreferredSize(new Dimension(120, 20));
                label2.setPreferredSize(new Dimension(120, 20));
                comp = cr;
                break;
        }
    }

    private JTextField sliderTextBox(JSlider sli, int defnum, Double[] data) {
        JTextField box = new JTextField();
        if (sli.getClass() == JRangeSlider.class && defnum == sli.getMinimum()) {
            box.setHorizontalAlignment(SwingConstants.LEADING);
        } else {
            box.setHorizontalAlignment(SwingConstants.TRAILING);
        }
        Color c = Tonga.frame().filterSettingsPanel.getBackground();
        box.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue()));
        box.setBorder(null);
        box.setText(formatNum(scaledNum(defnum, data)));
        if (sli.getClass() == JRangeSlider.class) {
            if (defnum == sli.getMinimum()) {
                sli.addChangeListener((ChangeEvent evt) -> {
                    box.setText(formatNum(scaledNum(sli.getValue(), data)));
                });
            } else if (defnum == sli.getMaximum()) {
                sli.addChangeListener((ChangeEvent evt) -> {
                    box.setText(formatNum(scaledNum(((JRangeSlider) sli).getUpperValue(), data)));
                });
            }
        } else {
            sli.addChangeListener((ChangeEvent evt) -> {
                box.setText(formatNum(scaledNum(sli.getValue(), data)));
            });
        }
        if (sli.getClass() == JRangeSlider.class) {
            if (defnum == sli.getMinimum()) {
                box.addFocusListener(new FocusAdapter() {

                    @Override
                    public void focusLost(FocusEvent evt) {
                        try {
                            double in = Double.parseDouble(box.getText().replaceAll("[^0-9.]", ""));
                            if (box.getText().charAt(0) == "-".charAt(0)) {
                                in = scaledNum(sli.getMinimum(), data);
                            } else {
                                in = Math.min(Math.max(in, scaledNum(sli.getMinimum(), data)), scaledNum(((JRangeSlider) sli).getUpperValue(), data));
                            }
                            box.setText(formatNum(in));
                            sli.setValue(scaledVal(in, data));
                        } catch (NumberFormatException ex) {
                            box.setText(formatNum(scaledNum(((JRangeSlider) sli).getUpperValue(), data)));
                        }
                    }

                    @Override
                    public void focusGained(FocusEvent evt) {
                        box.selectAll();
                    }
                });
            } else if (defnum == sli.getMaximum()) {
                box.addFocusListener(new FocusAdapter() {

                    @Override
                    public void focusLost(FocusEvent evt) {
                        try {
                            double in = Double.parseDouble(box.getText().replaceAll("[^0-9.]", ""));
                            if (box.getText().charAt(0) == "-".charAt(0)) {
                                in = scaledNum(sli.getValue(), data);
                            } else {
                                in = Math.min(Math.max(in, scaledNum(sli.getValue(), data)), scaledNum(sli.getMaximum(), data));
                            }
                            box.setText(formatNum(in));
                            ((JRangeSlider) sli).setUpperValue(scaledVal(in, data));
                        } catch (NumberFormatException ex) {
                            box.setText(formatNum(scaledNum(sli.getMaximum(), data)));
                        }
                    }

                    @Override
                    public void focusGained(FocusEvent evt) {
                        box.selectAll();
                    }
                });
            }
        } else {
            box.addFocusListener(new FocusAdapter() {

                @Override
                public void focusLost(FocusEvent evt) {
                    try {
                        double in = Double.parseDouble(box.getText().replaceAll("[^0-9.]", ""));
                        if (box.getText().charAt(0) == "-".charAt(0)) {
                            in = scaledNum(sli.getMinimum(), data);
                        } else {
                            in = Math.min(Math.max(in, scaledNum(sli.getMinimum(), data)), scaledNum(sli.getMaximum(), data));
                        }
                        box.setText(formatNum(in));
                        sli.setValue(scaledVal(in, data));
                    } catch (NumberFormatException ex) {
                        box.setText(formatNum(scaledNum(sli.getMaximum(), data)));
                    }
                }

                @Override
                public void focusGained(FocusEvent evt) {
                    box.selectAll();
                }
            });
        }
        return box;
    }

    private String formatNum(double num) {
        return df.format(num).replace(",", ".");
    }

    public static double scaledNum(int num, Double[] data) {
        if (data != null && data.length == 4) {
            if (num < data[3] / 2) {
                return data[0] + ((data[1] - data[0]) / (data[3] / 2.) * num);
            } else if (num > data[3] / 2) {
                return (data[2] - data[1]) / (data[3] / 2.) * (num - (data[3] / 2.)) + data[1];
            } else {
                return data[1];
            }
        } else {
            return num;
        }
    }

    public static int scaledVal(double num, Double[] data) {
        if (data != null && data.length == 4) {
            if (num < data[1]) {
                return (int) ((data[3] / 2.) / ((data[1] - data[0]) / num));
            } else if (num > data[1]) {
                return (int) ((data[3] / 2.) / ((data[2] - data[1]) / (num - data[1])) + (data[3] / 2.));
            } else {
                return data[1].intValue();
            }
        } else {
            return (int) num;
        }
    }
}
