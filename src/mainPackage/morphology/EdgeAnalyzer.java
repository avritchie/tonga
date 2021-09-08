package mainPackage.morphology;

import java.awt.Point;
import static java.lang.Double.NaN;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import mainPackage.Tonga;
import mainPackage.utils.GEO;

public class EdgeAnalyzer {

    public List<EdgePoint> primaryCornerPoints = new ArrayList<>();
    public List<EdgePoint> primaryCornerCandidates = new ArrayList<>();
    public List<EdgePoint> cornerPoints = new ArrayList<>();
    public List<EdgePoint> cornerCandidates = new ArrayList<>();
    public List<EdgePoint> interSectionPoints = new ArrayList<>();
    private List<EdgePoint> pixelEdge = new ArrayList<>();
    private int pixelEdgeSize;
    public int totalSharpPoints = 0; // how many points of the edge are part of a sharp corner of any kind
    private int targetSize, spanSize, pointGapJoin, pointGapMax;
    private double minDist;

    protected EdgeAnalyzer(ROI obj, int size) {
        setSizes(size);
        try {
            //analyze points on the outer edge
            analEdge(obj, (List<EdgePoint>) obj.outEdge.list);
            //analyze points on the inner edge if necessary (for segmentation)
            if (obj.innEdge != null) {
                analEdge(obj, (List<EdgePoint>) obj.innEdge.list);
            }
            // set identity
            cornerCandidates.forEach(p -> {
                p.isUnsure = true;
            });
            printPoints();
        } catch (Exception ex) {
            Tonga.catchError(ex, "Edge tracing failed.");
        }
    }

    private void analEdge(ROI obj, List<EdgePoint> edge) {
        pixelEdge = edge;
        pixelEdgeSize = pixelEdge.size();
        boolean[] surePoints = new boolean[pixelEdgeSize];
        boolean[] maybePoints = new boolean[pixelEdgeSize];
        int anyPoints = 0;
        // dont analyze objects which are too small
        if (pixelEdgeSize < spanSize * 3) {
            return;
        }
        // go thru the edge
        for (int i = 0; i < pixelEdgeSize; i++) {
            EdgePoint p = pixelEdge.get(i);
            getAngleDirectionData(p, obj, i);
            if (!Double.isNaN(p.distance)) {
                if ((p.distance >= minDist && p.distance <= spanSize) || p.angle < 135) {
                    anyPoints++;
                    if (p.angle < 135) {
                        surePoints[i] = true;
                        primaryCornerPoints.add(p);
                    } else {
                        maybePoints[i] = true;
                        primaryCornerCandidates.add(p);
                    }
                }
            }
        }
        // prevent duplicates next to each other
        List<List<EdgePoint>> filteredPoints = getFinalCornerPointDataNew(surePoints, maybePoints);
        cornerPoints.addAll(filteredPoints.get(0));
        cornerCandidates.addAll(filteredPoints.get(1));
        totalSharpPoints += anyPoints;
    }

    private void getAngleDirectionData(EdgePoint pointCentral, ROI obj, int location) {
        double angle, distance, direction;
        Point pointPrevious = getPosPrevious(location, spanSize, obj);
        Point pointNext = getPosNext(location, spanSize, obj);
        // it is possible to connect to additional filling pixels in which case getting is aborted
        if (pixelOrderWrong(pointPrevious, pointNext, location, obj)) {
            angle = 180;
            distance = NaN;
            direction = 90;
        } else {
            Point mP = GEO.getMidPoint(pointPrevious, pointNext);
            //distance = GEO.getDist(mP, pointCentral);
            angle = GEO.getAngle(pointCentral, pointPrevious, pointNext);
            double cornerAngle = GEO.getAngle(pointPrevious, pointCentral, pointNext);
            double spanDistHalf = GEO.getDist(pointNext, pointPrevious) / 2.;
            double spanDistPoint = GEO.getDist(pointCentral, pointPrevious);
            distance = GEO.triangleSide(spanDistHalf, spanDistPoint, cornerAngle);
            //direction = GEO.getDirection(mP, pointCentral);
            double direction1 = GEO.getDirection(pointPrevious, pointCentral);
            double direction2 = GEO.getDirection(pointNext, pointCentral);
            direction = GEO.getDirBetween(direction1, direction2);
            if (obj.area.area[mP.x][mP.y]) {
                angle = 360 - angle;
                distance = NaN;
            }
        }
        pointCentral.angle = angle;
        pointCentral.distance = distance;
        pointCentral.direction = direction;
        pointCentral.line = new Line(pointCentral.x, pointCentral.y, direction);
    }

    private EdgePoint getPosPrevious(int location, int span, ROI obj) {
        int prevPoint = location - span >= 0 ? location - span : pixelEdgeSize - (span - location);
        return pixelEdge.get(prevPoint);
    }

    private EdgePoint getPosNext(int location, int span, ROI obj) {
        int nextPoint = location + span < pixelEdgeSize ? location + span : location + span - pixelEdgeSize;
        return pixelEdge.get(nextPoint);
    }

