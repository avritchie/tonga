package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;
import mainPackage.utils.IMG;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;

public class __NucleusPrimaryMask extends Protocol {

    @Override
    public String getName() {
        return "Nucleus mask creation";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Source staining (e.g. DAPI) is on which layer"),
            new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        int nucleusSize = param.spinner[1];
        double limit = param.spinner[0];

        return new ProcessorFast(14, new String[]{"Nucleus Mask"}, 62) {

            ImageData layerCorrect, layerBackground, layerNuclei, layerComb;

            @Override
            protected void methodFinal() {
                int nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                // first gaussian
                int minNucl = (int) Math.max(1, nuclSize * 0.06); // 2
                // second gaussian for detecting nuclei edges
                int maxNucl = (int) (nuclSize * 0.3); // 15
                // second gaussian for nuclei edge mask
                int maxInn = (int) (nuclSize * 1.6); // 80
                // second gaussian for detecting background
                int maxBack = (int) (maxNucl * 10); // 60 and 90
                // second gaussian for detecting edges
                int maxEdge = (int) (nuclSize * 0.2); // 10
                // second gaussian for detecting edges
                int smooth = (int) Math.pow(nuclSize * 0.2, 0.8); // 2
                // initial noise filtering radius
                int noise = (int) Math.pow(nuclSize * 0.03, 0.7); // 2
                layerComb = new ImageData(sourceWidth[0], sourceHeight[0]);
                // illumination/contrast-corrected version of the image
                layerCorrect = FiltersPass.evenIllumination().runSingle(sourceLayer[0]);
                IMG.copyPixels(layerCorrect.pixels32, outImage[1].pixels32);
                // process with different difference of gaussians -settings
                if (noise > 0) {
                    layerCorrect = Filters.gaussApprox().runSingle(layerCorrect, noise);
                }
                layerNuclei = Filters.dog().runSingle(layerCorrect, minNucl, maxNucl, true);
                IMG.copyPixels(layerNuclei.pixels32, outImage[2].pixels32);
                layerNuclei = Filters.multiply().runSingle(layerNuclei, 300);
                IMG.copyPixels(layerNuclei.pixels32, outImage[3].pixels32);
                layerNuclei = Filters.dog().runSingle(layerNuclei, 1, maxInn, true);
                IMG.copyPixels(layerNuclei.pixels32, outImage[4].pixels32);
                // process with difference of gaussians 
                layerBackground = Filters.dog().runSingle(layerCorrect, minNucl + 1, maxBack, true);
                IMG.copyPixels(layerBackground.pixels32, outImage[5].pixels32);
                // substract the second combination from the newly created final gaussian
                Iterate.pixels(inImage[0], (int p) -> {
                    layerComb.pixels32[p] = (layerNuclei.pixels32[p] & 0xFF) > 1 && (layerBackground.pixels32[p] & 0xFF) < 1 ? COL.WHITE : COL.BLACK;
                });
                IMG.copyPixels(layerComb.pixels32, outImage[6].pixels32);
                ROISet set = new ImageTracer(layerComb, Color.BLACK).trace();
                set.quantifyStainAgainstChannel(Filters.dog().runSingle(layerCorrect, minNucl, nuclSize, false));
                set.filterOutDimObjects(set.avgStain() * 0.1);
                layerComb = set.drawToImageData(true);
                IMG.copyPixels(layerComb.pixels32, outImage[7].pixels32);
                //layerComb = FiltersPass.fillInnerAreasSizeShape().runSingle(layerComb, COL.WHITE, GEO.circleArea(nucleusSize) / 5, 90);
                //DRAW.copyPixels(layerComb.pixels32, outImage[9].pixels32);
                // additional stuff
                layerBackground = Filters.dog().runSingle(layerCorrect, 1, maxEdge, true);
                IMG.copyPixels(layerBackground.pixels32, outImage[8].pixels32);
                layerBackground = Filters.dog().runSingle(layerBackground, 1, maxEdge, false);
                IMG.copyPixels(layerBackground.pixels32, outImage[9].pixels32);
                layerBackground = Filters.dog().runSingle(layerBackground, 1, maxEdge, false);
                IMG.copyPixels(layerBackground.pixels32, outImage[10].pixels32);
                /*layerBackground = Filters.maximumDiffEdge().runSingle(layerBackground, 0, 1, false, 0);
                DRAW.copyPixels(layerBackground.pixels32, outImage[13].pixels32);
                layerBackground = Filters.cutFilter().runSingle(layerBackground, new Object[]{0,25});
                DRAW.copyPixels(layerBackground.pixels32, outImage[14].pixels32);
                layerBackground = Filters.thresholdBright().runSingle(layerBackground, 5);
                DRAW.copyPixels(layerBackground.pixels32, outImage[15].pixels32);*/
                // stack
                Iterate.pixels(inImage[0], (int p) -> {
                    layerNuclei.pixels32[p] = ((layerBackground.pixels32[p] & 0xFF) >= 1 || layerComb.pixels32[p] == COL.BLACK) ? COL.BLACK : COL.WHITE;
                });
                IMG.copyPixels(layerNuclei.pixels32, outImage[11].pixels32);
                layerBackground = FiltersPass.fillInnerAreasSizeShape().runSingle(layerNuclei, COL.BLACK, GEO.circleArea(nuclSize) / 5, 90);
                // remove nuclei or other irregular holes on nuclei edges
                if (noise > 0) {
                    layerBackground = FiltersPass.fillSmallEdgeHoles().runSingle(layerBackground, COL.BLACK, noise, Math.sqrt(GEO.circleArea(nuclSize)), false, false);
                    layerBackground = FiltersPass.gaussSmoothing().runSingle(layerBackground, noise, 1);
                    layerBackground = FiltersPass.fillInnerAreasSizeShape().runSingle(layerBackground, COL.BLACK, GEO.circleArea(nuclSize) / 5, 90);
                }
                IMG.copyPixels(layerBackground.pixels32, outImage[12].pixels32);
                // smooth the masks but on convex areas only
                if (smooth > 0) {
                    layerNuclei = Filters.gaussApprox().runSingle(layerBackground, smooth, true);
                    Iterate.pixels(inImage[0], (int p) -> {
                        layerBackground.pixels32[p] = (layerNuclei.pixels32[p] & 0xFF) < 127 && layerBackground.pixels32[p] == COL.WHITE ? COL.BLACK : layerBackground.pixels32[p];
                    });
                }
                IMG.copyPixels(layerBackground.pixels32, outImage[13].pixels32);
                //layerComb = new ObjectsSeparated().runSilent(sourceImage, new ImageData[]{layerBackground, layerNuclei}, COL.BLACK)[0];
                layerBackground = FiltersPass.filterObjectSize().runSingle(layerBackground, COL.BLACK, limit, false, 0);
                IMG.copyPixels(layerBackground.pixels32, outImage[0].pixels32);
            }
        };
    }
}
