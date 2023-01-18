package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.FiltersSet;
import static mainPackage.protocols.Processor.applyOperator;
import mainPackage.utils.COL;

public class ObjectEdges extends Protocol {

    @Override
    public String getName() {
        return "Edges";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(LAYER, "Original image is on which layer"),
            new ControlReference(COLOUR, "Positive colour", COL.GREEN),
            new ControlReference(COLOUR, "Negative colour", COL.RED)};
    }

    @Override
    protected Processor getProcessor() {
        int colPos = param.colorARGB[0];
        int colNeg = param.colorARGB[1];

        return new ProcessorFast(1, "Edges", 2) {

            ImageData temp;

            @Override
            protected void pixelProcessor() {
                temp = FiltersSet.getEdgeMask().runSingle(inImage[0], COL.BLACK);
                applyOperator(temp, temp, p -> temp.pixels32[p] == 0xFFFF0000
                        ? inImage[0].pixels32[p] == COL.WHITE ? colPos : colNeg
                        : inImage[1].pixels32[p]);
                setOutputBy(temp);
            }
        };
    }
}
