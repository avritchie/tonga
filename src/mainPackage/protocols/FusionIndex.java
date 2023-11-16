package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import mainPackage.counters.TableData;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.utils.COL;

public class FusionIndex extends Protocol {

    @Override
    public String getName() {
        return "Fusion Index";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "DAPI channel is which layer"),
            new ControlReference(LAYER, "Myotube channel is which layer"),
            new ControlReference(SLIDER, new Integer[]{0, 60}, "Sensitivity", 10)};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast(fullOutput() ? 9 : 3, "Myotubes", 300) {

            ImageData temp, temp2, temp3, nuclei, myoint;
            Protocol pt;
            double nuclSize;

            @Override
            protected void pixelProcessor() {
                //find nucleus size for reference
                pt = Protocol.load(_EstimateNucleusSize::new);
                pt.runSilent(sourceImage, inImage[0]);
                nuclSize = TableData.getType(pt.results.getVal(0, 1));
                //even the myotube signal without compromising the edges
                temp = Filters.gaussApprox().runSingle(inImage[1], 5, true);
                temp2 = Filters.clampAverage().runSingle(temp, 50, 0);
                temp3 = Filters.clampAverage().runSingle(temp, 25, 1);
                temp = Blender.renderBlend(temp, temp3, Blend.DIFFERENCE);
                temp2 = Blender.renderBlend(temp2, temp, Blend.DIFFERENCE);
                //detect and remove background to make non-myo areas black
                pt = Protocol.load(_BackgroundArea::new);
                //bg should be calculated on a 8-bit image
                temp3 = inImage[1].bits == 16 ? inImage[1].copy8bit() : inImage[1];
                temp3 = pt.runSilent(sourceImage, new ImageData[]{temp3, temp3}, 20)[0];
                int bgval = (int) (pt.results.getDouble(0, 4));
                Filters.reduce().runTo(temp2, bgval, true);
                Filters.crapCleaner().runTo(temp2, temp, 2.0);
                setSampleOutputBy(temp, 3);
                //combine max-signal and min-noise stack from myotube images
                Filters.maximumDiffEdge().runTo(temp2, 0, 2, false, 0);
                Filters.maximumDiffEdge().runTo(temp, temp3, 0, 2, false, 0);
                temp3 = Blender.renderBlend(temp2, temp3, Blend.MINIMUM);
                Filters.multiply().runTo(temp3, 500, true);
                temp2 = Blender.renderBlend(temp3, temp, Blend.ADD);
                setSampleOutputBy(temp2, 4);
                //segment nuclei
                pt = Protocol.load(__NucleusMask::new);
                temp3 = Filters.clampAverage().runSingle(inImage[0], Math.max(1, nuclSize / 10), 0);
                nuclei = pt.runSilent(sourceImage, temp3, false, false, true)[0];
                nuclei = FiltersSet.filterObjectSize().runSingle(nuclei, COL.BLACK, nuclSize * nuclSize * 0.2);
                setSampleOutputBy(nuclei, 5);
                //carve the nucleus masks hollow
                FiltersPass.edgeErode().runTo(nuclei, temp3, COL.BLACK, nuclSize / 5, true);
                temp3 = Blender.renderBlend(temp3, nuclei, Blend.DIFFERENCE);
                //use the myotube stack to detect nuclei in myotubes
                pt = Protocol.load(_AreaStainIntensity::new);
                temp3 = pt.runSilent(sourceImage, new ImageData[]{temp3, temp2, temp2}, COL.BLACK, true, 15, false, false, false)[0];
                Filters.thresholdBright().runTo(temp3, 90);
                temp3 = FiltersSet.fillInnerAreas().runSingle(temp3, COL.BLACK, true);
                setSampleOutputBy(temp3, 6);
                //dilate the myotube nuclei areas from the surrounding myotube values to remove myotube holes caused by nuclei
                Filters.invert().runTo(temp, temp2);
                FiltersPass.edgeDilate().runTo(temp3, temp, COL.BLACK, 3, true);
                applyOperator(inImage[0], temp2, p
                        -> temp.pixels32[p] == COL.WHITE ? COL.BLACK : temp2.pixels32[p]);
                for (int i = 0; i < 6; i++) {
                    temp2 = Filters.blurConditional().runSingle(temp2, COL.BLACK, Math.max(2, nuclSize / 10), false);
                }
                FiltersPass.edgeDilate().runTo(temp2, COL.BLACK, 20, true);
                myoint = Filters.invert().runSingle(temp2);
                Filters.multiply().runTo(myoint, 200, true);
                setSampleOutputBy(myoint, 7);
                //myoint = Blender.renderBlend(myoint, temp3, Blend.ADD);
                //binarize the final myotube signal layer and get nucleus outlines
                myoint = Filters.thresholdBright().runSingle(myoint, param.slider[0]);
                setSampleOutputBy(myoint, 8);
                myoint = FiltersSet.filterObjectSize().runSingle(myoint, COL.BLACK, nuclSize * nuclSize, true, nuclSize * nuclSize);
                nuclei = FiltersSet.getOutlines().runSingle(nuclei, COL.BLACK);
            }

            @Override
            protected void methodFinal() {
                //quantify 
                pt = Protocol.load(_AreaStainIntensity::new);
                setOutputBy(myoint, 2);
                temp = pt.runSilent(sourceImage, new ImageData[]{nuclei, myoint, myoint}, COL.BLACK, true, 70, false, false, false)[0];
                setOutputBy(temp, 1);
                addResultData(pt);
                //render outlines
                temp2 = Blender.renderBlend(inImage[0], inImage[1], Blend.ADD);
                temp3 = FiltersPass.getOutlines().runSingle(myoint, COL.BLACK);
                applyOperator(temp, temp, p -> temp.pixels32[p] == COL.BLACK
                        ? (temp3.pixels32[p] == COL.WHITE ? COL.WHITE : temp2.pixels32[p])
                        : (temp.pixels32[p] == COL.WHITE ? COL.GREEN : COL.RED));
                /*pt = Protocol.load(ObjectEdges::new);
                temp = pt.runSilent(sourceImage, new ImageData[]{temp, temp2}, COL.GREEN, COL.RED)[0];*/
                setOutputBy(temp,0);
            }

        };
    }
}
