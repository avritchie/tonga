package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;

public class __NucleusMaskOfSize extends Protocol {

    @Override
    public String getName() {
        return "Construct a nucleus mask";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Perform overlapping area segmenting", 1),
            new ControlReference(SPINNER, "Target size (pixels)", 40)};
    }

    @Override
    protected Processor getProcessor() {
        int nuclSize = param.spinner[0];
        boolean toucherMode = param.toggle[0];
        boolean deadMode = param.toggle[1];
        boolean segmMode = param.toggle[2];

        return new ProcessorFast(fullOutput() ? 3 : 1, "Nuclei", 165) {

            ImageData[] separation;
            ImageData adjusted;
            Protocol subprotocol;

            @Override
            protected void pixelProcessor() {
                subprotocol = Protocol.load(__NucleusPrimaryMask::new);
                separation = subprotocol.runSilent(sourceImage, inImage[0], nuclSize);
                adjusted = separation[1];
                setSampleOutputBy(separation[0], 1);
                subprotocol = Protocol.load(__ObjectSegment::new);
                separation = subprotocol.runSilent(sourceImage, separation[0], COL.BLACK, nuclSize, segmMode ? 0 : 3, true);
                setSampleOutputBy(separation[0], 2);
                subprotocol = Protocol.load(__NucleusFinalMask::new);
                separation = subprotocol.runSilent(sourceImage, new ImageData[]{separation[0], inImage[0], adjusted}, toucherMode, deadMode, nuclSize, true);
                setOutputBy(separation[0], 0);
            }
        };
    }

}
