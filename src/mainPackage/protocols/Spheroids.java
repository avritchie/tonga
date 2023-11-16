package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class Spheroids extends Protocol {

    @Override
    public String getName() {
        return "Spheroids";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image is on which layer"),
            new ControlReference(SLIDER, "Target size (pixels)", 25)};
    }

    @Override
    protected Processor getProcessor() {
        int targetSize = param.slider[0] * 2;
        int minSize = Math.max(1, targetSize / 50);
        int filterSize = param.slider[0] * 4;

        return new ProcessorFast(fullOutput() ? 5 : 1, "Spheroids", 43) {

            ImageData temp, temp2;

            @Override
            protected void pixelProcessor() {
                temp = Filters.illuminationCorrection().runSingle(inImage[0]);
                Filters.invert().runTo(temp);
                setSampleOutputBy(temp, 1);
                Filters.dog().runTo(temp, minSize, targetSize, false);
                temp2 = Filters.thresholdBright().runSingle(temp, 2);
                setSampleOutputBy(temp2, 2);
                temp = Protocol.load(DimRemover::new).runSilent(sourceImage, new ImageData[]{temp2, temp}, COL.BLACK, 5, false, 0)[0];
                setSampleOutputBy(temp, 3);
                temp = FiltersPass.fillSmallEdgeHoles().runSingle(temp, COL.BLACK, minSize, filterSize, true, false);
                temp = FiltersPass.fillSmallEdgeHoles().runSingle(temp, COL.BLACK, minSize * 2, filterSize, true, false);
                setSampleOutputBy(temp, 4);
                temp = FiltersSet.fillInnerAreasSizeShape().runSingle(temp, COL.BLACK, filterSize, 90, false, 0, true);
                setOutputBy(temp);
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(outImage[0], Color.BLACK).trace();
                addResultData(SetCounters.countObjectsImage(set).runSingle(sourceImage));
            }

        };
    }
}