    private boolean pixelOrderWrong(Point pointPrevious, Point pointNext, int location, ROI obj) {
        // to detect if a point does not follow the edge
        EdgePoint pointCentral = pixelEdge.get(location);
        Point point1Previous = getPosPrevious(location, spanSize - 1, obj);
        Point point1Next = getPosNext(location, spanSize - 1, obj);
        return GEO.getDist(pointPrevious, pointCentral) > spanSize
                || GEO.getDist(pointNext, pointCentral) > spanSize
                || GEO.getDist(point1Next, pointNext) > minDist
                || GEO.getDist(point1Previous, pointPrevious) > minDist;
    }

    private List<List<EdgePoint>> getFinalCornerPointDataNew(boolean[] surePxls, boolean[] maybePxls) {
        List<EdgePoint> sureCorners = new ArrayList<>();
        List<EdgePoint> maybeCorners = new ArrayList<>();
        int ps = -1, pe, gc = 0;
        // iterate through sure pixels first
        for (int i = 0; i < surePxls.length; i++) {
            if (ps == -1) {
                if (surePxls[i]) {
                    ps = i;
                }
            } else {
                if (!surePxls[i]) {
                    gc++;
                    if ((gc > pointGapMax && !maybePxls[i]) || gc > pointGapMax * 2) {
                        pe = i - gc;
                        // area from start position ps and end position pe found
                        //System.out.println("Area from " + ps + " to " + pe + " found of sure pixels");
                        EdgePoint[] bestPoints = getAreaBestPoints(ps, pe);
                        if (pe - ps <= pointGapMax && bestPoints.length == 1) {
                            //System.out.println("Changed to a unsure");
                            bestPoints[0].isUnsure = true;
                            Collections.addAll(maybeCorners, bestPoints);
                        } else {
                            Collections.addAll(sureCorners, bestPoints);
                        }
                        //System.out.println("The best point in the area is " + bestPoint.toString());
                        ps = -1;
                        gc = 0;
                    }
                } else {
                    gc = 0;
                }
            }
        }
        // then through maybe pixels
        ps = -1;
        gc = 0;
        for (int i = 0; i < maybePxls.length; i++) {
            if (ps == -1) {
                if (maybePxls[i]) {
                    ps = i;
                }
            } else {
                if (!maybePxls[i]) {
                    if (surePxls[i]) {
                        ps = -1;
                        gc = 0;
                    } else {
                        gc++;
                        if (gc > pointGapMax * 3) {
                            pe = i - gc;
                            // area from start position ps and end position pe found
                            // System.out.println("Area from " + ps + " to " + pe + " found of unsure pixels");
                            // exlude any areas that touch sure pixels
                            if (pe == ps && spanSize > 10) {
                                // System.out.println("But it was removed because it was solitary");
                            } else if (!surePxls[ROI.edgePosition(ps - 1, maybePxls)] && !surePxls[ROI.edgePosition(pe + 1, maybePxls)]) {
                                EdgePoint[] bestPoints = getAreaBestPoints(ps, pe);
                                //System.out.println("The best point in the area is " + bestPoint.toString());
                                Collections.addAll(maybeCorners, bestPoints);
                            } else {
                                // System.out.println("But it was removed because it touched sure pixels");
                            }
                            ps = -1;
                            gc = 0;
                        }
                    }
                } else {
                    gc = 0;
                }
            }
        }
        // then eliminate any duplicates close to each other having same direction
        List<EdgePoint> sureCornersFinal = new ArrayList<>();
        List<EdgePoint> maybeCornersFinal = new ArrayList<>();
        List<EdgePoint> failures = new ArrayList<>();
        EdgePoint s1, s2;
        boolean pass;
        for (int i = 0; i < sureCorners.size(); i++) {
            s1 = sureCorners.get(i);
            pass = true;
            for (int j = 0; j < sureCorners.size(); j++) {
                s2 = sureCorners.get(j);
                if (i != j) {
                    if (GEO.getDist(s1, s2) <= pointGapJoin && GEO.getParallelFactor(s1, s2) > 0.3) {
                        if (!failures.contains(s2)) {
                            failures.add(s1);
                            pass = false;
                        }
                    }
                }
            }
            if (pass) {
                sureCornersFinal.add(s1);
            }
        }
        for (int i = 0; i < maybeCorners.size(); i++) {
            s1 = maybeCorners.get(i);
            pass = true;
            for (int j = 0; j < maybeCorners.size(); j++) {
                s2 = maybeCorners.get(j);
                if (i != j) {
                    if (GEO.getDist(s1, s2) <= pointGapJoin && GEO.getParallelFactor(s1, s2) > 0.3) {
                        if (!failures.contains(s2)) {
                            failures.add(s1);
                            pass = false;
                        }
                    }
                }
            }
            if (pass) {
                maybeCornersFinal.add(s1);
            }
        }
        List<List<EdgePoint>> lists = new ArrayList<>();
        lists.add(sureCornersFinal);
        lists.add(maybeCornersFinal);
        return lists;
    }

