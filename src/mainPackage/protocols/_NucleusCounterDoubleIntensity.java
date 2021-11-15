package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersRender;
import mainPackage.utils.COL;

public class _NucleusCounterDoubleIntensity extends Protocol {

    @Override
    public String getName() {
        return "Measure intensity of several nuclear stains";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "The channel with DAPI/Hoechst"),
            new ControlReference(LAYER, "The channel with the stain one"),
            new ControlReference(LAYER, "The channel with the stain two"),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 1),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Detect and remove dividing/dead cells", 1),
            new ControlReference(TOGGLE, "Results as average per image", 0)};
    }

    @Override
    protected Processor getProcessor() {
        boolean bgMode = param.toggle[0];
        boolean toucherMode = param.toggle[1];
        boolean deadMode = param.toggle[2];
        boolean imgMode = param.toggle[3];

        return new ProcessorFast(2, "Nucleus Stainings", bgMode ? 173 : 161) {

            ImageData[] mask;

            @Override
            protected void pixelProcessor() {
                Protocol nc = Protocol.load(__NucleusMask::new);
                Protocol asi = Protocol.load(_AreaDoubleStainIntensity::new);
                mask = nc.runSilent(sourceImage, new ImageData[]{inImage[0]}, toucherMode, deadMode);
                mask = asi.runSilent(sourceImage, new ImageData[]{mask[0], inImage[1], inImage[2], inImage[0]},
                        COL.BLACK, imgMode, bgMode);
                setOutputBy(mask);
                setDatasBy(asi);
            }
        };
    }
}
