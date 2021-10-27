package mainPackage.morphology;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javafx.scene.image.WritableImage;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.utils.STAT;
import mainPackage.Tonga;
import mainPackage.utils.RGB;

public class ROISet {

    public List<? extends ROI> list;
    public int width, height;
    private STAT statsSize, statsStain;
    private boolean drawAngles;
    private boolean edgeOrder;
    protected int targetsize;

    public ROISet(List<? extends ROI> list, int width, int height) {
        this.list = list;
        this.width = width;
        this.height = height;
        this.drawAngles = false;
        this.edgeOrder = false;
        this.targetsize = 60;
        list.forEach(o -> {
            o.set = this;
        });
    }

    public void targetSize(int nucleusSize) {
        this.targetsize = nucleusSize;
    }

    abstract class setRenderer {

        int[] out;

        void preDrawingMethod(ROI o) {
            //override to introduce predrawing
        }

        abstract void drawingMethod(ROI o);

        public int[] draw() {
            Tonga.iteration();
            out = new int[width * height];
            IMG.fillArray(out, width, height, COL.BLACK);
            for (int i = 0; i < list.size(); i++) {
                preDrawingMethod(list.get(i));
            }
            for (int i = 0; i < list.size(); i++) {
                drawingMethod(list.get(i));
                Tonga.loader().appendProgress(list.size());
            }
            return out;
        }

        public int pos(Point p, ROI o) {
            return width * (p.y + o.area.ystart) + (p.x + o.area.xstart);
        }
    }

    public ImageData drawToImageData() {
        return drawToImageData(false);
    }

    public ImageData drawToImageData(boolean noEffects) {
        return new ImageData(drawToArray(noEffects), width, height);
    }

    public WritableImage drawToImage() {
        return drawToImage(false);
    }

    public WritableImage drawToImage(boolean noEffects) {
        int[] out = drawToArray(noEffects);
        return COL.turnArrayToImage(out, width, height);
    }

    public int[] drawToArray() {
        return drawToArray(false);
    }

    public int[] drawToArray(boolean noEffects) {
        return new setRenderer() {

            @Override
            void preDrawingMethod(ROI o) {
                if (!noEffects && o.mask != null) {
                    if (o.mask.outEdge == null) {
                        Iterate.areaPixels(o.mask, (int pos) -> {
                            out[pos] = COL.blendColor(COL.GRAY, out[pos]);
                        });
                    } else {
                        Iterate.areaPixels(o.mask, (int pos) -> {
                            out[pos] = out[pos] == COL.BLACK ? COL.DGRAY : out[pos];
                        });
                        o.mask.outEdge.list.forEach(p -> {
                            out[width * (p.y + o.mask.area.ystart) + (p.x + o.mask.area.xstart)] = COL.LGRAY;
                        });
                    }
                }
            }

            @Override
            void drawingMethod(ROI o) {
                Iterate.areaPixels(o, (int pos) -> {
                    out[pos] = (o.area.annotations[(pos % width) - o.area.xstart][(pos / width) - o.area.ystart] && !noEffects) ? COL.GRAY : COL.WHITE;
                });
                if (!noEffects) {
                    if (o.outEdge != null) {
                        o.outEdge.list.forEach(p -> {
                            int color;
                            if (drawAngles) {
                                color = ((int) ((EdgePoint) p).angle) << 16;
                            } else if (edgeOrder) {
                                color = RGB.argbAlpha(o.outEdge.list.indexOf(p) << 12, 255);
                            } else {
                                color = COL.RED;
                            }
                            out[pos(p, o)] = color;
                        });
                    }
                    if (o.innEdge != null) {
                        o.innEdge.list.forEach(p -> {
                            int color;
                            if (edgeOrder) {
                                color = RGB.argbAlpha(o.innEdge.list.indexOf(p) << 20, 255);
                            } else {
                                color = COL.LRED;
                            }
                            out[pos(p, o)] = color;
                        });
                    }
                    if (o.edgeData != null) {
                        o.edgeData.primaryCornerCandidates.forEach(p -> {
                            out[pos(p, o)] = COL.LBLUE;
                        });
                        o.edgeData.primaryCornerPoints.forEach(p -> {
                            out[pos(p, o)] = COL.LCYAN;
                        });
                        o.edgeData.cornerCandidates.forEach(p -> {
                            out[pos(p, o)] = COL.BLUE;
                        });
                        o.edgeData.cornerPoints.forEach(p -> {
                            out[pos(p, o)] = COL.CYAN;
                        });
                        o.edgeData.interSectionPoints.forEach(p -> {
                            out[pos(p, o)] = COL.GREEN;
                        });
                    }
                }
            }
        }.draw();
    }

