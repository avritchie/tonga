package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SLIDER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class _ObjectIntensity extends Protocol {

    @Override
    public String getName() {
        return "Mask intensity";
    }

    @Override
    protected PanelCreator.ControlReference[] getParameters() {
        return new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(LAYER, "Track objects at which layer"),
            new PanelCreator.ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new PanelCreator.ControlReference(TOGGLE, "Binary staining", 0, new int[]{3, 1, 4, 0}),
            new PanelCreator.ControlReference(SLIDER, "Binary threshold (%)"),
            new PanelCreator.ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        boolean binst = param.toggle[0];
        boolean perimg = param.toggle[1];
        double thresh = param.slider[0] / 100.;

        return new ProcessorFast("Objects") {
            ImageData bgid;
            double bgval;

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(inImage[0], param.color[0]).trace();
                set.quantifyEvenColour(inImage[0]);
                if (binst) {
                    outImage[0].pixels32 = set.drawStainArray(thresh, false);
                    datas.add(SetCounters.countObjectPositive(set, thresh).runSingle(sourceImage));
                } else {
                    outImage[0].pixels32 = set.drawStainArray(true);
                    if (perimg) {
                        datas.add(SetCounters.countObjectStainsImage(set).runSingle(sourceImage));
                    } else {
                        datas.add(SetCounters.countObjectStainsSingle(set).runSingle(sourceImage));
                    }
                }
            }
        };
    }
}
