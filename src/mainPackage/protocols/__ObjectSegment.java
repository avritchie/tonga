package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class __ObjectSegment extends Protocol {

    @Override
    public String getName() {
        return "Object segmentation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(SPINNER, "Average object diameter (pixels)", 60),
            new ControlReference(COMBO, new String[]{"Normal", "Strict", "Very strict"}, "Criteria for separation"),
            new ControlReference(TOGGLE, "Perform twice", 0),};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int nucleusSize = param.spinner[0];
        int mode = param.combo[0];

        return new ProcessorFast(Tonga.debug() ? 2 : 1, "Objects", 3) {

            int nuclSize;

            @Override
            protected void methodInit() {
                nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
            }

            @Override
            protected void pixelProcessor() {
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                set.targetSize(nuclSize);
                set.findOuterEdges();
                set.findInnerEdges();
                set.analyzeCorners();
                set.analyzeCornerIntersections();
                set.segment(mode);
                //this image is optional and for debug use
                setSampleOutputBy(set.drawToImageData(), 1);
                if (param.toggle[0]) {
                    set = new ImageTracer(set.drawToImageData(true), COL.BLACK).trace();
                    set.targetSize(nuclSize);
                    set.findOuterEdges();
                    set.findInnerEdges();
                    set.analyzeCorners();
                    set.analyzeCornerIntersections();
                    set.segment(mode);
                }
                setOutputBy(set.drawToImageData(true));
            }
        };
    }
}
