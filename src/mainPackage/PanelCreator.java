package mainPackage;

import mainPackage.utils.COL;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

public class PanelCreator {

    private ArrayList<PanelControl> controls;
    private JPanel panel;
    private static DecimalFormat df = new DecimalFormat("#.##");

    public PanelCreator(ArrayList<ControlReference> refs) {
        this(refs.toArray(new ControlReference[refs.size()]));
    }

    public PanelCreator(ControlReference[] refs) {
        makeControls(refs);
        makePanel();
    }

    public PanelCreator() {
        controls = new ArrayList<>();
        panel = null;
    }

    public void put(PanelControl pc) {
        controls.add(pc);
    }

    public ArrayList<PanelControl> getControls() {
        return controls;
    }

    public JPanel getPanel() {
        if (panel == null) {
            makePanel();
        }
        return panel;
    }

    private void makeControls(ControlReference[] refs) {
        controls = new ArrayList<>();
        for (ControlReference ref : refs) {
            put(new PanelControl(ref));
        }
        for (PanelControl pc : controls) {
            if (pc.type == ControlType.TOGGLE && pc.interaction != null) {
                ((JToggleButton) pc.comp).addActionListener((ActionEvent evt) -> {
                    toggleEnable(pc);
                });
                toggleEnable(pc);
            }
        }
    }

    private void toggleEnable(PanelControl pc) {
        for (int i = 0; i <= pc.interaction.length / 2; i += 2) {
            boolean status;
            status = ((JToggleButton) pc.comp).isSelected();
            status = pc.interaction[i + 1] == 1 ? status : !status;
            controls.get(pc.interaction[i]).comp.setEnabled(status);
        }
    }

