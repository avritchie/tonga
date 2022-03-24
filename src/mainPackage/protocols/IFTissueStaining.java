package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class IFTissueStaining extends Protocol {

    @Override
    public String getName() {
        return "Measure immunofluorescence-stained tissue areas";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Raw image is on which layer"),
            new ControlReference(COMBO, new String[]{"RED", "GREEN", "BLUE"}, "The first stain is stained with which colour", 1),
            new ControlReference(COMBO, new String[]{"RED", "GREEN", "BLUE"}, "The second stain is stained with which colour", 0),
            new ControlReference(SLIDER, "The second stain  should be how strong to be positive", 25),
            new ControlReference(SLIDER, "The first stain  should be how strong for double positivity", 40),
            new ControlReference(SLIDER, new Integer[]{0, 20}, "How sensitive the tissue separator should be", 8)};
    }

    @Override
    protected Processor getProcessor() {
        double fthresh = (100 - param.slider[0]) * 0.01; //threshold range 0-100;
        double sthresh = param.slider[1] * (255. / 100); //threshold range 0-255;
        int tissueThrshld = param.slider[2]; //threshold range 0-20;

        return new ProcessorFast("IF Stains", 7) {

            ImageData layer;
            int[] holeGrid;

            @Override
            protected void methodInit() {
                layer = Filters.gamma().runSingle(inImage[0], 0.5);
                layer = Filters.bwBrightness().runSingle(layer);
                layer = Filters.gaussApprox().runSingle(layer, 2.0);
                layer = Filters.thresholdBright().runSingle(layer, 10 + tissueThrshld);
            }

            @Override
            protected void methodCore(int p) {
                int col = inImage[0].pixels32[p];
                int fstain = COL.comboColorSelector(param.combo[0], col);
                int sstain = COL.comboColorSelector(param.combo[1], col);
                double stainratio = 1.25; //what is considered to be positive
                int bright = RGB.brightness(col);
                outImage[0].pixels32[p] = layer.pixels32[p] == COL.BLACK ? COL.BLACK
                        : (sstain * stainratio >= fstain) && (bright > sthresh)
                                ? fstain > sstain * fthresh ? COL.YELLOW : COL.RED : COL.GREEN;
            }

            @Override
            protected void methodFinal() {
                addResultData(Counters.countRGBCMYK().runSingle(sourceImage, outImage[0]));
            }

        };
    }
}
