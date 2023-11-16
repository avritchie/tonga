package mainPackage.utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import mainPackage.ImageData;
import mainPackage.morphology.DoublePoint;
import mainPackage.morphology.Line;

public class DRAW {

    public static void redDot(ImageData drawhere, int x, int y) {
        int w = drawhere.width;
        int h = drawhere.height;
        drawhere.pixels32[y * w + x] = COL.WHITE;
        if (x > 0) {
            drawhere.pixels32[y * w + x - 1] = COL.RED;
        }
        if (w - x > 1) {
            drawhere.pixels32[y * w + x + 1] = COL.RED;
        }
        if (y > 0) {
            drawhere.pixels32[(y - 1) * w + x] = COL.RED;
        }
        if (h - y > 1) {
            drawhere.pixels32[(y + 1) * w + x] = COL.RED;
        }
    }

    public static List<Point> ovalPoints(Point p1, Point p2, Point p3) {
        double w = GEO.getDist(p1, p2) / 2;
        double h = GEO.getPerpendicularDistance(p1, p2, p3);
        double rad = Math.toRadians(-GEO.getDirection(p1, p2));
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double sinh = sin * h, cosh = cos * h;
        double cosw = cos * w * (4 / 3.), sinw = sin * w * (4 / 3.);
        Point mp = GEO.getMidPoint(p1, p2);
        List<Point> lp = new ArrayList<>();
        Point tm = new DoublePoint(mp.getX() - sinh, mp.getY() + cosh);
        lp.add(tm);
        lp.add(new DoublePoint(tm.getX() + cosw, tm.getY() + sinw));
        lp.add(new DoublePoint(tm.getX() - cosw, tm.getY() - sinw));
        Point bm = new DoublePoint(mp.getX() + sinh, mp.getY() - cosh);
        lp.add(bm);
        lp.add(new DoublePoint(bm.getX() + cosw, bm.getY() + sinw));
        lp.add(new DoublePoint(bm.getX() - cosw, bm.getY() - sinw));
        return lp;
    }

    public static List<Point> rectanglePoints(Point p1, Point p2, Point p3) {
        double ld = GEO.getDirection(p1, p2);
        Point pp = new Line(p1.x, p1.y, ld).getPerpendicularIntersection(p3);
        double h = GEO.getDist(pp, p3);
        double dr = GEO.getDirection(pp, p3);
        List<Point> lp = new ArrayList<>();
        lp.add(p1);
        lp.add(GEO.getPointInDirection(p1, dr, h));
        lp.add(GEO.getPointInDirection(p2, dr, h));
        lp.add(p2);
        return lp;
    }

    public static double[] arcPoints(Point p1, Point p2, Point p3) {
        double d1 = GEO.getDist(p1, p2);
        double d2 = GEO.getDist(p1, p3);
        int radius = (int) (Math.min(Math.min(d1, d2), 300));
        double startDirection = GEO.getDirection(p1, p2);
        double endDirection = GEO.getDirection(p1, p3);
        double offset = radius / 2.;
        double totalAngle = (endDirection > startDirection) ? 360 - (endDirection - startDirection) : startDirection - endDirection;
        return new double[]{offset, radius, startDirection, -totalAngle};
    }

    public static class canvasDrawer {

        public static void drawDot(GraphicsContext cont, Point p, double size, double[] cv) {
            double xs = (p.x - cv[0]) * cv[4] - size / 2 + cv[4] / 2 + cv[2];
            double ys = (p.y - cv[1]) * cv[5] - size / 2 + cv[5] / 2 + cv[3];
            cont.fillOval(xs, ys, size, size);
        }

        public static void drawArc(GraphicsContext cont, Point p1, Point p2, Point p3, double size, double[] cv) {
            double[] arcPoints = arcPoints(p1, p2, p3);
            Object[] xys = getDoubleCoordinates(p1, p2, cv);
            cont.fillArc((double) xys[0] - arcPoints[0] * size, (double) xys[1] - arcPoints[0] * size, arcPoints[1] * size, arcPoints[1] * size, arcPoints[2], arcPoints[3], ArcType.ROUND);
        }

        public static void drawLine(GraphicsContext cont, Point p1, Point p2, double size, double[] cv) {
            Object[] xys = getDoubleCoordinates(p1, p2, cv);
            cont.setLineWidth(size);
            cont.strokeLine((double) xys[0], (double) xys[1], (double) xys[2], (double) xys[3]);
        }

        public static void drawPolyLine(GraphicsContext cont, List<Point> p, double size, double[] cv) {
            Object[] xys = getDoubleCoordinates(p, cv);
            cont.setLineWidth(size);
            cont.strokePolyline((double[]) xys[0], (double[]) xys[1], ((double[]) xys[0]).length);
        }

        public static void drawPolygon(GraphicsContext cont, List<Point> p, double size, double[] cv) {
            Object[] xys = getDoubleCoordinates(p, cv);
            cont.setLineWidth(size);
            cont.strokePolygon((double[]) xys[0], (double[]) xys[1], ((double[]) xys[0]).length);
        }

