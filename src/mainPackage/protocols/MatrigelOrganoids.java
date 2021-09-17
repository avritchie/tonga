package mainPackage.protocols;

import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaRender;
import mainPackage.counters.SetCounters;
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.Smoother;
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
                layer = TongaRender.blend(layer, work, BlendMode.MULTIPLY);
                layer = Filters.invert().runSingle(layer);
                layer = ConnectEdges.run().runSingle(layer);
                layer = ConnectEdges.run().runSingle(layer);
                layer = FiltersPass.filterObjectSize().runSingle(layer, COL.BLACK, limit, false, 0);
                work = new __NucleusFinalMask().runSilent(sourceImage,
                        new ImageData[]{layer, inImage[0], inImage[0]},
                        new Object[]{Color.BLACK, limit, false, 0, false})[0];
                l3 = Filters.bwSaturation().runSingle(work);
                l2 = Filters.cutFilter().runSingle(l3, new Object[]{57 + thresh * 9,180});
                work = Filters.thresholdBright().runSingle(l2, 11 + thresh * 7);
                layer = TongaRender.blend(layer, work, BlendMode.DIFFERENCE);
                layer = FiltersPass.edgeDilate().runSingle(layer, COL.BLACK, 1, false);
                layer = FiltersPass.fillInnerAreas().runSingle(layer, false);
                layer = FiltersPass.edgeErode().runSingle(layer, COL.BLACK, 3, false, true);
                layer = FiltersPass.edgeDilate().runSingle(layer, COL.BLACK, 1, false);
                layer = Smoother.run().runSingle(layer);
            }

            @Override
            protected void methodFinal() {
                ROISet set = new ImageTracer(layer, Color.BLACK).trace();
                set.filterOutSmallObjects(limit);
                outImage[0].pixels32 = set.drawToArray();
                outImage[1].pixels32 = l2.pixels32;
                outImage[2].pixels32 = l3.pixels32;
                datas.add(SetCounters.countObjectsSingle(set).runSingle(sourceImage, sourceLayer[0]));
            }
        };
    }

}