    public int[] drawSurroundArray(boolean useAverage) {
        return new setRenderer() {
            double max = getMaxStain();
            double fact = 255 / max;

            @Override
            void preDrawingMethod(ROI o) {
                Iterate.areaPixels(o.mask, (int pos) -> {
                    out[pos] = out[pos] == COL.BLACK ? COL.DGRAY : out[pos];
                });
                o.mask.outEdge.list.forEach(p -> {
                    out[width * (p.y + o.mask.area.ystart) + (p.x + o.mask.area.xstart)] = COL.LGRAY;
                });
            }

            @Override
            void drawingMethod(ROI o) {
                Iterate.areaPixels(o, (int pos) -> {
                    int val;
                    if (useAverage) {
                        val = 255 - ((int) (o.getStainAvg() * 255));
                    } else {
                        val = 255 - ((int) (o.getStain() * fact));
                    }
                    int color = 0xFFFF0000 | val << 8 | val;
                    out[pos] = color;
                });
            }
        }.draw();
    }

    public int[] drawMaskArray() {
        // render the set by marking masks and their overlaps
        return new setRenderer() {
            @Override
            void preDrawingMethod(ROI o) {
                if (o.mask != null) {
                    Iterate.areaPixels(o.mask, (int pos) -> {
                        out[pos] = out[pos] == COL.BLACK ? COL.DGRAY : COL.GRAY;
                    });
                }
            }

            @Override
            void drawingMethod(ROI o) {
                Iterate.areaPixels(o, (int pos) -> {
                    out[pos] = COL.WHITE;
                });
            }
        }.draw();
    }

    private ROI getBiggest() {
        return (ROI) list.stream().sorted((o1, o2) -> o2.getSize() - o1.getSize()).findFirst().get();
    }

    private double getMaxStain() {
        return list.stream().mapToDouble(o -> o.getStain()).max().getAsDouble();
    }

    public int[] drawStainArray(boolean useAverage) {
        // render the set by giving each shape colour based on the staining intensity
        return new setRenderer() {
            double max = getMaxStain();
            double fact = 255 / max;

            @Override
            void drawingMethod(ROI o) {
                Iterate.areaPixels(o, (int pos) -> {
                    int val;
                    if (useAverage) {
                        val = 255 - ((int) (o.getStainAvg() * 255));
                    } else {
                        val = 255 - ((int) (o.getStain() * fact));
                    }
                    int color = 0xFFFF0000 | val << 8 | val;
                    out[pos] = color;
                });
            }
        }.draw();
    }

    public ImageData drawEdgeImage() {
        return new ImageData(new setRenderer() {
            @Override
            void drawingMethod(ROI o) {
                if (o.outEdge != null) {
                    o.outEdge.list.forEach(p -> {
                        out[pos(p, o)] = COL.WHITE;
                    });
                }
            }
        }.draw(), width, height);
    }

    public ImageData drawEdgeStainImage(double binThreshold) {
        return new ImageData(new setRenderer() {
            @Override
            void drawingMethod(ROI o) {
                boolean positive = o.getStainAvg() > binThreshold;
                if (positive) {
                    if (o.outEdge != null) {
                        o.outEdge.list.forEach(p -> {
                            out[pos(p, o)] = COL.WHITE;
                        });
                    }
                }

            }
        }.draw(), width, height);
    }

    public int[] drawStainArray(double binThreshold, boolean remove) {
        // render the set by giving each shape either gray or white based on staining intensity and threshold
        return new setRenderer() {
            @Override
            void drawingMethod(ROI o) {
                if (remove) {
                    boolean positive = o.getStainAvg() > binThreshold;
                    if (positive) {
                        Iterate.areaPixels(o, (int pos) -> {
                            out[pos] = COL.WHITE;
                        });
                    }
                } else {
                    Iterate.areaPixels(o, (int pos) -> {
                        boolean positive = o.getStainAvg() > binThreshold;
                        int color = positive ? COL.WHITE : COL.GRAY;
                        out[pos] = color;
                    });
                }
            }
        }.draw();
    }