        public static void drawOval(GraphicsContext cont, Point p1, Point p2, Point p3, double size, double[] cv) {
            List<Point> lp = ovalPoints(p1, p2, p3);
            Object[] xys = getDoubleCoordinates(lp, cv);
            double[] xxs = (double[]) xys[0];
            double[] yys = (double[]) xys[1];
            cont.setLineWidth(size);
            cont.beginPath();
            cont.moveTo(xxs[3], yys[3]);
            cont.bezierCurveTo(xxs[4], yys[4], xxs[1], yys[1], xxs[0], yys[0]);
            cont.bezierCurveTo(xxs[2], yys[2], xxs[5], yys[5], xxs[3], yys[3]);
            cont.closePath();
            cont.stroke();
        }

        public static void drawRectangle(GraphicsContext cont, Point p1, Point p2, Point p3, double size, double[] cv) {
            /*int w = (p2.x - p1.x);
            int h = (p2.y - p1.y);
            double y = GEO.hypotenuse(w, h);
            List<Point> lp = new ArrayList<>();
            lp.add(p1);
            if (p3 != null) {
                double a = GEO.getDirection(p1, p3);
                double an = GEO.getDirection(p1, p2);
                double ana = GEO.getDirDifference(GEO.getParallelAngle(an), 180);
                double anb = GEO.getDirDifference(GEO.getParallelAngle(an), 90);
                Point lmt = GEO.getPointInDirection(p1, a, y);
                lp.add(GEO.getPointInDirection(lmt, GEO.getParallelAngle(a) - anb, h));
                lp.add(lmt);
                lp.add(GEO.getPointInDirection(lmt, GEO.getParallelAngle(a) + ana, w));
            } else {
                lp.add(new DoublePoint((p1.x + w), (p1.y)));
                lp.add(new DoublePoint((p1.x + w), (p1.y + h)));
                lp.add(new DoublePoint((p1.x), (p1.y + h)));
            }*/
            List<Point> lp = rectanglePoints(p1, p2, p3);
            Object[] xys = getDoubleCoordinates(lp, cv);
            cont.strokePolygon((double[]) xys[0], (double[]) xys[1], ((double[]) xys[0]).length);
            cont.setLineWidth(size);
        }

        public static void drawCircle(GraphicsContext cont, Point p1, Point p2, double size, double[] cv) {
            Object[] xys = getDoubleCoordinates(p1, p2, cv);
            cont.setLineWidth(size);
            Point pp1 = new DoublePoint((double) xys[0], (double) xys[1]);
            Point pp2 = new DoublePoint((double) xys[2], (double) xys[3]);
            Point mp = GEO.getMidPoint(pp1, pp2);
            double r = GEO.getDist(pp1, pp2) / 2.;
            cont.strokeOval(mp.getX() - r, mp.getY() - r, r * 2, r * 2);
        }

        private static Object[] getDoubleCoordinates(List<Point> p, double[] cv) {
            double[] xs = new double[p.size()];
            double[] ys = new double[p.size()];
            for (int i = 0; i < p.size(); i++) {
                xs[i] = (p.get(i).x - cv[0]) * cv[4] + cv[4] / 2 + cv[2];
                ys[i] = (p.get(i).y - cv[1]) * cv[5] + cv[5] / 2 + cv[3];
            }
            return new Object[]{xs, ys};
        }

        private static Object[] getDoubleCoordinates(Point p1, Point p2, double[] cv) {
            double xss = (p1.x - cv[0]) * cv[4] + cv[4] / 2 + cv[2];
            double yss = (p1.y - cv[1]) * cv[5] + cv[5] / 2 + cv[3];
            double xse = (p2.x - cv[0]) * cv[4] + cv[4] / 2 + cv[2];
            double yse = (p2.y - cv[1]) * cv[5] + cv[5] / 2 + cv[3];
            return new Object[]{xss, yss, xse, yse};
        }

    }

    public abstract static class lineDrawer {

        public boolean keepOnGoing;
        public Object returnData;

        public abstract void action(int x, int y);

        public Object drawLine(Point cur, Point comp) {
            keepOnGoing = true;
            int x1 = cur.x, x2 = comp.x, y1 = cur.y, y2 = comp.y;
            int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
            int dx2 = 2 * dx, dy2 = 2 * dy;
            int ix = x1 < x2 ? 1 : -1, iy = y1 < y2 ? 1 : -1;
            int d = 0, x = x1, y = y1;
            boolean mode = dx >= dy;
            while (keepOnGoing) {
                action(x, y);
                if (mode) {
                    if (x == x2) {
                        break;
                    }
                    x += ix;
                    d += dy2;
                    if (d > dx) {
                        y += iy;
                        d -= dx2;
                    }
                } else {
                    if (y == y2) {
                        break;
                    }
                    y += iy;
                    d += dx2;
                    if (d > dy) {
                        x += ix;
                        d -= dy2;
                    }
                }
            }
            return returnData();
        }

        public void abortLineDrawing() {
            keepOnGoing = false;
        }

        public Object returnData() {
            return null;
        }
    }
}
