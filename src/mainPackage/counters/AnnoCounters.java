package mainPackage.counters;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.TongaAnnotation;
import mainPackage.TongaAnnotator;
import mainPackage.morphology.Line;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.utils.DRAW;
import mainPackage.utils.GEO;
import mainPackage.utils.RGB;
import mainPackage.utils.STAT;

public class AnnoCounters {

    public static AnnoCounter areaIntensity() {
        return new AnnoCounter("Area intensity",
                new String[]{"Image", "Annotation", "Type", "X", "Y",
                    "Area %unit2", "<html><b>Stain %</b></html>", "<html><b>Stain sum</b></html>"},
                new String[]{"The name of the image",
                    "The ID of the annotation",
                    "The type of the annotation",
                    "The X-coordinate pixel of the centroid of this annotation in the image",
                    "The Y-coordinate pixel of the centroid of this annotation in the image",
                    "The area size of this annotation in %unit2",
                    "The average relative intensity of this annotation",
                    "The total relative intensity of this annotation"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            int stInd;

            @Override
            protected void preProcessor(ImageData targetImage) {
                stInd = 0;
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    row = data.newRow(imageName);
                    row[1] = ++stInd;
                    row[2] = annotation.getType().name();
                    ROI obj = new ROI(image, annotation.toArea());
                    obj.quantifyStain(image);
                    int[] cent = obj.getCentroid();
                    row[3] = (Integer) cent[0];
                    row[4] = (Integer) cent[1];
                    row[5] = scaleUnit(obj.getStainSTAT().getN(), 2);
                    row[6] = STAT.decToPerc(obj.getStainAvg());
                    row[7] = (Double) obj.getStainSum();
                }
            }
        };
    }

    public static AnnoCounter areaIntensityImage() {
        return new AnnoCounter("Area intensity",
                new String[]{"Image", "Annotations",
                    "<html><b>Avg.Stain %</b></html>", "Std.Stain %", "Med.Stain %",
                    "<html><b>Avg.Stain sum</b></html>", "Std.Stain sum", "Med.Stain sum"},
                new String[]{"The name of the image",
                    "The total number of annotations in the image",
                    "The average relative intensity from all annotations in the image",
                    "The standard deviation of the relative intensity measurement between the annotations",
                    "The median relative intensity from all the annotations in the image",
                    "The average relative intensity sum from all the annotations in the image",
                    "The standard deviation of the intensity sum measurement between the annotations",
                    "The median relative intensity sum from all the annotations in the image"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            List<ROI> rl;

            @Override
            protected void preProcessor(ImageData targetImage) {
                rl = new ArrayList<>();
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    rl.add(new ROI(image, annotation.toArea()));
                }
            }

            @Override
            protected void postProcessor(ImageData image) {
                ROISet set = new ROISet(rl, image.width, image.height);
                set.quantifyStainAgainstChannel(image);
                row = data.newRow(imageName);
                STAT stain = set.statsForStainAvg();
                row[1] = set.objectsCount();
                row[2] = STAT.decToPerc(stain.getMean());
                row[3] = STAT.decToPerc(stain.getStdDev());
                row[4] = STAT.decToPerc(stain.getMedian());
                stain = set.statsForStainSum();
                row[5] = stain.getMean();
                row[6] = stain.getStdDev();
                row[7] = stain.getMedian();
            }
        };
    }

    public static AnnoCounter annotationDimensions() {
        return new AnnoCounter("Annotation dimensions",
                new String[]{"Image", "Annotation", "Type", "X", "Y",
                    "Area %unit2", "Length %unit2", "Angle °"},
                new String[]{"The name of the image",
                    "The ID of the annotation",
                    "The type of the annotation",
                    "The X-coordinate pixel of the centroid of this nucleus in the image",
                    "The Y-coordinate pixel of the centroid of this nucleus in the image",
                    "The area size of this annotation in %unit2",
                    "The length of this annotation in %unit",
                    "The angle of this annotation in degrees"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            int stInd;

            @Override
            protected void preProcessor(ImageData targetImage) {
                stInd = 0;
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    row = data.newRow(imageName);
                    row[1] = ++stInd;
                    row[2] = annotation.getType().name();
                    row[3] = annotation.points.get(0).x;
                    row[4] = annotation.points.get(0).y;
                    row[5] = scaleUnit(annotation.getArea(), 2);
                    row[6] = scaleUnit(annotation.getLength(), 1);
                    row[7] = annotation.getAngle();
                }
            }
        };
    }

