package mainPackage;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import mainPackage.TongaAnnotator.AnnotationType;
import static mainPackage.TongaAnnotator.DOTSIZE;
import static mainPackage.TongaAnnotator.LINESIZE;
import static mainPackage.TongaAnnotator.AnnotationType.*;
import mainPackage.morphology.Area;
import mainPackage.utils.COL;
import mainPackage.utils.DRAW;
import mainPackage.utils.GEO;
import mainPackage.utils.SHAPE;

public class TongaAnnotation {

    public List<Point> points;
    private Color color;
    private AnnotationType type;
    private Shape shape; //geometric shape for collision detection (if r=1,w=2)
    private Node node; //rasterized node for masking (if r=1,w=3)
    private int span; //the size of the annotation (line width/circle diameter)
    private double length; //the length of the line
    private double area; //the size of the covered area
    private int width; //width range of all points/area
    private int height; //height range of all points/area
    private int nodes; //the number of points
    private double angle; //the total angle sum
    private int group; //user-defined group

    protected boolean hover;
    protected boolean select;
    protected boolean partial;

    public TongaAnnotation(Color color, int span, int group, AnnotationType type, Point firstPoint) {
        this(color, span, group, type);
        this.points = new ArrayList<>();
        this.points.add(firstPoint);
        this.partial = true;
    }

    public TongaAnnotation(Color color, int span, int group, AnnotationType type, List<Point> points) {
        this(color, span, group, type);
        this.points = points;
    }

    private TongaAnnotation(Color color, int span, int group, AnnotationType type) {
        this.color = color;
        this.type = type;
        this.span = span;
        this.group = group;
        this.hover = false;
        this.select = false;
    }

    protected void generateShape() {
        double r;
        switch (type) {
            case DOT:
                r = TongaAnnotator.DOTSIZE / 2.;
                shape = SHAPE.generateCircle(points.get(0), r, false);
                node = SHAPE.generateCircle(points.get(0), SHAPE.RASTER_DOT, true);
                break;
            case RADIUS:
                r = span / 2.;
                shape = SHAPE.generateCircle(points.get(0), r, false);
                node = SHAPE.generateCircle(points.get(0), r, true);
                break;
            case LINE:
                shape = SHAPE.generateLine(points.get(0), points.get(1), TongaAnnotator.LINESIZE, false);
                node = SHAPE.generateLine(points.get(0), points.get(1), SHAPE.RASTER_LINE, true);
                break;
            case CIRCLE:
                Point mp = GEO.getMidPoint(points.get(0), points.get(1));
                r = GEO.getDist(points.get(0), points.get(1)) / 2.;
                shape = SHAPE.generateCircle(mp, r, false);
                node = SHAPE.generateCircle(mp, r, true);
                break;
            case OVAL:
                shape = SHAPE.generateOval(points.get(0), points.get(1), points.get(2), false);
                node = SHAPE.generateOval(points.get(0), points.get(1), points.get(2), true);
                break;
            case RECTANGLE:
                shape = SHAPE.generateRectangle(points.get(0), points.get(1), points.get(2), false);
                node = SHAPE.generateRectangle(points.get(0), points.get(1), points.get(2), true);
                break;
            case POLYGON:
                shape = SHAPE.generatePolygon(points, false);
                node = SHAPE.generatePolygon(points, true);
                break;
            case ANGLE:
                shape = SHAPE.generateAngle(points.get(0), points.get(1), points.get(2), TongaAnnotator.LINESIZE, false);
                node = SHAPE.generateAngle(points.get(0), points.get(1), points.get(2), SHAPE.RASTER_LINE, true);
                break;
            case POLYLINE:
                shape = SHAPE.generatePolyline(points, StrokeLineCap.ROUND, TongaAnnotator.LINESIZE, false);
                node = SHAPE.generatePolyline(points, StrokeLineCap.ROUND, SHAPE.RASTER_LINE, true);
                break;
            case PLANE: {
                shape = SHAPE.generatePolyline(points, StrokeLineCap.BUTT, span, false);
                node = SHAPE.generatePolyline(points, StrokeLineCap.BUTT, span, true);
                break;
            }
        }
    }

    public List<mainPackage.morphology.Line> generateLines() {
        if (type == LINE || type == POLYLINE || type == PLANE || type == POLYGON) {
            List<mainPackage.morphology.Line> ll = new ArrayList<>();
            for (int i = 0; i < points.size() - 1; i++) {
                ll.add(new mainPackage.morphology.Line(points.get(i), points.get(i + 1)));
            }
            return ll;
        }
        Tonga.log.warn("Line generation for non-line annotation");
        return null;
    }

