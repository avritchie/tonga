package mainPackage.protocols;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import mainPackage.Blender;
import mainPackage.Tonga;
import mainPackage.utils.COL;
import mainPackage.MappedImage;
import mainPackage.utils.IMG;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;

public class OligoDendroBranch extends Protocol {

    @Override
    public String getName() {
        return "Oligodendrocyte branching";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "DAPI/Hoechst on which layer"),
            new ControlReference(LAYER, "MBP on which layer"),
            new ControlReference(LAYER, "HSP60 on which layer"),
            new ControlReference(LAYER, "O4 on which layer"),
            new ControlReference(SPINNER, "Radius of the area to inspect (pixels)"),
            new ControlReference(TOGGLE, "Only consider cell bodies with one nucleus")};
    }

    @Override
    protected Processor getProcessor() {
        int radius = param.spinner[0];
        boolean onlyOne = param.toggle[0];
        Color sCol = Color.BLACK;

        return new ProcessorOld(5, new String[]{"Nuclei", "Cell bodies", "Branches", "Branch Mapping", "MBP Mapping"}) {

            ImageData nucleiLayer, bodyLayer, areaLayer, nucleusSeparator;

            @Override
            protected void methodCore(int x, int y) {
                outWriter[3].setColor(x, y, Color.BLACK);
                outWriter[4].setColor(x, y, Color.BLACK);
            }

            @Override
            protected void methodFinal() {
                initTableData(new String[]{"Image", "Cell", "Area", "Intensity"});
                // NUKLEI
                nucleiLayer = Filters.multiplyColBright().runSingle(sourceLayer[0], 200.);
                nucleusSeparator = new NucleusEdUCounter().runSilent(sourceImage, nucleiLayer, new Object[]{null, null, false, null, 250})[0];
                nucleiLayer = Filters.box().runSingle(nucleusSeparator, 3.);
                nucleiLayer = Filters.thresholdBright().runSingle(nucleiLayer, 50);
                ROISet nuclei = new ImageTracer(nucleiLayer, sCol).trace();
                List<Point> centers = nuclei.getCenterPoints();
                Tonga.log.debug("There are {} nuclei", nuclei.objectsCount());
                IMG.copyImage(nucleiLayer, outImage[0]);
                // BADI
                bodyLayer = Blender.renderBlend(ImageData.convertToImageData(new MappedImage[]{
                    sourceLayer[0].layerImage,
                    sourceLayer[1].layerImage,
                    sourceLayer[2].layerImage,
                    sourceLayer[3].layerImage}));
                bodyLayer = Filters.multiplyColBright().runSingle(bodyLayer, 280.);
                bodyLayer = Filters.thresholdBright().runSingle(bodyLayer, 15);
                bodyLayer = FiltersPass.edgeErode().runSingle(bodyLayer, COL.BLACK, 1, false, true);
                bodyLayer = FiltersPass.filterObjectSize().runSingle(bodyLayer, COL.BLACK, 500, false, 0);
                bodyLayer = ConnectEdges.run().runSingle(bodyLayer);
                bodyLayer = Filters.gaussApprox().runSingle(bodyLayer, 4.0);
                bodyLayer = Filters.thresholdBright().runSingle(bodyLayer, 30);
                bodyLayer = Filters.distanceTransform().runSingle(bodyLayer);
                bodyLayer = Filters.thresholdBright().runSingle(bodyLayer, 4);
                bodyLayer = FiltersPass.filterObjectSize().runSingle(bodyLayer, COL.BLACK, 500, false, 0);
                bodyLayer = Blender.renderBlend(nucleusSeparator, bodyLayer);
                bodyLayer = FiltersPass.edgeDilate().runSingle(bodyLayer, COL.BLACK, 5, false);
                bodyLayer = Filters.box().runSingle(bodyLayer, 2.);
                bodyLayer = Filters.thresholdBright().runSingle(bodyLayer, 50);
                bodyLayer = FiltersPass.fillInnerAreas().runSingle(bodyLayer, false);
                IMG.copyImage(bodyLayer, outImage[1]);
                // BRÄNTSES
                areaLayer = Filters.multiplyColBright().runSingle(sourceLayer[3], 280.);
                areaLayer = Filters.thresholdBright().runSingle(areaLayer, 15);
                areaLayer = FiltersPass.filterObjectDimension().runSingle(areaLayer, COL.BLACK, 15, false, 0);
                IMG.copyImage(areaLayer, outImage[2]);
                //
                Tonga.log.trace("There are {} centers", centers.size());
                double[][] hitsMax = objectAreaTracing(centers,
                        new Image[]{areaLayer.toFXImage(), bodyLayer.toFXImage(), sourceLayer[1].layerImage.getFXImage(), outImage[3], outImage[4]},
                        radius, sCol, new PixelWriter[]{outWriter[3], outWriter[4]}, onlyOne);
                Tonga.log.trace("There are {} hitmaxes", centers.size());
                for (int cell = 1; cell < hitsMax.length; cell++) {
                    Object[] dataRow = data.newRow(sourceImage.imageName);
                    dataRow[1] = cell;
                    dataRow[2] = String.format("%.3f", ((double) hitsMax[cell][0] / (double) hitsMax[cell][1] * 100)) + "%";
                    dataRow[3] = String.format("%.4f", ((double) hitsMax[cell][2] / (double) hitsMax[cell][3] * 100)) + "%";
                }
            }
        };
    }

    public static double[][] objectAreaTracing(List<Point> centers, Image[] sources, int radius, Color sCol, PixelWriter[] canvasWriters, boolean onlyOne) {
        PixelReader areaReader = sources[0].getPixelReader();
        PixelReader bodyReader = sources[1].getPixelReader();
        PixelReader canvasReader = sources[3].getPixelReader();
        PixelReader canvas2Reader = sources[4].getPixelReader();
        PixelReader mbpReader = sources[2].getPixelReader();
        PixelWriter canvasWriter = canvasWriters[0];
        PixelWriter canvas2Writer = canvasWriters[1];
        ImageData newBranches = FiltersPass.edgeErode().runSingle(new ImageData(sources[0]), COL.BLACK, 1, false, true);
        PixelReader smallAreaReader = newBranches.toFXImage().getPixelReader();
        ROISet bodies = new ImageTracer(sources[1], sCol).trace();
        // tee blacklist josta suodatetaan pois ne joissa ei ole vain yhtä nukleusta
        List<Point> blackListPoints = new ArrayList<>();
        List<ROI> blackListAreas = new ArrayList<>();
        if (onlyOne) {
            for (int bodi = 0; bodi < bodies.objectsCount(); bodi++) {
                ROI body = bodies.list.get(bodi);
                List<Point> matches = new ArrayList<>();
                int bodycount = 0;
                for (int cell = 0; cell < centers.size(); cell++) {
                    Point pp = centers.get(cell);
                    if (pp.x >= body.area.xstart && pp.x < body.area.xstart + body.getWidth()
                            && pp.y >= body.area.ystart && pp.y < body.area.ystart + body.getHeight()
                            && body.pointIsInside(pp)) {
                        matches.add(pp);
                        bodycount++;
                    }
                }
                if (bodycount > 1) {
                    blackListPoints.addAll(matches);
                    blackListAreas.add(body);
                }
            }
        }
        double[][] resultArray = new double[centers.size() + 1][4]; //[soluid][{hits,max,intensity,intmax}];
        for (int cell = 1; cell <= centers.size(); cell++) {
            Point p = centers.get(cell - 1);
            double colSeed = new Random(p.hashCode()).nextDouble();
            int hits = 0, max = 0, intHits = 0;
            double intensity = 0;
            if (!blackListPoints.contains(p)) {
                for (int x = p.x - radius; x < p.x + radius; x++) {
                    for (int y = p.y - radius; y < p.y + radius; y++) {
                        if (GEO.getDist(p.x, p.y, x, y) <= radius) {
                            try {
                                if (bodyReader.getColor(x, y).equals(sCol)) {
                                    Color oc = canvasReader.getColor(x, y);
                                    Color nc;
                                    if (!areaReader.getColor(x, y).equals(sCol)) {
                                        hits++;
                                        nc = Color.hsb(colSeed * 360, 0.75, 1);
                                    } else {
                                        nc = Color.hsb(colSeed * 360, 0.5, 0.5);
                                    }
                                    if (!smallAreaReader.getColor(x, y).equals(sCol)) {
                                        intensity += mbpReader.getColor(x, y).getBrightness();
                                        intHits++;
                                    }
                                    if (oc.equals(Color.BLACK)) {
                                        canvasWriter.setColor(x, y, nc);
                                    } else {
                                        canvasWriter.setColor(x, y, COL.blendColor(nc, oc));
                                    }
                                    max++;
                                }
                            } catch (IndexOutOfBoundsException ex) {
                            }
                        }
                    }
                }
                resultArray[cell] = new double[]{hits, max, intensity, intHits};
            } else {
                resultArray[cell] = null;
            }
        }
        //DRAW MBP MAPPING
        for (int cell = 1; cell <= centers.size(); cell++) {
            Point p = centers.get(cell - 1);
            double colSeed = new Random(p.hashCode()).nextDouble();
            if (!blackListPoints.contains(p)) {
                Color mbpCol = Color.hsb(120, 1, (double) resultArray[cell][2] / (double) resultArray[cell][3]);
                for (int x = p.x - radius; x < p.x + radius; x++) {
                    for (int y = p.y - radius; y < p.y + radius; y++) {
                        if (GEO.getDist(p.x, p.y, x, y) <= radius) {
                            try {
                                if (bodyReader.getColor(x, y).equals(sCol)) {
                                    Color oc = canvas2Reader.getColor(x, y);
                                    Color nc;
                                    if (!smallAreaReader.getColor(x, y).equals(sCol)) {
                                        nc = mbpCol;
                                    } else {
                                        nc = Color.hsb(colSeed * 360, 0.5, 0.5);
                                    }
                                    if (oc.equals(Color.BLACK)) {
                                        canvas2Writer.setColor(x, y, nc);
                                    } else {
                                        canvas2Writer.setColor(x, y, COL.blendColor(nc, oc));
                                    }
                                }
                            } catch (IndexOutOfBoundsException ex) {
                            }
                        }
                    }
                }
            }
        }
        resultArray = Arrays.stream(resultArray).filter(a -> a != null).toArray(double[][]::new);
        for (int cell = 0; cell < centers.size(); cell++) {
            canvasWriter.setColor(centers.get(cell).x, centers.get(cell).y, Color.WHITE);
            canvasWriter.setColor(centers.get(cell).x - 1, centers.get(cell).y, Color.RED);
            canvasWriter.setColor(centers.get(cell).x + 1, centers.get(cell).y, Color.RED);
            canvasWriter.setColor(centers.get(cell).x, centers.get(cell).y + 1, Color.RED);
            canvasWriter.setColor(centers.get(cell).x, centers.get(cell).y - 1, Color.RED);
            canvas2Writer.setColor(centers.get(cell).x, centers.get(cell).y, Color.WHITE);
            canvas2Writer.setColor(centers.get(cell).x - 1, centers.get(cell).y, Color.RED);
            canvas2Writer.setColor(centers.get(cell).x + 1, centers.get(cell).y, Color.RED);
            canvas2Writer.setColor(centers.get(cell).x, centers.get(cell).y + 1, Color.RED);
            canvas2Writer.setColor(centers.get(cell).x, centers.get(cell).y - 1, Color.RED);
        }
        blackListAreas.forEach(bod -> {
            for (int xx = 0; xx < bod.getWidth(); xx++) {
                for (int yy = 0; yy < bod.getHeight(); yy++) {
                    if (bod.area.area[xx][yy]) {
                        canvasWriter.setColor(xx + bod.area.xstart, yy + bod.area.ystart, Color.GRAY);
                        canvas2Writer.setColor(xx + bod.area.xstart, yy + bod.area.ystart, Color.GRAY);
                    }
                }
            }
        });
        return resultArray;
    }

}
