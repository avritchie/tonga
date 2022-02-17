package mainPackage.protocols;

import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

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
                initTableData(new String[]{"Image", "Alive", "Dead/dividing", "Total"},
                        new String[]{"The name of the image",
                            "The number of nuclei considered alive",
                            "The number of nuclei considered dead or dividing",
                            "The total number of detected nuclei"});
                ROISet set = new ImageTracer(inImage[0], sourceCol).trace();
                int preCount = set.list.size();
                set.filterDeadDividing(inImage[1]);
                int postCount = set.list.size();
                int[] rem = set.drawToArray(true);
                Iterate.pixels(inImage[0], (int pos) -> {
                    outImage[0].pixels32[pos] = inImage[0].pixels32[pos] != sourceCol ? rem[pos] == COL.BLACK ? COL.WHITE : COL.GRAY : COL.BLACK;
                });
                newResultRow(postCount, preCount - postCount, preCount);
            }
        };
    }
}