    public static AnnoCounter annotationAreaSizeImage() {
        return new AnnoCounter("Annotation area size",
                new String[]{"Image", "Annotations",
                    "<html><b>Avg.Size %unit2</b></html>", "Std.Size %unit2", "Med.Size %unit2"},
                new String[]{"The name of the image",
                    "The total number of annotations in the image",
                    "The average area size from all annotations in the image",
                    "The standard deviation of the area size between the annotations",
                    "The median area size from all the annotations in the image"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            List<Integer> as;

            @Override
            protected void preProcessor(ImageData targetImage) {
                as = new ArrayList<>();
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    as.add((int) annotation.getArea());
                }
            }

            @Override
            protected void postProcessor(ImageData image) {
                STAT astat = new STAT((as.stream().mapToInt(v -> v).toArray()));
                row = data.newRow(imageName);
                row[1] = as.size();
                row[2] = scaleUnit(astat.getMean(), 2);
                row[3] = scaleUnit(astat.getStdDev(), 2);
                row[4] = scaleUnit(astat.getMedian(), 2);
            }
        };
    }

    public static AnnoCounter annotationLengthImage() {
        return new AnnoCounter("Annotation length",
                new String[]{"Image", "Annotations",
                    "<html><b>Avg.Length %unit</b></html>", "Std.Length %unit", "Med.Length %unit"},
                new String[]{"The name of the image",
                    "The total number of annotations in the image",
                    "The average length from all annotations in the image",
                    "The standard deviation of the length between the annotations",
                    "The median length from all the annotations in the image"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            List<Double> as;

            @Override
            protected void preProcessor(ImageData targetImage) {
                as = new ArrayList<>();
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    as.add(annotation.getLength());
                }
            }

            @Override
            protected void postProcessor(ImageData image) {
                STAT lstat = new STAT((as.stream().mapToDouble(v -> v).toArray()));
                row = data.newRow(imageName);
                row[1] = as.size();
                row[2] = scaleUnit(lstat.getMean(), 1);
                row[3] = scaleUnit(lstat.getStdDev(), 1);
                row[4] = scaleUnit(lstat.getMedian(), 1);
            }
        };
    }

    public static AnnoCounter annotationAngleImage() {
        return new AnnoCounter("Annotation angles",
                new String[]{"Image", "Annotations",
                    "<html><b>Avg.Angle °</b></html>", "Std.Angle °", "Med.Angle °"},
                new String[]{"The name of the image",
                    "The total number of annotations in the image",
                    "The average angle from all annotations in the image",
                    "The standard deviation of the angle between the annotations",
                    "The median angle from all the annotations in the image"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            List<Double> as;

            @Override
            protected void preProcessor(ImageData targetImage) {
                as = new ArrayList<>();
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && annotation.getType() == TongaAnnotator.AnnotationType.ANGLE) {
                    as.add(annotation.getAngle());
                }
            }

            @Override
            protected void postProcessor(ImageData image) {
                STAT astat = new STAT((as.stream().mapToDouble(v -> v).toArray()));
                row = data.newRow(imageName);
                row[1] = as.size();
                row[2] = astat.getMean();
                row[3] = astat.getStdDev();
                row[4] = astat.getMedian();
            }
        };
    }

