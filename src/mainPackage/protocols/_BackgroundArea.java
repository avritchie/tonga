package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.utils.RGB;

public class _BackgroundArea extends Protocol {

    @Override
    public String getName() {
        return "Background estimation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Stack/DAPI etc. to use to estimate the area"),
            new ControlReference(LAYER, "The stain to calculate the background for"),
            new ControlReference(SLIDER, "Strictness", 80)};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Background", 11) {

            ImageData temp;

            @Override
            protected void methodInit() {
                temp = Filters.dog().runSingle(inImage[0], 10, 30, false);
                temp = Filters.autoscaleWithAdapt().runSingle(temp, 50);
                temp = Filters.thresholdBright().runSingle(temp, 5);
                temp = Filters.box().runSingle(temp, (param.slider[0] + 40) / 3, true);
                temp = Filters.thresholdBright().runSingle(temp, 1);
                temp = Filters.box().runSingle(temp, (param.slider[0] + 40) / 6, true);
                temp = Filters.gaussApprox().runSingle(temp, (param.slider[0] + 40) / 3, true);
            }

            @Override
            protected void pixelProcessor() {
                if (inImage[1].bits == 16) {
                    outImageAs16Bits(0);
                    Iterate.pixels(inImage[0], (int pos) -> {
                        outImage[0].pixels16[pos] = temp.pixels32[pos] <= 0xFF191919
                                ? inImage[1].pixels16[pos] : COL.UWHITE;
                    });
                } else {
                    Iterate.pixels(inImage[0], (int pos) -> {
                        outImage[0].pixels32[pos] = temp.pixels32[pos] <= 0xFF191919
                                ? RGB.argb(RGB.brightness(inImage[1].pixels32[pos])) : COL.WHITE;
                    });
                }
            }

            @Override
            protected void methodFinal() {
                addResultData(Counters.countBWBG().runSingle(sourceImage, outImage[0]));
            }
        };
    }
}
