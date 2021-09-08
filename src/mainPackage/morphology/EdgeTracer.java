package mainPackage.morphology;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.IntStream;

public class EdgeTracer {

    protected static ListArea outerEdges(ROI obj) {
        Point sPoint = null;
        boolean[][] outArea = obj.getOutArea().area;
        // etsi aloituspiste
        int m = obj.area.height > 1 ? 2 : 1;
        for (int yy = 0; yy < m; yy++) {
            for (int xx = 0; xx < obj.getWidth(); xx++) {
                if (obj.area.area[xx][yy]) {
                    sPoint = new Point(xx, yy);
                }
            }
        }
        EdgePoint nPoint = new EdgePoint(sPoint.x, sPoint.y);
        ArrayList<EdgePoint> pxlOutEdg = new ArrayList<>();
        boolean[][] assigned = new boolean[obj.getWidth()][obj.getHeight()];
        Stack<Point> traceCrossing = new Stack<>();
        do {
            do {
                pxlOutEdg.add(nPoint);
                assigned[nPoint.x][nPoint.y] = true;
                nPoint = findNextEdgePixel(nPoint.x, nPoint.y, obj.area.area, assigned, outArea, sPoint, traceCrossing);
            } while (nPoint != null);
            nPoint = checkAllDone(obj.area.area, assigned, outArea);
        } while (nPoint != null);
        return new ListArea(pxlOutEdg, assigned);
    }

    protected static ListArea innerEdges(ROI obj) {
        Point sPoint = null;
        EdgePoint nPoint;
        ArrayList<EdgePoint> pxlInnEdg = new ArrayList<>();
        boolean[][] innArea = obj.getInnArea().area;
        boolean[][] assigned = new boolean[obj.getWidth()][obj.getHeight()];
        boolean[][] previousAssigned = obj.outEdge.area;
        // etsi aloituspiste
        int aXw = obj.area.area.length, aYw;
        findStart:
        for (int xx = 0; xx < aXw; xx++) {
            aYw = obj.area.area[xx].length;
            for (int yy = 0; yy < aYw; yy++) {
                if (obj.area.area[xx][yy]) {
                    if (isEdgePixel(xx, yy, obj.area.area) && !previousAssigned[xx][yy]) {
                        sPoint = new Point(xx, yy);
                        break findStart;
                    }
                }
            }
        }
        // if there is any inner area to begin with
        if (sPoint != null) {
            nPoint = new EdgePoint(sPoint.x, sPoint.y);
            Stack<Point> traceCrossing = new Stack<>();
            do {
                do {
                    if (!previousAssigned[nPoint.x][nPoint.y]) {
                        pxlInnEdg.add(nPoint);
                    }
                    assigned[nPoint.x][nPoint.y] = true;
                    nPoint = findNextEdgePixel(nPoint.x, nPoint.y, obj.area.area, assigned, innArea, sPoint, traceCrossing);
                } while (nPoint != null);
                nPoint = checkAllDone(obj.area.area, assigned, innArea);
            } while (nPoint != null);
        }
        return new ListArea(pxlInnEdg, assigned);
    }

    @Deprecated
    protected static ListArea innerEdges2(ROI obj) {
        ArrayList<Point> pxlInnEdg = new ArrayList<>();
        boolean[][] previousAssigned = obj.outEdge.area;
        boolean[][] assigned = new boolean[obj.getWidth()][obj.getHeight()];
        //find inner edges
        int aXw = obj.area.area.length, aYw;
        for (int xx = 0; xx < aXw; xx++) {
            aYw = obj.area.area[xx].length;
            for (int yy = 0; yy < aYw; yy++) {
                if (obj.area.area[xx][yy] && !previousAssigned[xx][yy] && !assigned[xx][yy]) {
                    if (isEdgePixel(xx, yy, obj.area.area)) {
                        assigned[xx][yy] = true;
                        pxlInnEdg.add(new Point(xx, yy));
                    }
                }
            }
        }
        return new ListArea(pxlInnEdg, assigned);
    }

