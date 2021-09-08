package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SLIDER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class __DeadDividing extends Protocol {

    @Override
    public String getName() {
        return "Remove dead and dividing";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Get mask from which layer"),
            new ControlReference(LAYER, "Original DAPI/Hoechst"),
            new ControlReference(COLOUR, "Background colour", -2)};
        //new ControlReference(SLIDER, new Integer[]{10, 200, 580, 38}, "Average size of a nucleus", 5)};
    }

    @Override
    protected Processor getProcessor() {
        int sourceCol = param.colorARGB[0];

        return new ProcessorFast("Filtered", 1) {
            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(inImage[0], sourceCol).trace();
                set.filterDeadDividing(inImage[1]);
                setOutputBy(set.drawToImageData(true));
            }
        };
    }
}
