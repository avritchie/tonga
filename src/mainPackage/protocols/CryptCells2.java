package mainPackage.protocols;

import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.ANNOTATION;
import static mainPackage.PanelCreator.ControlType.COMBO;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import mainPackage.TongaAnnotator.AnnotationType;
import static mainPackage.TongaAnnotator.AnnotationType.DOT;
import mainPackage.counters.SetCounters;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class CryptCells2 extends Protocol {

    @Override
    public String getName() {
        return "Cells in intestinal crypts";
    }

    @Override
    protected ControlReference[] getParameters() {

        return new ControlReference[]{
            new ControlReference(LAYER, "Layer to process"),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "DAPI is on which channel", 2),
            new ControlReference(SPINNER, "Average nucleus diameter (pixels)", 40),
            new ControlReference(ANNOTATION, new AnnotationType[]{DOT}, "Dot to mark the crypt center")};
    }

    @Override
    protected Processor getProcessor() {

        return new ProcessorFast(14, "Crypts", 278) {

            ImageData cells, cellsraw, cellmask, temp, temp2, temp3;
            int cellchannel = param.combo[0];
            int celldiam = param.spinner[0];

            @Override
            protected void pixelProcessor() {
                cellsraw = Filters.separateChannel().runSingle(inImage[0], cellchannel);
                Protocol nprot = Protocol.load(__NucleusPrimaryMask::new);
                cellmask = nprot.runSilent(sourceImage, cellsraw, (int) (celldiam * 0.75))[0];
                setSampleOutputBy(cellmask, 1);
                cells = nprot.runSilent(sourceImage, inImage[0], (int) (celldiam * 0.75))[0];
                setSampleOutputBy(cells, 2);
                temp = Blender.renderBlend(cells, cellmask, Blend.DIFFERENCE);
                temp2 = FiltersSet.filterObjectSizeDimension().runSingle(temp, (int) (celldiam * 2.5), (int) (celldiam * 0.25), COL.BLACK, false);
                cells = FiltersPass.gaussSmoothing().runSingle(temp2, 5, 1);
                cells = Protocol.load(ObjectsCommon::new).runSilent(sourceImage, new ImageData[]{cells, temp}, COL.BLACK, 2)[0];
                temp = Blender.renderBlend(cells, temp2, Blend.DIFFERENCE);
                cells = Blender.renderBlend(cellmask, cells, Blend.ADD);
                temp2 = Protocol.load(ObjectsSeparated::new).runSilent(sourceImage, new ImageData[]{cellmask, cells}, COL.BLACK, 1)[0];
                temp2 = FiltersSet.getRadiusOverlap().runSingle(temp2, COL.BLACK, (int) (celldiam * 0.15));
                Filters.invert().runTo(temp2);
                cells = Blender.renderBlend(cells, temp2, Blend.MULTIPLY);
                setSampleOutputBy(cells, 3);
                //extra block for additional separation
                temp2 = Protocol.load(__ObjectSegmentAngle::new).runSilent(sourceImage, cells, COL.BLACK, lparam.annotation[0], 45, 1, (int) (celldiam * 1.5))[0];
                temp2 = Protocol.load(__ObjectSegmentAngle::new).runSilent(sourceImage, temp2, COL.BLACK, lparam.annotation[0], 45, 1, (int) (celldiam * 1.5))[0];
                FiltersPass.fillSmallEdgeHoles().runTo(cells, COL.BLACK, 2, (int) (celldiam * 1.5), true, false);
                cells = Blender.renderBlend(cells, temp, Blend.DIFFERENCE);
                setSampleOutputBy(cells, 4);
                setSampleOutputBy(temp2, 5);
                temp = Protocol.load(MaskCutter::new).runSilent(sourceImage, new ImageData[]{temp2, cellsraw}, celldiam, 5)[0];
                setSampleOutputBy(temp, 6);
                temp = Protocol.load(ObjectsSeparated::new).runSilent(sourceImage, new ImageData[]{temp, temp2}, COL.BLACK, 1)[0];
                setSampleOutputBy(temp, 7);
                ROISet set = FiltersSet.filterObjectSize().runSet(temp, COL.BLACK, celldiam * 2, false, 0);
                temp2 = FiltersSet.getRadiusOverlap().runSingle(temp2, COL.BLACK, (int) (celldiam * 0.1) + 1);
                temp = FiltersSet.getRadiusOverlap().runSingle(set, COL.BLACK, (int) (celldiam * 0.1));
                temp = FiltersSet.filterObjectRatioAngle().runSingle(temp, COL.BLACK, lparam.annotation[0], 0.63, false);
                temp = Blender.renderBlend(new ImageData[]{temp, temp2}, Blend.ADD);
                setSampleOutputBy(temp, 8);
                Filters.invert().runTo(temp);
                cells = Blender.renderBlend(cells, temp, Blend.MULTIPLY);
                setSampleOutputBy(cells, 9);
                cells = FiltersPass.fillSmallEdgeHoles().runSingle(cells, COL.BLACK, 2, celldiam, false, false);
                //continue
                cells = FiltersSet.fillInnerAreasSizeShape().runSingle(cells, COL.BLACK, (int) (celldiam * 1.5), 90, false, 0, true);
                setSampleOutputBy(cells, 10);
                cells = Blender.renderBlend(cells, temp, Blend.MULTIPLY);
                FiltersPass.gaussSmoothing().runTo(cells, 1, 5);
                setSampleOutputBy(cells, 11);
                cells = FiltersPass.edgeErode().runSingle(cells, COL.BLACK, 2, true, true);
                FiltersSet.filterObjectSizeDimension().runTo(cells, (int) (celldiam * 1), (int) (celldiam * 0.25), COL.BLACK, false);
                Filters.dotConnectRemove().runTo(cells, COL.BLACK, false);
                temp = FiltersSet.getRadiusOverlap().runSingle(cells, COL.BLACK, (int) (celldiam * 0.15));
                Filters.invert().runTo(temp);
                cells = FiltersPass.edgeDilate().runSingle(cells, COL.BLACK, 2, true);
                cells = Blender.renderBlend(cells, temp, Blend.MULTIPLY);
                setSampleOutputBy(cells, 12);
                temp = FiltersPass.gaussSmoothing().runSingle(cells, 2, 5);
                cells = Blender.renderBlend(cells, temp, Blend.MAXIMUM);
                setSampleOutputBy(cells, 13);
                cells = Protocol.load(__ObjectSegmentAngle::new).runSilent(sourceImage, cells, COL.BLACK, lparam.annotation[0], 45, 1, (int) (celldiam * 1.5))[0];
                //finalize
                FiltersPass.gaussSmoothing().runTo(cells, 2, 2);
                FiltersPass.gaussSmoothing().runTo(cells, 1, 5);
                ROISet rs = FiltersSet.filterObjectSizeDimension().runSet(cells, (int) (celldiam * 2.5), (int) (celldiam * 0.25), COL.BLACK, false);
                setOutputBy(rs.drawToImageData());
                addResultData(SetCounters.countObjectsImage(rs).runSingle(sourceImage));
            }
        };
    }
}
