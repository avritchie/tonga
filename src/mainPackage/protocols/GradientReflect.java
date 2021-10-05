package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import mainPackage.filters.Filters;
import mainPackage.utils.RGB;

public class GradientReflect extends Protocol {

    @Override
    public String getName() {
        return "Gradient reflection";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The original unprocessed layer"),
            new ControlReference(LAYER, "The layer with the gradient"),
            new ControlReference(SPINNER, "Radius", 5)};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Reflected", 1) {
            int[] lBrV;

            @Override
            protected void methodInit() {
                lBrV = new int[inImage[1].totalPixels()];
                for (int i = 0; i < inImage[1].totalPixels(); i++) {
                    lBrV[i] = RGB.brightness(inImage[1].pixels32[i]);
                }
            }

            @Override
            protected void methodCore(int p) {
                int r = param.spinner[0];
                int min = Integer.MAX_VALUE, minPos = -1, max = Integer.MIN_VALUE, maxPos = -1;
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        if (Math.abs(x) != Math.abs(y) || Math.abs(x) != r) {
                            if (p / sourceWidth[0] == (p + x) / sourceWidth[0]) {
                                try {
                                    int pcol = lBrV[p + x + sourceWidth[0] * y];
                                    if (pcol < min) {
                                        min = pcol;
                                        minPos = p;
                                    }
                                    if (pcol > max) {
                                        max = pcol;
                                        maxPos = p;
                                    }
                                } catch (IndexOutOfBoundsException ex) {
                                    //nothing
                                }
                            }
                        }
                    }
                }
                outImage[0].pixels32[p] = RGB.argb((int) Math.min(255, max - min * (Math.max(0, RGB.brightness(inImage[0].pixels32[minPos]) - min) / 255.)));
            }
        };
    }
}
