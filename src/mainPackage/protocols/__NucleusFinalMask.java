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
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
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
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        int nucleusSize = param.spinner[0];
        boolean removeEdge = param.toggle[0];
        boolean removeDead = param.toggle[1];

        return new ProcessorFast(27, "Objects", 82) {
            ImageData mask, sMask, mMask, aMask;
            ROISet set;

            @Override
            protected void pixelProcessor() {
                int nuclSize = Settings.settingsOverrideSizeEstimate() ? (int) (sourceWidth[0] / 10.) : nucleusSize;
                int minNucl = (int) Math.max(1, nuclSize * 0.06); // 2
                int maxNucl = (int) (nuclSize * 0.8); // 40
                int smallLimit = (int) (nuclSize * 0.2 * nuclSize * 0.2); // 500
                int largeLimit = (int) (nuclSize * 0.33 * nuclSize * 0.33); // 500
                int local = (int) Math.pow(nuclSize * 0.3, 0.8); // 10
                //erosion for Dim / dividing detection
                int dimErode = (int) Math.pow(nuclSize * 0.2, 0.7) - 1; // 5
                //ALSO radius for detecting the gaps between objects by mask extension overlap
                int overlapRad = (int) Math.pow(nuclSize * 0.13, 0.9);
                int smoothErode = (int) Math.pow(nuclSize * 0.05, 0.6);
                int maxDiff = (int) Math.pow(nuclSize * 0.12, 0.4); // 3
                int finSmooth = (int) Math.pow(nuclSize * 0.33, 0.5) - 1; // 3
                int maxSmooth = (int) Math.pow(nuclSize * 0.05, 0.8); // 2
                //radius for dilating the edges for nucloli etc. removal from nucleus edges
                int edgeFiller = (int) Math.pow(nuclSize * 0.03, 0.5); // 1
                int noise = (int) Math.pow(nuclSize * 0.03, 0.7); // 2

                mask = Filters.gamma().runSingle(inImage[1], 0.5);
                mask = Filters.autoscale().runSingle(mask);
                if (noise > 0) {
                    sMask = Filters.gaussApprox().runSingle(mask, noise);
                    //blend only the background area
                    Iterate.pixels(inImage[0], (int p) -> {
                        mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? sMask.pixels32[p] : mask.pixels32[p];
                    });
                }
                setSampleOutputBy(mask, 1);
                //intensity gradient edge detection
                aMask = Filters.maximumDiffEdge().runSingle(mask, 0, maxDiff, false, 0);
                setSampleOutputBy(aMask, 2);
                aMask = Filters.invert().runSingle(aMask);
                //blend intensity max against the original to protect the nucleus edges from eroding
                mask = Blender.renderBlend(aMask, mask, Blend.MAXIMUM);
                setSampleOutputBy(mask, 3);
                //local thresholding of the intensity gradient for binarization
                aMask = Filters.localThreshold().runConditional(mask, inImage[0], 10, local);
                setSampleOutputBy(aMask, 4);
                //apply the thresholded gradient into the primary mask
                Iterate.pixels(inImage[0], (int p) -> {
                    aMask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : aMask.pixels32[p];
                });
                setSampleOutputBy(aMask, 5);
                //mask = FiltersPass.filterObjectDimension().runSingle(mask, COL.BLACK, erode, true, erode);
                //connect nearby pixels to form larger shapes
                if (maxDiff > 1) {
                    mask = ConnectEdges.run().runSingle(aMask, COL.WHITE);
                    setSampleOutputBy(mask, 6);
                } else {
                    IMG.copyPixels(aMask.pixels32, mask.pixels32);
                }
                //smoothen to remove "insignificant" shapes
                mMask = FiltersPass.gaussSmoothing().runSingle(mask, maxSmooth, 1);
                setSampleOutputBy(mMask, 7);
                //the difference - what was considered "insignificant" and rounded away
                mMask = Blender.renderBlend(mMask, mask, Blend.DIFFERENCE);
                setSampleOutputBy(mMask, 8);
                //connect these "insignificant" particles together to bigger shapes
                mMask = ConnectEdges.run().runSingle(mMask, COL.BLACK);
                setSampleOutputBy(mMask, 9);
                //remove things which did not form significant shapes
                mMask = FiltersPass.filterObjectDimension().runSingle(mMask, COL.BLACK, dimErode, false, 0);
                setSampleOutputBy(mMask, 10);
                //combine everything together - a mask with connected larger shapes detected with the intensity gradient
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = mMask.pixels32[p] == COL.WHITE ? COL.BLACK : mask.pixels32[p];
                });
                setSampleOutputBy(mask, 11);
                //remove the shapes which wont touche the edges of the nuclei and thus are likely only nucleoli etc. crap
                mask = FiltersPass.fillInnerAreasSizeShape().runSingle(mask, COL.BLACK, GEO.circleArea(nuclSize) / 10, 80);
                setSampleOutputBy(mask, 12);
                //attempt to do additional segmenting by detecting concave points and close pairings formed by the additional shapes..
                //..which were detected with the intensity gradient and connected and selected above
                Protocol subsegm = Protocol.load(__ObjectSegmentStrict::new);
                sMask = subsegm.runSilent(sourceImage, new ImageData[]{mask, mMask}, COL.BLACK, nuclSize)[0];
                setSampleOutputBy(sMask, 13);
                //remove small shapes - possibly leftover pieces from the segmentation - otherwise interfering with the overlap mask
                sMask = FiltersPass.filterObjectSize().runSingle(sMask, COL.BLACK, smallLimit, false, 0);
                setSampleOutputBy(sMask, 14);
                //detect the gaps between the separate nuclei - separated by either the segmenting right before this..
                //..or passively occuring segmentation due to nucleus-separating shapes from the intensity gradient + connection
                //this is for future reference, to be able to remember which shapes are supposed to be separated
                sMask = FiltersPass.getRadiusOverlap().runSingle(sMask, COL.BLACK, overlapRad);
                setSampleOutputBy(sMask, 15);
                //next proceed with shaping the nucleus edge as accurately as possible:
                //start by combining the connected shapes from the intensity gradient to the original unprocessed binarized intensity gradient mask
                //the point is to fill the holes etc. which were deemed insignificant, but yet still reverse the detail lost by the connecting etc.
                //during the intensity gradient mask processing above, which is especially important in nucleus edges
                mMask = Blender.renderBlend(aMask, mask);
                setSampleOutputBy(mMask, 16);
                //remove inner shapes which are clearly inner, the ones left at this point cant be used to segmented further
                mMask = FiltersPass.fillInnerAreas().runSingle(mMask, COL.BLACK, true);
                setSampleOutputBy(mMask, 17);
                //if any nuclei were separated during the secondary segmenting and intensity gradient processing, apply those separations..
                //..by using the overlap mask created above, since those separations are not visible anymore as we are using the unprocessed mask now
                Iterate.pixels(inImage[0], (int p) -> {
                    mMask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || mMask.pixels32[p] == COL.BLACK ? COL.BLACK : COL.WHITE;
                });
                setSampleOutputBy(mMask, 18);
                //prevent detecting overlap with debris by filling/removing holes and small objects
                mask = FiltersPass.fillInnerAreas().runSingle(mMask, COL.BLACK, true);
                mMask = FiltersPass.filterObjectSize().runSingle(mask, COL.BLACK, smallLimit, false, 0);
                setSampleOutputBy(mMask, 19);
                //get a new version of the overlap mask now when using the unfiltered original intensity gradient mask
                //otherwise areas on nuclei edges might be missing from the mask and the separation may fail later
                sMask = FiltersPass.getRadiusOverlap().runSingle(mMask, COL.BLACK, overlapRad);
                setSampleOutputBy(sMask, 20);
                //this filter uses dilation to detect and fill shapes that will be "enclosed" on the object edges
                //essentially we try to remove holes which will distort the shape of the final nucleus mask
                if (edgeFiller > 0) {
                    mask = FiltersPass.fillSmallEdgeHoles().runSingle(mask, COL.BLACK, edgeFiller, Math.sqrt(GEO.circleArea(nuclSize)), nuclSize > 70, false);
                }
                setSampleOutputBy(mask, 21);
                if (false) { //TODO THE CONDITIONAL CONNECTING HERE
                    mask = ConnectEdges.run().runSingle(mask, COL.BLACK);
                }
                //smoothen crispy edges, a side effect from the intensity gradient and local thresholding
                mask = FiltersPass.gaussSmoothing().runSingle(mask, maxSmooth, 1);
                setSampleOutputBy(mask, 22);
                //maintain the separation of segmented objects by combining the separations by the initial segmentation (original image)..
                //..and the ones done by using the intensity gradient and secondary segmentation, saved on the overlap mask
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                });
                setSampleOutputBy(mask, 23);


                //laugh at bad ideas which did not work
                //setSampleOutputBy(layerBigs,8);
                /* parse weak edges together to do additional segmenting
                    mask = FiltersRender.blendStack().runSingle(new ImageData[]{mask, layerBigs}, 3);
                    setSampleOutputBy(mask,9);
                    mask = FiltersPass.gaussSmoothing().runSingle(mask, minSmooth, 1);
                    setSampleOutputBy(mask,10);
                    mask = FiltersPass.connectLineObjects().runSingle(mask, COL.BLACK, nucleusSize);
                    setSampleOutputBy(mask,11);
                    int dimNucl = (int) Math.max(1, nuclSize * 0.3); // 10
                    mask = FiltersPass.filterObjectDimension().runSingle(mask, dimNucl, COL.BLACK, false, 0);
                    setSampleOutputBy(mask,12);
                    Iterate.pixels(inImage[0], (int p) -> {
                        mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK || mask.pixels32[p] == COL.WHITE ? COL.BLACK : layerBigs.pixels32[p];
                    });
                    mask = ConnectEdges.run().runSingle(mask, COL.WHITE);
                    layerBigs = FiltersPass.gaussSmoothing().runSingle(mask, finSmooth, 1);*/ /*Iterate.pixels(inImage[0], (int p) -> {
                        mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK
                                || mask.pixels32[p] == COL.BLACK
                                || layerBigs.pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                    });
                    setSampleOutputBy(mask,14);
                    layerBigs = FiltersPass.filterObjectSize().runSingle(mask, limit, 0, false, 0);
                    DRAW.copyPixels(layerBigs,3);
                    Iterate.pixels(inImage[0], (int p) -> {
                        boolean removed = layerBigs.pixels32[p] != mask.pixels32[p];
                        mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : removed ? COL.BLACK : COL.WHITE;
                    });
                    DRAW.copyPixels(mask,4);*/
                //smoothen the final shapes by erode-smooth-dilate principle
                if (smoothErode > 0) {
                    mask = FiltersPass.edgeErode().runSingle(mask, COL.BLACK, smoothErode, true, true);
                    mask = Filters.dotConnectRemove().runSingle(mask, COL.BLACK, false);
                }
                //get the separation mask from the new smoother and eroded version to avoid accidental merging due to the smoothing
                sMask = FiltersPass.getRadiusOverlap().runSingle(mask, COL.BLACK, overlapRad);
                setSampleOutputBy(mask, 24);
                if (finSmooth > 0) {
                    mask = FiltersPass.gaussSmoothing().runSingle(mask, finSmooth, 2);
                }
                setSampleOutputBy(mask, 25);
                if (smoothErode > 0) {
                    mask = FiltersPass.edgeDilate().runSingle(mask, COL.BLACK, smoothErode + 1, true);
                }
                //apply the overlap mask in case dilation and smoothing merged something
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = sMask.pixels32[p] == COL.WHITE || mask.pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                });
                //remove minor leftover holes
                mask = FiltersPass.fillInnerAreasSizeShape().runSingle(mask, COL.BLACK, smallLimit, 80);
                setSampleOutputBy(mask, 26);
                //dividing and dim detection - unwanted outlier shapes by using intensity and morphology information
                //first erode so that any edge bleed etc does not affect the measurements
                mMask = FiltersPass.edgeErode().runSingle(mask, COL.BLACK, dimErode, false, true);
                set = new ImageTracer(mMask, COL.BLACK).trace();
                int largeErodedLimit = (int) (GEO.circleCircumference(largeLimit) * dimErode * 1.25);
                set.quantifyStainAgainstChannel(Filters.dog().runSingle(inImage[1], minNucl, maxNucl, false));
                set.filterOutDimObjects(set.avgStain() * 0.05);
                set.filterOutDimSmallObjects(largeLimit - largeErodedLimit, set.avgStain() * 0.5);
                //since we used erosion, restore the original masks for anything that was not removed
                set = set.getPositionFilteredSet(mask, COL.BLACK, false);
                if (removeDead) {
                    set.filterDeadDividing(inImage[1]);
                }
                if (removeEdge) {
                    set.removeEdgeTouchers();
                }
                set.filterOutSmallObjectsEdgeAdjusted(smallLimit);
                setOutputBy(set.drawToImageData(true));
            }
        };
    }
}