    protected static ListArea maskEdges(ROI obj) {
        ArrayList<Point> pxlInnEdg = new ArrayList<>();
        boolean[][] maskArea = obj.mask.area.area;
        boolean[][] assigned = new boolean[obj.mask.getWidth()][obj.mask.getHeight()];
        //find inner edges
        int aXw = maskArea.length, aYw;
        for (int xx = 0; xx < aXw; xx++) {
            aYw = maskArea[xx].length;
            for (int yy = 0; yy < aYw; yy++) {
                if (maskArea[xx][yy] && !assigned[xx][yy]) {
                    if (isAreaEdgePixel(xx, yy, maskArea, obj.mask.getOutArea().area)) {
                        assigned[xx][yy] = true;
                        pxlInnEdg.add(new Point(xx, yy));
                    }
                }
            }
        }
        return new ListArea(pxlInnEdg, assigned);
    }

    protected static void fillHoles(ROI obj) {
        Area outerFill = obj.getOutArea();
        for (int xx = 0; xx < obj.getWidth(); xx++) {
            for (int yy = 0; yy < obj.getHeight(); yy++) {
                if (!obj.area.area[xx][yy]
                        && !outerFill.area[xx + 1][yy + 1]) {
                    obj.area.area[xx][yy] = true;
                }
            }
        }
    }

    protected static Area outerFill(ROI obj) {
        boolean[][] outFill = new boolean[obj.getWidth() + 2][obj.getHeight() + 2];
        for (int xx = 0; xx < obj.getWidth() + 1; xx++) {
            for (int yy = 0; yy < obj.getHeight() + 1; yy++) {
                try {
                    outFill[xx + 1][yy + 1] = obj.area.area[xx][yy];
                } catch (ArrayIndexOutOfBoundsException exp) {
                    outFill[xx + 1][yy + 1] = false;
                }
            }
        }
        return ImageTracer.traceFalseArea(outFill, obj.getWidth() + 2, obj.getHeight() + 2, 0);
    }

    protected static Area innerFill(ROI obj) {
        boolean[][] innFill = new boolean[obj.getWidth() + 2][obj.getHeight() + 2];
        Area outFill = obj.getOutArea();
        int fx = -1, fy = -1;
        for (int xx = 0; xx < obj.getWidth() + 1; xx++) {
            for (int yy = 0; yy < obj.getHeight() + 1; yy++) {
                try {
                    innFill[xx][yy] = !obj.area.area[xx - 1][yy - 1] && !outFill.area[xx][yy];
                    if (fx == -1) {
                        fx = xx;
                    }
                    if (fy == -1) {
                        fy = yy;
                    }
                } catch (ArrayIndexOutOfBoundsException exp) {
                    innFill[xx][yy] = false;
                }
            }
        }
        return new Area(innFill, outFill.xstart, outFill.ystart, fx, fy);
    }

    private static EdgePoint checkAllDone(boolean[][] area, boolean[][] assigned, boolean[][] outFill) {
        for (int x = 0; x < area.length; x++) {
            for (int y = 0; y < area[0].length; y++) {
                if (area[x][y] && !assigned[x][y]) {
                    if (isOuterPixel(x, y, outFill)) {
                        return new EdgePoint(x, y);
                    }
                }
            }
        }
        return null;
    }

