/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mainPackage.protocols;

import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.LAYER;
import static mainPackage.PanelCreator.ControlType.SLIDER;
import static mainPackage.PanelCreator.ControlType.SPINNER;
import static mainPackage.PanelCreator.ControlType.TOGGLE;
import mainPackage.filters.Filters;
import mainPackage.filters.FiltersPass;
import mainPackage.filters.FiltersSet;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class ISHNuclei extends Protocol {

    @Override
    public String getName() {
        return "ISH Nuclei";
    }

    @Override
    protected ControlReference[] getParameters() {
        return new ControlReference[]{
            new ControlReference(LAYER, "Image is on which layer"),
            new ControlReference(SPINNER, "Target size (pixels)", 25),
            new ControlReference(TOGGLE, "Use gradient", 1),
            new ControlReference(SLIDER, new Object[]{0.1, 1, 10, 36}, "Remove small objects (multiplier)", 26),
            new ControlReference(TOGGLE, "Use sum threshold", 0, new int[]{5, 0, 6, 1}),
            new ControlReference(SLIDER, "Binary threshold (%)"),
            new ControlReference(SPINNER, "Sum threshold (intensity)", 500)};
    }

    @Override
    protected Processor getProcessor() {
        int targetSize = param.spinner[0];
        double removSize = param.sliderScaled[0];
        boolean grad = param.toggle[0];
        boolean sumth = param.toggle[1];
        int sumthresh = param.spinner[1];
        double thresh = param.slider[1];

        return new ProcessorFast(8, "Nuclei", 254) {

            ImageData temp, temp2, adjusted;
            Protocol eprot;
            ImageData[] separation;

            @Override
            protected void pixelProcessor() {
                temp = Filters.invert().runSingle(inImage[0]);
                temp2 = Filters.cutFilter().runSingle(temp, 50, 230);
                eprot = Protocol.load(__NucleusPrimaryMask::new);
                separation = eprot.runSilentFull(sourceImage, temp2, targetSize);
                applyOperator(inImage[0], temp,
                        p -> (RGB.brightness(separation[10].pixels32[p]) > 0 || separation[6].pixels32[p] == COL.BLACK ? COL.BLACK : COL.WHITE));
                eprot = null;
                adjusted = separation[1];
                separation = null;
                FiltersSet.fillInnerAreasSizeShape().runTo(temp, COL.BLACK, targetSize * 2.5, 90, true, targetSize * 2.5, true);
                FiltersPass.gaussSmoothing().runTo(temp, 2, 1);
                FiltersPass.gaussSmoothing().runTo(temp, 1, 5);
                setSampleOutputBy(temp, 3);
                if (grad) {
                    eprot = Protocol.load(__NucleusFinalMask::new);
                    separation = eprot.runSilentFull(sourceImage, new ImageData[]{temp, temp2, adjusted}, false, false, targetSize * 2, true);
                    FiltersSet.fillInnerAreasSizeShape().runTo(separation[7], temp, COL.BLACK, targetSize * 2, 100, true, targetSize * 2, true);
                    applyOperator(inImage[0], temp,
                            p -> (separation[15].pixels32[p] == COL.WHITE || separation[20].pixels32[p] == COL.WHITE ? COL.BLACK : temp.pixels32[p]));
                    eprot = null;
                    separation = null;
                }
                FiltersPass.gaussSmoothing().runTo(temp, 2, 2);
                setSampleOutputBy(temp, 4);
                eprot = Protocol.load(__ObjectSegment::new);
                separation = eprot.runSilent(sourceImage, temp, COL.BLACK, targetSize * 2.5, 0, false);
                FiltersPass.gaussSmoothing().runTo(separation[0], temp, 2, 2);
                FiltersSet.filterObjectSize().runTo(temp, COL.BLACK, targetSize * removSize);
                setSampleOutputBy(temp, 5);
                separation = eprot.runSilent(sourceImage, temp, COL.BLACK, targetSize * 2.5, 0, true);
                FiltersSet.getRadiusOverlap().runTo(separation[0], temp2, COL.BLACK, 5);
                FiltersPass.edgeDilate().runTo(separation[0], temp, COL.BLACK, 1, true);
                FiltersPass.gaussSmoothing().runTo(temp, 2, 6);
                applyOperator(inImage[0], temp,
                        p -> (temp2.pixels32[p] == COL.WHITE ? COL.BLACK : temp.pixels32[p]));
                FiltersPass.gaussSmoothing().runTo(temp, 1, 6);
                setSampleOutputBy(temp, 6);
                separation = eprot.runSilent(sourceImage, temp, COL.BLACK, targetSize * 2, 0, false);
                FiltersSet.getRadiusOverlap().runTo(separation[0], temp2, COL.BLACK, 5);
                FiltersPass.gaussSmoothing().runTo(separation[0], temp, 2, 3);
                applyOperator(inImage[0], temp,
                        p -> (temp2.pixels32[p] == COL.WHITE ? COL.BLACK : temp.pixels32[p]));
                setSampleOutputBy(temp, 7);
                FiltersSet.filterObjectSize().runTo(temp, COL.BLACK, targetSize * (removSize + 2));
                eprot = Protocol.load(InSituRNA::new);
                separation = eprot.runSilent(sourceImage, inImage[0], 0, 80, 3, 0);
                applyOperator(inImage[0], temp,
                        p -> (temp.pixels32[p] == COL.BLACK ? COL.BLACK : separation[0].pixels32[p]));
                setSampleOutputBy(temp, 2);
            }

            @Override
            protected void methodFinal() {
                //quantify results
                eprot = Protocol.load(_ObjectIntensity::new);
                separation = eprot.runSilent(sourceImage, temp, COL.BLACK, true, sumth, thresh, sumthresh, false);
                setSampleOutputBy(separation[0], 1);
                addResultData(eprot.results);
                //render the outlines to the original image
                eprot = Protocol.load(ObjectEdges::new);
                separation = eprot.runSilent(sourceImage, new ImageData[]{separation[0], inImage[0]}, COL.GREEN, COL.RED);
                setOutputBy(separation[0]);
            }

        };
    }
}
