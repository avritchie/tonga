package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.utils.COL;

public class __NucleusCounterSelfIntensity extends Protocol {

    @Override
    public String getName() {
        return "Measure nuclear staining";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(LAYER, "The channel with the stain"),
            new ControlReference(TOGGLE, "Binary staining", 1, new int[]{3, 1}),
            new ControlReference(SLIDER, "Intensity to consider positive (%)", 50),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 1),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            //new ControlReference(SLIDER, new Integer[]{10, 200, 580, 38}, "Average size of a nucleus", 1),
            //new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Segment overlapping nuclei", 1),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        //Integer limit = param.spinner[0];
        //double size = param.sliderScaled[0];
        int thresh = param.slider[0];
        boolean binMode = param.toggle[0];
        boolean bgMode = param.toggle[1];
        boolean toucherMode = param.toggle[2];
        boolean segmMode = param.toggle[3];
        boolean deadMode = param.toggle[4];
        boolean imgMode = param.toggle[5];

        return new ProcessorFast("Nucleus Staining", bgMode ? 117 : 105) {

            ImageData[] mask;

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusMask::new);
                Protocol asi = Protocol.load(_AreaStainIntensity::new);
                mask = nc.runSilent(sourceImage, new ImageData[]{inImage[0]}, toucherMode, deadMode, segmMode);
                mask = asi.runSilent(sourceImage, new ImageData[]{mask[0], inImage[1], inImage[0]},
                        COL.BLACK, binMode, thresh, imgMode, true, bgMode);
                setOutputBy(mask);
                addResultData(asi);
            }
        };
    }
}
