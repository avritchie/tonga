package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.RGB;

public class NucleusMaskShitty extends Protocol {

    @Override
    public String getName() {
        return "Nucleus mask creation (crappy version)";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Source staining (e.g. DAPI) is on which layer"),
            new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("DAPI") {

            ImageData layerOne, layerTwo, layerComb;
            int[] layerDoG1, layerDoG2, layerDoG3, layerDoG4, layerInts;

            @Override
            protected void methodInit() {
                initTableData(new String[]{"Image", "Objects", "Cells"});
            }

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                layerInts = new int[inImage[0].totalPixels()];
                layerComb = new ImageData(sourceWidth[0], sourceHeight[0]);
                // original and gamma-enchanced version of the image
                layerOne = new ImageData(sourceLayer[0]);
                layerTwo = Filters.gamma().runSingle(layerOne, 0.5);
                // process with different difference of gaussians -settings
                layerDoG1 = Filters.dog().runSingle(layerOne, 3, 30).pixels32;
                layerDoG2 = Filters.dog().runSingle(layerOne, 5, 10).pixels32;
                layerDoG3 = Filters.dog().runSingle(layerTwo, 3, 30).pixels32;
                layerDoG4 = Filters.dog().runSingle(layerTwo, 5, 10).pixels32;
                // combine different gaussians into one excluding noise background by substracting the darkest values
                Iterate.pixels(inImage[0], (int p) -> {
                    int val = Math.max(0, Math.min(255, (int) (((layerDoG1[p] & 0xFF)
                            + (layerDoG2[p] & 0xFF)
                            + (layerDoG3[p] & 0xFF)
                            + (layerDoG4[p] & 0xFF)) * 1.05) - 10));
                    layerComb.pixels32[p] = RGB.argb(val);
                });
                // free memory
                layerDoG1 = null;
                layerDoG2 = null;
                layerDoG3 = null;
                layerDoG4 = null;
                // gamma-enchanced version of the dog
                layerOne = Filters.gamma().runSingle(layerComb, 0.5);
                // local and global thresholdings
                layerTwo = Filters.localThreshold().runSingle(layerOne, 10, 5);
                layerOne = Filters.thresholdBright().runSingle(layerOne, 20);
                // clean background and connect holes on the local threshold
                layerTwo = Filters.invert().runSingle(layerTwo);
                layerTwo = FiltersPass.edgeDilate().runSingle(layerTwo, COL.BLACK, 1, false);
                layerTwo = ConnectEdges.run().runSingle(layerTwo);
                layerTwo = FiltersPass.filterObjectDimension().runSingle(layerTwo, COL.BLACK, 20, false, 0);
                // combine thresholdings into one
                Iterate.pixels(inImage[0], (int p) -> {
                    layerInts[p] = layerOne.pixels32[p] == COL.BLACK || layerTwo.pixels32[p] == COL.WHITE ? COL.BLACK : COL.WHITE;
                });
                // trace objects
                layerOne = new ImageData(layerInts, sourceWidth[0], sourceHeight[0]);
                ROISet set = new ImageTracer(layerOne, Color.BLACK).trace();
                set.filterOutSmallObjects(param.spinner[0]);
                CellSet cells = new CellSet(set);
                setOutputBy(set);
                newResultRow(cells.objectsCount(), cells.totalCellCount());
            }
        };
    }
}
