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
import static mainPackage.PanelCreator.ControlType.COMBO;

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
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(COMBO, new String[]{"Corner", "Center", "Centroid"}, "Common location to assume", 0)};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int location = param.combo[0];

        return new ProcessorFast("Original Objects") {

            @Override
            protected void pixelProcessor() {
                ROISet filteredSet = new ImageTracer(inImage[0], bg).trace();
                ImageTracer origSet = new ImageTracer(inImage[1], bg);
                List<ROI> newRois = new ArrayList<>();
                filteredSet.list.forEach(roi -> {
                    if (location == 2) {
                        roi.getCentroid();
                    }
                    int x = location == 2 ? roi.xcentroid : location == 1 ? roi.xcenter : roi.area.firstxpos;
                    int y = location == 2 ? roi.ycentroid : location == 1 ? roi.ycenter : roi.area.firstypos;
                    ROI newRoi = origSet.traceSingleObjectAtPoint(x, y);
                    if (newRoi != null) {
                        newRois.add(newRoi);
                    }
                });
                setOutputBy(new ROISet(newRois, sourceWidth[0], sourceHeight[0]).drawToImageData());
            }
        };
    }
}
