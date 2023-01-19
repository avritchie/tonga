package mainPackage.protocols;

import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

public class AverageMaskThreshold extends Protocol {

    @Override
    public String getName() {
        return "Subtract average";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Layer to correct"),
            new ControlReference(LAYER, "Layer with the mask")};
    }

    @Override
    protected Processor getProcessor() {
        return new ProcessorFast("Subtracted", 2) {

            double avg;
            long val = 0;
            int cnt = 0;

            @Override
            protected void methodInit() {
                Iterate.pixels(inImage[0], (int pos) -> {
                    int c = inImage[1].pixels32[pos];
                    if (c == COL.BLACK) {
                        val += RGB.brightness(inImage[0].pixels32[pos]);
                        cnt++;
                    }
                });
                avg = val / (double) cnt;
            }

            @Override
            protected void methodCore(int p) {
                outImage[0].pixels32[p] = RGB.brightness(inImage[0].pixels32[p]) <= avg + 1 ? COL.BLACK : COL.WHITE;
            }
        };
    }
}
