package mainPackage.utils;

import java.awt.Point;
import java.util.List;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import mainPackage.morphology.DoublePoint;

public class SHAPE {

    public final static int RASTER_THRESHOLD = 96;
    public final static double RASTER_LINE = 2.5;
    public final static double RASTER_DOT = 2.5;
    final static double RASTER_X_OFFSET = 0.5;
    final static double RASTER_Y_OFFSET = 0.5;

    public static Shape generateCircle(Point p, double radius, boolean rasterized) {
        return new Circle(p.getX() + (rasterized ? RASTER_X_OFFSET : 0), p.getY() + (rasterized ? RASTER_Y_OFFSET : 0), radius);
    }

    public static Shape generateOval(Point p1, Point p2, Point p3, boolean rasterized) {
        CubicCurve bezier = new CubicCurve();
        List<Point> npoints;
        if (rasterized && (p1.x == p2.x)) {
            npoints = DRAW.ovalPoints(
                    new DoublePoint(p1.getX() + RASTER_X_OFFSET, p1.getY()),
                    new DoublePoint(p2.getX() + RASTER_X_OFFSET, p2.getY()),
                    new DoublePoint(p3.getX() + RASTER_X_OFFSET, p3.getY()));
        } else if (rasterized && (p1.y == p2.y)) {
            npoints = DRAW.ovalPoints(
                    new DoublePoint(p1.getX(), p1.getY() + RASTER_Y_OFFSET),
                    new DoublePoint(p2.getX(), p2.getY() + RASTER_Y_OFFSET),
                    new DoublePoint(p3.getX(), p3.getY() + RASTER_Y_OFFSET));
        } else {
            npoints = DRAW.ovalPoints(p1, p2, p3);
        }
        bezier.setStartX(npoints.get(3).getX());
        bezier.setStartY(npoints.get(3).getY());
        bezier.setControlX1(npoints.get(4).getX());
        bezier.setControlX2(npoints.get(1).getX());
        bezier.setControlY1(npoints.get(4).getY());
        bezier.setControlY2(npoints.get(1).getY());
        bezier.setEndX(npoints.get(0).getX());
        bezier.setEndY(npoints.get(0).getY());
        CubicCurve bezier2 = new CubicCurve();
        bezier2.setStartX(npoints.get(0).getX());
        bezier2.setStartY(npoints.get(0).getY());
        bezier2.setControlX1(npoints.get(2).getX());
        bezier2.setControlX2(npoints.get(5).getX());
        bezier2.setControlY1(npoints.get(2).getY());
        bezier2.setControlY2(npoints.get(5).getY());
        bezier2.setEndX(npoints.get(3).getX());
        bezier2.setEndY(npoints.get(3).getY());
        Shape shape = Shape.union(bezier, bezier2);
        return shape;
    }

    public static Shape generateRectangle(Point p1, Point p2, Point p3, boolean rasterized) {
        Polygon polygon = new Polygon();
        List<Point> points = DRAW.rectanglePoints(p1, p2, p3);
        polyPoints(polygon.getPoints(), points, rasterized);
        return polygon;
    }

    public static Shape generatePolygon(List<Point> points, boolean rasterized) {
        Polygon polygon = new Polygon();
        polyPoints(polygon.getPoints(), points, rasterized);
        return polygon;
    }

    public static Shape generateAngle(Point p1, Point p2, Point p3, double thickness, boolean rasterized) {
        double[] arcPoints = rasterized
                ? (DRAW.arcPoints(
                        new DoublePoint(p1.getX() + RASTER_X_OFFSET, p1.getY() + RASTER_Y_OFFSET),
                        new DoublePoint(p2.getX() + RASTER_X_OFFSET, p2.getY() + RASTER_Y_OFFSET),
                        new DoublePoint(p3.getX() + RASTER_X_OFFSET, p3.getY() + RASTER_Y_OFFSET)))
                : (DRAW.arcPoints(p1, p2, p3));
        Arc arc = new Arc(p1.getX() + (rasterized ? RASTER_X_OFFSET : 0), p1.getY() + (rasterized ? RASTER_Y_OFFSET : 0),
                arcPoints[0], arcPoints[0], arcPoints[2], arcPoints[3]);
        arc.setType(ArcType.ROUND);
        Line line1 = rasterized
                ? new Line(p1.getX() + RASTER_X_OFFSET, p1.getY() + RASTER_Y_OFFSET, p2.getX() + RASTER_X_OFFSET, p2.getY() + RASTER_Y_OFFSET)
                : new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        line1.setStrokeWidth(thickness);
        line1.setStrokeLineCap(StrokeLineCap.ROUND);
        line1.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Line line2 = rasterized
                ? new Line(p1.getX() + RASTER_X_OFFSET, p1.getY() + RASTER_Y_OFFSET, p3.getX() + RASTER_X_OFFSET, p3.getY() + RASTER_Y_OFFSET)
                : new Line(p1.getX(), p1.getY(), p3.getX(), p3.getY());
        line2.setStrokeWidth(thickness);
        line2.setStrokeLineCap(StrokeLineCap.ROUND);
        line2.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Shape shape = Shape.union(line1, line2);
        shape = Shape.union(arc, shape);
        return shape;
    }

    public static Shape generateLine(Point p1, Point p2, double thickness, boolean rasterized) {
        Line line = rasterized
                ? new Line(p1.x + RASTER_X_OFFSET, p1.y + RASTER_Y_OFFSET, p2.x + RASTER_X_OFFSET, p2.y + RASTER_Y_OFFSET)
                : new Line(p1.x, p1.y, p2.x, p2.y);
        line.setStrokeWidth(thickness);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return line;
    }

    public static Shape generatePolyline(List<Point> points, StrokeLineCap cap, double thickness, boolean rasterized) {
        Polyline polyline = new Polyline();
        polyline.setStrokeWidth(thickness);
        polyline.setStrokeLineCap(cap);
        polyline.setStrokeLineJoin(StrokeLineJoin.ROUND);
        polyPoints(polyline.getPoints(), points, rasterized);
        return polyline;
    }

    private static void polyPoints(ObservableList<Double> polypoints, List<Point> newpoints, boolean rasterized) {
        if (rasterized) {
            polypoints.addAll(newpoints.stream().flatMap(p -> Stream.of(p.getX() + RASTER_X_OFFSET, p.getY() + RASTER_Y_OFFSET)).toArray(Double[]::new));
        } else {
            polypoints.addAll(newpoints.stream().flatMap(p -> Stream.of(p.getX(), p.getY())).toArray(Double[]::new));
        }
    }
}
