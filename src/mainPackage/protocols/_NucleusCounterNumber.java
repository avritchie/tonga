package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;

public class _NucleusCounterNumber extends Protocol {

    @Override
    public String getName() {
        return "Count the number of cells";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Segment overlapping nuclei", 1),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Report results as individual objects")};
    }

    @Override
    protected Processor getProcessor() {
        boolean toucherMode = param.toggle[0];
        boolean segmMode = param.toggle[1];
        boolean deadMode = param.toggle[2];
        boolean imageMode = param.toggle[3];

        return new ProcessorFast("Nuclei", 223) {

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusMask::new);
                Protocol cnt = Protocol.load(_ObjectCount::new);
                ImageData[] mask = nc.runSilent(sourceImage, new ImageData[]{inImage[0]}, toucherMode, deadMode, segmMode);
                cnt.runSilent(sourceImage, new ImageData[]{mask[0]}, COL.BLACK, imageMode);
                setOutputBy(mask);
                addResultData(cnt);
            }
        };
    }
}
