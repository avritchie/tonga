package mainPackage.protocols;

import java.awt.Point;
import javafx.scene.paint.Color;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.TongaAnnotator;
import static mainPackage.TongaAnnotator.AnnotationType.DOT;
import mainPackage.morphology.EdgePoint;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.utils.GEO;

public class __ObjectSegmentAngle extends Protocol {

    @Override
    public String getName() {
        return "Object segmentation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(COLOUR, "Background is which colour", new int[]{0}),
            new ControlReference(ANNOTATION, new TongaAnnotator.AnnotationType[]{DOT}, "Has to align with this annotation"),
            new ControlReference(SLIDER, new Object[]{0, 90}, "Maximum angle to allow"),
            new ControlReference(COMBO, new String[]{"Normal", "Strong", "Strict", "Very strict"}, "Criteria for separation"),
            new ControlReference(SPINNER, "Average object diameter (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        Color bg = param.color[0];
        int nucleusSize = param.spinner[0];
        int mode = param.combo[0];
        int maxAngle = param.slider[0];

        return new ProcessorFast(fullOutput() ? 2 : 1, "Objects", 3) {

            int nuclSize;

            @Override
            protected void methodInit() {
                nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
            }

            @Override
            protected void pixelProcessor() {
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                set.targetSize(nuclSize);
                set.findOuterEdges();
                set.findInnerEdges();
                set.analyzeCorners();
                set.analyzeCornerIntersections();
                double as = set.avgSize();
                Point ap = lparam.annotation[0].points.get(0);
                set.segmentFiltered(mode, (ROI roi, Point p1, Point p2) -> {
                    if (!p1.getClass().equals(EdgePoint.class) || !p2.getClass().equals(EdgePoint.class)) {
                        return false;
                    }
                    Point pp1 = new Point(p1.x + roi.area.xstart, p1.y + roi.area.ystart);
                    Point pp2 = new Point(p2.x + roi.area.xstart, p2.y + roi.area.ystart);
                    Point mp = GEO.getMidPoint(pp1, pp2);
                    double d1 = GEO.getDirection(mp, ap);
                    double d2 = GEO.getDirection(pp1, pp2);
                    double dd = Math.min(
                            GEO.getDirDifference(((EdgePoint) p1).direction, d1),
                            GEO.getDirDifference(((EdgePoint) p2).direction, d1));
                    double a = GEO.getDirDifference(d1, d2);
                    dd = Math.min(dd, Math.abs(dd - 180));
                    double fa = Math.min(a, Math.abs(a - 180));
                    return fa < maxAngle && dd < maxAngle && roi.getSize() >= as;
                });
                setOutputBy(set.drawToImageData(true));
                //optional for debug use
                setSampleOutputBy(set.drawToImageData(), 1);
            }
        };
    }
}
