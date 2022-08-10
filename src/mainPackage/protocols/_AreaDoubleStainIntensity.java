package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.Filters;
import mainPackage.utils.RGB;

public class _AreaDoubleStainIntensity extends Protocol {

    @Override
    public String getName() {
        return "Calculate double staining on a mask";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(LAYER, "The image with the stain one"),
            new ControlReference(LAYER, "The image with the stain two"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(TOGGLE, "Results as average per image", 0),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 0, new int[]{6, 1}),
            new ControlReference(LAYER, "The image with DAPI/Hoechst")};
    }

    @Override
    protected Processor getProcessor() {
        boolean imgMode = param.toggle[0];
        boolean bgMode = param.toggle[1];

        return new ProcessorFast(2, "Objects", bgMode ? 21 : 5) {
            ImageData[] mask;
            ImageData stOne, stTwo, stComb;

            @Override
            protected void pixelProcessor() {
                Protocol asi = Protocol.load(_AreaStainIntensity::new);
                boolean is16bits = inImage[1].bits == 16 && inImage[2].bits == 16;
                stOne = is16bits ? inImage[1] : Filters.bwBrightness().runSingle(inImage[1]);
                stTwo = is16bits ? inImage[2] : Filters.bwBrightness().runSingle(inImage[2]);
                //multiply-stack
                if (is16bits) {
                    short[] combPixels = new short[sourceWidth[0] * sourceHeight[0]];
                    Iterate.pixels(outImage[0], (int p) -> {
                        combPixels[p] = (short) (stOne.pixels16[p] * stTwo.pixels16[p] / 65536);
                    });
                    stComb = new ImageData(combPixels, sourceWidth[0], sourceHeight[0]);
                } else {
                    int[] combPixels = new int[sourceWidth[0] * sourceHeight[0]];
                    Iterate.pixels(outImage[0], (int p) -> {
                        combPixels[p] = RGB.argb((stOne.pixels32[p] & 0xFF) * (stTwo.pixels32[p] & 0xFF) / 256);
                    });
                    stComb = new ImageData(combPixels, sourceWidth[0], sourceHeight[0]);
                }
                mask = asi.runSilent(sourceImage, new ImageData[]{inImage[0], stComb, inImage[3]},
                        param.colorARGB[0], false, 0, imgMode, false, bgMode);
                setOutputBy(stComb, 1);
                setOutputBy(mask[0], 0);
                addResultData(asi);
            }
        };
    }
}
