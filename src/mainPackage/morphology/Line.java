package mainPackage.morphology;

import java.awt.Point;
import mainPackage.Tonga;
import mainPackage.utils.GEO;

public class Line {

    // y = xk + b;
    public double k;
    public double b;
    public Point start = null;
    public Point end = null;

    Line(int x, int y, double dir) {
        k = GEO.getSlope(dir);
        b = GEO.getLineConstant(x, y, k);
    }

    public static Point intersection(Line line1, Line line2) {
        // if Line-instances have start/end data it is considered
        double x, y;
        if ((vertical(line1) || horizontal(line1)) && (vertical(line2) || horizontal(line2))) {
            x = Double.NaN;
            y = Double.NaN;
        } else if (vertical(line1) || vertical(line2)) {
            double[] pos = verticalIntersection(line1, line2);
            x = pos[0];
            y = pos[1];
        } else if (horizontal(line1) || horizontal(line2)) {
            double[] pos = horizontalIntersection(line1, line2);
            x = pos[0];
            y = pos[1];
        } else {
            x = (line2.b - line1.b) / (line1.k - line2.k);
            y = line1.k * x + line1.b;
        }
        if (nonMatching(x, y)) {
            return null;
        } else {
            if (intersectionInsideLineRange(x, y, line1, line2)) {
                return new Point((int) Math.round(x), (int) Math.round(-y));
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
        return (line1.start != null && line1.end != null && line2.start != null && line2.end != null)
                && pointIsOnLineBox(x, -y, line1) && pointIsOnLineBox(x, -y, line2);
    }

    @Override
    public String toString() {
        return "Line{" + "k=" + k + ", b=" + b + '}';
    }
}
