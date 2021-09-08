package mainPackage.protocols;

import java.awt.Color;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.morphology.ImageTracer;
import mainPackage.utils.DRAW;
import mainPackage.utils.RGB;

public class AreaAroundObject extends Protocol {

    @Override
    public String getName() {
        return "Measure cell surroundings";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Nuclei in which layer"),
            new ControlReference(LAYER, "Areas in which layer"),
            new ControlReference(LAYER, "Cell bodies in which layer"),
            new ControlReference(SPINNER, "Radius of the area to inspect (pixels)", 100),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0})};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast("Areas", 6) {
            ImageData nucleiLayer, areaLayer, bodyLayer;
            List<Point> centers;
            int bcol = param.colorARGB[0];
            int radius = param.spinner[0];

            @Override
            protected void methodCore(int p) {
            }

            @Override
            protected void methodFinal() {
                initTableData(new String[]{"Image", "Cell", "Area"});
                outImage[0].fill(COL.BLACK);
                nucleiLayer = Filters.thresholdBright().runSingle(sourceLayer[0], 100);
                areaLayer = Filters.thresholdBright().runSingle(sourceLayer[1], 100);
                bodyLayer = Filters.thresholdBright().runSingle(sourceLayer[2], 100);
                centers = new ImageTracer(nucleiLayer, param.color[0]).trace().getCenterPoints();
                //
                int[][] hitsMax = objectAreaTracing();
                for (int cell = 1; cell <= centers.size(); cell++) {
                    data.newRow(sourceImage.imageName, Integer.toString(cell),
                            String.format("%.3f", ((double) hitsMax[cell][0] / (double) hitsMax[cell][1] * 100)) + "%");
                }
            }

            public int[][] objectAreaTracing() {
                Tonga.iteration();
                int[][] resultArray = new int[centers.size() + 1][2]; //[soluid][{hits,max}];
                for (int cell = 1; cell <= centers.size(); cell++) {
                    Point p = centers.get(cell - 1);
                    double colSeed = new Random(p.hashCode()).nextDouble();
                    int hits = 0, max = 0;
                    for (int x = p.x - radius; x < p.x + radius; x++) {
                        for (int y = p.y - radius; y < p.y + radius; y++) {
                            if (GEO.getDist(p.x, p.y, x, y) <= radius) {
                                try {
                                    if (bodyLayer.pixels32[pos(x, y)] == bcol) {
                                        int oc = outImage[0].pixels32[pos(x, y)];
                                        int nc;
                                        if (areaLayer.pixels32[pos(x, y)] != bcol) {
                                            hits++;
                                            nc = Color.HSBtoRGB((float) colSeed, (float) 0.75, (float) 1);
                                        } else {
                                            nc = Color.HSBtoRGB((float) colSeed, (float) 0.5, (float) 0.5);
                                        }
                                        if (oc == COL.BLACK) {
                                            outImage[0].pixels32[pos(x, y)] = nc;
                                        } else {
                                            outImage[0].pixels32[pos(x, y)] = COL.blendColor(nc, oc);
                                        }
                                        max++;
                                    }
                                } catch (IndexOutOfBoundsException ex) {
                                }
                            }
                        }
                    }
                    resultArray[cell] = new int[]{hits, max};
                    Tonga.loader().appendProgress(centers.size());
                }
                resultArray = Arrays.stream(resultArray).filter(a -> a != null).toArray(int[][]::new);
                for (int cell = 0; cell < centers.size(); cell++) {
                    DRAW.redDot(outImage[0], centers.get(cell).x, centers.get(cell).y);
                }
                return resultArray;
            }
        };
    }
}
