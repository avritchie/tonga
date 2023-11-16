package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.utils.RGB;

public class ApplyMatrix extends Protocol {

    @Override
    public String getName() {
        return "Apply matrix correction";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Layer to correct"),
            new ControlReference(LAYER, "Matrix to use for correction"),
            new ControlReference(TOGGLE, "Ratios", 0)};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Correction", 4) {

            @Override
            protected void methodCore(int pos) {
                int reduce = (inImage[1].pixels32[pos]) & 0xFF;
                int adduce = (inImage[1].pixels32[pos] >> 16) & 0xFF;
                double b;
                if (param.toggle[0]) {
                    double re = (100. / (100 + reduce));
                    double ad = (100. / (100 - adduce));
                    if (adduce == 100) {
                        ad = 1;
                    }
                    b = Math.max(0, Math.min(255, RGB.brightness(inImage[0].pixels32[pos]) * re * ad)) / 255.;
                } else {
                    b = Math.max(0, Math.min(255, RGB.brightness(inImage[0].pixels32[pos]) - reduce + adduce)) / 255.;
                }
                double h = RGB.hue(inImage[0].pixels32[pos]);
                double s = RGB.saturation(inImage[0].pixels32[pos]);
                outImage[0].pixels32[pos] = java.awt.Color.HSBtoRGB((float) h, (float) s, (float) b);
            }
        };
    }
}
