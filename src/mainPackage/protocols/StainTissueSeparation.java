package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

public class StainTissueSeparation extends Protocol {

    @Override
    public String getName() {
        return "Stain-Tissue Separation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Original layer is which one"),
            new ControlReference(LAYER, "Collagen layer is which one"),
            new ControlReference(SLIDER, "Threshold for the tissue detection")};
    }

    @Override
    protected Processor getProcessor() {
        double threshTissue = param.slider[0];

        return new ProcessorFast("Tissue separation", 13) {

            int[] collVals;
            ImageData layer, layer2, layer3;

            @Override
            protected void methodInit() {
                collVals = new int[outImage[0].totalPixels()];
            }

            @Override
            protected void methodCore(int p) {
                collVals[p] = (int) (255 * RGB.relativeLuminance(inImage[1].pixels32[p]));
            }

            @Override
            protected void methodFinal() {
                layer = Filters.bwBrightness().runSingle(inImage[0]);
                layer2 = layer;
                layer = Filters.autoscaleWithAdapt().runSingle(layer, 5);
                layer = Filters.cutFilter().runSingle(layer, new Object[]{0, 225});
                layer = Filters.invert().runSingle(layer);
                layer3 = layer;
                layer = Filters.multiply().runSingle(layer, 1600.);
                layer = Filters.box().runSingle(layer, 2.);
                layer = Filters.invert().runSingle(layer);
                layer = Filters.thresholdBright().runSingle(layer, (int) threshTissue);
                layer = Filters.gaussApprox().runSingle(layer, 4.);
                layer = Filters.thresholdBright().runSingle(layer, 67);
                layer = FiltersPass.filterObjectSize().runSingle(layer, COL.BLACK, 0, false, 500);
                Iterate.pixels(inImage[0], (int p) -> {
                    int ishVal = 255 - collVals[p];
                    int ishARGB = (255 << 16 | (255 - ishVal) << 8 | (255 - ishVal) | 0xff << 24);
                    outImage[0].pixels32[p] = (layer.pixels32[p] & 0xFF) == 0 ? ishARGB : 0x0;
                });
                addResultData(Counters.countRBStain().runSingle(sourceImage, outImage[0]));
            }
        };
    }

}
