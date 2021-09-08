package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.utils.COL;

public class _NucleusCounterSurroundIntensity extends Protocol {

    @Override
    public String getName() {
        return "Measure intensity of nuclear surroundings";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(LAYER, "The channel with the stain"),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(SPINNER, "Radius", 50),
            new ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        boolean toucherMode = param.toggle[0];
        boolean deadMode = param.toggle[1];
        boolean imgMode = param.toggle[2];
        int radius = param.spinner[0];

        return new ProcessorFast("Nucleus Surroundings", 105) {

            ImageData[] mask;

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusMask::new);
                Protocol asi = Protocol.load(_AreaSurroundIntensity::new);
                mask = nc.runSilent(sourceImage, new ImageData[]{inImage[0]}, toucherMode, 50, deadMode);
                mask = asi.runSilent(sourceImage, new ImageData[]{mask[0], inImage[1], inImage[0]},
                        COL.BLACK, radius, imgMode);
                setOutputBy(mask);
                setDatasBy(asi);
            }
        };
    }
}
