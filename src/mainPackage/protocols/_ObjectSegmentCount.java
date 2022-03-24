package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class _ObjectSegmentCount extends Protocol {

    @Override
    public String getName() {
        return "Object segmentation and counting";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(SPINNER, "Ignore segmented objects smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Report results as individual objects", 1),
            new ControlReference(SLIDER, new Integer[]{10, 200, 580, 38}, "Average diameter of a nucleus", 1)};
    }

    @Override
    protected Processor getProcessor() {
        int limit = param.spinner[0];
        Color bg = param.color[0];
        boolean image = param.toggle[0];
        int nucleusSize = (int) param.sliderScaled[0];

        return new ProcessorFast("Objects", 4) {

            ImageData temp;

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                temp = new __ObjectSegment().runSilent(sourceImage, new ImageData[]{inImage[0]}, bg, nucleusSize)[0];
                ROISet set = new ImageTracer(temp, bg).trace();
                set.filterOutSmallObjects(limit);
                //set.findOuterEdges();
                setOutputBy(set);
                if (image) {
                    addResultData(SetCounters.countObjectsSingle(set).runSingle(sourceImage));
                } else {
                    addResultData(SetCounters.countObjectsImage(set).runSingle(sourceImage));
                }
            }
        };
    }
}
