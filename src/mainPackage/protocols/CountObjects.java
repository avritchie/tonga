package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class CountObjects extends Protocol {

    @Override
    public String getName() {
        return "Count objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(TOGGLE, "Fill holes inside of the recognized objects", 0),
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(SPINNER, "Ignore objects that are smaller than (pixels)"),
            new ControlReference(TOGGLE, "Report results as individual shapes", 1)};
    }

    @Override
    protected Processor getProcessor() {
        int limit = param.spinner[0];
        Color bg = param.color[0];
        boolean holes = param.toggle[0];
        boolean indv = param.toggle[1];

        return new ProcessorFast("Objects") {

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                set.filterOutSmallObjects(limit);
                if (holes) {
                    set.fillInnerHoles();
                }
                outImage[0] = set.drawToImageData();
                datas.add(indv
                        ? SetCounters.countObjectsSingle(set).runSingle(sourceImage, sourceLayer[0])
                        : SetCounters.countObjectsImage(set).runSingle(sourceImage, sourceLayer[0]));
            }
        };
    }
}
