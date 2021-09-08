package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ImageTracer;

public class CountCells extends Protocol {

    @Override
    public String getName() {
        return "Count cells";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(SPINNER, "Ignore objects that are smaller than (pixels)"),
            new ControlReference(TOGGLE, "Report results as individual cells")};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Cells") {
            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                CellSet set = new CellSet(new ImageTracer(sourceLayer[0], param.color[0]).trace());
                set.filterOutSmallObjects(param.spinner[0]);
                outImage[0] = set.drawToImageData();
                datas.add(param.toggle[0]
                        ? SetCounters.countCellsSingle(set).runSingle(sourceImage, sourceLayer[0])
                        : SetCounters.countCellsImage(set).runSingle(sourceImage, sourceLayer[0]));
            }
        };
    }
}
