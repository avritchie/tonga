package mainPackage.protocols;

import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class __DeadDividing extends Protocol {

    @Override
    public String getName() {
        return "Remove dead and dividing";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Get mask from which layer"),
            new ControlReference(LAYER, "Original DAPI/Hoechst"),
            new ControlReference(COLOUR, "Background colour", new int[]{0})};
        //new ControlReference(SLIDER, new Integer[]{10, 200, 580, 38}, "Average size of a nucleus", 5)};
    }

    @Override
    protected Processor getProcessor() {
        int sourceCol = param.colorARGB[0];

        return new ProcessorFast("Filtered", 1) {

            @Override
            protected void pixelProcessor() {
                initTableData(new String[]{"Image", "Alive", "Dead/dividing", "Total"});
                ROISet set = new ImageTracer(inImage[0], sourceCol).trace();
                int preCount = set.list.size();
                set.filterDeadDividing(inImage[1]);
                int postCount = set.list.size();
                setOutputBy(set.drawToImageData(true));
                Object[] newRow = data.newRow(sourceImage.imageName);
                newRow[1] = (Integer) postCount;
                newRow[2] = (Integer) (preCount - postCount);
                newRow[3] = (Integer) preCount;
            }
        };
    }
}
