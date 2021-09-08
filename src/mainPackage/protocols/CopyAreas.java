package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.COLOUR;

public class CopyAreas extends Protocol {

    @Override
    public String getName() {
        return "Copy areas";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Get mask from which layer"),
            new ControlReference(LAYER, "Merge mask into which layer"),
            new ControlReference(COLOUR, "The mask is masked with which colour"),
            new ControlReference(COLOUR, "The merged mask should be which colour")};
    }

    @Override
    protected Processor getProcessor() {
        int sourceCol = param.colorARGB[0];
        int finalCol = param.colorARGB[1];

        return new ProcessorFast("Merged", 1) {
            @Override
            protected void methodCore(int pos) {
                int thisCol = inImage[0].pixels32[pos];
                outImage[0].pixels32[pos] = thisCol == sourceCol ? finalCol : inImage[1].pixels32[pos];
            }
        };
    }
}
