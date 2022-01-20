package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;

public class _NucleusCounterPositivity extends Protocol {

    @Override
    public String getName() {
        return "Measure positivity for a nuclear staining";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(LAYER, "The channel with the stain"),
            new ControlReference(SLIDER, "Intensity to consider positive (%)", 50),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 1),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            //new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1)};
    }

    @Override
    protected Processor getProcessor() {
        //int limit = param.spinner[0];
        int thresh = param.slider[0];
        boolean bgMode = param.toggle[0];
        boolean toucherMode = param.toggle[1];
        boolean deadMode = param.toggle[2];

        return new ProcessorFast("Positive Nuclei", bgMode ? 174 : 159) {

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusCounterSelfIntensity::new);
                ImageData[] id = nc.runSilent(sourceImage,
                        new ImageData[]{inImage[0], inImage[1]},
                        true, thresh, bgMode, toucherMode, deadMode, true);
                setOutputBy(id);
                setDatasBy(nc);
            }
        };
    }
}
