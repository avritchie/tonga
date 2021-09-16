package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.TongaRender;
import mainPackage.counters.SetCounters;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class _AreaStainIntensity extends Protocol {

    @Override
    public String getName() {
        return "Calculate stainings on a mask";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(LAYER, "The image with the stain"),
            new ControlReference(COLOUR, "Background is which color", new int[]{0}),
            new ControlReference(TOGGLE, "Binary staining", 1, new int[]{4, 1, 7, 0}),
            new ControlReference(SLIDER, "Binary threshold (%)"),
            new ControlReference(TOGGLE, "Results as average per image", 0),
            new ControlReference(TOGGLE, "Render the image using the average stain", 0),
            new ControlReference(TOGGLE, "Estimate and subtract the background", 0, new int[]{7, 1}),
            new ControlReference(LAYER, "The image with DAPI/Hoechst")};
    }

    @Override
    protected Processor getProcessor() {
        boolean binst = param.toggle[0];
        boolean perimg = param.toggle[1];
        boolean rendav = param.toggle[2];
        boolean rembg = param.toggle[3];
        double thresh = param.slider[0] / 100.;

        return new ProcessorFast("Objects") {
            ImageData bgid;
            double bgval;

            @Override
            protected void pixelProcessor() {
                ROISet set = new ImageTracer(inImage[0], param.color[0]).trace();
                //Filters.gaussApprox().runSingle(inImage[2], 2, true)
                set.quantifyStainAgainstChannel(inImage[1]);
                if (rembg) {
                    // get the background mask and value
                    ImageData r = TongaRender.blend(inImage[1], inImage[2]);
                    Protocol a = Protocol.load(_BackgroundArea::new);
                    bgid = a.runSilent(sourceImage, new ImageData[]{r, inImage[1]}, 80)[0];
                    bgval = (double) a.results.getVal(0, 3) / 100.;
                }
                if (binst) {
                    outImage[0].pixels32 = set.drawStainArray(thresh);
                    if (rembg) {
                        datas.add(SetCounters.countObjectPositiveBG(set, bgval, thresh).runSingle(sourceImage));
                    } else {
                        datas.add(SetCounters.countObjectPositive(set, thresh).runSingle(sourceImage));

                    }
                } else {
                    outImage[0].pixels32 = set.drawStainArray(rendav);
                    if (rembg) {
                        if (perimg) {
                            datas.add(SetCounters.countObjectStainsBGImage(set, bgval).runSingle(sourceImage));
                        } else {
                            datas.add(SetCounters.countObjectStainsBGSingle(set, bgval).runSingle(sourceImage));
                        }
                    } else {
                        if (perimg) {
                            datas.add(SetCounters.countObjectStainsImage(set).runSingle(sourceImage));
                        } else {
                            datas.add(SetCounters.countObjectStainsSingle(set).runSingle(sourceImage));
                        }
                    }
                }
                if (rembg) {
                    // draw the background mask into the output image
                    Iterate.pixels(outImage[0], (int p) -> {
                        boolean white = bgid.bits == 16 ? bgid.pixels16[p] == COL.UWHITE : bgid.pixels32[p] == COL.WHITE;
                        outImage[0].pixels32[p] = white ? outImage[0].pixels32[p] : 0xFF222222;
                    });
                }
            }
        };
    }
}
