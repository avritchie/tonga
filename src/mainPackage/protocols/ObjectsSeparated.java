package mainPackage.protocols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import static mainPackage.PanelCreator.ControlType.COLOUR;

public class ObjectsSeparated extends Protocol {

    @Override
    public String getName() {
        return "Find separated objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Layer with separated objects"),
            new ControlReference(LAYER, "Original unseparated layer"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0})};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        return new ProcessorFast(2, new String[]{"Separated Objects", "Non-separated Objects"}) {

            @Override
            protected void pixelProcessor() {
                ROISet separSet = new ImageTracer(inImage[0], bg).trace();
                ImageTracer origSet = new ImageTracer(inImage[1], bg);
                List<ROI> newRois = new ArrayList<>();
                List<ROI> oldRois = new ArrayList<>();
                Map<Integer, ROI> detcPos = new HashMap<>();
                separSet.list.forEach(roi -> {
                    ROI newRoi = origSet.traceSingleObjectAtPoint(roi.xcenter, roi.ycenter);
                    if (newRoi != null) {
                        oldRois.add(newRoi);
                        Integer code = newRoi.ycenter * sourceWidth[0] + newRoi.xcenter;
                        if (detcPos.containsKey(code)) {
                            newRois.add(roi);
                            ROI origRoi = detcPos.get(code);
                            if (!newRois.contains(origRoi)) {
                                newRois.add(origRoi);
                            }
                        } else {
                            detcPos.put(code, roi);
                        }
                    }
                });
                Iterator<ROI> oldIt = oldRois.iterator();
                while (oldIt.hasNext()) {
                    ROI cRoi = oldIt.next();
                    if (newRois.contains(cRoi)) {
                        oldIt.remove();
                    }
                }
                setOutputBy(new ROISet(newRois, sourceWidth[0], sourceHeight[0]).drawToImageData(), 0);
                setOutputBy(new ROISet(oldRois, sourceWidth[0], sourceHeight[0]).drawToImageData(), 1);
            }
        };
    }
}
