package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;

public class __NucleusMask extends Protocol {

    @Override
    public String getName() {
        return "Construct a nucleus mask";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1)};
    }

    @Override
    protected Processor getProcessor() {
        int limit = param.spinner[0];
        boolean toucherMode = param.toggle[0];
        boolean deadMode = param.toggle[1];

        return new ProcessorFast(3, "Nuclei", 102) {

            ImageData[] separation;
            Protocol subprotocol;

            @Override
            protected void pixelProcessor() {
                //guess the size of the cells/
                subprotocol = Protocol.load(_EstimateNucleusSize::new);
                subprotocol.runSilent(sourceImage, inImage[0]);
                double nuclSize = (Integer) subprotocol.results.getVal(0, 1);
                subprotocol = Protocol.load(__NucleusPrimaryMask::new);
                separation = subprotocol.runSilent(sourceImage, inImage[0], limit, nuclSize);
                setOutputBy(separation[0], 1);
                subprotocol = Protocol.load(__ObjectSegment::new);
                separation = subprotocol.runSilent(sourceImage, separation[0], COL.BLACK, nuclSize);
                setOutputBy(separation[0], 2);
                subprotocol = Protocol.load(__NucleusFinalMask::new);
                separation = subprotocol.runSilent(sourceImage, new ImageData[]{separation[0], inImage[0]}, toucherMode, limit, deadMode, nuclSize);
                setOutputBy(separation[0], 0);
            }
        };
    }

}
