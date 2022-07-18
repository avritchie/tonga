package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;

public class NucleusEduCounterShitty extends Protocol {

    @Override
    public String getName() {
        return "Nucleus/EdU counter (crappy version)";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Source staining (e.g. DAPI) is on which layer"),
            new ControlReference(LAYER, "Target staining (e.g. EdU) is on which layer")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("DAPI/EdU") {

            ImageData dapiLayer, eduLayer;

            @Override
            protected void methodFinal() {
                initTableData(new String[]{"Image", "Cells", "EdU", "Ratio"});
                dapiLayer = Filters.maximumDiffEdge().runSingle(inImage[0], 0, 1, false, 0);
                dapiLayer = Filters.crapCleaner().runSingle(dapiLayer, 3);
                dapiLayer = Filters.gamma().runSingle(dapiLayer, 0.5);
                dapiLayer = Filters.thresholdBright().runSingle(dapiLayer, 10);
                dapiLayer = Filters.connectEdges().runSingle(dapiLayer);
                dapiLayer = FiltersPass.fillInnerAreas().runSingle(dapiLayer, false);
                eduLayer = Filters.gamma().runSingle(inImage[1], 0.6);
                eduLayer = Filters.thresholdBright().runSingle(eduLayer, 30);
                eduLayer = FiltersPass.edgeErode().runSingle(eduLayer, COL.BLACK, 2, false, true);
                eduLayer = FiltersPass.fillInnerAreas().runSingle(eduLayer, false);
                CellSet imageDAPI = new CellSet(new ImageTracer(dapiLayer, COL.BLACK).trace());
                imageDAPI.filterOutSmallObjects(500);
                CellSet imageEdU = new CellSet(new ImageTracer(eduLayer, COL.BLACK).trace());
                imageEdU.filterOutSmallObjects(500);
                setOutputBy(imageDAPI, 0);
                setOutputBy(imageEdU, 1);
                int totalDAPI = imageDAPI.totalCellCount();
                int totalEdU = imageEdU.totalCellCount();
                newResultRow(totalDAPI, totalEdU, totalEdU / (double) totalDAPI);
            }
        };
    }
}
