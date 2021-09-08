package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.utils.RGB;

public class SubtractLayers extends Protocol {

    @Override
    public String getName() {
        return "Subtract layers";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "This is the layer to substract values to"),
            new ControlReference(LAYER, "This is the layer to substract values of")};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Subtracted", 1) {
            @Override
            protected void methodCore(int p) {
                int c1 = inImage[0].pixels32[p];
                int c2 = inImage[1].pixels32[p];
                int r1 = (c1 >> 16) & 0xFF, r2 = (c2 >> 16) & 0xFF;
                int g1 = (c1 >> 8) & 0xFF, g2 = (c2 >> 8) & 0xFF;
                int b1 = c1 & 0xFF, b2 = c2 & 0xFF;
                int rf = Math.max(0, Math.min(255, r1 - r2));
                int gf = Math.max(0, Math.min(255, g1 - g2));
                int bf = Math.max(0, Math.min(255, b1 - b2));
                outImage[0].pixels32[p] = RGB.argb(rf, gf, bf);
            }
        };
    }
}
