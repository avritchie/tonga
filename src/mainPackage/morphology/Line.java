package mainPackage.morphology;

import java.awt.Point;
import mainPackage.Tonga;
import mainPackage.utils.GEO;

public class Line {

    // y = xk + b;
    public double k;
    public double b;
    public double length;
    public double direction;
    public Point start = null;
    public Point end = null;
    public Point mid = null;

    public Line(int x, int y, double dir) {
        k = GEO.getSlope(dir);
        b = GEO.getLineConstant(x, y, k);
        direction = dir;
    }

    public Line(double k, double b) {
        this.k = k;
        this.b = b;
    }

    public Line(Point s, Point e) {
        start = s;
        end = e;
        mid = GEO.getMidPoint(s, e);
        length = GEO.getDist(s, e);
        direction = GEO.getDirection(s, e);
        k = GEO.getSlope(direction);
        b = GEO.getLineConstant(start.x, start.y, k);
    }

    public void setStartEndPoints(Point midpoint, double distance) {
        double x = midpoint.getX();
        double y = midpoint.getY();
        if (Double.isInfinite(k)) {
            start = new DoublePoint(Math.round(x), Math.round(y + distance));
            end = new DoublePoint(Math.round(x), Math.round(y - distance));
        } else {
            double c = GEO.hypotenuse(1, k);
            double r = distance / c;
            start = new DoublePoint(Math.round(x - r), Math.round(y + (k * r)));
            end = new DoublePoint(Math.round(x + r), Math.round(y - (k * r)));
        }
        mid = midpoint;
        Point pp = GEO.getPointInDirection(midpoint, direction, distance);
        if (GEO.getDist(pp, start) < GEO.getDist(pp, end)) {
            Point t = start;
            start = end;
            end = t;
        }
        length = GEO.getDist(start, end);
        direction = GEO.getDirection(start, end);
    }

    public Line getPerpendicularAlong(double distance) {
        Point p = GEO.getPointInDirection(start, direction, distance);
        return getPerpendicular(p);
    }

    public Line getPerpendicular(Point p) {
        double x = p.getX();
        double y = p.getY();
        Line l;
        if (k == 0) {
            l = new Line(new Point((int) x, Integer.MAX_VALUE), new Point((int) x, Integer.MIN_VALUE));
        } else {
            double kn = -1 / k;
            double bn = -(x * kn + y);
            l = new Line(kn, bn);
        }
        l.mid = p;
        return l;
    }

    public Point getPerpendicularIntersection(Point p) {
        Line sl = getPerpendicular(p);
        return Line.intersectionFree(sl, this);
    }

    public static Point intersectionFree(Line line1, Line line2) {
        // not restricted by start/end data
        double x, y;
        boolean line1Vertical = vertical(line1), line1Horizontal = horizontal(line1), line2Vertical = vertical(line2), line2Horizontal = horizontal(line2);
        if ((line1Vertical || line1Horizontal) && (line2Vertical || line2Horizontal)) {
            if (line1Vertical && line2Horizontal) {
                x = line1.b;
                y = line2.b;
            } else if (line2Vertical && line1Horizontal) {
                x = line2.b;
                y = line1.b;
            } else {
                x = Double.NaN;
                y = Double.NaN;
            }
        } else if (line1Vertical || line2Vertical) {
            double[] pos = verticalIntersection(line1, line2);
            x = pos[0];
            y = pos[1];
        } else if (line1Horizontal || line2Horizontal) {
            double[] pos = horizontalIntersection(line1, line2);
            x = pos[0];
            y = pos[1];
        } else {
            x = (line2.b - line1.b) / (line1.k - line2.k);
            y = line1.k * x + line1.b;
        }
        return new DoublePoint(x, -y);
    }

    public static Point intersection(Line line1, Line line2) {
        // if Line-instances have start/end data it is considered
        Point p = intersectionFree(line1, line2);
        if (nonMatching(p.x, -p.y)) {
            return null;
        } else {
            if (intersectionInsideLineRange(p.x, -p.y, line1, line2)) {
                return p;
            } else {
                return null;
            }
        }
    }

    private static boolean pointIsOnLineBox(double x, double y, Line line) {
        boolean isOnX, isOnY;
        int sx = line.start.x, sy = line.start.y;
        int ex = line.end.x, ey = line.end.y;
        if (sx > ex) {
            isOnX = x <= sx && x >= ex;
        } else {
            isOnX = x >= sx && x <= ex;
        }
        if (sy > ey) {
            isOnY = y <= sy && y >= ey;
        } else {
            isOnY = y >= sy && y <= ey;
        }
        return isOnX && isOnY;
    }

    private static boolean vertical(Line l) {
        return l.k == Double.POSITIVE_INFINITY || l.k == Double.NEGATIVE_INFINITY;
    }

    private static boolean horizontal(Line l) {
        return l.k <= 0.0000001 && l.k >= -0.0000001;
    }

    private static double[] verticalIntersection(Line line1, Line line2) {
        double x, y;
        Tonga.log.trace("Vertical intersection");
        if (vertical(line1)) {
            x = line1.start.x;
            y = line2.k * x + line2.b;
        } else {
            x = line2.start.x;
            y = line1.k * x + line1.b;
        }
        return new double[]{x, y};
    }

    private static double[] horizontalIntersection(Line line1, Line line2) {
        double x, y;
        Tonga.log.trace("Horizontal intersection");
        if (horizontal(line1)) {
            y = -line1.start.y;
            x = (y - line2.b) / line2.k;
        } else {
            y = -line2.start.y;
            x = (y - line1.b) / line1.k;
        }
        return new double[]{x, y};
    }

    private static boolean nonMatching(double x, double y) {
        // is interjection out of range = lines dont intersect
        return (x == Double.NaN || y == Double.NaN
                || Math.abs(x) == Double.POSITIVE_INFINITY || Math.abs(y) == Double.POSITIVE_INFINITY
                || (int) x == Integer.MAX_VALUE || (int) x == Integer.MIN_VALUE
                || (int) y == Integer.MAX_VALUE || (int) y == Integer.MIN_VALUE);
    }

    private static boolean intersectionInsideLineRange(double x, double y, Line line1, Line line2) {
        Tonga.log.trace("Intersection:\n{}\n{}\nFound at {}.{}", line1, line2, x, y);
        return ((line1.start == null || line1.end == null || pointIsOnLineBox(x, -y, line1))
                && (line2.start == null || line2.end == null || pointIsOnLineBox(x, -y, line2)));
    }

    @Override
    public String toString() {
        return "Line{" + "k=" + k + ", b=" + b + '}';
    }
}
