package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COMBO;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.counters.SetCounters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class CryptCells extends Protocol {

    @Override
    public String getName() {
        return "Count cells in intestinal crypts";
    }

    @Override
    protected ControlReference[] getParameters() {

        return new ControlReference[]{
            new ControlReference(LAYER, "Layer to process"),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "E-cadherin is on which channel", 0),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "Target staining is on which channel", 1),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "DAPI is on which channel", 2)};
    }

    @Override
    protected Processor getProcessor() {
        int ecadLayer = param.combo[0];
        int targetLayer = param.combo[1];
        int dapiLayer = param.combo[2];

        return new ProcessorFast("Crypts") {

            ImageData layer, layer2;
            int[] ecadVals, targetVals;
            int[] dapiVals;

            @Override
            protected void methodInit() {
                ecadVals = new int[outImage[0].totalPixels()];
                dapiVals = new int[outImage[0].totalPixels()];
                targetVals = new int[outImage[0].totalPixels()];
            }

            @Override
            protected void methodCore(int p) {
                int r = (inImage[0].pixels32[p] >> 16) & 0xFF;
                int g = (inImage[0].pixels32[p] >> 8) & 0xFF;
                int b = inImage[0].pixels32[p] & 0xFF;
                switch (ecadLayer) {
                    case 0:
                        ecadVals[p] = r;
                        break;
                    case 1:
                        ecadVals[p] = g;
                        break;
                    case 2:
                        ecadVals[p] = b;
                        break;
                }
                switch (dapiLayer) {
                    case 0:
                        dapiVals[p] = r > 20 ? 0xFFFFFF : 0x0;
                        break;
                    case 1:
                        dapiVals[p] = g > 20 ? 0xFFFFFF : 0x0;
                        break;
                    case 2:
                        dapiVals[p] = b > 20 ? 0xFFFFFF : 0x0;
                        break;
                }
                switch (targetLayer) {
                    case 0:
                        targetVals[p] = r;
                        break;
                    case 1:
                        targetVals[p] = g;
                        break;
                    case 2:
                        targetVals[p] = b;
                        break;
                }
            }

            @Override
            protected void methodFinal() {
                layer = new ImageData(ecadVals, sourceWidth[0], sourceHeight[0]);
                layer = Filters.box().runSingle(layer, 1.);
                layer = Filters.niblack().runSingle(layer, 0, 20, 10);
                layer = Filters.connectEdges().runSingle(layer);
                layer = FiltersSet.filterObjectSize().runSingle(layer, COL.BLACK, 200, true, 200);
                layer2 = new ImageData(dapiVals, sourceWidth[0], sourceHeight[0]);
                layer2 = Filters.distanceTransform().runSingle(layer2);
                for (int y = 0; y < sourceHeight[0]; y++) {
                    for (int x = 0; x < sourceWidth[0]; x++) {
                        int p = (y * sourceWidth[0] + x);
                        boolean ecad = (layer.pixels32[p] & 0xFF) == 0xFF;
                        boolean dapi = (layer2.pixels32[p] & 0xFF) > 5;
                        outImage[0].pixels32[p] = !ecad && dapi ? 0xFFFFFF : 0x0;
                    }
                }
                layer = Filters.distanceTransform().runSingle(outImage[0]);
                layer = Filters.thresholdBright().runSingle(layer, 1);
                layer = Filters.sharpenEdges().runSingle(layer);
                layer = Filters.evenEdges().runSingle(layer);
                ROISet set = new ImageTracer(layer, Color.BLACK).trace();
                set.filterOutSmallObjects(100);
                setOutputBy(set);
                addResultData(SetCounters.countObjectsImage(set).runSingle(sourceImage));
            }
        };
    }
}
