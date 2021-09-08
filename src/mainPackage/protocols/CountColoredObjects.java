package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.RGB;

public class CountColoredObjects extends Protocol {

    @Override
    public String getName() {
        return "Count coloured areas in objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Process which layer"),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "Areas masked with which color"),
            new ControlReference(SLIDER, "Mask threshold"),
            new ControlReference(SPINNER, "Ignore objects that are smaller than (pixels)"),
            new ControlReference(TOGGLE, "Report results as individual shapes", 0)};
    }

    @Override
    protected Processor getProcessor() {
        int limit = param.spinner[0];
        double thresh = param.slider[0] / 100.0;
        int color = param.combo[0];
        boolean indv = param.toggle[0];
        
        return new ProcessorFast("Areas", 1) {

            @Override
            protected void methodCore(int p) {
                int col = inImage[0].pixels32[p];
                boolean gb = false;
                switch (color) {
                    case 0:
                        gb = (col >> 16 & 0xFF) > thresh;
                        break;
                    case 1:
                        gb = (col >> 8 & 0xFF) > thresh;
                        break;
                    case 2:
                        gb = (col & 0xFF) > thresh;
                        break;
                }
                gb = RGB.brightness(col) > 127 && RGB.saturation(col) > 0.5 ? gb : false;
                outImage[0].pixels32[p] = gb ? COL.WHITE : COL.BLACK;
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(outImage[0], Color.BLACK).trace();
                set.filterOutSmallObjects(limit);
                outImage[0] = set.drawToImageData();
                datas.add(indv
                        ? SetCounters.countObjectsSingle(set).runSingle(sourceImage, sourceLayer[0])
                        : SetCounters.countObjectsImage(set).runSingle(sourceImage, sourceLayer[0]));
            }
        };
    }
}