    private void makePanel() {
        panel = new JPanel();
        Arrays.stream(panel.getComponents()).forEach(c -> {
            panel.remove(c);
        });
        if (controls.size() > 0) {
            GroupLayout layout = new GroupLayout(panel);
            panel.setLayout(layout);
            //horizontal
            GroupLayout.ParallelGroup hz = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
            GroupLayout.ParallelGroup subleft = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
            GroupLayout.ParallelGroup subright = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
            GroupLayout.ParallelGroup slidergroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
            boolean hasSliderGroup = false;
            for (int i = 0; i < controls.size(); i++) {
                PanelControl pc = controls.get(i);
                switch (pc.type) {
                    case COLOUR:
                    case COMBO:
                    case LAYER:
                    case SPINNER:
                    case TOGGLE:
                        subleft.addComponent(pc.comp, GroupLayout.Alignment.LEADING, 75, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
                        subright.addComponent(pc.label, GroupLayout.Alignment.TRAILING, 75, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
                        break;
                    case SELECT:
                        slidergroup.addGroup(layout.createSequentialGroup()
                                .addComponent(pc.label, 120, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGap(6, 6, 6));
                        hz.addComponent(pc.comp, 150, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
                        break;
                    case SLIDER:
                        hasSliderGroup = true;
                        slidergroup.addGroup(layout.createSequentialGroup()
                                .addComponent(pc.label, 120, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGap(6, 6, 6)
                                .addComponent(pc.jtbox, 30, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
                        hz.addComponent(pc.comp, 150, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
                        break;
                    case RANGE:
                        hasSliderGroup = true;
                        slidergroup.addGroup(layout.createSequentialGroup()
                                .addComponent(pc.jtbox, 25, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGap(6, 6, 6)
                                .addComponent(pc.label, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(pc.label2, 50, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addGap(6, 6, 6)
                                .addComponent(pc.jtbox2, 25, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
                        hz.addComponent(pc.comp, 150, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
                        break;
                }
                if (i + 1 == controls.size() || controls.get(i + 1).type == ControlType.SLIDER && pc.type != ControlType.SLIDER) {
                    hz.addGroup(layout.createSequentialGroup().addGroup(subleft).addGap(18, 18, 18).addGroup(subright));
                    subleft = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
                    subright = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
                }
                if (i + 1 == controls.size() && hasSliderGroup) {
                    hz.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGroup(slidergroup));
                }
            }
            layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                            .addContainerGap().addGroup(hz).addContainerGap()));
            //vertical
            GroupLayout.SequentialGroup vr = layout.createSequentialGroup().addContainerGap();
            for (int i = 0; i < controls.size(); i++) {
                PanelControl pc = controls.get(i);
                switch (pc.type) {
                    case COLOUR:
                    case COMBO:
                    case LAYER:
                    case SPINNER:
                    case TOGGLE:
                        vr.addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(pc.comp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(pc.label, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
                        break;
                    case SELECT:
                        vr.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(pc.label, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
                        vr.addGap(6, 6, 6);
                        vr.addComponent(pc.comp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
                        break;
                    case SLIDER:
                        vr.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(pc.label, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(pc.jtbox));
                        vr.addGap(2, 2, 2);
                        vr.addComponent(pc.comp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
                        break;
                    case RANGE:
                        vr.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(pc.jtbox)
                                .addComponent(pc.label, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(pc.label2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(pc.jtbox2));
                        vr.addGap(2, 2, 2);
                        vr.addComponent(pc.comp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
                        break;
                }
                if (i + 1 < controls.size()) {
                    boolean nextWide = controls.get(i + 1).type == ControlType.SLIDER || controls.get(i + 1).type == ControlType.SELECT;
                    boolean thisWide = pc.type == ControlType.SLIDER;
                    if (nextWide && thisWide) {
                        vr.addGap(2, 2, 2);
                    } else if (nextWide || thisWide) {
                        vr.addGap(8, 8, 8);
                    } else {
                        vr.addGap(12, 12, 12);
                    }
                }
            }
            vr.addContainerGap();
            layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.TRAILING, vr));
        } else {
            JLabel noSettings = new javax.swing.JLabel();
            noSettings.setFont(new java.awt.Font("Tahoma", 2, 11));
            noSettings.setForeground(new java.awt.Color(153, 153, 153));
            noSettings.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            noSettings.setText("(no adjustable settings)");

            GroupLayout protocolSettingsPanelLayout = new GroupLayout(panel);
            panel.setLayout(protocolSettingsPanelLayout);
            protocolSettingsPanelLayout.setHorizontalGroup(
                    protocolSettingsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(noSettings, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            );
            protocolSettingsPanelLayout.setVerticalGroup(
                    protocolSettingsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(noSettings, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            );
        }
        TongaFrame.handleComponents(panel);
    }

    public enum ControlType {
        COMBO,
        LAYER,
        SELECT,
        TOGGLE,
        SLIDER,
        SPINNER,
        COLOUR,
        RANGE
    }

    public static class ControlReference {

        public ControlType type; //what type of a control it is
        public Object[] data; //parameters, for combo String[]{options}, for slider Integer[]{bounds}
        public int[] interaction; //with other components, for toggle, control indexes to disable when "NO"; for colour, control index to follow (must be LAYER) to change the colour
        String desc; //description
        int defsel; //default selection

        public ControlReference(ControlType type, String description) {
            this(type, null, description, -1, null);
        }

        public ControlReference(ControlType type, Object[] data, String description) {
            this(type, data, description, -1, null);
        }

        public ControlReference(ControlType type, String description, int defaultoption) {
            this(type, null, description, defaultoption, null);
        }

        public ControlReference(ControlType type, String description, int[] interaction) {
            this(type, null, description, -1, interaction);
        }

        public ControlReference(ControlType type, String description, int defaultoption, int[] interaction) {
            this(type, null, description, defaultoption, interaction);
        }

        public ControlReference(ControlType type, Object[] data, String description, int defaultoption) {
            this(type, data, description, defaultoption, null);
        }

        public ControlReference(ControlType type, Object[] data, String description, int defaultoption, int[] interaction) {
            this.type = type;
            this.data = data;
            this.desc = description;
            this.defsel = defaultoption;
            this.interaction = interaction;
        }
    }

    public static class PanelControl {

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
                case LAYER:
                    JComboBox cy = new JComboBox<>();
                    cy.setPreferredSize(new Dimension(100, 25));
                    label.setPreferredSize(new Dimension(200, 25));
                    comp = cy;
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
                    return (data[1] - data[0]) / (data[3] / 2.) * num;
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
}
