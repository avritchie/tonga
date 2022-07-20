package mainPackage.filters;

import static mainPackage.filters.Filter.noParams;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class TestFilter {

    public static FilterFast test() {
        return new FilterFast("Test", noParams) {
            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, COL.BLACK).trace();
                setOutputBy(set.drawToImageData());
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }
}