    private EdgePoint[] getAreaBestPoints(int startPos, int endPos) {
        // if the area is very long and has a big direction span, divide to two points
        int size = endPos - startPos;
        ArrayList<Double> nt = new ArrayList<>();
        for (int i = startPos; i <= endPos; i++) {
            EdgePoint p = pixelEdge.get(i);
            nt.add(p.direction);
        }
        Collections.sort(nt);
        double maxGap = 0;
        double angDiff = nt.stream().mapToDouble(o -> o).max().getAsDouble()
                - nt.stream().mapToDouble(o -> o).min().getAsDouble();
        for (int i = 0; i < nt.size() - 1; i++) {
            double gap = nt.get(i + 1) - nt.get(i);
            if (gap > maxGap) {
                maxGap = gap;
            }
        }
        boolean divideToTwoPoints = size > (pointGapJoin * 2.5) && (maxGap > 10 || angDiff > 180);
        //System.out.println("Divide to two points: " + divideToTwoPoints + ", with ddiff of " + maxGap + " and size of " + size);
        int midPoint = startPos + (size / 2);
        if (!divideToTwoPoints) {
            double smallestAngle = 360;
            int anglePoint = -1;
            for (int i = startPos; i <= endPos; i++) {
                EdgePoint p = pixelEdge.get(i);
                if (p.angle < smallestAngle) {
                    smallestAngle = p.angle;
                    anglePoint = i;
                }
            }
            return new EdgePoint[]{getNonNull(midPoint - ((midPoint - anglePoint) / 2))};
        } else {
            double smallestAngle1 = 360;
            double smallestAngle2 = 360;
            int anglePoint1 = -1;
            int anglePoint2 = -1;
            EdgePoint p;
            for (int i = 1; i <= (size / 2) - 1; i++) {
                p = pixelEdge.get(midPoint - i);
                if (p.angle < smallestAngle1) {
                    smallestAngle1 = p.angle;
                    anglePoint1 = i;
                }
                p = pixelEdge.get(midPoint + i);
                if (p.angle < smallestAngle2) {
                    smallestAngle2 = p.angle;
                    anglePoint2 = i;
                }
            }
            return new EdgePoint[]{getNonNull(midPoint - anglePoint1), getNonNull(midPoint + anglePoint2)};
        }
    }

    @Deprecated
    private List<EdgePoint> getFinalCornerPointData(boolean[] surePxls, ROI obj) {
        List<EdgePoint> pxlCorners = new ArrayList<>();
        // viimeiseimmän tunnistetun kulman indeksi
        int pos = -1;
        // käy koko reuna läpi
        for (int j = 6; j < surePxls.length - 6; j++) {
            if (!surePxls[j - 1] && !surePxls[j - 2] && surePxls[j]) {
                pos = j;
            }
            if (!surePxls[j + 1] && !surePxls[j + 2] && surePxls[j]) {
                if (j - pos >= 2) {
                    EdgePoint p = pixelEdge.get(j - ((j - pos) / 2));
                    Point mp = GEO.getMidPoint(pixelEdge.get(pos - 4), pixelEdge.get(j + 4));
                    p.direction = GEO.getDirection(mp, p);
                    pxlCorners.add(p);
                }
            }
        }
        return pxlCorners;
    }

    private void setSizes(int size) {
        targetSize = size; //expected/average cell width
        spanSize = (int) (Math.pow(targetSize, 0.8) / 1.75); // 12 , span to look front/back for calculating corners
        minDist = Math.max(1.42, Math.pow(targetSize, 0.8) / 10.); // 1.42 (hyp c for a=1,b=1) , minimum concaveness to consider
        pointGapJoin = (int) (targetSize / 6.5); // 9 , combine points closer than this to each other if similar direction
        pointGapMax = Math.max(1, targetSize / 50); // 1 , gap allowed to still considering a corner area to be the same
        //System.out.println("Sizes: " + targetSize + " | " + spanSize + " | " + minDist + " | " + pointGapJoin + " | " + pointGapMax);
    }

    public static void removeBorders(ROI o, List<Point> edges) {
        //remove edge points which touch image edges
        Iterator<Point> it = edges.iterator();
        while (it.hasNext()) {
            Point ep = it.next();
            int xx = ep.x + o.area.xstart;
            int yy = ep.y + o.area.ystart;
            ROISet rs = o.set;
            if (xx == 0 || yy == 0 || xx == rs.width - 1 || yy == rs.height - 1) {
                it.remove();
            }
        }
    }

    private void printPoints() {
        /*
            System.out.println("//////////////////////////////////////");
            System.out.println("The recognized points for this shape:");
            System.out.println("///Sure///");
            cornerPoints.forEach(p -> {
                System.out.println(p);
            });
            System.out.println("///Unsure///");
            cornerCandidates.forEach(p -> {
                System.out.println(p);
            });
            System.out.println("//////////////////////////////////////");
         */
    }

    private EdgePoint getNonNull(int i) {
        EdgePoint ep = pixelEdge.get(i);
        int span = 1;
        while (Double.isNaN(ep.distance)) {
            if (span > 0) {
                span = -1 * span;
            } else {
                span = -1 * span + 1;
            }
            ep = pixelEdge.get(i + span);
        }
        return ep;
    }
}
