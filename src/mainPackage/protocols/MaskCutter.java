package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SLIDER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.utils.COL;

public class MaskCutter extends Protocol {

    @Override
    public String getName() {
        return "Cut masks based on internal lines";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Masks are on which layer"),
            new ControlReference(LAYER, "Raw image is on which layer"),
            new ControlReference(SPINNER, "Radius", 50),
            new ControlReference(SLIDER, new Object[]{1, 20}, "Sensitivity", 3)};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("Cut") {

            ImageData temp, temp2;

            @Override
            protected void pixelProcessor() {
                temp = Filters.dog().runSingle(inImage[1], Math.max(1, param.spinner[0] / 50), param.spinner[0] / 10, true);
                Filters.thresholdBright().runTo(temp, param.slider[0]);
                temp2 = Blender.renderBlend(inImage[0], temp, Blend.MULTIPLY);
                temp = Blender.renderBlend(inImage[0], temp2, Blend.DIFFERENCE);
                FiltersSet.fillInnerAreas().runTo(temp, COL.BLACK, true);
                FiltersPass.fillSmallEdgeHoles().runTo(temp, COL.BLACK, 1, param.spinner[0], false, false);
                FiltersPass.gaussSmoothing().runTo(temp, 1, 5);
                setOutputBy(temp);
            }
        };
    }
}