    protected void render(GraphicsContext cont, double[] dvals, double scaling, boolean nodecoration) {
        int dotsize = nodecoration ? 3 : DOTSIZE;
        int linesize = nodecoration ? 2 : LINESIZE;
        int outlinesize = nodecoration ? 1 : LINESIZE;
        switch (type) {
            case DOT:
            case RADIUS: {
                if (type == AnnotationType.RADIUS) {
                    DRAW.canvasDrawer.drawDot(cont, points.get(0), span * scaling, dvals);
                }
                DRAW.canvasDrawer.drawDot(cont, points.get(0), dotsize, dvals);
                break;
            }
            case CIRCLE: {
                DRAW.canvasDrawer.drawCircle(cont, points.get(0), points.get(1), outlinesize, dvals);
                break;
            }
            case RECTANGLE: {
                DRAW.canvasDrawer.drawRectangle(cont, points.get(0), points.get(1), points.get(2), outlinesize, dvals);
                break;
            }
            case OVAL: {
                DRAW.canvasDrawer.drawOval(cont, points.get(0), points.get(1), points.get(2), outlinesize, dvals);
                break;
            }
            case POLYGON: {
                cont.setLineJoin(StrokeLineJoin.ROUND);
                cont.setLineCap(StrokeLineCap.ROUND);
                DRAW.canvasDrawer.drawPolygon(cont, points, outlinesize, dvals);
                if (!nodecoration) {
                    renderDots(cont, linesize, dvals);
                }
                break;
            }
            case ANGLE:
                cont.setLineJoin(StrokeLineJoin.ROUND);
                cont.setLineCap(StrokeLineCap.ROUND);
                List<Point> npoints = new ArrayList<>();
                npoints.add(points.get(1));
                npoints.add(points.get(0));
                npoints.add(points.get(2));
                DRAW.canvasDrawer.drawArc(cont, points.get(0), points.get(1), points.get(2), scaling, dvals);
                DRAW.canvasDrawer.drawPolyLine(cont, npoints, linesize, dvals);
                if (!nodecoration) {
                    renderDots(cont, linesize, dvals);
                }
                break;
            case LINE:
            case POLYLINE:
            case PLANE: {
                cont.setLineJoin(StrokeLineJoin.ROUND);
                if (type == AnnotationType.PLANE) {
                    cont.setLineCap(StrokeLineCap.BUTT);
                    DRAW.canvasDrawer.drawPolyLine(cont, points, span * scaling, dvals);
                }
                cont.setLineCap(StrokeLineCap.ROUND);
                if (type == LINE) {
                    DRAW.canvasDrawer.drawLine(cont, points.get(0), points.get(1), linesize, dvals);
                } else {
                    DRAW.canvasDrawer.drawPolyLine(cont, points, linesize, dvals);
                }
                if (!nodecoration) {
                    renderDots(cont, linesize, dvals);
                }
                break;
            }
        }
    }

    private void renderDots(GraphicsContext cont, int linesize, double[] dvals) {
        int ds = linesize + (int) Math.min(1, linesize * 0.5);
        cont.setFill(color.darker());
        for (Point p : points) {
            DRAW.canvasDrawer.drawDot(cont, p, ds, dvals);
        }
    }

    protected void renderActive(GraphicsContext cont, double[] dvals, double scaling, Point mp) {
        switch (type) {
            case CIRCLE:
                DRAW.canvasDrawer.drawCircle(cont, points.get(0), mp, LINESIZE, dvals);
                break;
            case RECTANGLE: {
                if (points.size() == 1) {
                    cont.setLineCap(StrokeLineCap.ROUND);
                    DRAW.canvasDrawer.drawLine(cont, points.get(0), mp, LINESIZE, dvals);
                } else {
                    DRAW.canvasDrawer.drawRectangle(cont, points.get(0), points.get(1), mp, LINESIZE, dvals);
                }
                break;
            }
            case OVAL: {
                if (points.size() == 1) {
                    cont.setLineCap(StrokeLineCap.ROUND);
                    DRAW.canvasDrawer.drawLine(cont, points.get(0), mp, LINESIZE, dvals);
                } else {
                    DRAW.canvasDrawer.drawOval(cont, points.get(0), points.get(1), mp, LINESIZE, dvals);
                }
                break;
            }
            case POLYGON: {
                points.add(mp);
                cont.setLineJoin(StrokeLineJoin.ROUND);
                DRAW.canvasDrawer.drawPolyLine(cont, points, LINESIZE, dvals);
                points.remove(points.size() - 1);
                break;
            }
            case ANGLE:
                cont.setLineJoin(StrokeLineJoin.ROUND);
                cont.setLineCap(StrokeLineCap.ROUND);
                if (points.size() == 1) {
                    DRAW.canvasDrawer.drawLine(cont, points.get(0), mp, LINESIZE, dvals);
                } else {
                    List<Point> npoints = new ArrayList<>();
                    npoints.add(points.get(1));
                    npoints.add(points.get(0));
                    npoints.add(mp);
                    DRAW.canvasDrawer.drawPolyLine(cont, npoints, LINESIZE, dvals);
                    DRAW.canvasDrawer.drawArc(cont, points.get(0), points.get(1), mp, scaling, dvals);
                }
                break;
            case LINE:
            case POLYLINE:
            case PLANE: {
                points.add(mp);
                render(cont, dvals, scaling, false);
                points.remove(points.size() - 1);
            }
            break;
        }
    }

