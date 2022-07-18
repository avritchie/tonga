package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.SetCounters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class MatrigelOrganoids extends Protocol {

    @Override
    public String getName() {
        return "Count organoids";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Gel image is at which layer"),
            new ControlReference(SLIDER, new Integer[]{0, 10}, "Threshold for the tissue", 2),
            new ControlReference(SPINNER, "Ignore objects that are smaller than (pixels)", 100)};
    }

    @Override
    protected Processor getProcessor() {
        int limit = param.spinner[0];
        int thresh = param.slider[0];

        return new ProcessorFast(3, "Organoids", 82) {

            ImageData layer, work, l2, l3;

            @Override
            protected void methodInit() {
                layer = Filters.thresholdBright().runSingle(inImage[0], 34 + thresh * 3);
                work = Filters.niblack().runSingle(inImage[0], 10, 16 + thresh * 2, 3 + thresh);
                layer = Blender.renderBlend(layer, work, Blend.MULTIPLY);
                layer = Filters.invert().runSingle(layer);
                layer = Filters.connectEdges().runSingle(layer);
                layer = Filters.connectEdges().runSingle(layer);
                layer = FiltersPass.filterObjectSize().runSingle(layer, COL.BLACK, limit, false, 0);
                // old nucl final mask starts
                work = Filters.gamma().runSingle(inImage[0], 50);
                work = Filters.maximumDiffEdge().runSingle(work, 0, 2, false, 0);
                work = Filters.invert().runSingle(work);
                work = Filters.localThreshold().runSingle(work, 10, 0, 5.);
                Iterate.pixels(inImage[0], (int p) -> {
                    work.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : work.pixels32[p];
                });
                l2 = FiltersPass.filterObjectSize().runSingle(work, limit);
                Iterate.pixels(inImage[0], (int p) -> {
                    boolean removed = l2.pixels32[p] != work.pixels32[p];
                    work.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : removed ? COL.BLACK : COL.WHITE;
                });
                work = FiltersPass.edgeErode().runSingle(work, COL.BLACK, 1, true, true);
                work = FiltersPass.gaussSmoothing().runSingle(work);
                // dim erosion
                l3 = FiltersPass.edgeErode().runSingle(work, COL.BLACK, 5, true, true);
                ROISet set = new ImageTracer(l3, Color.BLACK).trace();
                set.quantifyStainAgainstChannel(Filters.dog().runSingle(inImage[0], 2, 40, false));
                set.filterOutDimObjects(2);
                set.filterOutSmallObjects(limit);
                set = set.getPositionFilteredSet(work, COL.BLACK, false);
                work = set.drawToImageData();
                // old nucl final mask ends
                //work = new __NucleusFinalMask().runSilent(sourceImage,new ImageData[]{layer, inImage[0], inImage[0]},new Object[]{Color.BLACK, limit, false, 0, false})[0];
                l3 = Filters.bwSaturation().runSingle(work);
                l2 = Filters.cutFilter().runSingle(l3, new Object[]{57 + thresh * 9, 180});
                work = Filters.thresholdBright().runSingle(l2, 11 + thresh * 7);
                layer = Blender.renderBlend(layer, work, Blend.DIFFERENCE);
                layer = FiltersPass.edgeDilate().runSingle(layer, COL.BLACK, 1, false);
                layer = FiltersPass.fillInnerAreas().runSingle(layer, COL.BLACK, false);
                layer = FiltersPass.edgeErode().runSingle(layer, COL.BLACK, 3, false, true);
                layer = FiltersPass.edgeDilate().runSingle(layer, COL.BLACK, 1, false);
                layer = Filters.smoothenCorners().runSingle(layer);
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(layer, Color.BLACK).trace();
                set.filterOutSmallObjects(limit);
                outImage[0].pixels32 = set.drawToArray();
                outImage[1].pixels32 = l2.pixels32;
                outImage[2].pixels32 = l3.pixels32;
                addResultData(SetCounters.countObjectsSingle(set).runSingle(sourceImage));
            }
        };
    }

}
