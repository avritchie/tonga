package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Tonga;
import mainPackage.counters.Counters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import static mainPackage.filters.FiltersPass.averageGradientIntensity;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class IFCorrectAll extends Protocol {

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
            new ControlReference(TOGGLE, "Correct uneven staining", 1),
            new ControlReference(TOGGLE, "Correct background staining", 1),
            new ControlReference(TOGGLE, "Remove non-tissue areas", 1, new int[]{6, 1}),
            new ControlReference(COMBO, new String[]{"Binarization series", "DAPI subtraction", "Combined"}, "Removal method", 2)};
    }

    @Override
    protected Processor getProcessor() {
        int tissueScale = param.slider[0];
        int dapiChannel = param.combo[0];
        int remMethod = param.combo[1];
        int iterations = 0;
        iterations += param.toggle[0] ? 93 : 0;
        iterations += param.toggle[1] ? 20 : 0;
        iterations += param.toggle[2] ? 41 : 0;
        iterations -= param.toggle[0] && param.toggle[1] ? 1 : 0;
        iterations += param.toggle[2] && param.toggle[1] ? 8 : 0;

        return new ProcessorFast(fullOutput() ? 5 : 1, "Normalized", iterations) {

            ImageData normalized, dapi, ifone, iftwo, binar, temp;
            double avgdapi, avgifone, avgiftwo;

            @Override
            protected void pixelProcessor() {
                if (param.toggle[0]) {
                    normalized = Protocol.load(IFNormalize::new).runSilent(sourceImage, inImage[0], tissueScale, dapiChannel, true)[0];
                    setSampleOutputBy(normalized, 1);
                }
                if (param.toggle[2]) {
                    dapi = Filters.separateChannel().runSingle(param.toggle[0] ? normalized : inImage[0], param.combo[0]);
                    //construct one or two masks based on the selected method(s)
                    if (remMethod != 1) {
                        binar = FiltersPass.multiLocalThreshold().runSingle(dapi, tissueScale);
                    }
                    if (remMethod != 0 && param.toggle[0]) {
                        temp = Filters.separateChannel().runSingle(inImage[0], param.combo[0]);
                        temp = Blender.renderBlend(new ImageData[]{dapi, temp, temp}, Blend.SUBTRACT);
                    }
                    //combine them into one
                    switch (remMethod) {
                        case 0: {
                            //nothing to do since the first method already uses the binary channel
                            break;
                        }
                        case 1: {
                            //transform the result into binary and transfer into the binary channel
                            applyOperator(temp, temp, p -> temp.pixels32[p] != COL.BLACK ? COL.BLACK : COL.WHITE);
                            binar = temp;
                            break;
                        }
                        case 2: {
                            //combine the result with the binary from the first method
                            if (param.toggle[0]) {
                                applyOperator(binar, binar, p -> temp.pixels32[p] != COL.BLACK || binar.pixels32[p] == COL.BLACK ? COL.BLACK : COL.WHITE);
                            }
                            break;
                        }
                    }
                    /*
                    temp = FiltersPass.adaptiveThreshold().runSingle(temp, COL.dataCornerColour(temp), 0.0, tissueScale);
                    temp = Filters.connectEdges().runSingle(temp, COL.BLACK, true);
                    temp = Protocol.load(BrightRemover::new).runSilent(null, new ImageData[]{temp, param.toggle[0] ? temp2 : inImage[0]}, COL.WHITE, 0, 1.0)[0];
                    temp = FiltersPass.edgeDilate().runSingle(temp, COL.BLACK, 1, true);
                     */
                    temp = Blender.renderBlend(binar, normalized == null ? inImage[0] : normalized, Blend.MULTIPLY);
                    setSampleOutputBy(temp, 2);
                }
                if (param.toggle[1]) {
                    //extract the channels from either the input or the normalized image if done
                    dapi = Filters.separateChannel().runSingle(param.toggle[2] ? temp : param.toggle[0] ? normalized : inImage[0], param.combo[0]);
                    ifone = Filters.separateChannel().runSingle(temp == null ? normalized == null ? inImage[0] : normalized : temp, param.combo[0] == 0 ? 1 : 0);
                    iftwo = Filters.separateChannel().runSingle(temp == null ? normalized == null ? inImage[0] : normalized : temp, param.combo[0] < 2 ? 2 : 1);
                    //obtain average from areas with low gradient
                    int rad = Math.max(1, 5 - (tissueScale / 10));
                    avgdapi = averageGradientIntensity(dapi, dapi.pixels32, rad, 25, COL.BLACK);
                    avgifone = averageGradientIntensity(ifone, ifone.pixels32, rad, 25, COL.BLACK);
                    avgiftwo = averageGradientIntensity(iftwo, iftwo.pixels32, rad, 25, COL.BLACK);
                    //but apply it to the image with tissue area
                    if (param.toggle[2]) {
                        ifone = Filters.separateChannel().runSingle(normalized == null ? inImage[0] : normalized, param.combo[0] == 0 ? 1 : 0);
                        iftwo = Filters.separateChannel().runSingle(normalized == null ? inImage[0] : normalized, param.combo[0] < 2 ? 2 : 1);
                    }
                    applyOperator(ifone, ifone, p -> RGB.argb(
                            (int) Math.max(0x00, (ifone.pixels32[p] >> 16 & 0xFF) - (int) avgifone),
                            (int) Math.max(0x00, (ifone.pixels32[p] >> 8 & 0xFF) - (int) avgifone),
                            (int) Math.max(0x00, (ifone.pixels32[p] & 0xFF) - (int) avgifone), 255));
                    applyOperator(iftwo, iftwo, p -> RGB.argb(
                            (int) Math.max(0x00, (iftwo.pixels32[p] >> 16 & 0xFF) - (int) avgiftwo),
                            (int) Math.max(0x00, (iftwo.pixels32[p] >> 8 & 0xFF) - (int) avgiftwo),
                            (int) Math.max(0x00, (iftwo.pixels32[p] & 0xFF) - (int) avgiftwo), 255));
                    dapi = Filters.separateChannel().runSingle(normalized == null ? inImage[0] : normalized, param.combo[0]);
                    normalized = Blender.renderBlend(new ImageData[]{ifone, iftwo, dapi}, Blend.ADD);
                    setSampleOutputBy(normalized, 3);
                    //then find "non-tissue areas" where there is tissue
                    //this is needed because the tissue detection is based on dapi but there may be tissue area also not marked by nuclei
                    //this must be done only now that background has been corrected because otherwise the background will be detected as tissue
                    if (param.toggle[2]) {
                        //mask areas of strong if staining
                        applyOperator(temp, temp, p -> RGB.brightness(ifone.pixels32[p] | iftwo.pixels32[p]) >= avgdapi - 1 ? COL.WHITE : COL.BLACK);
                        Filters.connectEdges().runTo(temp, COL.BLACK, true);
                        //merge with the mask constructed with dapi only
                        temp = Blender.renderBlend(new ImageData[]{binar, temp}, Blend.ADD);
                        setSampleOutputBy(binar, 3);
                        temp = FiltersPass.edgeDilate().runSingle(temp, COL.BLACK, 1, true);
                        setSampleOutputBy(temp, 4);
                        //apply the mask to the non-masked but otherwise finished image
                        applyOperator(temp, temp, p -> binar.pixels32[p] == COL.WHITE || temp.pixels32[p] == COL.WHITE ? normalized.pixels32[p] : COL.BLACK);
                    }
                    //temp = FiltersPass.backgroundStainingScaling().runSingle(temp == null ? inImage[0] : temp, param.combo[0] != 0, param.combo[0] != 1, param.combo[0] != 2);
                }
                /*
                if (param.toggle[2]) {
                    temp = Filters.tissueOverlapCorrection().runSingle(temp == null : inImage[0] ? temp, true, true, true);
                }*/
                setOutputBy(temp == null ? normalized == null ? inImage[0] : normalized : temp);
            }

            /*protected double average(ImageData channel, int threshold) {
                temp = Filters.maximumDiffEdge().runSingle(channel, 0, rad, false, threshold);
                applyOperator(channel, temp, p -> channel.pixels32[p] == COL.BLACK || (temp.pixels32[p] & 0xFF) > 25 ? COL.BLACK : channel.pixels32[p]);
                return Filters.averageIntensity(inImage[0], channel.pixels32, COL.BLACK);
            }*/
            @Override
            protected void methodFinal() {
                addResultData(Counters.countIFChannels().runSingle(sourceImage, outImage[0]));
            }

        };
    }
}
