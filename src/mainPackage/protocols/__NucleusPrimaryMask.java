package mainPackage.protocols;

import javafx.scene.paint.Color;
import mainPackage.utils.COL;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
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
        int iter = nucleusSize < 34 ? 52 : 70;

        return new ProcessorFast(fullOutput() ? 13 : 2, new String[]{"Nucleus Mask"}, iter) {

            ImageData layerCorrect, layerBackground, layerNuclei, layerComb, layerDog;
            ROISet quantSet, fillSet;
            int nuclSize, nuclArea, minNucl, maxNucl, maxInn, maxBack, maxEdge, smooth, noise, dilate, inRem;

            @Override
            protected void methodInit() {
                layerCorrect = initTempData();
                layerBackground = initTempData();
                layerNuclei = initTempData();
                layerComb = initTempData();
                layerDog = initTempData();
                nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                nuclArea = (int) GEO.circleArea(nuclSize);
                // first gaussian
                minNucl = (int) Math.max(1, nuclSize * 0.06); // 2
                // second gaussian for detecting nuclei edges
                maxNucl = (int) (nuclSize * 0.3); // 15
                // second gaussian for nuclei edge mask
                maxInn = (int) (nuclSize * 1.6); // 80
                // second gaussian for detecting background
                maxBack = (int) (maxNucl * 10); // 60 and 90
                // second gaussian for detecting edges
                maxEdge = (int) (nuclSize * 0.2); // 10
                // second gaussian for detecting edges
                smooth = (int) Math.pow(nuclSize * 0.2, 0.8); // 2
                // initial noise filtering radius
                noise = (int) Math.pow(nuclSize * 0.03, 0.7); // 1
                // dilation radius for hole removal
                dilate = (int) Math.pow(nuclSize * 0.1, 0.5); // 2
                // remove inner areas
                inRem = (int) (nuclArea / Math.max(Math.pow(nuclSize * 2, 0.4), 6)); // 5
            }

            @Override
            protected void pixelProcessor() {
                // illumination/contrast-corrected version of the image
                Filters.gamma().runTo(inImage[0], layerCorrect, 0.5);
                Filters.autoscale().runTo(layerCorrect);
                setOutputBy(layerCorrect, 1);
                // process with different difference of gaussians -settings
                if (noise > 0) {
                    Filters.gaussApprox().runTo(layerCorrect, noise, true);
                }
                setSampleOutputBy(layerCorrect, 2);
                Filters.dog().runTo(layerCorrect, layerNuclei, minNucl, maxNucl, true);
                Filters.multiply().runTo(layerNuclei, 300);
                setSampleOutputBy(layerNuclei, 3);
                Filters.dog().runTo(layerNuclei, layerDog, 1, maxInn, true);
                setSampleOutputBy(layerDog, 4);
                // process with difference of gaussians 
                Filters.dog().runTo(layerCorrect, layerBackground, minNucl + 1, maxBack, true);
                setSampleOutputBy(layerBackground, 5);
                // substract the second combination from the newly created final gaussian
                applyOperator(inImage[0], layerComb, p
                        -> (layerDog.pixels32[p] & 0xFF) > 1 && (layerBackground.pixels32[p] & 0xFF) < 1 ? COL.WHITE : COL.BLACK);
                setSampleOutputBy(layerComb, 6);
                quantSet = new ImageTracer(layerComb, Color.BLACK).trace();
                Filters.dog().runTo(layerCorrect, layerComb, minNucl, nuclSize, false);
                quantSet.quantifyStainAgainstChannel(layerComb);
                quantSet.filterOutDimObjects(0.05);
                quantSet.filterOutDimObjects(quantSet.avgStain() * 0.1);
                quantSet.drawTo(layerComb, true);
                setSampleOutputBy(layerComb, 7);
                // additional stuff
                Filters.dog().runTo(layerCorrect, layerBackground, 1, maxEdge, true);
                setSampleOutputBy(layerBackground, 8);
                Filters.dog().runTo(layerBackground, 1, maxEdge, false);
                setSampleOutputBy(layerBackground, 9);
                Filters.dog().runTo(layerBackground, 1, maxEdge, false);
                // stack
                applyOperator(inImage[0], layerNuclei, p
                        -> (layerBackground.pixels32[p] & 0xFF) >= 1 || layerComb.pixels32[p] == COL.BLACK ? COL.BLACK : COL.WHITE);
                setSampleOutputBy(layerNuclei, 10);
                fillSet = FiltersSet.fillInnerAreasSizeShape().runSet(layerNuclei, COL.BLACK, inRem, 90, false, 0, true);
                //setSampleOutputBy(layerBackground, 10);// remove nuclei or other irregular holes on nuclei edges
                if (noise > 0) {
                    layerComb = FiltersPass.fillSmallEdgeHoles().runSingle(fillSet, layerNuclei, COL.BLACK, noise, Math.sqrt(nuclArea), false, false);
                    FiltersPass.gaussSmoothing().runTo(layerComb, noise, 1);
                    FiltersSet.fillInnerAreasSizeShape().runTo(layerComb, layerBackground, COL.BLACK, inRem, 90, false, 0, true);
                } else {
                    layerBackground = fillSet.drawToImageData(true);
                }
                //fill inner shapes most likely NOT background
                setSampleOutputBy(layerBackground, 11);
                Protocol.load(BrightRemover::new).runSilentTo(sourceImage, new ImageData[]{layerBackground, layerDog}, layerBackground, COL.WHITE, dilate, 1.0);
                setSampleOutputBy(layerBackground, 12);
                // smooth the masks but on convex areas only
                if (smooth > 0) {
                    Filters.gaussApprox().runTo(layerBackground, layerNuclei, smooth, true);
                    applyOperator(inImage[0], layerBackground, p
                            -> (layerNuclei.pixels32[p] & 0xFF) < 127 && layerBackground.pixels32[p] == COL.WHITE ? COL.BLACK : layerBackground.pixels32[p]);
                }
                setOutputBy(layerBackground);
            }
        };
    }
}