    public ImageData drawStainImage(double binThreshold, boolean remove) {
        return new ImageData(drawStainArray(binThreshold, remove), width, height);
    }

    public final void getExtendedMasks(int radius) {
        Tonga.iteration();
        list.forEach(o -> {
            if (o.outEdge == null) {
                o.findOuterEdges();
            }
            o.getExtendedROI(radius);
            Tonga.loader().appendProgress(list.size());
        });
    }

    public final void findOuterMaskEdges() {
        list.forEach(o -> {
            o.mask.outEdge = EdgeTracer.maskEdges(o);
        });
    }

    public final void findOuterEdges() {
        list.forEach(o -> {
            o.findOuterEdges();
        });
    }

    public final void findInnerEdges() {
        // do outer edge analyzing first
        list.forEach(o -> {
            o.findInnerEdges();
        });
    }

    public final void fillInnerHoles() {
        list.forEach(o -> {
            EdgeTracer.fillHoles(o);
            o.findOuterEdges();
        });
    }

    public void filterCornersNotTouching(ImageData mMask) {
        list.forEach(o -> {
            // do analyze corners first
            o.filterCornersNotTouching(o.edgeData.cornerPoints, mMask);
            o.filterCornersNotTouching(o.edgeData.cornerCandidates, mMask);
        });
    }

    public void filterUnsureCorners() {
        //removes all unsure corners, leave only the sure ones
        list.forEach(o -> {
            // do analyze corners first
            o.edgeData.cornerCandidates = new ArrayList<>();
        });
    }

    public final void analyzeCorners() {
        // will also supplement outer edge data with angle/direction info
        // corner points will get line functions based on the direction
        list.forEach(o -> {
            o.edgeData = new EdgeAnalyzer(o, targetsize);
        });
    }

    public final void analyzeCornerpairs() {
        // do edge analyzing first
        list.forEach(o -> {
            o.cornerPairs();
        });
    }

    public final boolean segment(int mode) {
        Segmentor.segmentedSomething = false;
        list.forEach(o -> {
            o.sectionBasedOnIntersections(this, mode);
        });
        return Segmentor.segmentedSomething;
    }

    public void imageEdgeFixer(boolean separate) {
        // do this first before edge tracing!
        // separate true = wont touch the edges
        // separate false = only fill the holes
        list.forEach(o -> {
            o.fixEdges(width, height, separate);
        });
    }

    public final void quantifyEvenColour(ImageData img) {
        list.forEach(o -> {
            o.quantifyColor(img);
        });
    }

    public final void quantifyStainAgainstChannel(ImageData img) {
        list.forEach(o -> {
            o.quantifyStain(img);
        });
    }

    public final void quantifyStainOnMaskAgainstChannel(ImageData exclude, ImageData img) {
        list.forEach(o -> {
            o.quantifyMaskStain(exclude, img);
        });
    }

    public final ROISet getPositionFilteredSet(ImageData img, int bgcol,boolean keeporiginal) {
        ImageTracer origSet = new ImageTracer(img, bgcol);
        List<ROI> newRois = new ArrayList<>();
        list.forEach(roi -> {
            ROI newRoi = origSet.traceSingleObjectAtPoint(roi.area.firstxpos, roi.area.firstypos);
            if (newRoi != null) {
                newRois.add(keeporiginal ? roi : newRoi);
            }
        });
        return new ROISet(newRois, img.width, img.height);
    }

