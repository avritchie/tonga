package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.utils.COL;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class DAPICorrect extends Protocol {

    @Override
    public String getName() {
        return "Correct Nonlinearly";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image is on which layer"),
            new ControlReference(SLIDER, "Tissue scale factor", 40),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "DAPI is on which channel", 2)};
    }

    @Override
    protected Processor getProcessor() {
        int tissueScale = param.slider[0];

        return new ProcessorFast(fullOutput() ? 4 : 1, "Corrected", 35) {

            ImageData binar, inten, temp;

            @Override
            protected void pixelProcessor() {
                //get the blue channel
                temp = Filters.separateChannel().runSingle(inImage[0], param.combo[0]);
                //get the binary mask
                binar = FiltersPass.adaptiveThreshold().runSingle(temp, COL.dataCornerColour(temp), 2.5, tissueScale);
                setSampleOutputBy(binar, 1);
                //make linearily adapted and gamma-adapted versions of the blue channel
                inten = Filters.autoscaleWithAdapt().runSingle(temp, 1.0);
                //get scaling percentage during adaptive autoscale
                int[] adapt = HISTO.getMinMaxAdapt(HISTO.getHistogram(temp.pixels32), 1.0);
                double scale = adapt[1] / 255.;
                setSampleOutputBy(inten, 2);
                Filters.gamma().runTo(temp, 0.1);
                //get intensity difference comparing linear and gamma
                inten = Blender.renderBlend(inten, temp, Blend.DIFFERENCE);
                Filters.bwBrightness().runTo(inten);
                Filters.invert().runTo(inten);
                //scale with the value calculated above
                applyOperator(inten, inten, p -> RGB.multiplyColor(inten.pixels32[p], scale) | 0xFF000000);
                //fill gaps with the surrounding average to avoid over-correcting non-tissue areas
                applyOperator(inten, inten, p -> binar.pixels32[p] == COL.BLACK ? COL.BLACK : inten.pixels32[p]);
                Filters.blurConditional().runTo(inten, temp, COL.BLACK, tissueScale / 4, false);
                inten = FiltersPass.edgeDilate().runSingle(temp, COL.BLACK, tissueScale * 2);
                setSampleOutputBy(inten, 3);
                //perform the correcting and output
                Protocol.load(ApplyIllumination::new).runSilentTo(sourceImage, new ImageData[]{inImage[0], inten}, outImage[0], true, true, false, 0);
            }

        };
    }
}
