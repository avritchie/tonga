package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;

public class _NucleusCounterIntensity extends Protocol {

    @Override
    public String getName() {
        return "Measure intensity of a nuclear staining";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(LAYER, "The channel with the stain"),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 1),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Segment overlapping nuclei", 1),
            //new ControlReference(SLIDER, new Integer[]{10, 200, 580, 38}, "Average size of a nucleus", 1),
            //new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        //int limit = param.spinner[0];
        //double size = param.sliderScaled[0];
        boolean bgMode = param.toggle[0];
        boolean toucherMode = param.toggle[1];
        boolean segmMode = param.toggle[2];
        boolean deadMode = param.toggle[3];
        boolean imgMode = param.toggle[4];

        return new ProcessorFast("Nucleus Intensities", bgMode ? 187 : 171) {

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusCounterSelfIntensity::new);
                ImageData[] id = nc.runSilent(sourceImage,
                        new ImageData[]{inImage[0], inImage[1]},
                        false, 0, bgMode, toucherMode, segmMode, deadMode, imgMode);
                setOutputBy(id);
                addResultData(nc);
            }
        };
    }
}