    public final void analyzeCornerIntersections() {
        // do edge analyzing first
        list.forEach(o -> {
            o.edgeData.cornerPoints.forEach(p -> {
                // will give point a Pairings-object with all possible pairs
                o.findPossiblePairings(p);
            });
            o.edgeData.cornerCandidates.forEach(p -> {
                // will give point a Pairings-object with all possible pairs
                o.findPossiblePairings(p);
            });
            o.edgeData.cornerPoints.forEach(p -> {
                // will give point's line function start/end position
                // start is the point itself, end is the opposite edge
                // also gives "nearestFriend" point on the other side
                o.findIntersectionOnEdges(p);
                // the closest possible point on the opposite edge
                o.findClosestOpposite(p);
            });
            o.edgeData.cornerCandidates.forEach(p -> {
                // same as above but for unsure pixels
                o.findIntersectionOnEdges(p);
                // the closest possible point on the opposite edge
                o.findClosestOpposite(p);
            });
            o.edgeData.cornerPoints.forEach(p -> {
                // will give point's intersect-list other points it intersects itself with
                o.findIntersectionOnOthers(p);
            });
            o.edgeData.cornerPoints.forEach(p -> {
                p.pairings.pairingMap.values().forEach(pm
                        -> {
                    pm.fill(o);
                }
                );
            });
            o.edgeData.cornerCandidates.forEach(p -> {
                p.pairings.pairingMap.values().forEach(pm
                        -> {
                    pm.fill(o);
                }
                );
            });
            o.edgeData.cornerPoints.forEach(p -> {
                p.pairings.fill();
            });
            o.edgeData.cornerCandidates.forEach(p -> {
                p.pairings.fill();
            });
            o.drawPointLines();
        });
    }

    public final void enableAngleDrawing() {
        // do edge analyzing first
        drawAngles = true;
    }

    public final void enableEdgeOrderDrawing() {
        // do edge analyzing first
        edgeOrder = true;
    }

    public ArrayList<Point> connectLongAndThinObjects() {
        ArrayList<Line> lines = new ArrayList<>();
        ArrayList<ROI> rois = new ArrayList<>();
        ArrayList<Point> points = new ArrayList<>();
        list.forEach(o -> {
            Line l = o.getLongThinLine();
            if (l != null) {
                lines.add(l);
                rois.add(o);
            }
        });
        for (int i1 = 0; i1 < lines.size(); i1++) {
            for (int i2 = i1 + 1; i2 < lines.size(); i2++) {
                if (i1 != i2) {
                    Point p = Line.intersection(lines.get(i1), lines.get(i2));
                    if (p != null) {
                        int r = 0;
                        Point r1p = null, r2p = null;
                        ROI r1 = rois.get(i1);
                        ROI r2 = rois.get(i2);
                        while (r1p == null || r2p == null) {
                            for (int d = 0; d < (r == 0 ? 1 : 4); d++) {
                                for (int l = -r; l < (r == 0 ? 1 : r); l++) {
                                    int x = p.x + (d == 0 ? l : d == 1 ? r : d == 2 ? -l : -r);
                                    int y = p.y + (d == 0 ? -r : d == 1 ? l : d == 2 ? r : -l);
                                    if (r1p == null) {
                                        try {
                                            if (r1.area.area[x - r1.area.xstart][y - r1.area.ystart]) {
                                                r1p = new Point(x, y);
                                            }
                                        } catch (ArrayIndexOutOfBoundsException ex) {
                                            Tonga.log.warn("Invalid array check when connecting lines.");
                                        }
                                    }
                                    if (r2p == null) {
                                        try {
                                            if (r2.area.area[x - r2.area.xstart][y - r2.area.ystart]) {
                                                r2p = new Point(x, y);
                                            }
                                        } catch (ArrayIndexOutOfBoundsException ex) {
                                            Tonga.log.warn("Invalid array check when connecting lines.");
                                        }
                                    }
                                }
                            }
                            r++;
                        }
                        Tonga.log.trace("Long and thin object connection found between {} and {}", r1p, r2p);
                        points.add(r1p);
                        points.add(r2p);
                    }
                }
            }
        }
        return points;
    }

