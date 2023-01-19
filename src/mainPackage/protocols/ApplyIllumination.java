package mainPackage.protocols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;
import mainPackage.utils.STAT;

public class ApplyIllumination extends Protocol {

    @Override
    public String getName() {
        return "Apply manual correction";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Layer to correct"),
            new ControlReference(LAYER, "Map to use for correction"),
            new ControlReference(TOGGLE, "Map is in grayscale", 1, new int[]{3, 1}),
            new ControlReference(TOGGLE, "Balance with step size", 1),
            new ControlReference(TOGGLE, "Exclude a colour", 0, new int[]{5, 1}),
            new ControlReference(COLOUR, "Colour to exclude", new int[]{0}),
            new ControlReference(TOGGLE, "Balance map values on a binary mask", 0, new int[]{7, 1}),
            new ControlReference(LAYER, "Binary mask to use for balancing")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("Correction", 4) {
            int[] ovals, fvals;
            int[][] omvals;
            double avga, avgr, avgg, avgb;
            STAT as, rs, gs, bs;
            boolean grayscale, exclude, balance, mapcorrect;
            long avgm = 0, avgs = 0;
            int cntm = 0, cnts = 0;
            double mrat;

            @Override
            protected void methodInit() {
                grayscale = param.toggle[0];
                balance = param.toggle[1];
                exclude = param.toggle[2];
                mapcorrect = param.toggle[3];
            }

            @Override
            protected void pixelProcessor() {
                ovals = inImage[1].pixels32;
                if (mapcorrect) {
                    List<Integer> vin = new ArrayList<>();
                    if (exclude) {
                        for (int p = 0; p < inImage[1].pixels32.length; p++) {
                            if (inImage[1].pixels32[p] != param.colorARGB[0] && inImage[2].pixels32[p] != COL.BLACK) {
                                vin.add(inImage[1].pixels32[p]);
                            }
                        }
                        fvals = vin.stream().mapToInt(i -> i).toArray();
                    } else {
                        for (int p = 0; p < inImage[1].pixels32.length; p++) {
                            if (inImage[2].pixels32[p] != COL.BLACK) {
                                vin.add(inImage[1].pixels32[p]);
                            }
                        }
                    }
                    fvals = vin.stream().mapToInt(i -> i).toArray();
                } else if (exclude) {
                    fvals = Arrays.stream(ovals).filter(pc -> pc != param.colorARGB[0]).toArray();
                } else {
                    fvals = ovals;
                }
                bs = new STAT(Arrays.stream(fvals).map(i -> i & 0xFF).toArray());
                avgb = (int) bs.getMean();
                if (!grayscale) {
                    as = new STAT(Arrays.stream(fvals).map(i -> i >> 24 & 0xFF).toArray());
                    rs = new STAT(Arrays.stream(fvals).map(i -> i >> 16 & 0xFF).toArray());
                    gs = new STAT(Arrays.stream(fvals).map(i -> i >> 8 & 0xFF).toArray());
                    avga = (int) as.getMean();
                    avgr = (int) rs.getMean();
                    avgg = (int) gs.getMean();
                    applyOperator(inImage[0], outImage[0], p -> exclude && ovals[p] == param.colorARGB[0] ? param.colorARGB[0] : RGB.argb(
                            (int) Math.min(0xFF, Math.max(0x00, (inImage[0].pixels32[p] >> 16 & 0xFF) - (ovals[p] >> 16 & 0xFF) + avgr)),
                            (int) Math.min(0xFF, Math.max(0x00, (inImage[0].pixels32[p] >> 8 & 0xFF) - (ovals[p] >> 8 & 0xFF) + avgg)),
                            (int) Math.min(0xFF, Math.max(0x00, (inImage[0].pixels32[p] & 0xFF) - (ovals[p] & 0xFF) + avgb)),
                            (int) Math.min(0xFF, Math.max(0x00, (inImage[0].pixels32[p] >> 24 & 0xFF) - (ovals[p] >> 24 & 0xFF) + avga))));
                } else {
                    if (balance) {
                        if (exclude) {
                            Iterate.pixels(inImage[0], (int p) -> {
                                if (ovals[p] != param.colorARGB[0]) {
                                    avgs += RGB.brightness(inImage[0].pixels32[p]);
                                    avgm += ovals[p] & 0xFF;
                                    cnts++;
                                    cntm++;
                                }
                            });
                        } else {
                            Iterate.pixels(inImage[0], (int p) -> {
                                avgs += RGB.brightness(inImage[0].pixels32[p]);
                                avgm += ovals[p] & 0xFF;
                                cnts++;
                                cntm++;
                            });
                        }
                        double avgsf = avgs / (double) cnts;
                        double avgmf = avgm / (double) cntm;
                        mrat = Math.min(1, avgmf / avgsf);
                        //mrat = Math.max(0, 1 - Math.sqrt(1 / (avgmf - 1)));
                    } else {
                        mrat = 1;
                    }
                    Iterate.pixels(inImage[0], (int p) -> {
                        double fact = (ovals[p] & 0xFF) == 0 ? 1 : ((avgb / (ovals[p] & 0xFF)) - 1) * mrat + 1;
                        outImage[0].pixels32[p] = exclude && ovals[p] == param.colorARGB[0] ? param.colorARGB[0] : RGB.multiplyColor(inImage[0].pixels32[p], fact) | 0xFF000000;
                    });
                }
            }
        };
    }
}
