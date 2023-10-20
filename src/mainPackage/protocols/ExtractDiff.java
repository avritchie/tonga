package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;

/**
 *
 * @author aritchie
 */
public class ExtractDiff extends Protocol {

    @Override
    public String getName() {
        return "Difference";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "This is the layer to substract values to"),
            new ControlReference(LAYER, "This is the layer to substract values of")};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Difference", 1) {
            @Override
            protected void methodCore(int p) {
                int c1 = inImage[0].pixels32[p];
                int c2 = inImage[1].pixels32[p];
                outImage[0].pixels32[p] = (c1==c2) ? 0x00000000 : c2;
            }
        };
    }
}