    public void filterOutSmallObjects(int limitPxls) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getSize() < limitPxls) {
                it.remove();
            }
        }
    }

    public void filterOutByDimension(int limitPxls) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getDimension() < limitPxls) {
                it.remove();
            }
        }
    }

    public final void filterOutDimSmallObjects(int size, double limit) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getStainAvg() < limit && roi.getSize() < size) {
                it.remove();
            }
        }
    }

    public final void filterOutDimObjects(double limit) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getStainAvg() < limit) {
                it.remove();
            }
        }
    }

    public final void filterOutBrightObjects(double limit) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getStainAvg() > limit) {
                it.remove();
            }
        }
    }

    public void removeEdgeTouchers() {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.touchesImageEdges()) {
                it.remove();
            }
        }
    }

    public void filterDeadDividing(ImageData origImg) {
        if (this.list.get(0).outEdge == null) {
            findOuterEdges();
        }
        if (this.list.get(0).edgeData == null) {
            analyzeCorners();
            analyzeCornerIntersections();
        }
        quantifyStainAgainstChannel(origImg);
        filterOutDeadDividing();
    }

    private void filterOutDeadDividing() {
        Iterator<? extends ROI> it = list.iterator();
        double avgSize = avgSize();//1915;//
        double avgStain = avgStain();//0.396;//
        double avgSTD = 0;
        double avgDiaRat = 0;
        for (int i = 0; i < list.size(); i++) {
            ROI roi = list.get(i);
            STAT stat = roi.getStainSTAT();
            avgSTD += stat.getStdDev();
            avgDiaRat += roi.getSize() / (double) roi.getDimension();
        }
        avgSTD = avgSTD / list.size();//26.45;//
        avgDiaRat = avgDiaRat / list.size();//34.14;//
        Tonga.log.trace("Dead and dividing detection:\nAverages: size={}, stain={}, std={}, diarat={}", avgSize, avgStain, avgSTD, avgDiaRat);
        while (it.hasNext() && !Thread.currentThread().isInterrupted()) {
            ROI roi = it.next();
            STAT stat = roi.getStainSTAT();
            int totalPixels = roi.edgeData.cornerCandidates.size() + roi.edgeData.cornerPoints.size();
            double angles = new STAT(roi.outEdge.list.stream().mapToDouble((o) -> ((EdgePoint) o).angle).toArray()).getStdDev();
            double stain = roi.getStainAvg();
            double std = stat.getStdDev();
            int size = roi.getSize();
            double pixelSize = roi.edgeData.totalSharpPoints > 0 ? (size / (double) roi.edgeData.totalSharpPoints) : Integer.MAX_VALUE;
            double dimratio = roi.getSize() / (double) roi.getDimension();
            Tonga.log.trace("Nucleus x{}y{}, size={}, stain={}, dim={}, angles={}, pixels={}, sharps={}, surepoints={}, unsurepoints={}, stddev={}, var={}, mean={}, median={}",
                    roi.xcenter, roi.ycenter, roi.getSize(), roi.getStainAvg(),
                    dimratio, angles, pixelSize,
                    roi.edgeData.totalSharpPoints, roi.edgeData.cornerCandidates.size(), roi.edgeData.cornerPoints.size(),
                    stat.getStdDev(), stat.getVariance(), stat.getMean(), stat.getMedian());
            if (stain * 100 + std > 1.85 * (avgStain * 100 + avgSTD)) {
                Tonga.log.trace("was REMOVED because of ev 1");
                it.remove();
            } else if (size < 0.67 * avgSize && stain > 1.75 * avgStain) {
                Tonga.log.trace("was REMOVED because of ev 2");
                it.remove();
            } else if (size < avgSize && dimratio < 0.67 * avgDiaRat && std > 1.5 * avgSTD) {
                Tonga.log.trace("was REMOVED because of ev 3");
                it.remove();
            } else if (size < 1.1 * avgSize && totalPixels > 0 && stain > avgStain && std > 2 * avgSTD) {
                Tonga.log.trace("was REMOVED because of ev 4");
                it.remove();
            } else if (size < 0.5 * avgSize && stain > 1.2 * avgStain && std > 1.5 * avgSTD) {
                Tonga.log.trace("was REMOVED because of ev 6");
                it.remove();
            }
            //too many false positives
            if (size < 1.1 * avgSize && totalPixels > 0 && pixelSize < 200 && angles > 25) {
                Tonga.log.trace("was NOT REMOVED because of ev 5");
                //it.remove();
            }
        }
    }

    public void filterOutSmallAndShapey() {
        if (avgCornerlessSize() != 0) {
            Iterator<? extends ROI> it = list.iterator();
            double avgSize = avgCornerlessSize();
            double circ = GEO.circleCircumference(avgSize) / 2;
            Tonga.log.trace("Small and shapey detection:\nAverages: size={}, circ={}", avgSize, circ);
            while (it.hasNext()) {
                ROI roi = it.next();
                int ss = roi.edgeData.cornerCandidates.size() + roi.edgeData.cornerPoints.size();
                Tonga.log.trace("Nucleus size={}, shapeyness={}, sharps={}, surepoints={}, unsurepoints={}",
                        roi.getSize(), (roi.getSize() / (double) roi.edgeData.totalSharpPoints / ss),
                        roi.edgeData.totalSharpPoints, roi.edgeData.cornerPoints.size(), roi.edgeData.cornerCandidates.size());
                if (roi.getSize() < avgSize * 1.2 && roi.getSize() / (double) roi.edgeData.totalSharpPoints / ss < circ) {
                    Tonga.log.trace("was NOT REMOVED");
                    //it.remove();
                }
            }
        }
    }

    public void filterOutSmallAndRound(int minsize, double maxroundness) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getSize() < minsize && roi.getCircularity() > maxroundness) {
                it.remove();
            }
        }
    }

    public void filterOutLargeAndNonRound(int maxsize, double minroundness) {
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            if (roi.getSize() > maxsize && roi.getCircularity() < minroundness) {
                it.remove();
            }
        }
    }

    public void filterOutVeryVariable() {
        if (avgCornerlessSize() != 0) {
            Iterator<? extends ROI> it = list.iterator();
            double avgSize = avgCornerlessSize();
            Tonga.log.trace("Filter out very variable:");
            while (it.hasNext()) {
                ROI roi = it.next();
                STAT stat = roi.getStainSTAT();
                Tonga.log.trace("Nucleus size={}, stddev={}, var={}, mean={}, median={}",
                        roi.getSize(), stat.getStdDev(), stat.getVariance(), stat.getMean(), stat.getMedian());
                if (roi.getSize() < avgSize * 1.2 && stat.getStdDev() > 60) {
                    Tonga.log.trace("was NOT REMOVED");
                    //it.remove();
                }
            }
        }
    }

    public void filterOutBrightAndSmall(int val) {
        double multiplier = 0.5 + ((100 - val) / 100.);
        double avgStain = avgStain();
        double avgSize = avgSize();
        Tonga.log.trace("Bright and small removal:\nAverages: stain={}, size={}", avgStain, avgSize);
        Iterator<? extends ROI> it = list.iterator();
        while (it.hasNext()) {
            ROI roi = it.next();
            Tonga.log.trace("Nucleus stain={}, ={}, avgstainmult={}, size={}, avgsizemult={}", roi.getStain(), (avgStain * (2.5 * multiplier)), roi.getSize(), (avgSize * (0.5 + (0.5 * multiplier))));
            if (roi.getStainAvg() > avgStain * (2.5 * multiplier) && roi.getSize() * (0.5 + (0.5 * multiplier)) < avgSize) {
                Tonga.log.trace("was NOT REMOVED");
                //   it.remove();
            }
        }
    }

    public List<Point> getCenterPoints() {
        List<Point> cps = new ArrayList<>();
        list.forEach(o -> {
            cps.add(new Point(o.xcenter, o.ycenter));
        });
        return cps;
    }

    public int objectsCount() {
        return list.size();
    }

    public int objectsCountStainPositive(double binThreshold) {
        return (int) list.stream().filter((o) -> (o.getStainAvg() > binThreshold)).count();
    }

    public double avgCornerlessSize() {
        try {
            return list.stream().filter(d -> d.edgeData.cornerPoints.isEmpty()).mapToDouble(l -> l.getSize()).average().getAsDouble();
        } catch (NoSuchElementException ex) {
            try {
                return list.stream().mapToDouble(l -> l.getSize()).average().getAsDouble();
            } catch (NoSuchElementException exx) {
                return 0;
            }
        }
    }

    public double avgSize() {
        try {
            return list.stream().mapToDouble(l -> l.getSize()).average().getAsDouble();
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }

    public double avgStain() {
        try {
            return list.stream().mapToDouble(l -> l.getStainAvg()).average().getAsDouble();
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }

    public double totalAreaSize() {
        try {
            return list.stream().mapToDouble(l -> l.getSize()).sum();
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }

    public STAT statsForTotalSize() {
        if (statsSize == null) {
            statsSize = new STAT(list.stream().mapToDouble(l -> l.getSize()).toArray());
        }
        return statsSize;
    }

    public STAT statsForStain() {
        if (statsStain == null) {
            statsStain = new STAT(list.stream().mapToDouble(l -> l.getStainAvg()).toArray());
        }
        return statsStain;
    }

    public double avgCornerlessRoundness() {
        try {
            return list.stream().filter(d -> d.edgeData.cornerPoints.isEmpty()).mapToDouble(l -> l.getCircularity()).average().getAsDouble();
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }
}
