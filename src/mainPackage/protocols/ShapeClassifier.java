package mainPackage.protocols;

import java.awt.Point;
import java.util.Iterator;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaAnnotator;
import static mainPackage.TongaAnnotator.AnnotationType.DOT;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;

/**
 *
 * @author aritchie
 */
public class ShapeClassifier extends Protocol {

    @Override
    public String getName() {
        return "Ratio classifier";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Objects are on which layer"),
            new ControlReference(LAYER, "Stain is on which layer"),
            new ControlReference(COLOUR, "Background is", new int[]{0}),
            new ControlReference(ANNOTATION, new TongaAnnotator.AnnotationType[]{DOT},"Annotation to direct to"),
            new ControlReference(SLIDER, new Object[]{0.1, 1, 5.5, 180}, "Classifying ratio", 90),
            new ControlReference(TOGGLE, "Report results as individual shapes", 1)};
    }

    @Override
    protected Processor getProcessor() {
        int bg = param.colorARGB[0];
        double sc = param.sliderScaled[0];
        boolean indv = param.toggle[0];

        return new ProcessorFast(1, "Pibb", 4) {
            @Override
            protected void pixelProcessor() {
                ROISet set = new ImageTracer(inImage[0], bg).trace();
                set.findOuterEdges();
                int posx = lparam.annotation[0].points.get(0).x;
                int posy = lparam.annotation[0].points.get(0).y;
                ////
                Iterator<? extends ROI> it = set.list.iterator();
                while (it.hasNext()) {
                    ROI roi = it.next();
                    int[] ct = roi.getCentroid();
                    Point mp = new Point(ct[0], ct[1]);
                    double a = GEO.getDirection(mp, new Point(posx, posy));
                    double[] dims = roi.getDimensionsInAngle(a);
                    roi.setClassifyValue(dims[0] / dims[1]);
                    roi.classify((ROI r) -> {
                        return r.getClassifierValue() > sc ? 1 : 2;
                    });
                }
                int[] cols = new int[]{COL.GREEN, COL.RED};
                set.quantifyStainAgainstChannel(inImage[1]);
                int[] dest = set.drawStainArray(false);
                set.drawAppendClasses(cols, dest);
                ////
                setOutputBy(dest);
                String[] classNames = new String[]{"Green", "Red"};
                addResultData(indv
                        ? SetCounters.countObjectsStainClassSingle(set, classNames).runSingle(sourceImage)
                        : SetCounters.countObjectsStainClassImage(set, classNames).runSingle(sourceImage));
            }

        };
    }
}
