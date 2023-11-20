package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.utils.RGB;

public class InSituRNA extends Protocol {

    @Override
    public String getName() {
        return "Measure Hematoxylin/DAB stainings";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Source layer is which one"),
            new ControlReference(SELECT, new String[]{
                "Default",
                "Default (low contrast)",
                "Sensitive",
                "Sensitive (artefacts)",
                "Adaptive (low contrast)",
                "Adaptive (low contrast and pale colours)"}, "Method for chromogen separation"),
            new ControlReference(SLIDER, "Threshold for the chromogen separation", 60),
            new ControlReference(SELECT, new String[]{"Default detailed",
                "Default rough",
                "Low contrast and bright colours",
                "Low contrast and pale colours",
                "Do not segment tissue"}, "Method for tissue segmentation"),
            new ControlReference(SLIDER, "Threshold for the tissue segmentation", 50)};
    }

    @Override
    protected Processor getProcessor() {
        int ishMethod = param.select[0];
        int tissueMethod = param.select[1];
        double thresh = param.slider[0];
        double threshTissue = param.slider[1];

        return new ProcessorFast("DAB signal", calculateIterations()) {

            double[] ishVals;
            double threshReduce;
            ImageData layer, layer2, layer3, tissueVals;

            @Override
            protected void methodInit() {
                ishVals = new double[outImage[0].totalPixels()];
                tissueVals = new ImageData(sourceWidth[0], sourceHeight[0]);
                threshReduce = 1;
            }

            @Override
            protected void methodCore(int p) {
                switch (ishMethod) {
                    case 1: {//its complicated
                        //colors
                        int r = (inImage[0].pixels32[p] >> 16) & 0xFF;
                        int g = (inImage[0].pixels32[p] >> 8) & 0xFF;
                        int b = inImage[0].pixels32[p] & 0xFF;
                        double redBlue = ((double) r) / ((double) b);
                        double redGreen = ((double) r) / ((double) g);
                        double greenBlue = ((double) g) / ((double) b);
                        double bright = (r + g + b) / (255. * 3);
                        //calcs
                        double BLUETHRESH = (100 - thresh) / 16.67 + 1; //reduce pale greyish background by INCREASING THIS!!!!!!!
                        double DARKTHRESH = thresh / 250. + 0.2; //reduce dark blue background by INCREASING THIS!!!!!!!
                        double blueFilter = Math.min(1, Math.max(0, (redBlue - 1) * BLUETHRESH) + BLUETHRESH * redBlue * (redGreen - 1));
                        double blackFilter = Math.min(0, bright - 0.2) * 3;
                        double darkBluFilter = (greenBlue - DARKTHRESH);
                        double brightBalancer = Math.min(1, (1 - bright) * 1.2);
                        double darkVal = ((1 - bright) * (redGreen - 1));
                        double brightVal = bright * 2 * ((redGreen - 1) * 3 * ((redBlue - 0.5) * 2));
                        //output
                        ishVals[p] = 255 - (brightBalancer * Math.min(255, Math.max(0, 333 * ((blueFilter * (brightVal + darkVal * darkBluFilter + blackFilter * 1.5)) - 0.1))));
                        threshReduce = 1;
                        tissueVals.pixels32[p] = RGB.argb(RGB.brightness(inImage[0].pixels32[p] & COL.RED));
                    }
                    break;
                    case 0: {//its complicated 2
                        //colors
                        //double considerer = (100 - thresh2) / 500. - 0.1; // increase this to include MORE, put to negative to include LESS;
                        int r = ((inImage[0].pixels32[p] >> 16) & 0xFF) + 1;
                        int g = ((inImage[0].pixels32[p] >> 8) & 0xFF) + 1;
                        int b = (inImage[0].pixels32[p] & 0xFF) + 1;
                        double redBlue = ((double) r) / ((double) b);
                        double redGreen = ((double) r) / ((double) g);
                        double greenBlue = ((double) g) / ((double) b);
                        double bright = (r + g + b) / (256. * 3);
                        //calcs
                        double variesFilter = Math.min(1, Math.max(0, Math.abs(bright - ((bright + redBlue + greenBlue) / 3.)) - 0.1) * 10);
                        /* bad ideas start
                        //double blackFilter = 0; //Math.min(0, bright - 0.2) * 3;
                        //double rebfFilter = Math.max(0, Math.min(1, (redBlue + redGreen - (1.9 - considerer)) / 0.6));
                        //double bluedomFilter = Math.min(0, (redBlue - (1.05 - considerer)) * 2 + ((redGreen - 1.1) * 2));
                        //double brightCutter = (bluedomFilter + gbtoomFilter) * (thresh * 1.667) / 100.;
                        //double multipScale = Math.min(1, Math.max(0, (rebfFilter + rbovrgFilter) / 2.) + brightCutter + blackFilter);
                        bad ideas end */
                        double rbovrgFilter = Math.max(0, Math.min(1, (redGreen - (0.85 + thresh / 300.)) / 0.2));
                        double gbtoomFilter = -Math.max(0, (Math.max(0, greenBlue - 0.92)) - (Math.max(0, redBlue - 1.07))) * (thresh / 10.);
                        double brightScale = Math.min(1, 1 - bright);
                        //output
                        ishVals[p] = 255 - (variesFilter * rbovrgFilter + gbtoomFilter) * brightScale * 255;
                        // ishVals[p] = 255 - (int) (Math.max(0, variesFilter * multipScale - 0.1) * 1.11111 * 255 * brightScale);
                        threshReduce = 1;
                        tissueVals.pixels32[p] = RGB.argb(RGB.brightness(inImage[0].pixels32[p] & COL.RED));
                    }
                    break;
                    default:
                        double threshMoreDots = Math.min(1, (thresh / 200 + 0.75));
                        int redChannelBright = RGB.brightness(inImage[0].pixels32[p] & COL.RED);
                        int greenChannelBright = RGB.brightness(inImage[0].pixels32[p] & COL.GREEN);
                        int blueChannelBright = RGB.brightness(inImage[0].pixels32[p] & COL.BLUE);
                        int rgValue = (int) Math.max(0, Math.min(255, threshMoreDots * (greenChannelBright + redChannelBright)));
                        int bgValue = (int) Math.max(0, Math.min(255, threshMoreDots * (blueChannelBright + greenChannelBright)));
                        int rbValue = (int) Math.max(0, Math.min(255, threshMoreDots * (blueChannelBright + redChannelBright)));
                        tissueVals.pixels32[p] = RGB.argb(redChannelBright);
                        switch (ishMethod) {
                            case 2: //goodcontrast
                            case 3: //graystains
                                int bValue = Math.max(0, Math.min(255, blueChannelBright - redChannelBright));
                                int redGreen = Math.max(0, Math.min(255, redChannelBright - greenChannelBright));
                                if (ishMethod == 3) {
                                    redGreen = RGB.levels(255 - redGreen, (int) 255, (int) 225) & 0xFF;
                                } else {
                                    redGreen = 0;
                                }
                                ishVals[p] = (bValue * 2.5 + redGreen + blueChannelBright + greenChannelBright) / 2;
                                threshReduce = thresh / 50.0;
                                break;
                            case 5: //lowcontrast
                                int stains = Math.max(0, Math.min(255, Math.abs(bgValue - rbValue)));
                                stains = RGB.levels(stains, (int) 50, (int) 0) & 0xFF;
                                bgValue = RGB.levels(bgValue, (int) 255, (int) 127) & 0xFF;
                                ishVals[p] = (int) Math.max(0, (Math.min(255, stains + bgValue)));
                                threshReduce = (2 - thresh / 100.);
                                break;
                            case 4: //shitstains
                                int excessiveBlue = (int) Math.max(0, (Math.min(255, (255 - bgValue) + rgValue)));
                                int shitStains = (int) Math.max(0, (Math.min(255, (255 - rgValue) + bgValue)));
                                int combinedVals = (int) Math.max(0, (Math.min(255, (255 - excessiveBlue) + (255 - shitStains) + rgValue)));
                                ishVals[p] = RGB.levels(combinedVals, (int) 255, (int) 127) & 0xFF;
                                threshReduce = Math.max(1, (thresh / 75.));
                                break;
                        }
                        break;
                }
            }

            @Override
            protected void methodFinal() {
                switch (tissueMethod) {
                    case 0:
                    case 1: {//goodcontrast
                        layer = Filters.autoscaleWithAdapt().runSingle(tissueVals, 5);
                        layer = Filters.cutFilter().runSingle(layer, new Object[]{0, 225});
                        layer = Filters.invert().runSingle(layer);
                        layer = Filters.multiply().runSingle(layer, 800.);
                        layer = Filters.box().runSingle(layer, 2.);
                        layer = Filters.thresholdBright().runSingle(layer, (int) threshTissue);
                        layer = Filters.gaussApprox().runSingle(layer, 3., true);
                        layer = Filters.thresholdBright().runSingle(layer, 33);
                        layer = FiltersSet.filterObjectSize().runSingle(layer, COL.BLACK, 500, false, 0);
                        if (tissueMethod == 0) {
                            layer2 = Filters.invert().runSingle(inImage[0]);
                            applyOperator(layer2, layer2, p -> layer2.pixels32[p] & 0xFFFF0000);
                            layer2 = Filters.autoscaleWithAdapt().runSingle(layer2, 2.0);
                            layer2 = Filters.cutFilter().runSingle(layer2, new Object[]{75, 255});
                            layer2 = FiltersPass.dogSementing().runSingle(layer2, (int) threshTissue, 100, 0, true);
                            layer2 = Filters.thresholdBiol().runSingle(layer2, 99);
                            layer2 = Filters.gaussApprox().runSingle(layer2, 2., true);
                            layer2 = Filters.thresholdBright().runSingle(layer2, 60);
                            layer2 = FiltersSet.filterObjectSize().runSingle(layer2, COL.BLACK, 100, false, 0);
                            layer = Blender.renderBlend(layer, layer2, Blend.MULTIPLY);
                        }
                    }
                    break;
                    case 2: {//lowcontrast
                        layer = Filters.autoscaleWithAdapt().runSingle(inImage[0], 1);
                        layer = Filters.bwLuminance().runSingle(layer);
                        layer2 = Filters.box().runSingle(layer, 1.);
                        layer = Filters.autoscaleWithAdapt().runSingle(layer2, 10);
                        layer = Filters.maximumDiffEdge().runSingle(layer, 0, 1, false, 0);
                        layer = Filters.thresholdBright().runSingle(layer, (int) (8 + (threshTissue / 25.)));
                        layer3 = Filters.invert().runSingle(layer2);
                        layer3 = Filters.thresholdBright().runSingle(layer3, (int) (50 + (threshTissue / 5.)));
                        layer = Blender.renderBlend(layer, layer3);
                        layer = Filters.gaussApprox().runSingle(layer, 6., true);
                        layer = Filters.thresholdBright().runSingle(layer, (int) (threshTissue / 5.));
                        layer = FiltersSet.filterObjectSize().runSingle(layer, COL.BLACK, 500, true, 500);
                    }
                    break;
                    case 3: {//shitstains
                        layer = Filters.autoscaleWithAdapt().runSingle(inImage[0], 1);
                        layer3 = Filters.bwLuminance().runSingle(layer);
                        layer2 = Filters.bwSaturation().runSingle(layer);
                        layer3 = Filters.gaussApprox().runSingle(layer3, 3., true);
                        layer2 = Filters.gaussApprox().runSingle(layer2, 3., true);
                        layer3 = Filters.invert().runSingle(layer3);
                        layer3 = Filters.autoscaleWithAdapt().runSingle(layer3, 5);
                        layer2 = Filters.autoscaleWithAdapt().runSingle(layer2, 5);
                        layer3 = Filters.cutFilter().runSingle(layer3, new Object[]{50, 255});
                        layer2 = Filters.cutFilter().runSingle(layer2, new Object[]{50, 255});
                        layer = Blender.renderBlend(layer3, layer2);
                        layer = Filters.thresholdBright().runSingle(layer, 15);
                        layer = Filters.gaussApprox().runSingle(layer, 3., true);
                        layer = Filters.thresholdBright().runSingle(layer, (int) (threshTissue / 1.25));
                        layer = FiltersSet.filterObjectSize().runSingle(layer, COL.BLACK, 500, true, 500);
                    }
                    break;
                }
                for (int y = 0; y < sourceHeight[0]; y++) {
                    for (int x = 0; x < sourceWidth[0]; x++) {
                        int p = (y * sourceWidth[0] + x);
                        int ishVal = 255 - Math.min(255, (int) (threshReduce * ishVals[p]));
                        int ishARGB = (255 << 16 | (255 - ishVal) << 8 | (255 - ishVal) | 0xFF << 24);
                        boolean eval = (tissueMethod == 4 || (layer.pixels32[p] & 0xFF) == 0xFF);
                        outImage[0].pixels32[p] = eval ? ishARGB : COL.BLACK;
                    }
                }
                addResultData(Counters.countRBStain().runSingle(sourceImage, outImage[0]));
            }
        };
    }

    private int calculateIterations() {
        switch (param.select[1]) {
            case 0:
                return 59;
            case 1:
                return 14;
            case 2:
                return 19;
            case 3:
                return 23;
            default:
                return 2;
        }
    }
}
