package mainPackage.utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import mainPackage.morphology.DoublePoint;
import mainPackage.morphology.EdgePoint;
import mainPackage.morphology.Line;

public class GEO {

    public static Point getMidPoint(Point p1, Point p2) {
        return new DoublePoint((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
    }

    public static double getAngle(Point crossing, Point previous, Point next) {
        return getAngle(crossing.x, crossing.y, previous.x, previous.y, next.x, next.y);
    }

    public static double getDirection(Point a, Point b) {
        double d = Math.toDegrees(Math.atan2(b.x - a.x, b.y - a.y)) - 90;
        return d < 0 ? d + 360 : d;
    }

    public static Point getCenterPoint(List<Point> list) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Point p = list.get(i);
            minX = Math.min(p.x, minX);
            minY = Math.min(p.y, minY);
            maxX = Math.max(p.x, maxX);
            maxY = Math.max(p.y, maxY);
        }
        return getMidPoint(new Point(minX, minY), new Point(maxX, maxY));
    }

    public static Point createCommonMidpoint(List<EdgePoint> points) {
        // the central point of the line intersections of these points
        List<Point> iss = new ArrayList<>();
        EdgePoint p1, p2;
        for (int i = 0; i < points.size(); i++) {
            p1 = points.get(i);
            for (int j = 0; j < points.size(); j++) {
                p2 = points.get(j);
                if (!p1.equals(p2)) {
                    Point is = Line.intersection(p1.line, p2.line);
                    if (is != null) {
                        iss.add(is);
                    }
                }
            }
        }
        iss = iss.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        return GEO.getCenterPoint(iss);
    }

    public static double getParallelFactor(EdgePoint p1, EdgePoint p2) {
        return getParallelFactor(p1.direction, p2.direction);
    }

    public static double getParallelFactor(double p1, double p2) {
        // how antiparallel (180 vs 0) the angles are? 0=100%, 0.5=0%
        return Math.abs(Math.abs(Math.abs(((p1 - 180 - p2) / 360.)) - 0.5) - 0.5);
    }

    public static double getSlope(double angle) {
        double rad = Math.toRadians(angle);
        double kx;
        if (rad == Math.PI) {
            kx = 0;
        } else {
            kx = Math.sin(rad) / Math.cos(rad);
        }
        if (kx > Integer.MAX_VALUE) {
            return Double.POSITIVE_INFINITY;
        }
        if (kx < Integer.MIN_VALUE) {
            return Double.NEGATIVE_INFINITY;
        }
        return kx;
    }

    public static double getLineConstant(int x, int y, double k) {
        if (k == Double.POSITIVE_INFINITY || k == Double.NEGATIVE_INFINITY) {
            return x;
        } else {
            return -y - k * x;
        }
    }

    public static Point getPointInDirection(Point startPoint, double angle, double distance) {
        double rad = Math.toRadians(angle);
        double fx = (Math.cos(rad) * distance);
        double fy = (-Math.sin(rad) * distance);
        return new DoublePoint(startPoint.x + fx, startPoint.y + fy);
    }

    public static Point getDirectionSigns(int angle) {
        double rad = Math.toRadians(angle);
        int fx = (int) Math.signum(Math.cos(rad));
        int fy = (int) Math.signum(-Math.sin(rad));
        return new Point(fx, fy);
    }

    public static double getPerpendicularDistance(Point lineStart, Point lineEnd, Point space) {
        double ld = getDirection(lineStart, lineEnd);
        Point pp = new Line(lineStart.x, lineStart.y, ld).getPerpendicularIntersection(space);
        return getDist(pp, space);
    }

    public static double hypotenuse(double a, double b) {
        return Math.sqrt(a * a + b * b);
    }

    public static double ellipsePerimeter(double largeRadius, double smallRadius) {
        return Math.PI * (3 * (smallRadius + largeRadius) - Math.sqrt((3 * smallRadius + largeRadius) * (3 * largeRadius + smallRadius)));
    }

    public static double circleCircumference(double area) {
        return Math.sqrt(area / Math.PI) * 2 * Math.PI;
    }

    public static double ellipseArea(double largeRadius, double smallRadius) {
        return Math.PI * smallRadius * largeRadius;
    }

    public static double circleArea(double diameter) {
        return Math.PI * Math.pow(diameter / 2., 2);
    }

    public static double polygonArea(Point[] points) {
        int p = points.length;
        double area = 0;
        for (int i = 0; i < p; i++) {
            Point nextp = points[(i + 1) % p];
            Point prevp = points[(i + p - 1) % p];
            area += points[i].x * (nextp.y - prevp.y);
        }
        return 0.5 * Math.abs(area);
    }

    public static double planeArea(Point[] points, int span) {
        int p = points.length;
        double area = 0;
        for (int i = 0; i < p - 1; i++) {
            area += getDist(points[i], points[i + 1]) * span;
            if (i < p - 2) {
                double angle = 180 - getAngle(points[i + 1], points[i], points[i + 2]);
                double radp = Math.pow(span / 2., 2);
                area += Math.PI * radp * (angle / 360.);
                area -= (radp * Math.sin(Math.toRadians(angle / 2))) / (2 * Math.sin(Math.toRadians(90 - angle / 2))) * 2;
            }
        }
        return area;
    }

    public static double getAngle(int x1, int y1, int x2, int y2, int x3, int y3) {
        double sideA, sideB, sideC;
        sideA = getDist(x2, y2, x3, y3);
        sideB = getDist(x1, y1, x3, y3);
        sideC = getDist(x1, y1, x2, y2);
        double rad = Math.acos((Math.pow(sideB, 2) + (Math.pow(sideC, 2) - (Math.pow(sideA, 2)))) / (2 * sideB * sideC));
        if (Double.isNaN(rad)) {
            return 180;
        }
        return Math.toDegrees(rad);
    }

    public static double getParallelAngle(double angle) {
        return angle + 180 - (Math.floor((angle + 180) / 360) * 360);
    }

    public static double getPerpendicularAngle(double angle) {
        return angle + 90 - (Math.floor((angle + 90) / 360) * 360);
    }

    public static double getDist(Point p1, Point p2) {
        return getDist(p1.x, p1.y, p2.x, p2.y);
    }

    public static double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + (Math.pow(y1 - y2, 2)));
    }

    public static double getDirDifference(double angl1, double angl2) {
        return getListDifference(angl1, angl2, 360);
    }

    public static double getListDifference(double angl1, double angl2, int max) {
        double diff = Math.abs(angl2 - angl1) % max;
        return diff > (max / 2) ? max - diff : diff;
    }

    public static double triangleSide(double sidea, double sideb, double anglec) {
        //get the third side of a triangle when side a, side b, and angle c are known
        double rad = Math.toRadians(anglec);
        return Math.sqrt(Math.pow(sidea, 2) + Math.pow(sideb, 2) - (2 * sidea * sideb * Math.cos(rad)));
    }

    public static double getDirBetween(double direction1, double direction2) {
        double dd1 = direction1 - direction2;
        dd1 = dd1 < 0 ? dd1 + 360 : dd1;
        double dd2 = direction2 - direction1;
        dd2 = dd2 < 0 ? dd2 + 360 : dd2;
        boolean order = dd1 < dd2;
        double span = order ? dd1 / 2 : dd2 / 2;
        if (order) {
            dd1 = direction2 + span;
            return dd1 % 360;
        } else {
            dd2 = direction1 + span;
            return dd2 % 360;
        }
    }

}
