package mainPackage.filters;

import static mainPackage.filters.Filter.bgcol;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class DrawObjectImage {

    public static FilterFast run() {
        return new FilterFast("Traced", bgcol) {

            @Override
            protected void processor() {
                ImageTracer tracer = new ImageTracer(inData, param.color[0]);
                ROISet rois = tracer.trace();
                rois.findOuterEdges();
                outData.pixels32 = rois.drawToArray();
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }
}
