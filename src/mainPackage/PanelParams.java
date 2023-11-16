package mainPackage;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaAnnotator.AnnotationType;
import mainPackage.utils.COL;

public class PanelParams {

    public PanelParams(ControlReference[] parameterData) {
        initArrays(parameterData);
    }

    public PanelParams(PanelParams parameterParent) {
        initArrays(parameterParent);
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
    public File[] folder;
    private String[] folderRaw;
    public TongaAnnotation[] annotation;
    private int[] annotationIndex;
    private AnnotationType[][] annotationIndexType;
    public int[] annotationGroup;
    public AnnotationType[] annotationType;

    public void getFilterParameters(PanelCreator panelCreator) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0, layers = 0, folders = 0, annos = 0,
                annotypes = 0, annogroups = 0;
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
                    colorARGB[colors] = COL.FX2awt(color[colors]).getRGB();
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
                case ANNOTATION:
                    annotationIndex[annos] = ((JComboBox) pc.comp).getSelectedIndex();
                    annotationIndexType[annos] = (AnnotationType[]) pc.data;
                    annos++;
                    break;
                case ANNOTATION_TYPE:
                    Object at = ((JComboBox) pc.comp).getSelectedItem();
                    if (at.getClass() == AnnotationType.class) {
                        annotationType[annotypes] = (AnnotationType) at;
                    } else {
                        annotationType[annotypes] = null;
                    }
                    annotypes++;
                    break;
                case ANNOTATION_GROUP:
                    String ag = ((String) ((JComboBox) pc.comp).getSelectedItem());
                    annotationGroup[annogroups] = ag.equals("All") ? -1 : Integer.parseInt(ag);
                    annogroups++;
                    break;
                case FOLDER:
                    folderRaw[folders] = (pc.comp.getForeground().equals(new Color(0, 128, 0)))
                            ? "%SOURCE%" : ((JButton) pc.comp).getText();
                    folders++;
                    break;
                case TOGGLE:
                    toggle[toggles] = ((JToggleButton) pc.comp).isSelected();
                    toggles++;
                    break;
            }
        }
    }

    public void setImageFilterParameters(PanelParams paramParent, TongaImage ti) {
        //create local parameters using parent protocol parameters
        for (int a = 0; a < paramParent.annotationIndex.length; a++) {
            try {
                List<TongaAnnotation> annos = ti.annotations.getAnnotations();
                if (paramParent.annotationIndex[a] != -1) {
                    if (paramParent.annotationIndexType[a] != null) {
                        List<AnnotationType> atl = Arrays.asList((AnnotationType[]) paramParent.annotationIndexType[a]);
                        annos = annos.stream().filter(n -> atl.contains(n.getType())).toList();
                    }
                    annotation[a] = annos.get(paramParent.annotationIndex[a]);
                }
            } catch (Exception ex) {
                Tonga.catchError(ex, "Annotation " + a + " was not loaded from " + ti.imageName);
            }
        }
        for (int f = 0; f < paramParent.folderRaw.length; f++) {
            try {
                folder[f] = new File(paramParent.folderRaw[f].equals("%SOURCE%")
                        ? IO.folderPath(ti.getLayer(0).layerImage.source) : paramParent.folderRaw[f]);
            } catch (Exception ex) {
                Tonga.catchError(ex, "No acceptable path for " + ti.imageName);
            }
        }
    }

    public void setFilterParameters(ControlReference[] parameterData, Object... parameters) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0, folders = 0, annotations = 0,
                annotationtypes = 0, annotationgroups = 0;
        for (int i = 0, j = 0; i < parameters.length; i++, j++) {
            if (parameters[i] != null) {
                try {
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
                        case ANNOTATION:
                            if (parameters[i].getClass().equals(TongaAnnotation.class)) {
                                annotationIndex[annotations] = -1;
                                annotation[annotations] = (TongaAnnotation) parameters[i];
                            } else {
                                annotationIndex[annotations] = (int) parameters[i];
                                annotation[annotations] = null;
                            }
                            annotations++;
                            break;
                        case ANNOTATION_TYPE:
                            if (parameters[i].getClass().equals(AnnotationType.class)) {
                                annotationType[annotationtypes] = (AnnotationType) parameters[i];
                            } else {
                                //"All" has been selected
                                annotationType[annotationtypes] = null;
                            }
                            annotationtypes++;
                            break;
                        case ANNOTATION_GROUP:
                            if (parameters[i].getClass().equals(String.class)) {
                                String ag = (String) parameters[i];
                                if (ag.equals("All")) {
                                    annotationGroup[annotationgroups] = -1;
                                } else {
                                    annotationGroup[annotationgroups] = Integer.parseInt(ag);
                                }
                            } else {
                                annotationGroup[annotationgroups] = (Integer) parameters[i];
                            }
                            annotationgroups++;
                            break;
                        case FOLDER:
                            if (parameters[i].getClass().equals(File.class)) {
                                folderRaw[folders] = null;
                                folder[folders] = (File) parameters[i];
                            } else {
                                folderRaw[folders] = (String) parameters[i];
                                folder[folders] = null;
                            }
                            folders++;
                            break;
                        case TOGGLE:
                            toggle[toggles] = (boolean) parameters[i];
                            toggles++;
                            break;
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Tonga.log.warn("Ignored a supplied a method parameter which does not have a target");
                } catch (ClassCastException ex) {
                    Tonga.log.warn("Passed a parameter which is not compatible with the method parameters");
                }
            }
        }
    }

    public void setControlParameters(PanelCreator panelControls, Object... parameters) {
        try {
            for (int i = 0, j = 0; i < parameters.length; i++, j++) {
                PanelControl pc = panelControls.getControls().get(j);
                if (pc.type == LAYER || pc.type == ANNOTATION || pc.type == ANNOTATION_TYPE || pc.type == ANNOTATION_GROUP) {
                    // layers and annotations skipped because their value depends on the selected image and can't be set globally
                    i--;
                } else if (parameters[i] != null) {
                    switch (pc.type) {
                        case FOLDER:
                            ((JButton) pc.comp).setText((String) parameters[i]);
                            break;
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
                        case TOGGLE:
                            ((JToggleButton) pc.comp).setSelected((boolean) parameters[i]);
                            break;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            Tonga.catchError(ex, "Too many launch parameters supplied.");
        }
    }

    private void initArrays(ControlReference[] parameterData) {
        int sliders = 0, colors = 0, spinners = 0, combos = 0, toggles = 0, ranges = 0, selects = 0, layers = 0, folders = 0, annos = 0,
                annotypes = 0, annogroups = 0;
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
                case ANNOTATION:
                    annos++;
                    break;
                case ANNOTATION_TYPE:
                    annotypes++;
                    break;
                case ANNOTATION_GROUP:
                    annogroups++;
                    break;
                case FOLDER:
                    folders++;
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
        folderRaw = new String[folders];
        annotationIndex = new int[annos];
        annotationIndexType = new AnnotationType[annos][];
        annotation = new TongaAnnotation[annos];
        annotationType = new AnnotationType[annotypes];
        annotationGroup = new int[annogroups];
        folder = new File[folders];
        colorARGB = new int[colors];
        spinner = new int[spinners];
        toggle = new boolean[toggles];
    }

    private void initArrays(PanelParams parameterParent) {
        range = new int[parameterParent.range.length];
        slider = new int[parameterParent.slider.length];
        sliderScaled = new double[parameterParent.slider.length];
        spinner = new int[parameterParent.spinner.length];
        toggle = new boolean[parameterParent.toggle.length];
        combo = new int[parameterParent.combo.length];
        select = new int[parameterParent.select.length];
        layer = new int[parameterParent.layer.length];
        color = new javafx.scene.paint.Color[parameterParent.color.length];
        colorARGB = new int[parameterParent.color.length];
        folderRaw = new String[parameterParent.folder.length];
        folder = new File[parameterParent.folder.length];
        annotation = new TongaAnnotation[parameterParent.annotation.length];
        annotationIndex = new int[parameterParent.annotation.length];
        annotationIndexType = new AnnotationType[parameterParent.annotation.length][];
        annotationType = new AnnotationType[parameterParent.annotationType.length];
        annotationGroup = new int[parameterParent.annotationGroup.length];
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
            return new Double[]{ddata[0], ddata[0] + (ddata[1] - ddata[0]) / 2, ddata[1], ddata[2]};
        }
        return ddata;
    }
}
