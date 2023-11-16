package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaAnnotator.AnnotationType;
import static mainPackage.TongaAnnotator.AnnotationType.DOT;
import mainPackage.utils.GEO;

/**
 *
 * @author aritchie
 */
public class RadialArea extends Protocol {

    @Override
    public String getName() {
        return "Radial";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image is on which layer"),
            new ControlReference(ANNOTATION, new AnnotationType[]{DOT}, "Which annotation to use"),
            new ControlReference(COLOUR, "Set the background as", new int[]{0}),
            new ControlReference(SPINNER, "Radius", 1000),
            new ControlReference(SPINNER, "Bins", 4)};
    }

    @Override
    protected Processor getProcessor() {
        int radi = param.spinner[0];
        int bins = param.spinner[1];
        String[] names = new String[bins];
        for (int i = 0; i < bins; i++) {
            names[i] = "Bin " + (i + 1);
        }

        return new ProcessorFast(bins, names, 4) {

            @Override
            protected void pixelProcessor() {
                int posx = lparam.annotation[0].points.get(0).x;
                int posy = lparam.annotation[0].points.get(0).y;
                int[] rads = new int[2];
                for (int b = 0; b < bins; b++) {
                    outImage[b] = initTempData();
                    rads[0] = radi * (b + 1);
                    rads[1] = radi * (b);
                    applyOperator(outImage[b], outImage[b], p
                            -> GEO.getDist(posx, posy, p % sourceWidth[0], p / sourceWidth[0]) < rads[0]
                            && GEO.getDist(posx, posy, p % sourceWidth[0], p / sourceWidth[0]) >= rads[1]
                            ? inImage[0].pixels32[p] : param.colorARGB[0]);
                    setOutputBy(outImage[b], b);
                }
            }

        };
    }
}
