package mainPackage;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PanelCreator {

    private ArrayList<PanelControl> controls;
    private JPanel panel;

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
        //use for control actions which require specific control reference data
        //for any others, use the handleComponents
        PanelUtils.initControlListeners(controls);
    }

    private void makePanel() {
        panel = new JPanel();
        Arrays.stream(panel.getComponents()).forEach(c -> {
            panel.remove(c);
        });
        if (!controls.isEmpty()) {
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
                    case ANNOTATION:
                    case ANNOTATION_TYPE:
                    case ANNOTATION_GROUP:
                    case FOLDER:
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
                    case ANNOTATION:
                    case ANNOTATION_TYPE:
                    case ANNOTATION_GROUP:
                    case FOLDER:
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
        PanelUtils.initPanelListeners(panel);
    }

    public enum ControlType {
        COMBO,
        LAYER,
        FOLDER,
        ANNOTATION,
        ANNOTATION_TYPE,
        ANNOTATION_GROUP,
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

}
