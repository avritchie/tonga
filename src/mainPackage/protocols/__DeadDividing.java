package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.COLOUR;
import static mainPackage.PanelCreator.ControlType.LAYER;
import mainPackage.counters.Counter;
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
    }

    @Override
    protected Processor getProcessor() {
        int sourceCol = param.colorARGB[0];

        return new ProcessorFast("Dead/dividing", 3) {

            int preCount, postCount;
            int[] rem;

            @Override
            protected void methodInit() {
                ROISet set = new ImageTracer(inImage[0], sourceCol).trace();
                preCount = set.list.size();
                set.filterDeadDividing(inImage[1]);
                postCount = set.list.size();
                rem = set.drawToArray(true);
            }

            @Override
            protected void methodCore(int p) {
                outImage[0].pixels32[p] = inImage[0].pixels32[p] != sourceCol ? rem[p] == COL.BLACK ? COL.WHITE : COL.GRAY : COL.BLACK;
            }

            @Override
            protected void methodFinal() {
                addResultData(sourceImage);
                //newResultRow(,,);
            }

            @Override
            protected Counter processorCounter() {
                return new Counter("Count dead and dividing nuclei", new String[]{"Image", "Alive", "Dead/dividing", "Total"},
                        new String[]{"The name of the image",
                            "The number of nuclei considered alive",
                            "The number of nuclei considered dead or dividing",
                            "The total number of detected nuclei"}) {

                    @Override
                    protected void pixelProcessor(ImageData targetImage) {
                        row[1] = postCount;
                        row[2] = preCount - postCount;
                        row[3] = preCount;
                    }
                };
            }
        };

    }
}
