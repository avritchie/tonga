package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

public class BrightRemover extends Protocol {

    @Override
    public String getName() {
        return "Remove bright inner objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(LAYER, "The image with the stain"),
            new ControlReference(COLOUR, "The objects are which color", new int[]{0}),
            new ControlReference(SPINNER, "Dilation radius", 2),
            new ControlReference(SLIDER, new Object[]{0, 5, 100}, "If brighter than x times the background")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("Inner filler") {
            ImageData mask;

            @Override
            protected void pixelProcessor() {
                double[] bgInts = bgIntensities();
                ROISet set = new ImageTracer(inImage[0], param.color[0]).trace();
                ROI big = set.getBiggestObject();
                set.list.remove(big);
                //mask = FiltersPass.edgeErode().runSingle(set.drawToImageData(true), COL.BLACK, param.spinner[0], false, true);
                //set = new ImageTracer(mask, COL.BLACK).trace();
                set.quantifyStainAgainstChannel(inImage[1]);
                set.filterOutBrightObjects(((bgInts[0] - bgInts[1]) / 2 + bgInts[1]) * param.sliderScaled[0]);
                set.list.add(big);
                setOutputBy(Filters.invert().runSingle(set.drawToImageData(true)));
            }

            private double[] bgIntensities() {
                long[] bfi = new long[2];
                int[] bfc = new int[2];
                boolean bits = inImage[1].bits == 16;
                Iterate.pixels(inImage[0], (int p) -> {
                    int ind = inImage[0].pixels32[p] == param.colorARGB[0] ? 1 : 0;
                    bfi[ind] += bits ? inImage[1].pixels16[p] & COL.UWHITE : RGB.brightness(inImage[1].pixels32[p]);
                    bfc[ind]++;
                });
                return new double[]{
                    bfi[0] / (double) bfc[0] / (bits ? 65535 : 255),
                    bfi[1] / (double) bfc[1] / (bits ? 65535 : 255)};
            }
        };
    }
}
