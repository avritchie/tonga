package mainPackage.protocols;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaLayer;
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.FilterCrisps;
import mainPackage.filters.Filters;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class NucleusEdUCounter extends Protocol {

    @Override
    public String getName() {
        return "Nucleus/EdU counter";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Source staining (e.g. DAPI) is on which layer"),
            new ControlReference(SLIDER, "Source staining strength multiplier", 50),
            new ControlReference(TOGGLE, "Compare the staining against another channel", 1, new int[]{3, 1, 4, 1}),
            new ControlReference(LAYER, "Target staining (e.g. EdU) is on which layer"),
            new ControlReference(SLIDER, "Target staining strength multiplier", 50),
            new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 100)};
    }

    @Override
    protected Processor getProcessor() {
        int sourceVal = param.slider[0];
        int targetVal = param.slider[0];
        boolean twoChannels = param.toggle[0];
        int limit = param.spinner[0];

        return new ProcessorFast(twoChannels ? 2 : 1, new String[]{"DAPI", "EdU"}) {
            ImageData layerDoG, layerThrs, layerDoG2, layerGauss, layerF, layerB;

            @Override
            protected void methodFinal() {
                initTableData(new String[]{"Image", "Cells", "EdU", "Ratio"});
                int[] total = new int[2];
                for (int i = 0; i < (twoChannels ? 2 : 1); i++) {
                    layerDoG = Filters.dog().runSingle(inImage[i], 2, 20, false);
                    ///
                    //layerDoG = Filters.multiply().runSingle(layerDoG, i == 0 ? 50 : 25);
                    layerDoG = Filters.cutFilter().runSingle(layerDoG, new Object[]{(int) 5,
                        (i == 0) ? (int) (125 + (50 - sourceVal)) : (int) (100 + (50 - sourceVal))});
                    ///
                    layerThrs = Filters.crapCleaner().runSingle(layerDoG, 6);
                    layerThrs = Filters.thresholdBright().runSingle(layerThrs, 1);
                    ///
                    layerGauss = Filters.multiply().runSingle(layerDoG, 200.);
                    layerGauss = Filters.gaussApprox().runSingle(layerGauss, 25.0);
                    layerGauss = Filters.thresholdBright().runSingle(layerGauss, 3 - i);
                    ///
                    layerDoG2 = Filters.dog().runSingle(inImage[i], 3, 6, true);
                    layerDoG2 = Filters.autoscale().runSingle(layerDoG2);
                    layerDoG2 = Filters.thresholdBright().runSingle(layerDoG2, (int) ((i == 0) ? (1 + (sourceVal / 5.0)) : (1 + (targetVal / 5.0))));
                    ///
                    PixelReader readA = layerThrs.toFXImage().getPixelReader();
                    PixelReader readB = layerGauss.toFXImage().getPixelReader();
                    PixelReader readC = layerDoG2.toFXImage().getPixelReader();
                    WritableImage mask = new WritableImage(layerDoG.width, layerDoG.height);
                    PixelWriter write = mask.getPixelWriter();
                    int w = layerDoG.width, h = layerDoG.height;
                    for (int xx = 0; xx < w; xx++) {
                        for (int yy = 0; yy < h; yy++) {
                            write.setColor(xx, yy, readC.getColor(xx, yy).equals(Color.WHITE)
                                    || readB.getColor(xx, yy).equals(Color.BLACK)
                                    ? Color.BLACK : readA.getColor(xx, yy));
                        }
                    }
                    /// BLENDING TO REMOVE CRISPINESS ETC ACCURACY INCREASE 20%
                    layerF = Filters.distanceTransform().runSingle(new TongaLayer(mask, null));
                    layerF = Filters.gaussApprox().runSingle(layerF, 4.0);
                    layerF = Filters.thresholdBright().runSingle(layerF, 1);
                    /// 
                    layerF = ConnectEdges.run().runSingle(layerF);
                    layerF = FilterCrisps.run().runSingle(layerF);
                    ROISet set = new ImageTracer(layerF, Color.BLACK).trace();
                    set.filterOutSmallObjects(limit);
                    CellSet cells = new CellSet(set);
                    setOutputBy(set, i);
                    total[i] = cells.totalCellCount();
                }
                newResultRow(total[0], total[1], total[1] / (double) total[0]);
            }
        };
    }
}
