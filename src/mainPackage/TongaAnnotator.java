package mainPackage;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import static mainPackage.TongaAnnotator.AnnotationType.*;
import mainPackage.utils.COL;
import mainPackage.utils.DRAW;

public class TongaAnnotator {

    private static AnnotationType annoType;
    private static boolean visible = true;
    private static boolean annotating;
    private static boolean selecting;
    private static int annoGroup = 1;
    private static Color annoColor;
    private static int annoSize = (int) Tonga.frame().annotRadius.getValue();
    private static int hash;

    private static boolean pending = false;
    private static TongaAnnotation pendingAnno;

    final static int DOTSIZE = 10;
    final static int LINESIZE = 5;

    public enum AnnotationType {
        DOT,
        RADIUS,
        LINE,
        POLYLINE,
        POLYGON,
        RECTANGLE,
        CIRCLE,
        OVAL,
        PLANE,
        ANGLE,
        ERASER
    }

    public static int getHash() {
        return hash;
    }

    public static void select() {
        selecting = true;
    }

    public static void deselect() {
        selecting = false;
    }

    public static boolean selection() {
        return selecting;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void visible(boolean isit) {
        visible = isit;
        hash = isit ? generateHash() : 0;
        TongaRender.redraw();
    }

    public static void activateAnnotator(boolean activate) {
        annotating = activate;
        Tonga.frame().annotButtonToggle();
    }

    public static boolean annotationPending() {
        return pending;
    }

    public static AnnotationType getAnnoType() {
        return annoType;
    }

    public static boolean annotating() {
        return annotating;
    }

    public static void setAnnotationType(AnnotationType annoType) {
        annotating = true;
        TongaAnnotator.annoType = annoType;
        Tonga.frame().annotButtonToggle();
    }

    public static void setAnnotationSize(int annoSize) {
        TongaAnnotator.annoSize = annoSize;
    }

    public static void setAnnotationColor(Color annoColor) {
        TongaAnnotator.annoColor = annoColor;
    }

    public static void setAnnotationGroup(int annoGroup) {
        TongaAnnotator.annoGroup = annoGroup;
    }

    private static Color getColor(AnnotationType type) {
        if (annoColor != null) {
            return annoColor;
        }
        switch (type) {
            case DOT:
            case RADIUS:
                return COL.awt2FX(Tonga.frame().colorYellow.getForeground());
            case ANGLE:
            case LINE:
            case POLYLINE:
            case PLANE:
                return COL.awt2FX(Tonga.frame().colorRed.getForeground());
            case RECTANGLE:
            case CIRCLE:
            case OVAL:
            case POLYGON:
                return COL.awt2FX(Tonga.frame().colorGreen.getForeground());
        }
        return Color.WHITE;
    }

    public static void deleteAll() {
        TongaImage ti = Tonga.getImage();
        if (ti != null) {
            ti.annotations.removeAll();
            refresh(ti);
        }
    }

    public static void delete(int[] i) {
        TongaImage ti = Tonga.getImage();
        if (ti != null) {
            Tonga.getImage().annotations.remove(i);
            refresh(ti);
        }
    }

    private static void clearTable() {
        Tonga.frame().annotationTable.clearData();
    }

    public static void update() {
        TongaImage ti = Tonga.getImage();
        if (ti == null) {
            clearTable();
        } else {
            refresh(ti);
        }
    }

    private static void finalizeAnnotation() {
        if (pendingAnno != null) {
            pendingAnno.resolve();
            pendingAnno.partial = false;
            pendingAnno = null;
            pending = false;
        }
        update();
    }

    public static void mouseEvent(MouseEvent me, int type, int imgx, int imgy) {
        //perform both render array modifications and rendering in the JFX thread
        Platform.runLater(() -> {
            TongaImage ti = Tonga.getImage();
            if (annotating && ti != null && acceptableEvent(type)) {
                TongaAnnotations cimgAnnos = ti.annotations;
                Point np = activePoint(imgx, imgy);
                switch (annoType) {
                    case DOT:
                    case RADIUS: {
                        if (type == MouseEvent.MOUSE_RELEASED) {
                            pendingAnno = new TongaAnnotation(getColor(annoType), annoSize, annoGroup, annoType, np);
                            cimgAnnos.add(pendingAnno);
                            finalizeAnnotation();
                        }
                        break;
                    }
                    case ANGLE:
                    case LINE:
                    case CIRCLE:
                    case RECTANGLE:
                    case OVAL: {
                        if (type == MouseEvent.MOUSE_RELEASED) {
                            if (!pending) {
                                pendingAnno = new TongaAnnotation(getColor(annoType), annoSize, annoGroup, annoType, np);
                                cimgAnnos.add(pendingAnno);
                                pending = true;
                            } else {
                                pendingAnno.points.add(np);
                                if (pendingAnno.points.size() == 2 && (annoType == LINE || annoType == CIRCLE)
                                        || pendingAnno.points.size() == 3) {
                                    finalizeAnnotation();
                                }
                            }
                        }
                        break;
                    }
                    case POLYLINE:
                    case PLANE:
                    case POLYGON: {
                        switch (type) {
                            case MouseEvent.MOUSE_RELEASED: {
                                if (!pending) {
                                    pendingAnno = new TongaAnnotation(getColor(annoType), annoSize, annoGroup, annoType, np);
                                    cimgAnnos.add(pendingAnno);
                                    pending = true;
                                } else if (me.getClickCount() == 2) {
                                    finalizeAnnotation();
                                } else {
                                    pendingAnno.points.add(np);
                                    pendingAnno.resolve();
                                }
                                break;
                            }
                            case MouseEvent.MOUSE_DRAGGED: {
                                if (pending) {
                                    pendingAnno.points.add(np);
                                    pendingAnno.resolve();
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case ERASER: {
                        if (type == MouseEvent.MOUSE_RELEASED) {
                            cimgAnnos.removeHovered();
                        }
                    }
                }
                refresh(ti);
            }
        });
    }

    protected static void cancelAnnotation() {
        if (annoType == CIRCLE || annoType == OVAL || annoType == RECTANGLE || annoType == ANGLE
                || pendingAnno.points.size() <= 1) {
            pending = false;
            Tonga.getImage().annotations.remove(pendingAnno);
            pendingAnno = null;
        } else {
            finalizeAnnotation();
        }
        update();
    }

    protected static void finalizePendingAnnotation() {
        Point np = activePoint(TongaRender.imgx, TongaRender.imgy);
        if (pendingAnno != null) {
            switch (annoType) {
                case ANGLE:
                case OVAL:
                case RECTANGLE:
                    if (pendingAnno.points.size() == 2) {
                        pendingAnno.points.add(np);
                        finalizeAnnotation();
                    }
                    break;
                case CIRCLE:
                case POLYGON:
                case LINE:
                case POLYLINE:
                case PLANE:
                    pendingAnno.points.add(np);
                    finalizeAnnotation();
                    break;
            }
            update();
        }
    }

    protected static void renderAnnotations(GraphicsContext cont, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        TongaImage ti = Tonga.getImage();
        if (ti != null && isVisible()) {
            double scalex = dw / (double) sw;
            double scaley = dh / (double) sh;
            double[] dvals = new double[]{sx, sy, dx, dy, scalex, scaley};
            double scale = (scalex + scaley) / 2;
            cont.setGlobalAlpha(0.5);
            for (TongaAnnotation anno : ti.annotations.getAnnotations()) {
                if (pendingAnno != anno) {
                    Color col = anno.hover ? Color.WHITE : anno.getColor();
                    cont.setFill(col);
                    cont.setStroke(col);
                    anno.render(cont, dvals, scale, false);
                }
            }
            //draw pending annotation and the hover dot
            if (annotating && TongaRender.onCanvas && Tonga.frame().annotationTabOpen()) {
                cont.setFill(getColor(annoType));
                cont.setStroke(getColor(annoType));
                Point mp = activePoint(TongaRender.imgx, TongaRender.imgy);;
                if (annoType == TongaAnnotator.AnnotationType.RADIUS) {
                    DRAW.canvasDrawer.drawDot(cont, mp, annoSize * scale, dvals);
                }
                DRAW.canvasDrawer.drawDot(cont, mp, DOTSIZE, dvals);
                if (pending) {
                    cont.setFill(pendingAnno.getColor().brighter());
                    cont.setStroke(pendingAnno.getColor().brighter());
                    pendingAnno.renderActive(cont, dvals, scale, mp);
                }
            }
            cont.setGlobalAlpha(1);
        }
    }

    public static Point activePoint(int imgx, int imgy) {
        Point np;
        if (pending && Key.keyAlt) {
            Point pp = pendingAnno.points.get(pendingAnno.points.size() - 1);
            int xd = Math.abs(imgx - pp.x);
            int yd = Math.abs(imgy - pp.y);
            np = new Point(xd <= yd ? pp.x : imgx, yd < xd ? pp.y : imgy);
            return np;
        } else {
            return new Point(imgx, imgy);
        }
    }

    @Deprecated
    public static ImageData renderAsImage(int width, int height, List<TongaAnnotation> annos) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext cont = canvas.getGraphicsContext2D();
        double[] dvals = new double[]{0, 0, 0, 0, 1, 1};
        cont.setGlobalAlpha(1);
        cont.setFill(Color.WHITE);
        cont.setStroke(Color.WHITE);
        for (TongaAnnotation anno : annos) {
            anno.render(cont, dvals, 1, true);
        }
        return new ImageData(TongaRender.snapshot(canvas, Color.BLACK), "Annotations");
    }

    private static void refresh(TongaImage ti) {
        int newHash = generateHash();
        if (newHash != hash) {
            hash = newHash;
            if (!ti.annotations.isEmpty()) {
                ti.annotations.tableAnnotations();
            } else {
                clearTable();
            }
        } else if (!Tonga.frame().annotationTable.isData() && !ti.annotations.isEmpty()) {
            ti.annotations.tableAnnotations();
        }
        Tonga.frame().updatePanels();
        TongaRender.redraw();
    }

    public static void updateHash() {
        hash = generateHash();
    }

    public static int generateHash() {
        TongaImage ti = Tonga.getImage();
        if (ti != null) {
            return ti.annotations.hashCode();
        }
        return 0;
    }

    private static boolean acceptableEvent(int type) {
        return type == MouseEvent.MOUSE_RELEASED || type == MouseEvent.MOUSE_CLICKED || type == MouseEvent.MOUSE_DRAGGED;
    }

    public static void exportAnnos(TongaImage ti, File file) {
        new IO.binaryWriter() {
            @Override
            protected void write(DataOutputStream out) throws IOException {
                List<TongaAnnotation> annos = ti.annotations.getAnnotations();
                out.writeUTF("TONGA_ANNOTATIONS");
                out.writeInt(annos.size());
                out.writeByte(-1);
                for (TongaAnnotation anno : annos) {
                    out.write(anno.compileBinary());
                }
                Tonga.setStatus("Annotations exported to file.");
                Tonga.log.info("Annotations exported to file.");
            }
        }.save(file, "annotation file");
    }

    public static void importAnnos(List<File> files, boolean allimages) {
        if (Tonga.thereIsImage()) {
            List<List<TongaAnnotation>> loadedAnnos = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            IO.binaryReader br = new IO.binaryReader() {
                @Override
                protected void read(DataInputStream in) throws IOException {
                    String title = in.readUTF();
                    if (title.equals("TONGA_ANNOTATIONS")) {
                        List<TongaAnnotation> newannos = new ArrayList<>();
                        int count = in.readInt();
                        in.read();
                        for (int i = 0; i < count; i++) {
                            TongaAnnotation nta = TongaAnnotation.decompileBinary(in);
                            if (nta != null) {
                                nta.resolve();
                                newannos.add(nta);
                            } else {
                                errors.add("Could not read the file.");
                            }
                        }
                        loadedAnnos.add(newannos);
                    } else {
                        errors.add("Invalid file format");
                    }
                }
            };
            try {
                for (File file : files) {
                    br.load(file, "annotation file");
                }
                //parse the loaded annotations to images
                if (allimages) {
                    for (int i = 0; i < Tonga.picList.size(); i++) {
                        Tonga.getImage(i).annotations.addAll(loadedAnnos.get(i));
                    }
                } else {
                    loadedAnnos.forEach(a -> {
                        Tonga.getImage().annotations.addAll(a);
                    });
                }
                update();
                if (loadedAnnos.isEmpty() && !errors.isEmpty()) {
                    Tonga.catchError("Annotation importing failed. " + errors.get(0));
                } else {
                    String status;
                    if (!errors.isEmpty()) {
                        status = "Annotations imported from file but " + errors.size() + " error(s) occurred. " + errors.get(0);
                    } else {
                        status = "Annotations imported from file.";
                    }
                    Tonga.log.info(status);
                    Tonga.setStatus(status);
                }
                Tonga.frame().annotationTable.focus();
            } catch (Exception ex) {
                Tonga.catchError(ex.getMessage());
            }
        } else {
            Tonga.setStatus("There is no image to import annotations to.");
        }
    }
}
