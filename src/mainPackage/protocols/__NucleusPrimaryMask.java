package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;
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
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        int nucleusSize = param.spinner[0];

        return new ProcessorFast(13, new String[]{"Nucleus Mask"}, 73) {

            ImageData layerCorrect, layerBackground, layerNuclei, layerComb;

            @Override
            protected void methodFinal() {
                int nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                int nuclArea = GEO.circleArea(nuclSize);
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
                int noise = (int) Math.pow(nuclSize * 0.03, 0.7); // 1
                // dilation radius for hole removal
                int dilate = (int) Math.pow(nuclSize * 0.1, 0.5); // 2
                // remove inner areas
                int inRem = (int) (nuclArea / Math.max(Math.pow(nuclSize * 2, 0.4), 6)); // 5
                layerComb = new ImageData(sourceWidth[0], sourceHeight[0]);
                // illumination/contrast-corrected version of the image
                layerCorrect = Filters.gamma().runSingle(inImage[0], 0.5);
                layerCorrect = Filters.autoscale().runSingle(layerCorrect);
                setSampleOutputBy(layerCorrect, 1);
                // process with different difference of gaussians -settings
                if (noise > 0) {
                    layerCorrect = Filters.gaussApprox().runSingle(layerCorrect, noise);
                }
                layerNuclei = Filters.dog().runSingle(layerCorrect, minNucl, maxNucl, true);
                setSampleOutputBy(layerNuclei, 2);
                layerNuclei = Filters.multiply().runSingle(layerNuclei, 300);
                setSampleOutputBy(layerNuclei, 3);
                layerNuclei = Filters.dog().runSingle(layerNuclei, 1, maxInn, true);
                setSampleOutputBy(layerNuclei, 4);
                // process with difference of gaussians 
                layerBackground = Filters.dog().runSingle(layerCorrect, minNucl + 1, maxBack, true);
                setSampleOutputBy(layerBackground, 5);
                // substract the second combination from the newly created final gaussian
                Iterate.pixels(inImage[0], (int p) -> {
                    layerComb.pixels32[p] = (layerNuclei.pixels32[p] & 0xFF) > 1 && (layerBackground.pixels32[p] & 0xFF) < 1 ? COL.WHITE : COL.BLACK;
                });
                setSampleOutputBy(layerComb, 6);
                ROISet set = new ImageTracer(layerComb, Color.BLACK).trace();
                set.quantifyStainAgainstChannel(Filters.dog().runSingle(layerCorrect, minNucl, nuclSize, false));
                set.filterOutDimObjects(0.05);
                set.filterOutDimObjects(set.avgStain() * 0.1);
                layerComb = set.drawToImageData(true);
                setSampleOutputBy(layerComb, 7);
                //layerComb = FiltersPass.fillInnerAreasSizeShape().runSingle(layerComb, COL.WHITE, GEO.circleArea(nucleusSize) / 5, 90);
                //DRAW.copyPixels(layerComb,9);
                // additional stuff
                layerBackground = Filters.dog().runSingle(layerCorrect, 1, maxEdge, true);
                layerBackground = Filters.dog().runSingle(layerBackground, 1, maxEdge, false);
                layerBackground = Filters.dog().runSingle(layerBackground, 1, maxEdge, false);
                setSampleOutputBy(layerBackground, 8);
                /*layerBackground = Filters.maximumDiffEdge().runSingle(layerBackground, 0, 1, false, 0);
                DRAW.copyPixels(layerBackground,13);
                layerBackground = Filters.cutFilter().runSingle(layerBackground, new Object[]{0,25});
                DRAW.copyPixels(layerBackground,14);
                layerBackground = Filters.thresholdBright().runSingle(layerBackground, 5);
                DRAW.copyPixels(layerBackground,15);*/
                // stack
                Iterate.pixels(inImage[0], (int p) -> {
                    layerNuclei.pixels32[p] = ((layerBackground.pixels32[p] & 0xFF) >= 1 || layerComb.pixels32[p] == COL.BLACK) ? COL.BLACK : COL.WHITE;
                });
                setSampleOutputBy(layerNuclei, 9);
                layerBackground = FiltersPass.fillInnerAreasSizeShape().runSingle(layerNuclei, COL.BLACK, inRem, 90, false, 0, true);
                setSampleOutputBy(layerBackground, 10);// remove nuclei or other irregular holes on nuclei edges
                if (noise > 0) {
                    layerBackground = FiltersPass.fillSmallEdgeHoles().runSingle(layerBackground, COL.BLACK, noise, Math.sqrt(nuclArea), false, false);
                    layerBackground = FiltersPass.gaussSmoothing().runSingle(layerBackground, noise, 1);
                    layerBackground = FiltersPass.fillInnerAreasSizeShape().runSingle(layerBackground, COL.BLACK, inRem, 90, false, 0, true);
                }
                setSampleOutputBy(layerBackground, 11);
                //fill inner shapes most likely NOT background
                layerBackground = Protocol.load(BrightRemover::new).runSilent(sourceImage, new ImageData[]{layerBackground, inImage[0]}, COL.WHITE, dilate, 0.5)[0];
                setSampleOutputBy(layerBackground, 12);
                // smooth the masks but on convex areas only
                if (smooth > 0) {
                    layerNuclei = Filters.gaussApprox().runSingle(layerBackground, smooth, true);
                    Iterate.pixels(inImage[0], (int p) -> {
                        layerBackground.pixels32[p] = (layerNuclei.pixels32[p] & 0xFF) < 127 && layerBackground.pixels32[p] == COL.WHITE ? COL.BLACK : layerBackground.pixels32[p];
                    });
                }
                //setSampleOutputBy(layerBackground,13);
                //layerComb = new ObjectsSeparated().runSilent(sourceImage, new ImageData[]{layerBackground, layerNuclei}, COL.BLACK)[0];
                //layerBackground = FiltersPass.filterObjectSize().runSingle(layerBackground, COL.BLACK, limit, false, 0);
                setOutputBy(layerBackground);
            }
        };
    }
}
