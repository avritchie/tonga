package mainPackage.protocols;

import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.filters.ConnectEdges;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersRender;
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
            new ControlReference(SPINNER, "Ignore nuclei that are smaller than (pixels)", 500),
            new ControlReference(TOGGLE, "Remove dividing/dead cells", 1),
            new ControlReference(SPINNER, "Average nucleus size (pixels)", 60)};
    }

    @Override
    protected Processor getProcessor() {
        int nucleusSize = param.spinner[1];
        boolean removeEdge = param.toggle[0];
        boolean removeDead = param.toggle[1];

        return new ProcessorFast(18, "Objects", 82) {
            ImageData mask, sMask, mMask;
            int minNucl = (int) Math.max(1, nucleusSize * 0.06); // 2
            int maxNucl = (int) (nucleusSize * 0.8); // 40
            int dimNucl = (int) Math.max(1, nucleusSize * 0.3); // 10
            int limit = (int) (nucleusSize * 0.4 * nucleusSize * 0.4); // 500
            int local = (int) Math.pow(nucleusSize * 0.3, 0.8); // 10
            int erode = (int) Math.pow(nucleusSize * 0.12, 0.8); // 5
            int finSmooth = (int) Math.pow(nucleusSize * 0.1, 0.7); // 3
            int maxSmooth = (int) Math.pow(nucleusSize * 0.05, 0.8); // 2
            int minSmooth = (int) Math.pow(nucleusSize * 0.02, 0.7); // 1

            @Override
            protected void pixelProcessor() {
            }

            @Override
            protected void methodFinal() {
                mask = Filters.gamma().runSingle(inImage[1], 0.5);
                IMG.copyPixels(mask.pixels32, outImage[1].pixels32);
                mask = Filters.maximumDiffEdge().runSingle(mask, 0, 2, false, 0);
                IMG.copyPixels(mask.pixels32, outImage[2].pixels32);
                mask = Filters.invert().runSingle(mask);
                IMG.copyPixels(mask.pixels32, outImage[3].pixels32);
                mask = Filters.localThreshold().runSingle(mask, 10, local);
                IMG.copyPixels(mask.pixels32, outImage[4].pixels32);
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                });
                IMG.copyPixels(mask.pixels32, outImage[5].pixels32);
                //mask = FiltersPass.filterObjectDimension().runSingle(mask, COL.BLACK, erode, true, erode);
                mask = ConnectEdges.run().runSingle(mask, COL.WHITE);
                IMG.copyPixels(mask.pixels32, outImage[6].pixels32);
                mMask = FiltersPass.gaussSmoothing().runSingle(mask, maxSmooth, 1);
                IMG.copyPixels(mMask.pixels32, outImage[7].pixels32);
                mMask = FiltersRender.blendStack().runSingle(new ImageData[]{mMask, mask}, 2);
                IMG.copyPixels(mMask.pixels32, outImage[8].pixels32);
                mMask = ConnectEdges.run().runSingle(mMask, COL.BLACK);
                IMG.copyPixels(mMask.pixels32, outImage[9].pixels32);
                mMask = FiltersPass.filterObjectDimension().runSingle(mMask, COL.BLACK, erode, false, 0);
                IMG.copyPixels(mMask.pixels32, outImage[10].pixels32);
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = mMask.pixels32[p] == COL.WHITE ? COL.BLACK : mask.pixels32[p];
                });
                IMG.copyPixels(mask.pixels32, outImage[11].pixels32);
                mask = FiltersPass.fillInnerAreasSizeShape().runSingle(mask, COL.BLACK, GEO.circleArea(nucleusSize) / 10, 80);
                IMG.copyPixels(mask.pixels32, outImage[12].pixels32);
                ROISet set = new ImageTracer(mask, COL.BLACK).trace();
                set.targetSize(nucleusSize);
                set.findOuterEdges();
                set.findInnerEdges();
                set.analyzeCorners();
                set.analyzeCornerIntersections();
                set.segment(false);
                mMask = set.drawToImageData(true);
                IMG.copyPixels(mMask.pixels32, outImage[13].pixels32);
                mask = new ObjectsSeparated().runSilent(sourceImage, new ImageData[]{mMask, mask}, COL.BLACK)[0];
                mMask = FiltersRender.blendStack().runSingle(new ImageData[]{mask, mMask}, 2);
                sMask = FiltersPass.gaussSmoothing().runSingle(mMask, maxSmooth, 2);
                IMG.copyPixels(sMask.pixels32, outImage[14].pixels32);
                mask = FiltersRender.blendStack().runSingle(new ImageData[]{mask, sMask}, 0);
                //layerBigs = FiltersPass.gaussSmoothing().runSingle(mask, maxSmooth, 7);
                //IMG.copyPixels(layerBigs.pixels32, outImage[8].pixels32);
                /* parse weak edges together to do additional segmenting
                mask = FiltersRender.blendStack().runSingle(new ImageData[]{mask, layerBigs}, 3);
                IMG.copyPixels(mask.pixels32, outImage[9].pixels32);
                mask = FiltersPass.gaussSmoothing().runSingle(mask, minSmooth, 1);
                IMG.copyPixels(mask.pixels32, outImage[10].pixels32);
                mask = FiltersPass.connectLineObjects().runSingle(mask, COL.BLACK, nucleusSize);
                IMG.copyPixels(mask.pixels32, outImage[11].pixels32);
                mask = FiltersPass.filterObjectDimension().runSingle(mask, dimNucl, COL.BLACK, false, 0);
                IMG.copyPixels(mask.pixels32, outImage[12].pixels32);
                Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK || mask.pixels32[p] == COL.WHITE ? COL.BLACK : layerBigs.pixels32[p];
                });
                mask = ConnectEdges.run().runSingle(mask, COL.WHITE);
                layerBigs = FiltersPass.gaussSmoothing().runSingle(mask, finSmooth, 1);*/
 /*Iterate.pixels(inImage[0], (int p) -> {
                    mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK
                            || mask.pixels32[p] == COL.BLACK
                            || layerBigs.pixels32[p] == COL.BLACK ? COL.BLACK : mask.pixels32[p];
                });
                IMG.copyPixels(mask.pixels32, outImage[14].pixels32);
                
                layerBigs = FiltersPass.filterObjectSize().runSingle(mask, limit, 0, false, 0);
                DRAW.copyPixels(layerBigs.pixels32, outImage[3].pixels32);
                Iterate.pixels(inImage[0], (int p) -> {
                    boolean removed = layerBigs.pixels32[p] != mask.pixels32[p];
                    mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK ? COL.BLACK : removed ? COL.BLACK : COL.WHITE;
                });
                DRAW.copyPixels(mask.pixels32, outImage[4].pixels32);
                 */
 /*deal with cracks/nucleoli etc.
                if (minSmooth > 0) {
                    mMask = FiltersPass.edgeDilate().runSingle(mask, COL.BLACK, minSmooth, false);
                    IMG.copyPixels(mMask.pixels32, outImage[8].pixels32);
                    sMask = FiltersPass.fillInnerAreasSizeShape().runSingle(mMask, COL.BLACK, GEO.circleArea(nucleusSize) / 10, 80);
                    IMG.copyPixels(sMask.pixels32, outImage[9].pixels32);
                    sMask = FiltersRender.blendStack().runSingle(new ImageData[]{mMask, sMask}, 2);
                    IMG.copyPixels(sMask.pixels32, outImage[10].pixels32);
                    sMask = FiltersPass.edgeDilate().runSingle(sMask, COL.BLACK, minSmooth + 1, false);
                    IMG.copyPixels(sMask.pixels32, outImage[11].pixels32);
                    Iterate.pixels(inImage[0], (int p) -> {
                        mask.pixels32[p] = inImage[0].pixels32[p] == COL.BLACK
                                || (mask.pixels32[p] == COL.BLACK && sMask.pixels32[p] == COL.BLACK)
                                        ? COL.BLACK : COL.WHITE;
                    });
                    //mask = FiltersRender.blendStack().runSingle(new ImageData[]{mask, layerBigs}, 0);
                    IMG.copyPixels(mask.pixels32, outImage[12].pixels32);
                }*/
                //smoothen the cuttings
                mask = FiltersPass.edgeErode().runSingle(mask, COL.BLACK, maxSmooth, true);
                IMG.copyPixels(mask.pixels32, outImage[15].pixels32);
                mask = FiltersPass.gaussSmoothing().runSingle(mask, maxSmooth + 1, 3);
                IMG.copyPixels(mask.pixels32, outImage[16].pixels32);
                mask = FiltersPass.edgeDilate().runSingle(mask, COL.BLACK, maxSmooth);
                IMG.copyPixels(mask.pixels32, outImage[17].pixels32);
                // dividing and dim detection
                mMask = FiltersPass.edgeErode().runSingle(mask, COL.BLACK, erode, false, true);
                set = new ImageTracer(mMask, COL.BLACK).trace();
                set.quantifyStainAgainstChannel(Filters.dog().runSingle(inImage[1], minNucl, maxNucl, false));
                set.filterOutDimObjects(2);
                set = set.getPositionFilteredSet(mask, COL.BLACK);
                if (removeDead) {
                    set.filterDeadDividing(inImage[1]);
                }
                if (removeEdge) {
                    set.removeEdgeTouchers();
                }
                set.filterOutSmallObjects(param.spinner[0]);
                setOutputBy(set.drawToImageData(true));
            }
        };
    }
}
