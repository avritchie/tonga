package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersSet;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

public class HETissueSeparation extends Protocol {

    @Override
    public String getName() {
        return "Measure HE-stained tissue areas";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Original layer is which one"),
            new ControlReference(SLIDER, new Integer[]{25, 75}, "Threshold for the hematoxylin", 50),
            new ControlReference(SLIDER, new Integer[]{0, 255}, "Threshold for stain separation", 240),
            new ControlReference(SLIDER, new Integer[]{25, 75}, "Threshold for the eosin", 50),
            new ControlReference(COMBO, new String[]{"5x", "10x", "20x"}, "Resolution", 1)};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast(5, "HE Tissue separation", 17) {

            int threshHemxyl = param.slider[0];
            int threshStain = param.slider[1];
            int threshEosin = param.slider[2];
            double magn = Math.pow(2, param.combo[0]);

            int[] collVals;
            ImageData layer, layer2;

            @Override
            protected void methodInit() {
                collVals = new int[outImage[0].totalPixels()];
                int[] histo = HISTO.getHistogram(inImage[0].pixels32);
                int high = HISTO.getHighestPointIndex(histo, false);
                layer = Filters.cutFilter().runSingle(inImage[0], new Object[]{50, high});
                layer = Filters.invert().runSingle(layer);
                layer = Filters.gaussApprox().runSingle(layer, 5 * magn);
                layer = Filters.thresholdBright().runSingle(layer, threshHemxyl);
                outImage[0].pixels32 = layer.pixels32;
            }

            @Override
            protected void methodCore(int p) {
                int r = (inImage[0].pixels32[p] >> 16) & 0xFF;
                int g = (inImage[0].pixels32[p] >> 8) & 0xFF;
                int b = inImage[0].pixels32[p] & 0xFF;
                double redBlue = ((double) r) / ((double) b);
                double redGreen = ((double) r) / ((double) g);
                double greenBlue = ((double) g) / ((double) b);
                double bright = (r + g + b) / (255. * 3);
                //calcs
                double blueFilter = Math.min(1, Math.max(0, (redBlue - 1) * 3.5) + 3.5 * redBlue * (redGreen - 1));
                double darkBluFilter = (greenBlue - 0.45);
                double brightBalancer = Math.min(1, (1 - bright) * 1.2);
                double darkVal = ((1 - bright) * (redGreen - 1));
                double brightVal = bright * 2 * ((redGreen - 1) * 3 * ((redBlue - 0.5) * 2));
                //output
                double val = brightBalancer * Math.min(255, Math.max(0, 333 * ((blueFilter * (brightVal + darkVal * darkBluFilter)) - 0.1)));
                int col = Math.min(255, (int) (2 * val));
                col = (255 << 16 | (255 - col) << 8 | (255 - col) | 0xFF << 24);
                col = (layer.pixels32[p] & 0xFF) == 0xFF ? col : COL.BLACK;
                collVals[p] = RGB.relativeLuminance(col) * 255 >= threshStain ? COL.WHITE : COL.BLACK;
            }

            @Override
            protected void methodFinal() {
                layer2 = new ImageData(collVals, sourceWidth[0], sourceHeight[0]);
                outImage[1].pixels32 = layer2.pixels32;
                layer2 = Filters.gaussApprox().runSingle(layer2, 5 * magn);
                outImage[2].pixels32 = layer2.pixels32;
                layer2 = Filters.thresholdBright().runSingle(layer2, threshEosin);
                outImage[3].pixels32 = layer2.pixels32;
                layer2 = Blender.renderBlend(layer, layer2, Blend.MULTIPLY);
                layer = FiltersSet.filterObjectSize().runSingle(layer2, COL.BLACK, 60, true, 250 * magn);
                outImage[4].pixels32 = layer.pixels32;
            }
        };
    }
}