    private static boolean localShare(boolean[][] localPoint, int xShift, int yShift) {
        for (int xx = 0 + xShift; xx < 3 + xShift; xx++) {
            for (int yy = 0 + yShift; yy < 3 + yShift; yy++) {
                try {
                    if (localPoint[xx][yy]) {
                        return true;
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
            }
        }
        return false;
    }

    private static EdgePoint findNextEdgePixel(int x, int y, boolean[][] area, boolean[][] assigned, boolean[][] outarea, Point startPoint, Stack<Point> traceCrossing) {
        boolean[][] localPoint = new boolean[3][3];
        boolean right = isUnassignedEdgePixel(x + 1, y, area, assigned, outarea);
        boolean down = isUnassignedEdgePixel(x, y + 1, area, assigned, outarea);
        boolean left = isUnassignedEdgePixel(x - 1, y, area, assigned, outarea);
        boolean up = isUnassignedEdgePixel(x, y - 1, area, assigned, outarea);
        boolean[] allBools = new boolean[]{right, down, left, up};
        int[] allDirs = IntStream.range(0, allBools.length).map(i -> allBools[i] ? 1 : 0).toArray();
        switch (Arrays.stream(allDirs).sum()) {
            case 1:
                // vain yksi
                if (right) {
                    return new EdgePoint(x + 1, y);
                } else if (down) {
                    return new EdgePoint(x, y + 1);
                } else if (left) {
                    return new EdgePoint(x - 1, y);
                } else if (up) {
                    return new EdgePoint(x, y - 1);
                }
                break;
            case 0:
                // kaikki negatiivisia
                if (!traceCrossing.isEmpty() && !(new Point(x, y).equals(startPoint))) {
                    Point retPoint = traceCrossing.pop();
                    x = retPoint.x;
                    y = retPoint.y;
                    return new EdgePoint(x, y);
                } else {
                    return null;
                }
            default:
                //useampi plussa
                traceCrossing.push(new Point(x, y));
                localPoint = fillInLocal(localPoint, x, y, area);
                if (left && localShare(localPoint, -1, 0)) {
                    return new EdgePoint(x - 1, y);
                } else if (up && localShare(localPoint, 0, -1)) {
                    return new EdgePoint(x, y - 1);
                } else if (right && localShare(localPoint, 1, 0)) {
                    return new EdgePoint(x + 1, y);
                } else if (down && localShare(localPoint, 0, 1)) {
                    return new EdgePoint(x, y + 1);
                } else {
                    if (left) {
                        return new EdgePoint(x - 1, y);
                    } else if (up) {
                        return new EdgePoint(x, y - 1);
                    } else if (right) {
                        return new EdgePoint(x + 1, y);
                    } else if (down) {
                        return new EdgePoint(x, y + 1);
                    }
                }
                break;
        }
        return null;
    }

    private static boolean isUnassignedEdgePixel(int x, int y, boolean[][] area, boolean[][] assigned, boolean[][] outarea) {
        try {
            return area[x][y] && !assigned[x][y]
                    && (isAreaEdgePixel(x, y, area, outarea));
        } catch (IndexOutOfBoundsException exp) {
            return false;
        }
    }

    private static boolean isOuterPixel(int x, int y, boolean[][] outFill) {
        try {
            for (int yy = y - 1; yy < y + 2; yy++) {
                for (int xx = x - 1; xx < x + 2; xx++) {
                    if (outFill[xx + 1][yy + 1]) {
                        return true;
                    }
                }
            }
        } catch (IndexOutOfBoundsException exp) {
            return true;
        }
        return false;
    }

    private static boolean isEdgePixel(int x, int y, boolean[][] area) {
        try {
            for (int yy = y - 1; yy < y + 2; yy++) {
                for (int xx = x - 1; xx < x + 2; xx++) {
                    if (!area[xx][yy]) {
                        return true;
                    }
                }
            }
        } catch (IndexOutOfBoundsException exp) {
            return true;
        }
        return false;
    }

    private static boolean isAreaEdgePixel(int x, int y, boolean[][] area, boolean[][] outarea) {
        for (int yy = y - 1; yy < y + 2; yy++) {
            for (int xx = x - 1; xx < x + 2; xx++) {
                try {
                    if (!area[xx][yy] && outarea[xx + 1][yy + 1]) {
                        return true;
                    }
                } catch (IndexOutOfBoundsException exp) {
                    return outarea[xx + 1][yy + 1];
                }
            }
        }
        return false;
    }

    private static boolean[][] fillInLocal(boolean[][] localPoint, int x, int y, boolean[][] area) {
        for (int xx = 0; xx < 3; xx++) {
            for (int yy = 0; yy < 3; yy++) {
                try {
                    localPoint[xx][yy] = !area[x - 1 + xx][y - 1 + yy];
                } catch (IndexOutOfBoundsException ex) {
                    localPoint[xx][yy] = false;
                }
            }
        }
        return localPoint;
    }

}
