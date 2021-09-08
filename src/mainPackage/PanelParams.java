package mainPackage;

import java.awt.Color;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.PanelCreator.PanelControl;
import mainPackage.utils.COL;

public class PanelParams {

    public PanelParams(ControlReference[] parameterData) {
        initArrays(parameterData);
    }

    public int[] slider;
    public double[] sliderScaled;
    public int[] range;
    public int[] combo;
    public int[] select;
    public int[] layer;
    public javafx.scene.paint.Color[] color;
    public int[] colorARGB;
    public int[] spinner;
    public boolean[] toggle;

    public void getFilterParameters(PanelCreator panelCreator) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0, layers = 0;
        for (PanelControl pc : panelCreator.getControls()) {
            switch (pc.type) {
                case SLIDER:
                    slider[sliders] = ((JSlider) pc.comp).getValue();
                    sliderScaled[sliders] = PanelControl.scaledNum(slider[sliders], sliderParams(pc.data));
                    sliders++;
                    break;
                case RANGE:
                    range[ranges] = ((JRangeSlider) pc.comp).getValue();
                    ranges++;
                    range[ranges] = ((JRangeSlider) pc.comp).getUpperValue();
                    ranges++;
                    break;
                case COLOUR:
                    color[colors] = COL.awt2FX(((JButton) pc.comp).getBackground());
                    colorARGB[colors] = COL.FX2awt(color[colors]).getRGB() | 0xFF000000;
                    colors++;
                    break;
                case SPINNER:
                    spinner[spinners] = Integer.parseInt(((JSpinner) pc.comp).getValue().toString());
                    spinners++;
                    break;
                case COMBO:
                    combo[combos] = ((JComboBox) pc.comp).getSelectedIndex();
                    combos++;
                    break;
                case SELECT:
                    select[selects] = ((JComboBox) pc.comp).getSelectedIndex();
                    selects++;
                    break;
                case LAYER:
                    layer[layers] = ((JComboBox) pc.comp).getSelectedIndex();
                    layers++;
                    break;
                case TOGGLE:
                    toggle[toggles] = ((JToggleButton) pc.comp).isSelected();
                    toggles++;
                    break;
            }
        }
    }

    public void setFilterParameters(ControlReference[] parameterData, Object... parameters) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0;
        for (int i = 0, j = 0; i < parameters.length; i++, j++) {
            if (parameters[i] != null) {
                ControlReference pc = parameterData[j];
                switch (pc.type) {
                    case SLIDER:
                        if (parameters[i].getClass() == Double.class) {
                            sliderScaled[sliders] = (double) parameters[i];
                            slider[sliders] = PanelControl.scaledVal(sliderScaled[sliders], sliderParams(pc.data));
                        } else {
                            slider[sliders] = (int) parameters[i];
                            sliderScaled[sliders] = PanelControl.scaledNum(slider[sliders], sliderParams(pc.data));
                        }
                        sliders++;
                        break;
                    case RANGE:
                        range[ranges] = (int) parameters[i];
                        ranges++;
                        i++;
                        range[ranges] = (int) parameters[i];
                        ranges++;
                        break;
                    case COLOUR:
                        if (parameters[i].getClass() == Integer.class) {
                            color[colors] = COL.ARGBintToColor((Integer) parameters[i]);
                            colorARGB[colors] = (Integer) parameters[i];
                        } else if (parameters[i].getClass() == javafx.scene.paint.Color.class) {
                            color[colors] = (javafx.scene.paint.Color) parameters[i];
                            colorARGB[colors] = COL.FX2awt(color[colors]).getRGB() | 0xFF000000;
                        }
                        colors++;
                        break;
                    case SPINNER:
                        if (parameters[i].getClass() == Integer.class) {
                            spinner[spinners] = (Integer) parameters[i];
                        } else if (parameters[i].getClass() == Double.class) {
                            spinner[spinners] = ((Double) parameters[i]).intValue();
                        }
                        spinners++;
                        break;
                    case COMBO:
                        combo[combos] = (int) parameters[i];
                        combos++;
                        break;
                    case SELECT:
                        select[selects] = (int) parameters[i];
                        selects++;
                        break;
                    case LAYER:
                        // layers skipped
                        i--;
                        break;
                    case TOGGLE:
                        toggle[toggles] = (boolean) parameters[i];
                        toggles++;
                        break;
                }
            }
        }
    }

    public void setControlParameters(PanelCreator panelControls, Object... parameters) {
        for (int i = 0, j = 0; i < parameters.length; i++, j++) {
            if (parameters[i] != null) {
                PanelControl pc = panelControls.getControls().get(j);
                switch (pc.type) {
                    case SLIDER:
                        if (parameters[i].getClass() == Double.class) {
                            ((JSlider) pc.comp).setValue(PanelControl.scaledVal((double) parameters[i], sliderParams(pc.data)));
                        } else {
                            ((JSlider) pc.comp).setValue((int) parameters[i]);
                        }
                        break;
                    case RANGE:
                        ((JRangeSlider) pc.comp).setValue((int) parameters[i]);
                        i++;
                        ((JRangeSlider) pc.comp).setUpperValue((int) parameters[i]);
                        break;
                    case COLOUR:
                        if (parameters[i].getClass() == Integer.class) {
                            ((JButton) pc.comp).setBackground(new Color((int) parameters[i]));
                        } else if (parameters[i].getClass() == javafx.scene.paint.Color.class) {
                            ((JButton) pc.comp).setBackground(COL.FX2awt(((javafx.scene.paint.Color) parameters[i])));
                        }
                        break;
                    case SPINNER:
                        if (parameters[i].getClass() == Integer.class) {
                            ((JSpinner) pc.comp).setValue((int) parameters[i]);
                        } else if (parameters[i].getClass() == Double.class) {
                            ((JSpinner) pc.comp).setValue(((Double) parameters[i]).intValue());
                        }
                        break;
                    case COMBO:
                        ((JComboBox) pc.comp).setSelectedIndex((int) parameters[i]);
                        break;
                    case SELECT:
                        ((JComboBox) pc.comp).setSelectedIndex((int) parameters[i]);
                        break;
                    case LAYER:
                        // layers skipped
                        i--;
                        break;
                    case TOGGLE:
                        ((JToggleButton) pc.comp).setSelected((boolean) parameters[i]);
                        break;
                }
            }
        }
    }

    private void initArrays(ControlReference[] parameterData) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0, layers = 0;
        for (ControlReference pc : parameterData) {
            switch (pc.type) {
                case SLIDER:
                    sliders++;
                    break;
                case RANGE:
                    ranges += 2;
                    break;
                case COLOUR:
                    colors++;
                    break;
                case SPINNER:
                    spinners++;
                    break;
                case COMBO:
                    combos++;
                    break;
                case LAYER:
                    layers++;
                    break;
                case SELECT:
                    selects++;
                    break;
                case TOGGLE:
                    toggles++;
                    break;
            }
        }
        range = new int[ranges];
        slider = new int[sliders];
        sliderScaled = new double[sliders];
        combo = new int[combos];
        select = new int[selects];
        layer = new int[layers];
        color = new javafx.scene.paint.Color[colors];
        colorARGB = new int[colors];
        spinner = new int[spinners];
        toggle = new boolean[toggles];
    }

    public static Double[] sliderParams(Object[] data) {
        if (data == null) {
            return null;
        }
        Double[] ddata = new Double[data.length];
        for (int i = 0; i < data.length; i++) {
            ddata[i] = data[i].getClass() == Double.class ? (Double) data[i] : ((Integer) data[i]).doubleValue();
        }
        if (data.length == 3) {
            return new Double[]{ddata[0], (ddata[1] - ddata[0]) / 2, ddata[1], ddata[2]};
        }
        return ddata;
    }
}
