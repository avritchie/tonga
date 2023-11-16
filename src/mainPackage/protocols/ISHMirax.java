package mainPackage.protocols;

import java.util.Arrays;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.Counter;
import mainPackage.counters.Counters;
import mainPackage.counters.TableData;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.utils.STAT;
import ome.units.UNITS;
import ome.units.quantity.Length;

public class ISHMirax extends Protocol {

    @Override
    public String getName() {
        return "Mirax ISH";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Preview image is on which layer"),
            new ControlReference(FOLDER, "Folder of the Mirax pieces"),
            new ControlReference(COMBO, new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}, "Mirax zoom level", 2),
            new ControlReference(SELECT, new String[]{
                "Default",
                "Default (low contrast)",
                "Sensitive",
                "Sensitive (artefacts)",
                "Adaptive (low contrast)",
                "Adaptive (low contrast and pale colours)"}, "Method for chromogen separation"),
            new ControlReference(SLIDER, "Threshold for the chromogen separation", 60),
            new ControlReference(SLIDER, "Threshold for the tissue", 50)};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorMirax("DAB signal", 1, 14) {

            Protocol ish;
            int ishMethod = param.select[0];
            double thresh = param.slider[0];
            double threshTissue = param.slider[1];

            @Override
            protected void methodInit() {
                ish = Protocol.load(InSituRNA::new);
                IMG.fillArray(outImage[0].pixels32, inImage[0].width, inImage[0].height, COL.BLACK);
            }

            @Override
            protected Object[] methodCore(int xpos, int ypos, ImageData tile) {
                ImageData signal = ish.runSilent(sourceImage, tile, ishMethod, thresh, 1, threshTissue)[0];
                applyToPreview(xpos, ypos, 0, signal);
                TableData tb = (Counters.countRBStain().runSingle(sourceImage, signal));
                return new Object[]{tb.getDouble(0, 4), tb.getInteger(0, 3), xpos, ypos};
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
                addResultData(sourceImage);
            }

            @Override
            protected Counter processorCounter() {
                return new Counter("Count ISH signal", new String[]{"Image", "<html><b>Stain ‰</b></html>", "Tissue %unit2", "Stain raw"},
                        new String[]{"The name of the image",
                            "How much staining intensity there is in the tissue area, in promilles",
                            "How big is the tissue area in the image in %unit2",
                            "The raw value of the total staining intensity detected in the image"}) {

                    @Override
                    protected void pixelProcessor(ImageData targetImage) {
                        double totStain = Arrays.stream(miraxTileValues).filter(i -> i != null).mapToDouble(d -> (Double) d[0]).sum();
                        int totTis = Arrays.stream(miraxTileValues).filter(i -> i != null).mapToInt(i -> (Integer) i[1]).sum();
                        row[1] = STAT.decToProm(totStain / totTis);
                        row[2] = scaleUnit(totTis, 2, miraxScaleUnit(imageScaling));
                        row[3] = totStain;
                    }
                };
            }
        };
    }
}
