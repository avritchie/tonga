package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.counters.TableData;

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
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Perform overlapping area segmenting", 1)};
    }

    @Override
    protected Processor getProcessor() {
        boolean toucherMode = param.toggle[0];
        boolean deadMode = param.toggle[1];
        boolean segmMode = param.toggle[2];

        return new ProcessorFast(Tonga.debug() ? 3 : 1, "Nuclei", 165) {

            ImageData[] separation;
            ImageData adjusted;
            Protocol subprotocol;

            @Override
            protected void pixelProcessor() {
                //guess the size of the cells/
                subprotocol = Protocol.load(_EstimateNucleusSize::new);
                subprotocol.runSilent(sourceImage, inImage[0]);
                double nuclSize = TableData.getType(subprotocol.results.getVal(0, 1));
                subprotocol = Protocol.load(__NucleusPrimaryMask::new);
                separation = subprotocol.runSilent(sourceImage, inImage[0], nuclSize);
                adjusted = separation[1];
                setSampleOutputBy(separation[0], 1);
                subprotocol = Protocol.load(__ObjectSegment::new);
                separation = subprotocol.runSilent(sourceImage, separation[0], COL.BLACK, nuclSize, segmMode ? 0 : 2, true);
                setSampleOutputBy(separation[0], 2);
                subprotocol = Protocol.load(__NucleusFinalMask::new);
                separation = subprotocol.runSilent(sourceImage, new ImageData[]{separation[0], inImage[0], adjusted}, toucherMode, deadMode, nuclSize, true);
                setOutputBy(separation[0], 0);
            }
        };
    }

}
