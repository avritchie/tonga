package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

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
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int nucleusSize = param.spinner[0];

        return new ProcessorFast(3, "Objects", 5) {

            ImageData temp;

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                int nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                //set.filterOutSmallObjects(GEO.circleArea(nucleusSize) / 5);
                set.targetSize(nuclSize);
                set.findOuterEdges();
                set.findInnerEdges();
                set.analyzeCorners();
                set.analyzeCornerIntersections();
                set.segment(0);
                outImage[0] = set.drawToImageData(true);
                //everything below is optional and for debug use
                outImage[1] = set.drawToImageData();
                set = new ImageTracer(outImage[0], bg).trace();
                set.findOuterEdges();
                outImage[2] = set.drawToImageData();
                datas.add(SetCounters.countObjectsSingle(set).runSingle(sourceImage, outImage[0]));
            }
        };
    }
}
