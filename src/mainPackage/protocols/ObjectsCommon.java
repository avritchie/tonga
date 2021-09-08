package mainPackage.protocols;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import static mainPackage.PanelCreator.ControlType.COLOUR;

public class ObjectsCommon extends Protocol {

    @Override
    public String getName() {
        return "Find common objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Layer with filtered objects"),
            new ControlReference(LAYER, "Original unfiltered layer"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0})};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        return new ProcessorFast("Original Objects") {

            @Override
            protected void pixelProcessor() {
                ROISet filteredSet = new ImageTracer(sourceLayer[0], bg).trace();
                ImageTracer origSet = new ImageTracer(sourceLayer[1], bg);
                List<ROI> newRois = new ArrayList<>();
                filteredSet.list.forEach(roi -> {
                    ROI newRoi = origSet.traceSingleObjectAtPoint(roi.area.firstxpos, roi.area.firstypos);
                    if (newRoi != null) {
                        newRois.add(newRoi);
                    }
                });
                setOutputBy(new ROISet(newRois, sourceWidth[0], sourceHeight[0]).drawToImageData());
            }
        };
    }
}
