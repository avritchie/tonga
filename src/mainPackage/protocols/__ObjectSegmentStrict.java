package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class __ObjectSegmentStrict extends Protocol {

    @Override
    public String getName() {
        return "Strict object segmentation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(LAYER, "Only points touching objects on this layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int nucleusSize = param.spinner[0];

        return new ProcessorFast(Tonga.debug() ? 2 : 1, "Objects", 5) {

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
                set.filterUnsureCorners();
                set.analyzeCornerIntersections();
                set.segment(2);
                set.filterCornersNotTouching(inImage[1]);
                set.segment(1);
                setOutputBy(set.drawToImageData(true));
                //optional for debug use
                setSampleOutputBy(set.drawToImageData(), 1);
            }
        };
    }
}
