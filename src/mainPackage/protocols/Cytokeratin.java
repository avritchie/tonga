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
public class Cytokeratin extends Protocol {

    @Override
    public String getName() {
        return "Measure cytokeratin-stained tissue areas";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Raw image is on which layer"),
            new ControlReference(COMBO, new String[]{"RED", "GREEN", "BLUE"}, "The CK8 is stained with which colour", 1),
            new ControlReference(COMBO, new String[]{"RED", "GREEN", "BLUE"}, "The CK14 is stained with which colour", 0),
            new ControlReference(SLIDER, "The CK14 should be how strong to be positive", 25),
            new ControlReference(SLIDER, "The CK8 should be how strong for double positivity", 40),
            new ControlReference(SLIDER, new Integer[]{0, 20}, "How sensitive the tissue separator should be", 8)};
    }

    @Override
    protected Processor getProcessor() {
        double ck8ValThrshld = (100 - param.slider[0]) * 0.01; //threshold range 0-100;
        double ck14Thrshld = param.slider[1] * (255. / 100); //threshold range 0-255;
        int tissueThrshld = param.slider[2]; //threshold range 0-20;

        return new ProcessorFast("CK8/14", 7) {

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
                int ck8 = COL.comboColorSelector(param.combo[0], col);
                int ck14 = COL.comboColorSelector(param.combo[1], col);
                double ck8ck14ratio = 1.25; //what is considered to be positive
                int bright = RGB.brightness(col);
                outImage[0].pixels32[p] = layer.pixels32[p] == COL.BLACK ? COL.BLACK
                        : (ck14 * ck8ck14ratio >= ck8) && (bright > ck14Thrshld)
                                ? ck8 > ck14 * ck8ValThrshld ? COL.YELLOW : COL.RED : COL.GREEN;
            }

            @Override
            protected void methodFinal() {
                datas.add(Counters.countRGBCMYK().runSingle(sourceImage.imageName, outImage[0]));
            }

        };
    }
}
