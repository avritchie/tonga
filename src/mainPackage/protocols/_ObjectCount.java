package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class _ObjectCount extends Protocol {

    @Override
    public String getName() {
        return "Object counting";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(TOGGLE, "Report results as individual objects")};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        boolean image = param.toggle[0];

        return new ProcessorFast(0, "Objects", 1) {

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                if (image) {
                    addResultData(SetCounters.countObjectsSingle(set).runSingle(sourceImage));
                } else {
                    addResultData(SetCounters.countObjectsImage(set).runSingle(sourceImage));
                }
            }
        };
    }
}