    public static AnnoCounter annotationGroupsImage() {
        return new AnnoCounter("Annotation counter",
                new String[]{"Image", "Group", "Annotations"},
                new String[]{"The name of the image", "The annotation group", "The total number of annotations in the image"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_TYPE, "Restrict to annotation type", 0),
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0)
                }) {

            Map<Integer, Integer> groupCounter;

            @Override
            protected void preProcessor(ImageData targetImage) {
                groupCounter = new HashMap<>();
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (correctGroup(annotation) && correctType(annotation)) {
                    int occurences = 0;
                    int group = annotation.getGroup();
                    if (groupCounter.containsKey(group)) {
                        occurences = groupCounter.get(group);
                    }
                    groupCounter.put(group, occurences + 1);
                }
            }

            @Override
            protected void postProcessor(ImageData image) {
                groupCounter.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
                    row = data.newRow(imageName);
                    row[1] = e.getKey();
                    row[2] = e.getValue();
                });
            }
        };
    }

    public static AnnoCounter planeIntensities() {
        return new AnnoCounter("Polyline radial intensitites",
                new String[]{"Image", "Line", "Position", "Value"},
                new String[]{"The name of the image",
                    "The ID of the plane",
                    "The position from the beginning (X,Y) until the end along the line",
                    "The intensity value in the position"},
                new ControlReference[]{
                    new ControlReference(ANNOTATION_GROUP, "Restrict to annotation group", 0),
                    new ControlReference(TOGGLE, "Stretch to range", 1, new int[]{2, 1}),
                    new ControlReference(SPINNER, "Range to use for all", 100)}) {

            double size;
            int stInd;
            HashMap<Point, Integer> accesses;
            HashMap<Integer, HashSet<Point>> lengthPoints;
            HashMap<Point, Double> values;
            HashMap<Point, Double> fracts;
            List<HashMap<String, Object>> segms;
            List<Line> planeLines;
            List<Line> nickLines;
            List<Double> anValues;

            @Override
            protected void preProcessor(ImageData targetImage) {
                stInd = 0;
            }

            @Override
            void annotationIterator(ImageData image, TongaAnnotation annotation) {
                if (annotation.getType() == TongaAnnotator.AnnotationType.PLANE && correctGroup(annotation)) {
                    row = data.newRow(imageName);
                    row[1] = ++stInd;
                    size = annotation.getSpan() / 2;
                    accesses = new HashMap<>();
                    lengthPoints = new HashMap<>();
                    segms = new ArrayList<>();
                    planeLines = annotation.generateLines();
                    createNickLines();
                    createSegments();
                    //
                    for (int n = 0; n < Math.round(annotation.getLength()) + 1; n++) {
                        Object[] segPoint = calculateSegmentPoint(n);
                        evaluateOne((Line) segPoint[0], (int) (double) segPoint[1], n, (HashMap<String, Object>) segPoint[2]);
                    }
                    calculatePointIntensities(image.pixels32);
                    mapIntensitiesAlongLine();
                    normalizeAndPublish();
                }
            }

            Object[] calculateSegmentPoint(int p) {
                double lc = 0;
                HashMap<String, Object> segm = null;
                Line l = null;
                for (int i = 0; i < segms.size(); i++) {
                    segm = segms.get(i);
                    l = new Line((Point) segm.get("start"), (Point) segm.get("end"));
                    double len = l.length;
                    if (lc + len > p) {
                        double df = p - lc;
                        return new Object[]{l, df, segm};
                    } else {
                        lc += len;
                    }
                }
                return new Object[]{l, p - lc, segm};
            }

            void createNickLines() {
                nickLines = new ArrayList<>();
                for (int i = 0; i < planeLines.size() - 1; i++) {
                    Line a = planeLines.get(i);
                    Line b = planeLines.get(i + 1);
                    double db = GEO.getDirBetween(GEO.getParallelAngle(a.direction), b.direction);
                    Line nl = new Line(a.end.x, a.end.y, db);
                    nl.setStartEndPoints(a.end, size);
                    double d = GEO.getDist(nl.end, a.getPerpendicularIntersection(nl.end));
                    double r = size / d;
                    nl.end = GEO.getPointInDirection(a.end, nl.direction, size * r);
                    nickLines.add(nl);
                }
            }

            void createSegments() {
                Point lastPoint = planeLines.get(0).start;
                if (nickLines.isEmpty()) {
                    createSegment(lastPoint, planeLines.get(0).end, true, null);
                } else {
                    for (int i = 0; i < nickLines.size(); i++) {
                        Line nl = nickLines.get(i);
                        Line[] segLines = new Line[2];
                        segLines[0] = createSegmentLine(i, nl);
                        segLines[1] = createSegmentLine(i + 1, nl);
                        if (i > 0 && Line.intersection(segLines[0], nickLines.get(i - 1)) != null) {
                            segLines[0] = new Line(nl.end, nickLines.get(i - 1).end);
                        } else {
                            createSegment(lastPoint, segLines[0].mid, true, null);
                        }
                        if (nickLines.size() - 1 > i && Line.intersection(segLines[1], nickLines.get(i + 1)) != null) {
                            segLines[1] = new Line(nl.end, nickLines.get(i + 1).end);
                        }
                        //createSegment(l1.mid, nl.mid, false, GEO.getDirDifference(l1.direction, nl.direction), l1.direction > nl.direction);
                        createSegment(segLines[0].mid, nl.mid, false, nl.end);
                        createSegment(nl.mid, segLines[1].mid, false, nl.end);
                        lastPoint = segLines[1].mid;
                        if (nickLines.size() - 1 == i) {
                            createSegment(segLines[1].mid, planeLines.get(planeLines.size() - 1).end, true, null);
                        }
                    }
                }
            }

            Line createSegmentLine(int i, Line nl) {
                Line pl = planeLines.get(i);
                Line sl = pl.getPerpendicular(nl.end);
                Point p1 = Line.intersection(sl, pl);
                if (p1 == null) {
                    sl = new Line(nl.end, pl.end);
                    p1 = pl.end;
                }
                sl.setStartEndPoints(p1, size);
                return sl;
            }

            void createSegment(Point s, Point e, boolean straight, Point fixed) {
                HashMap<String, Object> nick = new HashMap<>();
                nick.put("mode", straight);
                nick.put("start", s);
                nick.put("end", e);
                nick.put("fpoint", fixed);
                segms.add(nick);
            }

            void evaluateOne(Line l, int segmunit, int totalunit, HashMap<String, Object> segm) {
                HashSet<Point> hl = new HashSet<>();
                DRAW.lineDrawer drawer = new DRAW.lineDrawer() {
                    @Override
                    public void action(int x, int y) {
                        for (int i = -1; i <= 1; i++) {
                            for (int j = -1; j <= 1; j++) {
                                hl.add(new Point(x + i, y + j));
                            }
                        }
                    }
                };
                Line aa;
                if ((boolean) segm.get("mode")) {
                    aa = l.getPerpendicularAlong(segmunit);
                    aa.setStartEndPoints(aa.mid, size);
                } else {
                    Point pm = GEO.getPointInDirection(l.start, l.direction, segmunit);
                    double dir = GEO.getDirection((Point) segm.get("fpoint"), pm);
                    Point pe = GEO.getPointInDirection(pm, dir, size);
                    aa = new Line((Point) segm.get("fpoint"), pe);
                }
                drawer.drawLine(aa.start, aa.end);
                hl.forEach(p -> {
                    if (accesses.containsKey(p)) {
                        accesses.put(p, accesses.get(p) + 1);
                    } else {
                        accesses.put(p, 1);
                    }
                });
                lengthPoints.put(totalunit, hl);
            }

            private void calculatePointIntensities(int[] pixels32) {
                values = new HashMap<>();
                fracts = new HashMap<>();
                accesses.entrySet().forEach(e -> {
                    Point pp = e.getKey();
                    double count = e.getValue();
                    int px = pp.y * imageWidth + pp.x;
                    fracts.put(pp, 1 / count);
                    values.put(pp, RGB.brightness(pixels32[px]) / count);
                });
            }

            private void mapIntensitiesAlongLine() {
                anValues = new ArrayList<>();
                lengthPoints.entrySet().forEach(e -> {
                    int dist = e.getKey();
                    HashSet<Point> points = e.getValue();
                    double sum = points.stream().mapToDouble(p -> values.get(p)).sum();
                    double count = points.stream().mapToDouble(p -> fracts.get(p)).sum();
                    anValues.add(sum / count);
                });
            }

            private void normalizeAndPublish() {
                List<Double> finAnValues = anValues;
                if (param.toggle[1]) {
                    double frac = anValues.size() / (double) param.spinner[1];
                    double stv = 0;
                    double env = 0;
                    finAnValues = new ArrayList<>();
                    for (int i = 0; i < param.spinner[1]; i++) {
                        double nv = 0;
                        stv = env;
                        env = stv + frac;
                        double span = Math.ceil(env) - Math.floor(stv);
                        for (int j = 0; j < span; j++) {
                            double tfrc = 1;
                            if (stv > ((int) stv + j)) {
                                tfrc -= stv - (int) stv + j;
                            }
                            if ((int) stv + j + 1 > env) {
                                tfrc -= (int) stv + j + 1 - env;
                            }
                            if ((int) stv + j < anValues.size()) {
                                Tonga.log.trace("for {} taking {} from {}", anValues.size(), tfrc, (int) (stv + j));
                                nv += anValues.get((int) stv + j) * tfrc;
                            } else {
                                Tonga.log.trace("for {} skipping {} from {}", anValues.size(), tfrc, (int) (stv + j));
                            }
                        }
                        finAnValues.add(nv / frac);
                    }
                }
                String annInd = row[1].toString();
                for (int i = 0; i < finAnValues.size(); i++) {
                    row[1] = annInd;
                    row[2] = i;
                    row[3] = finAnValues.get(i);
                    if (i < finAnValues.size() - 1) {
                        row = data.newRow(imageName);
                    }
                }
            }

        };
    }
}
