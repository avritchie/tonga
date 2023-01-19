package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import static mainPackage.protocols.Processor.applyOperator;
import mainPackage.utils.COL;

/**
 *
 * @author aritchie
 */
public class IFNormalize extends Protocol {

    @Override
    public String getName() {
        return "Correct IF";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image is on which layer"),
            new ControlReference(SLIDER, "Tissue scale factor", 40),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "DAPI is on which channel", 2),
            new ControlReference(TOGGLE, "Correct each channel separately", 1)};
    }

    @Override
    protected Processor getProcessor() {
        int tissueScale = param.slider[0];
        boolean allWithDAPI = !param.toggle[0];
        String[] names = new String[]{"Corrected", "DAPI Mask", "Channel 1 Mask", "Channel 2 Mask"};

        return new ProcessorFast(Tonga.debug() ? 4 : 1, names, allWithDAPI ? 32 : 58) {

            ImageData dapi, ifchan1, ifchan2, binar, temp;
            int rad;
            int color;

            @Override
            protected void pixelProcessor() {
                rad = Math.max(1, 5 - (param.slider[0] / 10));
                color = COL.dataCornerColour(inImage[0]);
                //get the blue channel
                dapi = Filters.separateChannel().runSingle(inImage[0], param.combo[0]);
                ifchan1 = Filters.separateChannel().runSingle(inImage[0], param.combo[0] == 0 ? 1 : 0);
                ifchan2 = Filters.separateChannel().runSingle(inImage[0], param.combo[0] < 2 ? 2 : 1);
                //get the binary mask
                binar = FiltersPass.adaptiveThreshold().runSingle(dapi, color, 5.0, tissueScale);
                //perform dapi
                dapi = Blender.renderBlend(binar, dapi, Blend.MULTIPLY);
                temp = Filters.blurConditional().runSingle(dapi, COL.BLACK, tissueScale / 4, false);
                setSampleOutputBy(temp, 1);
                Protocol.load(ApplyIllumination::new).runSilentTo(sourceImage, new ImageData[]{dapi, temp, binar}, outImage[0], true, false, true, COL.BLACK, true, binar);
                //correct the bg mask abnormality
                //applyOperator(outImage[0], outImage[0], p -> RGB.brightness(outImage[0].pixels32[p]) - RGB.brightness(dapi.pixels32[p]) - RGB.brightness(temp.pixels32[p]) > 0 ? COL.BLACK : outImage[0].pixels32[p]);
                //perform other channels
                processChannel(ifchan1);
                setSampleOutputBy(temp, 2);
                processChannel(ifchan2);
                setSampleOutputBy(temp, 3);
                //apply background removal
                //get tissue mask
                setOutputBy(Blender.renderBlend(new ImageData[]{outImage[0], ifchan1, ifchan2}, Blend.ADD));
            }

            private void processChannel(ImageData channel) {
                if (!allWithDAPI) {
                    temp = FiltersPass.backgroundStainingAvgSubtraction().runSingle(channel, true, true, true, color, tissueScale, 1.0, true);
                    //temp = Filters.maximumDiffEdge().runSingle(channel, 0, rad, true, 20);
                    //applyOperator(temp, temp, p -> temp.pixels32[p] == COL.WHITE || binar.pixels32[p] == COL.BLACK ? COL.BLACK : channel.pixels32[p]);
                    applyOperator(temp, temp, p -> binar.pixels32[p] == COL.BLACK ? COL.BLACK : temp.pixels32[p]);
                    /*
                    int peak = HISTO.getHighestPointIndex(HISTO.getHistogram(channel.pixels32), true);
                    double scale = peak / 255.;
                    Filters.cutFilter().runTo(channel, temp, 0, peak + 3);
                    applyOperator(temp, temp, p -> RGB.multiplyColor(temp.pixels32[p], scale) | 0xFF000000);
                    temp = Blender.renderBlend(binar, temp, Blend.MULTIPLY);*/
                    temp = Filters.blurConditional().runSingle(temp, COL.BLACK, tissueScale / 4, false);
                }
                Protocol.load(ApplyIllumination::new).runSilentTo(sourceImage, new ImageData[]{channel, temp, binar}, channel, true, true, true, COL.BLACK, true, binar);
            }

        };
    }
}
