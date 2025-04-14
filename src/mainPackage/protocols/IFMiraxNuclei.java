package mainPackage.protocols;

import java.util.Arrays;
import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.counters.Counter;
import mainPackage.counters.SetCounters;
import mainPackage.counters.TableData;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;
import mainPackage.utils.HISTO;
import mainPackage.utils.IMG;
import mainPackage.utils.RGB;
import static mainPackage.utils.RGB.cut;
import mainPackage.utils.STAT;

/**
 *
 * @author aritchie
 */
public class IFMiraxNuclei extends Protocol {

    @Override
    public String getName() {
        return "Mirax IF Nuclei";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Preview image is on which layer"),
            new ControlReference(FOLDER, "Folder of the Mirax pieces"),
            new ControlReference(COMBO, new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}, "Mirax zoom level", 0),
            new ControlReference(SPINNER, "Nucleus size (pixels)", 40),
            new ControlReference(TOGGLE, "Binary staining", 1, new int[]{5, 1}),
            new ControlReference(SLIDER, "Positivity threshold (%)"),
            new ControlReference(TOGGLE, "Exclude low-density areas", 0),
            new ControlReference(TOGGLE, "Correct uneven staining", 0),
            new ControlReference(TOGGLE, "Stretch intensity values", 0),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "DAPI is on which channel", 2),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "Stain is on which channel", 0)};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorMirax(fullOutput() ? 12 : 2, "Positive Nuclei", 198, 178) {

            int dapi = param.combo[1];
            int stain = param.combo[2];
            int nsize = param.spinner[0];
            boolean bins = param.toggle[0];
            boolean elda = param.toggle[1];
            boolean matrix = param.toggle[2];
            boolean stretch = param.toggle[3];
            double thresh = param.slider[0] / 100.;
            ImageData[] matrices = new ImageData[3];
            ImageData denses;
            int[] backgrounds = new int[3];
            int[] stainstretch = new int[2];
            int dapiavg, rdapiavg;
            Protocol segm, asgm, bgcr, nprm, objc;
            int tsize, maxstain;

            @Override
            protected void methodInit() {
                //tsize = 30 - miraxPreviewLevel * 3;
                tsize = (int) (70 * Math.pow(1.33, -miraxPreviewLevel));
                segm = Protocol.load(__NucleusMaskOfSize::new);
                nprm = Protocol.load(__NucleusPrimaryMask::new);
                asgm = Protocol.load(__ObjectSegment::new);
                //bgcr = Protocol.load(_BackgroundArea::new);
                objc = Protocol.load(ObjectsCommon::new);
                Tonga.log.info("Starting general corrections for " + miraxSlide.slideName);
                Object[] crs = (Object[]) getCorrections(dapi, stain);
                matrices[dapi] = (ImageData) crs[0];
                matrices[stain] = (ImageData) crs[2];
                backgrounds[dapi] = (int) crs[1];
                backgrounds[stain] = (int) crs[3];
                if (elda) {
                    denses = (ImageData) crs[4];
                }
                dapiavg = (int) crs[5];
                rdapiavg = (int) crs[6];
                stainstretch[0] = (int) crs[7];
                stainstretch[1] = (int) crs[8];
                maxstain = (int) GEO.circleArea(nsize) * stainstretch[1] / 255;
                Tonga.log.info("Background is " + backgrounds[dapi] + " for DAPI and " + backgrounds[stain] + " for stain");
                Tonga.log.info("Average DAPI is " + dapiavg + " for corrected and " + rdapiavg + " for raw");
                Tonga.log.info("Stretch values are " + stainstretch[0] + " and " + stainstretch[1] + " for stain");
                IMG.fillArray(outImage[1].pixels32, inImage[0].width, inImage[0].height, COL.BLACK);
            }

            @Override
            protected Object[] methodCore(int xpos, int ypos, ImageData tile) {
                ImageData dapitile, staintile, rdapitile;
                dapitile = Filters.separateChannel().runSingle(tile, dapi, true);
                staintile = Filters.separateChannel().runSingle(tile, stain, true);
                rdapitile = dapitile.copy();
                //dapi masks
                dump(dapitile, xpos, ypos, 1);
                dump(staintile, xpos, ypos, 2);
                double[] dapirb = applyFromPreview(xpos, ypos, dapitile, dapi, true);
                applyFromPreview(xpos, ypos, staintile, stain, matrix);
                dump(dapitile, xpos, ypos, 3);
                dump(staintile, xpos, ypos, 4);
                //do stretching
                if (stretch) {
                    applyOperator(staintile, staintile,
                            p -> RGB.argb(cut((staintile.pixels32[p] & 0xFF), stainstretch[1] - backgrounds[stain], 0)));
                }
                //combine corrected channels to one
                applyOperator(tile, tile,
                        p -> 0xFF000000
                        | (dapitile.pixels32[p] & 0xFF) << (8 * (2 - dapi))
                        | (staintile.pixels32[p] & 0xFF) << (8 * (2 - stain)));
                applyToPreview(xpos, ypos, 1, tile);
                //segment
                ImageData segworkid = FiltersPass.heterochromatin().runSingle(dapitile, 5);
                segworkid = segm.runSilent(sourceImage, segworkid, false, false, true, nsize)[0];
                dump(segworkid, xpos, ypos, 5);
                segworkid = asgm.runSilent(sourceImage, segworkid, COL.BLACK, nsize, 1, false)[0];
                dump(segworkid, xpos, ypos, 6);
                //dump(segworkid, xpos, ypos, 7);
                segworkid = FiltersPass.edgeErode().runSingle(segworkid, COL.BLACK, Math.max(1, nsize / 20.), true, true);
                /*/bg correction
                bgcr.runSilent(sourceImage, new ImageData[]{dapitile, staintile}, 0);
                double bgval = bgcr.results.getInteger(0, 1) == 0 ? 0 : (double) bgcr.results.getVal(0, 3) / 100.;
                bgval = 0;*/
                //render and quant
                ROISet bintset = new ImageTracer(segworkid, COL.BLACK).trace();
                bintset.filterOutSmallObjectsEdgeAdjusted(nsize * 5);
                //remove dark
                bintset.quantifyStainAgainstChannel(dapitile);
                bintset.filterOutDimObjects(dapiavg / 255. * 0.5);
                bintset.quantifyStainAgainstChannel(rdapitile);
                bintset.filterOutDimObjects(rdapiavg / 255. * 0.25 / dapirb[0] / dapirb[1]);
                //quantify
                bintset.quantifyStainAgainstChannel(staintile);
                //double ststain = (stainstretch[1] - backgrounds[stain]) / 255. * thresh;
                double ststain = thresh;
                TableData tb;
                ImageData ftile = new ImageData(bins
                        ? bintset.drawStainArray(ststain, false, false)
                        : bintset.drawStainArray(true, maxstain), tile.width, tile.height);
                applyToPreview(xpos, ypos, 0, ftile);
                bintset.removeEdgeTouchers(1);
                ftile = new ImageData(bins
                        ? bintset.drawStainArray(ststain, false, false)
                        : bintset.drawStainArray(true, maxstain), tile.width, tile.height);
                dump(ftile, xpos, ypos, 8);
                if (bins) {
                    tb = SetCounters.countObjectPositiveBG(bintset, 0, ststain).runSingle(sourceImage);
                    return new Object[]{tb.getInteger(0, 1), tb.getInteger(0, 2)};
                } else {
                    if (bintset.objectsCount() == 0) {
                        return null;
                    } else {
                        tb = SetCounters.countObjectStainsBGSingle(bintset, 0).runSingle(sourceImage);
                        return new Object[]{tb, xpos, ypos};
                    }
                }
            }

            private void dump(ImageData img, int xx, int yy, int id) {
                /*try {
                    ImageIO.write(img.toStreamedImage().get8BitCopy(), "png", new File("F://test//test" + xx + "." + yy + "." + id + ".png"));
                } catch (IOException ex) {
                    Tonga.catchError(ex, "Debug failed");
                }*/
            }

            private double[] applyFromPreview(int xpos, int ypos, ImageData tile, int channel, boolean matrix) {
                double[] rbvals = new double[3];
                Iterate.pixels(tile, (int p) -> {
                    int px = p % tile.width, py = p / tile.width;
                    px = (int) (px / (double) miraxBlockRatio);
                    py = (int) (py / (double) miraxBlockRatio);
                    int mp = xpos * miraxBlockWidth + px + ((ypos * miraxBlockHeight + py) * inImage[0].width);
                    boolean include = elda ? denses.pixels32[mp] == COL.WHITE : true;
                    if (include) {
                        int brightness;
                        int bg = backgrounds[channel];
                        double re, ad;
                        if (matrix) {
                            int reduce = (matrices[channel].pixels32[mp]) & 0xFF;
                            int adduce = (matrices[channel].pixels32[mp] >> 16) & 0xFF;
                            re = (100. / (100 + reduce));
                            ad = (100. / (100 - adduce));
                            if (adduce == 100) {
                                ad = 1;
                            }
                            if (reduce != 0 || adduce != 0) {
                                rbvals[0] += re;
                                rbvals[1] += ad;
                                rbvals[2]++;
                            }
                        } else {
                            re = 1;
                            ad = 1;
                        }
                        brightness = (int) Math.max(0, Math.min(255, (RGB.brightness(tile.pixels32[p]) * re * ad) - bg));
                        tile.pixels32[p] = RGB.argb(brightness);
                    } else {
                        tile.pixels32[p] = COL.BLACK;
                    }

                });
                return new double[]{rbvals[2] == 0 ? 1 : rbvals[0] / rbvals[2], rbvals[2] == 0 ? 1 : rbvals[1] / rbvals[2]};
            }

            private void applyToPreview(int xpos, int ypos, int pid, ImageData tile) {
                int[] pacc = new int[tile.pixels32.length];
                Iterate.pixels(tile, (int p) -> {
                    int px = p % tile.width, py = p / tile.width;
                    px = (int) (px / (double) miraxBlockRatio);
                    py = (int) (py / (double) miraxBlockRatio);
                    int mp = xpos * miraxBlockWidth + px + ((ypos * miraxBlockHeight + py) * inImage[0].width);
                    outImage[pid].pixels32[mp] = COL.blendColorWeighted(tile.pixels32[p], outImage[0].pixels32[mp], 1. / (pacc[p] + 1));
                    pacc[p] = pacc[p] + 1;
                });
            }

            @Override
            protected void methodFinal() {
                Tonga.log.info("Finalizing " + miraxSlide.slideName);
                ImageData dapii = Filters.separateChannel().runSingle(outImage[1], dapi, false);
                ImageData staini = Filters.separateChannel().runSingle(outImage[1], stain, false);
                //Filters.scaleRGB().runTo(dapii, 20);
                setOutputBy(Blender.renderBlend(new ImageData[]{dapii, staini}, Blend.ADD), 1);
                addResultData(sourceImage);
                //render the histo image
                if (!bins && fullOutput()) {
                    renderIntScale(11);
                }
            }

            private void renderIntScale(int id) {
                IMG.fillArray(outImage[id].pixels32, inImage[0].width, inImage[0].height, 0x00000000);
                int count = datas.get(0).rowCount();
                double max = datas.get(0).rows.stream().mapToDouble(r -> (Double) r[6]).max().getAsDouble();
                for (int x = 0; x < outImage[id].pixels32.length / count; x++) {
                    for (int i = 0; i < count; i++) {
                        double avg = datas.get(0).getDouble(i, 5);
                        double sum = datas.get(0).getDouble(i, 6);
                        int inten = (int) (sum / max * 255);
                        //outImage[id].pixels32[i+x*count] = RGB.argb(inten);
                        outImage[id].pixels32[i] = RGB.argb(Math.min(255, (int) sum), (int) (avg * 2.55), inten);
                    }
                }
            }

            @Override
            protected Counter processorCounter() {
                return bins
                        ? new Counter("Total nuclei", new String[]{"Image", "Nuclei", "Positive", "Ratio (%)"},
                                new String[]{"The name of the image",
                                    "The total number of nuclei in the image",
                                    "The total number of positive nuclei in the image",
                                    "The ratio of positive nuclei out of all detected nuclei"}) {

                    @Override
                    protected void pixelProcessor(ImageData targetImage) {
                        row[1] = Arrays.stream(miraxTileValues).filter(i -> i != null).mapToInt(i -> (int) i[0]).sum();
                        row[2] = Arrays.stream(miraxTileValues).filter(i -> i != null).mapToInt(i -> (int) i[1]).sum();
                        row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
                    }
                }
                        : new Counter("Total nuclei", new String[]{"Image", "Object", "Tile X", "Tile Y",
                    "Area %unit2", "<html><b>Stain %</b></html>", "<html><b>Stain sum</b></html>"},
                                new String[]{"The name of the image",
                                    "The unique id number of nucleus in the image",
                                    "The X-coordinate of the tile of this nucleus in the image",
                                    "The Y-coordinate of the tile of this nucleus in the image",
                                    "The area size of this nucleus in %unit2",
                                    "The average relative intensity of this nucleus",
                                    "The total relative intensity of this nucleus"}) {

                    @Override
                    protected void pixelProcessor(ImageData targetImage) {
                        int[] counter = new int[]{0};
                        for (Object[] vals : miraxTileValues) {
                            if (vals != null) {
                                TableData td = (TableData) vals[0];
                                if (td.rowCount() > 0) {
                                    if (counter[0] > 0) {
                                        initRows();
                                    }
                                    Rows(td.rowCount(), (int index) -> {
                                        row[1] = "#" + ++counter[0];
                                        row[2] = (Integer) vals[1];
                                        row[3] = (Integer) vals[2];
                                        row[4] = scaleUnit(td.getVal(index, 4), 2, miraxScaleUnit(imageScaling));
                                        row[5] = td.getVal(index, 6);
                                        row[6] = td.getVal(index, 8);
                                    });
                                }
                            }
                        }
                    }
                };
            }

            private Object getCorrections(int dapichannel, int stainchannel) {
                ImageData dapiCorrected, dapiRaw, dapiMatrix, stainCorrected, stainRaw, stainMatrix;
                ImageData maskDense, maskBackground, maskGradient;
                ImageData temp, temp2;
                int dapiBackground, stainBackgound, dapiRawAverage, dapiCorrectedAverage;
                int[] stainAdapt;
                //separate dapi and stain channels
                dapiRaw = Filters.separateChannel().runSingle(inImage[0], dapichannel, false);
                stainRaw = Filters.separateChannel().runSingle(inImage[0], stainchannel, false);
                //make gradient mask from uncorrected dapi
                maskGradient = FiltersPass.intensityGradientSegmenting().runSingle(dapiRaw, Math.max(1, 4 - miraxPreviewLevel), 5);
                setSampleOutputBy(maskGradient, 2);
                //make dapi correction matrix and correct dapi with it
                temp = Blender.renderBlend(new ImageData[]{dapiRaw, maskGradient}, Blend.MULTIPLY);
                dapiBackground = 0;
                dapiMatrix = FiltersPass.getCorrectionMatrix().runSingle(temp, dapichannel, tsize, true, true);
                dapiCorrected = Protocol.load(ApplyMatrix::new).runSilent(sourceImage, new ImageData[]{dapiRaw, dapiMatrix}, true)[0];
                setSampleOutputBy(dapiMatrix, 7);
                setSampleOutputBy(dapiCorrected, 8);
                //measure avarage DAPI intensities
                temp = Blender.renderBlend(new ImageData[]{maskGradient, dapiCorrected}, Blend.MULTIPLY);
                dapiCorrectedAverage = (int) Filters.averageIntensity(temp, temp.pixels32, COL.BLACK);
                temp = Blender.renderBlend(new ImageData[]{maskGradient, dapiRaw}, Blend.MULTIPLY);
                dapiRawAverage = (int) Filters.averageIntensity(temp, temp.pixels32, COL.BLACK);
                //create a major tissue mask from the corrected and matrix-balanced dapi layer (major tissue areas, little detail)
                /*temp = Filters.thresholdBright().runSingle(dapiCorrected, 2);
                temp = FiltersPass.gaussSmoothing().runSingle(temp, 10 / (miraxPreviewLevel + 1), 1);
                temp = FiltersPass.edgeErode().runSingle(temp, COL.BLACK, 6 - miraxPreviewLevel, true);
                temp = FiltersSet.filterObjectSize().runSingle(temp, COL.BLACK, tsize * 30 / (miraxPreviewLevel + 1), false, 0);
                maskMajor = FiltersPass.edgeDilate().runSingle(temp, COL.BLACK, 6 - miraxPreviewLevel, true, true);
                setSampleOutputBy(maskMajor, 3);*/
                //create a mask for the background (areas immediately surrounding nuclei)
                /*
                temp = nprm.runSilent(sourceImage, dapiCorrected, tsize)[0];
                temp2 = Blender.renderBlend(maskGradient, temp, Blend.MULTIPLY);
                temp = FiltersPass.edgeDilate().runSingle(temp2, COL.BLACK, Math.max(2, 8 - miraxPreviewLevel), true);
                temp2 = (4 - miraxPreviewLevel > 0) ? FiltersPass.edgeDilate().runSingle(temp2, COL.BLACK, 4 - miraxPreviewLevel, true) : temp2;
                maskBackground = Blender.renderBlend(temp, temp2, Blend.DIFFERENCE);
                setSampleOutputBy(maskBackground, 5);*/
                temp = Blender.renderBlend(dapiCorrected, maskGradient, Blend.MULTIPLY);
                temp = Filters.scaleRGB().runSingle(temp, 70, 2);
                temp = Filters.thresholdBright().runSingle(temp, 70 + Math.min(2, Math.max(0, (miraxPreviewLevel - 1) / 2)) * 5);
                //temp2 = FiltersPass.edgeErode().runSingle(maskGradient, COL.BLACK, Math.max(1, ((4 - miraxPreviewLevel))), true, true);
                maskBackground = Blender.renderBlend(temp, maskGradient, Blend.DIFFERENCE);
                setSampleOutputBy(maskBackground, 5);
                //record average stain background using the background mask limited to nucleus area
                temp = Blender.renderBlend(stainRaw, maskBackground, Blend.MULTIPLY);
                stainBackgound = (int) Filters.averageIntensity(temp, temp.pixels32, COL.BLACK);
                //make stain correction matrix using non-nuclear areas and correct stain with it
                //must use non-nuclear areas or otherwise it will correct based on the real differences in the signal
                temp = Filters.blurConditional().runSingle(temp, COL.BLACK, 4, true);
                for (int i = 0; i < 5; i++) {
                    temp = Filters.blurConditional().runSingle(temp, COL.BLACK, 2, false);
                }
                setSampleOutputBy(temp, 6);
                temp2 = FiltersPass.adaptiveThreshold().runSingle(temp, COL.BLACK, 1.0, 10, false);
                temp = Blender.renderBlend(temp, temp2, Blend.MULTIPLY);
                temp = Filters.blurConditional().runSingle(temp, COL.BLACK, tsize, false);
                stainMatrix = FiltersPass.getCorrectionMatrixIntensity().runSingle(temp, stainBackgound, false, true);
                stainCorrected = Protocol.load(ApplyMatrix::new).runSilent(sourceImage, new ImageData[]{stainRaw, stainMatrix}, true)[0];
                setSampleOutputBy(stainMatrix, 9);
                setSampleOutputBy(stainCorrected, 10);
                //calculate stain stretching values
                stainAdapt = HISTO.getMinMaxAdapt(HISTO.getHistogram(stainCorrected.pixels32), 0.1);
                //create a dense tissue mask from the corrected and matrix-balanced dapi layer for removal of low-density areas
                temp = Filters.autoscaleWithPixelAdapt().runSingle(dapiCorrected, 50, false, true);
                temp = Filters.multiply().runSingle(temp, 200, true);
                temp = Filters.thresholdBright().runSingle(temp, 70);
                temp = FiltersSet.filterObjectSize().runSingle(temp, COL.BLACK, tsize * 30 / (miraxPreviewLevel + 1), false, 0);
                maskDense = FiltersPass.edgeDilate().runSingle(temp, COL.BLACK, 6 - miraxPreviewLevel, true, true);
                setSampleOutputBy(maskDense, 4);
                return new Object[]{dapiMatrix, dapiBackground, stainMatrix, stainBackgound, maskDense, dapiCorrectedAverage, dapiRawAverage, stainAdapt[0], stainAdapt[1]};
            }

            private int[][] getPeaks() {
                ImageData ch;
                ImageData[] ids = new ImageData[3];
                int[][] peak = new int[3][];
                for (int i = 0; i < 3; i++) {
                    peak[i] = new int[]{0, 0, 0};
                    if (matrices[i] != null) {
                        ch = Filters.separateChannel().runSingle(inImage[0], i, true);
                        ids[i] = Protocol.load(ApplyMatrix::new).runSilent(sourceImage, new ImageData[]{ch, matrices[i]}, true)[0];
                        ids[i] = Blender.renderBlend(miraxArea, ids[i], Blender.Blend.MULTIPLY);
                        int bgh = HISTO.getHighestPointIndex(HISTO.getHistogram(ids[i].pixels32), true);
                        int rad = Math.max(1, 5 - (tsize / 10));
                        int bgg = (int) FiltersPass.averageGradientIntensity(ids[i], ids[i].pixels32, rad, 25, COL.BLACK);
                        peak[i] = new int[]{bgh, bgg};
                    }
                }
                return peak;
            }
        };
    }
}
