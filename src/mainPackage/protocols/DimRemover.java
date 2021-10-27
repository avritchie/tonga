package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class DimRemover extends Protocol {

    @Override
    public String getName() {
        return "Remove dim objects";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(LAYER, "The image with the stain"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(SLIDER, "Remove if intensity is less than (%)", 10),
            new ControlReference(TOGGLE, "Compared to the average stain", 0, new int[]{3, 0, 5, 1}),
            new ControlReference(SLIDER, new Object[]{0, 20, 220, 400}, "Remove if intensity is less than (%) of the average")};
    }

    @Override
    protected Processor getProcessor() {
        double thresh = param.slider[0] / 100.;
        double thresh2 = param.sliderScaled[1] / 100.;

        return new ProcessorFast("Dim", 4) {
            ImageData temp;

            @Override
            protected void pixelProcessor() {
                /*ROISet set = new ImageTracer(inImage[0], param.colorARGB[0]).trace();
                set.imageEdgeFixer(false);
                set.findOuterEdges();
                temp = set.drawEdgeImage();
                ROISet set2 = new ImageTracer(temp, COL.BLACK).trace();
                set2.quantifyStainAgainstChannel(inImage[1]);
                temp = set2.drawStainImage(thresh, true);
                setOutputBy(set.getPositionFilteredSet(temp, COL.BLACK, true).drawToImageData(true));*/

                ROISet set = new ImageTracer(inImage[0], param.colorARGB[0]).trace();
                set.quantifyStainAgainstChannel(inImage[1]);
                if (param.toggle[0]) {
                    set.filterOutDimObjects(set.avgStain() * thresh2);
                } else {
                    set.filterOutDimObjects(thresh);
                }
                setOutputBy(set.drawToImageData(true));
            }
        };
    }
}