    public Area toArea() {
        Bounds shapeBounds = node.boundsInLocalProperty().getValue();
        int w = (int) Math.ceil(shapeBounds.getWidth() + 1);
        int h = (int) Math.ceil(shapeBounds.getHeight() + 1);
        WritableImage im = new WritableImage(w, h);
        Platform.runLater(() -> {
            node.snapshot(null, im);
        });
        IO.waitForJFXRunLater();
        int[] pixels = new int[w * h];
        im.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        boolean[][] ba = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                ba[x][y] = (pixels[y * w + x] & 0xFF) < SHAPE.RASTER_THRESHOLD;
            }
        }
        return new Area(ba, (int) shapeBounds.getMinX(), (int) shapeBounds.getMinY(), points.get(0).x, points.get(0).y);
    }

    protected void resolve() {
        generateShape();
        calculateNodes();
        calculateDimensions();
        calculateArea();
        calculateLength();
        calculateAngle();
    }

    protected void calculateNodes() {
        switch (type) {
            case DOT:
            case ANGLE:
            case RADIUS: {
                nodes = 1;
                break;
            }
            case RECTANGLE: {
                nodes = 4;
                break;
            }
            case CIRCLE:
            case OVAL: {
                nodes = 0;
                break;
            }
            case LINE:
            case POLYLINE:
            case PLANE:
            case POLYGON: {
                nodes = points.size();
                break;
            }
        }
    }

    protected void calculateLength() {
        switch (type) {
            case DOT:
            case RADIUS:
                length = 0;
                break;
            case CIRCLE:
                length = width * Math.PI * 2;
                break;
            case RECTANGLE:
            case OVAL:
                double hei = GEO.getDist(points.get(0), points.get(1));
                double wid = GEO.getPerpendicularDistance(points.get(0), points.get(1), points.get(2));
                if (type == RECTANGLE) {
                    length = hei * 2 + wid * 2;
                }
                if (type == OVAL) {
                    length = GEO.ellipsePerimeter(hei / 2., wid);
                }
                break;
            case ANGLE:
                length = GEO.getDist(points.get(0), points.get(1)) + GEO.getDist(points.get(0), points.get(2));
                break;
            case LINE:
            case POLYLINE:
            case PLANE:
            case POLYGON:
                double ll = 0;
                for (int i = 0; i < points.size() - 1; i++) {
                    ll += GEO.getDist(points.get(i), points.get(i + 1));
                }
                if (type == POLYGON) {
                    ll += GEO.getDist(points.get(0), points.get(points.size() - 1));
                }
                length = ll;
                break;
        }
    }

    protected void calculateArea() {
        switch (type) {
            case DOT:
                area = 0;
                break;
            case RADIUS:
                area = GEO.circleArea(span);
                break;
            case CIRCLE:
                area = GEO.circleArea(width);
                break;
            case RECTANGLE:
            case OVAL:
                double hei = GEO.getDist(points.get(0), points.get(1));
                double wid = GEO.getPerpendicularDistance(points.get(0), points.get(1), points.get(2));
                if (type == RECTANGLE) {
                    area = hei * wid;
                }
                if (type == OVAL) {
                    area = GEO.ellipseArea(hei / 2., wid);
                }
                break;
            case ANGLE:
            case LINE:
            case POLYLINE:
                area = 0;
                break;
            case PLANE:
                area = GEO.planeArea((Point[]) points.toArray(new Point[nodes]), span);
                break;
            case POLYGON:
                area = GEO.polygonArea((Point[]) points.toArray(new Point[nodes]));
                break;
        }
    }

    protected void calculateDimensions() {
        switch (type) {
            case DOT:
                width = 1;
                height = 1;
                break;
            case RADIUS:
                width = span;
                height = span;
                break;
            case RECTANGLE:
            case OVAL:
            case PLANE:
                width = (int) shape.getLayoutBounds().getWidth();
                height = (int) shape.getLayoutBounds().getHeight();
                break;
            case CIRCLE:
                width = (int) Math.ceil(GEO.getDist(points.get(0), points.get(1)));
                height = width;
                break;
            case ANGLE:
            case LINE:
            case POLYLINE:
            case POLYGON:
                int max = points.stream().mapToInt(p -> p.x).max().getAsInt();
                int mix = points.stream().mapToInt(p -> p.x).min().getAsInt();
                int may = points.stream().mapToInt(p -> p.y).max().getAsInt();
                int miy = points.stream().mapToInt(p -> p.y).min().getAsInt();
                width = max - mix;
                height = may - miy;
                break;
        }
    }

    protected void calculateAngle() {
        switch (type) {
            case DOT:
            case RADIUS:
            case CIRCLE:
            case OVAL:
            case RECTANGLE:
                angle = 360;
                break;
            case ANGLE:
                double dir1 = GEO.getDirection(points.get(0), points.get(1));
                double dir2 = GEO.getDirection(points.get(0), points.get(2));
                angle = (dir2 > dir1) ? 360 - (dir2 - dir1) : dir1 - dir2;
                break;
            case PLANE:
            case LINE:
            case POLYLINE:
                angle = 0;
                if (points.size() > 2) {
                    for (int i = 1; i < points.size() - 1; i++) {
                        double dirr1 = GEO.getDirection(points.get(i), points.get(i + 1));
                        double dirr2 = GEO.getDirection(points.get(i), points.get(i - 1));
                        angle += (dirr2 > dirr1) ? 360 - (dirr2 - dirr1) : dirr1 - dirr2;
                    }
                }
                break;
            case POLYGON:
                angle = (points.size() - 2) * 180;
                break;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNodes() {
        return nodes;
    }

    public double getLength() {
        return length;
    }

    public double getAngle() {
        return angle;
    }

    public int getGroup() {
        return group;
    }

    public int getSpan() {
        return span;
    }

    public double getArea() {
        return area;
    }

    public Color getColor() {
        return color;
    }

    public AnnotationType getType() {
        return type;
    }

    public Shape getShape() {
        return shape;
    }

    public TongaColor getColorObject() {
        return new TongaColor(COL.colorToARGBInt(color));
    }

    protected byte[] compileBinary() {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream os = new DataOutputStream(bs);
            os.writeByte(getTypeIndex(type));
            os.writeInt(COL.colorToARGBInt(color));
            os.writeInt(span);
            os.writeInt(points.size());
            for (Point point : points) {
                os.writeInt(point.x);
                os.writeInt(point.y);
            }
            os.writeInt(group);
            os.writeByte(-1);
            os.flush();
            return bs.toByteArray();
        } catch (IOException ex) {
            Tonga.catchError(ex, "Failed to compile annotation binary");
        }
        return null;
    }

    protected static TongaAnnotation decompileBinary(DataInputStream in) {
        try {
            AnnotationType type = TongaAnnotation.getTypeIndex(in.read());
            Color color = COL.ARGBintToColor(in.readInt());
            int span = in.readInt();
            int listSize = in.readInt();
            List<Point> points = new ArrayList<>();
            for (int i = 0; i < listSize; i++) {
                points.add(new Point(in.readInt(), in.readInt()));
            }
            int group = in.readInt();
            in.read();
            return new TongaAnnotation(color, span, group, type, points);
        } catch (IOException ex) {
            Tonga.catchError(ex, "Failed to decompile annotation binary");
        }
        return null;
    }

    public static int getTypeIndex(AnnotationType type) {
        switch (type) {
            case DOT:
                return 0;
            case LINE:
                return 1;
            case POLYLINE:
                return 2;
            case POLYGON:
                return 3;
            case RECTANGLE:
                return 4;
            case CIRCLE:
                return 5;
            case OVAL:
                return 6;
            case PLANE:
                return 7;
            case RADIUS:
                return 8;
            case ANGLE:
                return 9;
            default:
                Tonga.log.warn("No type index for annotation type {}.", type.name());
                return 0;
        }
    }

    public static AnnotationType getTypeIndex(int type) {
        switch (type) {
            case 0:
                return DOT;
            case 1:
                return LINE;
            case 2:
                return POLYLINE;
            case 3:
                return POLYGON;
            case 4:
                return RECTANGLE;
            case 5:
                return CIRCLE;
            case 6:
                return OVAL;
            case 7:
                return PLANE;
            case 8:
                return RADIUS;
            case 9:
                return ANGLE;
            default:
                Tonga.log.warn("No type annotation for index {}.", type);
                return DOT;
        }
    }

    @Override
    public String toString() {
        return type + " (" + COL.colorName(COL.colorToARGBInt(color)) + ") (" + points.get(0).x + "," + points.get(0).y + ')';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.color);
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + Objects.hashCode(this.points);
        hash = 59 * hash + Objects.hashCode(this.shape);
        hash = 59 * hash + this.span;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.length) ^ (Double.doubleToLongBits(this.length) >>> 32));
        hash = 59 * hash + this.nodes;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TongaAnnotation other = (TongaAnnotation) obj;
        if (this.span != other.span) {
            return false;
        }
        if (!Objects.equals(this.color, other.color)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return Objects.equals(this.points, other.points);
    }
}
