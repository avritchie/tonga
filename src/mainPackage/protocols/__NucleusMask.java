package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
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

        return new ProcessorFast(fullOutput() ? 3 : 1, "Nuclei", 165) {

            ImageData[] separation;
            Protocol subprotocol;

            @Override
            protected void pixelProcessor() {
                //guess the size of the cells/
                subprotocol = Protocol.load(_EstimateNucleusSize::new);
                subprotocol.runSilent(sourceImage, inImage[0]);
                double nuclSize = TableData.getType(subprotocol.results.getVal(0, 1));
                subprotocol = Protocol.load(__NucleusMaskOfSize::new);
                separation = subprotocol.runSilent(sourceImage, inImage[0], toucherMode, deadMode, segmMode, nuclSize);
                setOutputBy(separation[0]);
                setSampleOutputBy(separation[0], 1);
                setSampleOutputBy(separation[0], 2);
            }
        };
    }

}
