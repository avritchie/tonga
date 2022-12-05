package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.Settings;
import mainPackage.Tonga;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.GEO;

public class __NucleusFinalMask extends Protocol {

    @Override
    public String getName() {
        return "Measure nuclear stainings on a mask";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Track objects at which layer"),
            new ControlReference(LAYER, "The original image"),
            new ControlReference(TOGGLE, "Ignore nuclei touching the edges", 1),
            new ControlReference(TOGGLE, "Remove dividing/dead cells", 1),
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60),
            new ControlReference(TOGGLE, "Skip layer adjustments", 0)};
    }

    @Override
    protected Processor getProcessor() {
        int nucleusSize = param.spinner[0];
        boolean removeEdge = param.toggle[0];
        boolean removeDead = param.toggle[1];
        boolean skipAdjust = param.toggle[2];
        int iter = nucleusSize < 13 ? 55 : nucleusSize < 20 ? 61 : nucleusSize < 34 ? 68 : nucleusSize < 48 ? 84 : 85;

        return new ProcessorFast(Tonga.debug() ? 27 : 1, "Objects", iter) {

            ImageData mask, sMask, mMask, aMask;
            int nuclSize, minNucl, maxNucl, smallLimit, largeLimit, local, dimErode, overlapRad, smoothErode, maxDiff, finSmooth, maxSmooth, edgeFiller, noise;
            ROISet quantSet, fillSet;

            @Override
            protected void methodInit() {
                mask = initTempData();
                sMask = initTempData();
                mMask = initTempData();
                aMask = initTempData();
                nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                minNucl = (int) Math.max(1, nuclSize * 0.06); // 2
                maxNucl = (int) (nuclSize * 0.8); // 40
                smallLimit = (int) (nuclSize * 0.2 * nuclSize * 0.2); // 500
                largeLimit = (int) (nuclSize * 0.33 * nuclSize * 0.33); // 500
                local = (int) Math.pow(nuclSize * 0.3, 0.8); // 10
                //erosion for Dim / dividing detection
                dimErode = (int) Math.pow(nuclSize * 0.2, 0.7) - 1; // 5
                //ALSO radius for detecting the gaps between objects by mask extension overlap
                overlapRad = (int) Math.pow(nuclSize * 0.13, 0.9);
                smoothErode = (int) Math.pow(nuclSize * 0.05, 0.6);
                maxDiff = (int) Math.pow(nuclSize * 0.12, 0.4); // 3
                finSmooth = (int) Math.pow(nuclSize * 0.33, 0.5) - 1; // 3
                maxSmooth = (int) Math.pow(nuclSize * 0.05, 0.8); // 2
                //radius for dilating the edges for nucloli etc. removal from nucleus edges
                edgeFiller = (int) Math.pow(nuclSize * 0.03, 0.5); // 1
                noise = (int) Math.pow(nuclSize * 0.03, 0.7); // 2
            }

            @Override
            protected void pixelProcessor() {
                if (!skipAdjust) {
                    Filters.gamma().runTo(inImage[1], mask, 0.5);
                    Filters.autoscale().runTo(mask);
                } else {
                    mask = inImage[2];
                }
                if (noise > 0) {
                    Filters.gaussApprox().runTo(mask, sMask, noise, true);
                    //blend only the background area
                    applyOperator(inImage[0], mask, p -> inImage[0].pixels32[p] == COL.BLACK ? sMask.pixels32[p] : mask.pixels32[p]);
                }
                setSampleOutputBy(mask, 1);
                //intensity gradient edge detection
                Filters.maximumDiffEdge().runTo(mask, aMask, 0, maxDiff, false, 0);
                setSampleOutputBy(aMask, 2);
                Filters.invert().runTo(aMask);
                //blend intensity max against the original to protect the nucleus edges from eroding
                mask = Blender.renderBlend(aMask, mask, Blend.MAXIMUM);
                setSampleOutputBy(mask, 3);
                //local thresholding of the intensity gradient for binarization
                aMask = Filters.localThreshold().runConditional(mask, inImage[0], COL.BLACK, 10, local);
                setSampleOutputBy(aMask, 4);
                //apply the thresholded gradient into the primary mask
                applyOperator(inImage[0], aMask, p -> inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : aMask.pixels32[p]);
                setSampleOutputBy(aMask, 5);
                //connect nearby pixels to form larger shapes
                if (maxDiff > 1) {
                    Filters.connectEdges().runTo(aMask, mask, COL.WHITE);
                    setSampleOutputBy(mask, 6);
                } else {
                    IMG.copyPixels(aMask.pixels32, mask.pixels32);
                }
                //smoothen to remove "insignificant" shapes
                FiltersPass.gaussSmoothing().runTo(mask, mMask, maxSmooth, 1);
                setSampleOutputBy(mMask, 7);
                //the difference - what was considered "insignificant" and rounded away
                mMask = Blender.renderBlend(mMask, mask, Blend.DIFFERENCE);
                setSampleOutputBy(mMask, 8);
                //connect these "insignificant" particles together to bigger shapes
                Filters.connectEdges().runTo(mMask, COL.BLACK);
                setSampleOutputBy(mMask, 9);
                //remove things which did not form significant shapes
                FiltersSet.filterObjectDimension().runTo(mMask, COL.BLACK, dimErode, false, 0);
                setSampleOutputBy(mMask, 10);
                //combine everything together - a mask with connected larger shapes detected with the intensity gradient
                applyOperator(inImage[0], mask, p -> mMask.pixels32[p] == COL.WHITE ? COL.BLACK : mask.pixels32[p]);
                setSampleOutputBy(mask, 11);
                //remove the shapes which wont touche the edges of the nuclei and thus are likely only nucleoli etc. crap
                mask = FiltersSet.fillInnerAreasSizeShape().runSingle(mask, COL.BLACK, GEO.circleArea(nuclSize) / 10, 80);
                setSampleOutputBy(mask, 12);
                //attempt to do additional segmenting by detecting concave points and close pairings formed by the additional shapes..
                //..which were detected with the intensity gradient and connected and selected above
                Protocol subsegm = Protocol.load(__ObjectSegmentStrict::new);
                subsegm.runSilentTo(sourceImage, new ImageData[]{mask, mMask}, sMask, COL.BLACK, nuclSize);
                setSampleOutputBy(sMask, 13);
                //remove small shapes - possibly leftover pieces from the segmentation - otherwise interfering with the overlap mask
                fillSet = FiltersSet.filterObjectSize().runSet(sMask, COL.BLACK, smallLimit, false, 0);
                setSampleOutputBy(sMask, 14);
                //detect the gaps between the separate nuclei - separated by either the segmenting right before this..
                //..or passively occuring segmentation due to nucleus-separating shapes from the intensity gradient + connection
                //this is for future reference, to be able to remember which shapes are supposed to be separated
                sMask = FiltersSet.getRadiusOverlap().runSingle(fillSet, COL.BLACK, overlapRad);
                setSampleOutputBy(sMask, 15);
                //next proceed with shaping the nucleus edge as accurately as possible:
                //start by combining the connected shapes from the intensity gradient to the original unprocessed binarized intensity gradient mask
                //the point is to fill the holes etc. which were deemed insignificant, but yet still reverse the detail lost by the connecting etc.
                //during the intensity gradient mask processing above, which is especially important in nucleus edges
                mMask = Blender.renderBlend(aMask, mask);
                setSampleOutputBy(mMask, 16);
                //remove inner shapes which are clearly inner, the ones left at this point cant be used to segmented further
                FiltersSet.fillInnerAreas().runTo(mMask, mMask, COL.BLACK, true);
                setSampleOutputBy(mMask, 17);
                //if any nuclei were separated during the secondary segmenting and intensity gradient processing, apply those separations..
                //..by using the overlap mask created above, since those separations are not visible anymore as we are using the unprocessed mask now
                Iterate.pixels(inImage[0], (int p) -> {
                    mMask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || mMask.pixels32[p] == COL.BLACK ? COL.BLACK : COL.WHITE;
                });
                setSampleOutputBy(mMask, 18);
                //prevent detecting overlap with debris by filling/removing holes and small objects
                fillSet = FiltersSet.fillInnerAreas().runSet(mMask, COL.BLACK, true);
                if (edgeFiller > 0) {
                    quantSet = fillSet.copy();
                }
                fillSet = FiltersSet.filterObjectSize().runSet(fillSet, COL.BLACK, smallLimit, false, 0);
                setSampleOutputBy(mMask, 19);
                //get a new version of the overlap mask now when using the unfiltered original intensity gradient mask
                //otherwise areas on nuclei edges might be missing from the mask and the separation may fail later
                sMask = FiltersSet.getRadiusOverlap().runSingle(fillSet, COL.BLACK, overlapRad);
                setSampleOutputBy(sMask, 20);
                //this filter uses dilation to detect and fill shapes that will be "enclosed" on the object edges
                //essentially we try to remove holes which will distort the shape of the final nucleus mask
                if (edgeFiller > 0) {
                    double adjSize = Math.sqrt(GEO.circleArea(nuclSize));
                    mask = FiltersPass.fillSmallEdgeHoles().runSingle(quantSet, quantSet.drawToImageData(true), COL.BLACK, edgeFiller, adjSize, nuclSize > 70, false);
                }
                setSampleOutputBy(mask, 21);
                if (false) { //TODO THE CONDITIONAL CONNECTING HERE
                    Filters.connectEdges().runTo(mask, COL.BLACK);
                }
                //smoothen crispy edges, a side effect from the intensity gradient and local thresholding
                FiltersPass.gaussSmoothing().runTo(mask, maxSmooth, 1);
                setSampleOutputBy(mask, 22);
                //maintain the separation of segmented objects by combining the separations by the initial segmentation (original image)..
                //..and the ones done by using the intensity gradient and secondary segmentation, saved on the overlap mask
                applyOperator(inImage[0], mask, p -> mask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p]);
                setSampleOutputBy(mask, 23);
                //smoothen the final shapes by erode-smooth-dilate principle
                if (smoothErode > 0) {
                    FiltersPass.edgeErode().runTo(mask, COL.BLACK, smoothErode, true, true);
                    Filters.dotConnectRemove().runTo(mask, COL.BLACK, false);
                }
                //get the separation mask from the new smoother and eroded version to avoid accidental merging due to the smoothing
                FiltersSet.getRadiusOverlap().runTo(mask, sMask, COL.BLACK, overlapRad);
                setSampleOutputBy(mask, 24);
                if (finSmooth > 0) {
                    FiltersPass.gaussSmoothing().runTo(mask, finSmooth, 2);
                }
                setSampleOutputBy(mask, 25);
                if (smoothErode > 0) {
                    FiltersPass.edgeDilate().runTo(mask, COL.BLACK, smoothErode + 1, true);
                }
                //apply the overlap mask in case dilation and smoothing merged something
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || mask.pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                });
                //remove minor leftover holes
                fillSet = FiltersSet.fillInnerAreasSizeShape().runSet(mask, COL.BLACK, smallLimit, 80);
                aMask = fillSet.drawToImageData(true);
                setSampleOutputBy(aMask, 26);
                //dividing and dim detection - unwanted outlier shapes by using intensity and morphology information
                //first erode so that any edge bleed etc does not affect the measurements
                mMask = FiltersPass.edgeErode().runSingle(fillSet, aMask, COL.BLACK, dimErode, false, true);
                quantSet = new ImageTracer(mMask, COL.BLACK).trace();
                int largeErodedLimit = (int) (GEO.circleCircumference(largeLimit) * dimErode * 1.25);
                quantSet.quantifyStainAgainstChannel(Filters.dog().runSingle(inImage[1], minNucl, maxNucl, false));
                quantSet.filterOutDimObjects(quantSet.avgStain() * 0.05);
                quantSet.filterOutDimSmallObjects(largeLimit - largeErodedLimit, quantSet.avgStain() * 0.5);
                //since we used erosion, restore the original masks for anything that was not removed
                quantSet = quantSet.getPositionFilteredSet(aMask, COL.BLACK, false);
                if (removeDead) {
                    quantSet.filterDeadDividing(inImage[1]);
                }
                if (removeEdge) {
                    quantSet.removeEdgeTouchers();
                }
                quantSet.filterOutSmallObjectsEdgeAdjusted(smallLimit);
                setOutputBy(quantSet.drawToImageData(true));
            }
        };
    }
}
