/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage.filters;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import mainPackage.Blender;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.Tonga;
import mainPackage.utils.IMG;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import static mainPackage.filters.Filter.noParams;
import mainPackage.filters.FilterSet.OutputType;
import mainPackage.morphology.EdgeAnalyzer;
import mainPackage.protocols.ApplyIllumination;
import mainPackage.protocols.AverageMaskThreshold;
import mainPackage.protocols.DimRemover;
import mainPackage.protocols.NucleusEdUCounter;
import mainPackage.protocols.Protocol;
import mainPackage.utils.DRAW;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

/**
 *
 * @author aritchie
 */
public class FiltersPass {

    public static FilterSet fillSmallEdgeHoles() {
        return new FilterSet("Edge filler", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Dilation radius", 1),
            new ControlReference(SPINNER, "Size smaller than (pixels)", 50),
            new ControlReference(TOGGLE, "Pre-remove small objects", 1),
            new ControlReference(TOGGLE, "Return only the filling", 0)}, OutputType.OUTDATA) {

            ImageData mMask, sMask, oMask;

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    mMask = FiltersSet.filterObjectSize().runSingle(inSet, param.colorARGB[0], param.spinner[1], false, 0);
                }
                oMask = FiltersSet.getRadiusOverlap().runSingle(inSet, param.colorARGB[0], param.spinner[0] * 4);
                mMask = FiltersPass.edgeDilate().runSingle(inSet, param.toggle[0] ? mMask : inData, param.colorARGB[0], param.spinner[0], false);
                sMask = FiltersSet.fillInnerAreasSizeShape().runSingle(mMask, param.colorARGB[0], param.spinner[1], 80);
                sMask = Blender.renderBlend(mMask, sMask, Blend.DIFFERENCE);
                sMask = FiltersPass.edgeDilate().runSingle(sMask, COL.BLACK, param.spinner[0] + 1, false);
                if (param.toggle[1]) {
                    Iterate.pixels(inData, (int p) -> {
                        outData.pixels32[p] = oMask.pixels32[p] == COL.BLACK && sMask.pixels32[p] == COL.WHITE
                                ? COL.WHITE : COL.BLACK;
                    });
                } else {
                    Iterate.pixels(inData, (int p) -> {
                        outData.pixels32[p] = oMask.pixels32[p] == COL.BLACK && (inData.pixels32[p] != param.colorARGB[0] || sMask.pixels32[p] == COL.WHITE)
                                ? COL.WHITE : COL.BLACK;
                    });
                }
            }
        };
    }

    public static FilterSet connectLineObjects() {
        return new FilterSet("Size/shape Filter", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Maximum distance allowed", 20)}, OutputType.OUTDATA) {

            @Override
            protected void processorSet() {
                setOutputBy(inSet);
                ArrayList<Point> points = inSet.connectLongAndThinObjects();
                for (int i = 0; i < points.size() / 2; i += 2) {
                    if (GEO.getDist(points.get(i), points.get(i + 1)) < param.spinner[0]) {
                        new DRAW.lineDrawer() {
                            @Override
                            public void action(int x, int y) {
                                for (int i = -1; i <= 1; i++) {
                                    for (int j = -1; j <= 1; j++) {
                                        try {
                                            outData.pixels32[(y + j) * width + (x + i)] = COL.WHITE;
                                        } catch (ArrayIndexOutOfBoundsException ex) {
                                        }
                                    }
                                }
                            }
                        }.drawLine(points.get(i), points.get(i + 1));
                    }
                }
            }
        };
    }

    public static FilterSet edgeErode() {
        return new FilterSet("Eroded",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Radius (px)", 2),
                    new ControlReference(TOGGLE, "Diagonal erosion", 0),
                    new ControlReference(TOGGLE, "No border erosion", 1)}, OutputType.OUTDATA, 3) {

            @Override
            protected void processorSet() {
                boolean diagonal = param.toggle[0];
                boolean border = param.toggle[1];
                IMG.copyPixels(in32, out32);
                inSet.findOuterEdges();
                inSet.findInnerEdges();
                int r = param.spinner[0] - 1;
                Tonga.iteration(2);
                inSet.list.forEach(o -> {
                    List<Point> edges = new ArrayList<>();
                    edges.addAll(o.innEdge.getPoints());
                    edges.addAll(o.outEdge.getPoints());
                    if (border) {
                        EdgeAnalyzer.removeBorders(o, edges);
                    }
                    edges.forEach(pp -> {
                        for (int xx = -r; xx <= r; xx++) {
                            for (int yy = -r; yy <= r; yy++) {
                                int xl = pp.x + xx, yl = pp.y + yy;
                                int p = width * (yl + o.area.ystart) + (xl + o.area.xstart);
                                try {
                                    if (out32[p] != param.colorARGB[0] && p / width == yl + o.area.ystart
                                            && (!diagonal || GEO.getDist(pp, new Point(xl, yl)) <= r)) {
                                        out32[p] = param.colorARGB[0];
                                    }
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    //outside area
                                }
                            }
                        }
                    });
                    Tonga.loader().appendProgress(2. / inSet.list.size());
                });
            }
        };
    }

    public static FilterSet edgeDilate() {
        return new FilterSet("Dilated",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Radius (px)", 2),
                    new ControlReference(TOGGLE, "Circular dilation", 0)}, OutputType.OUTDATA, 3) {

            @Override
            protected void processorSet() {
                IMG.copyPixels(in32, out32);
                inSet.findOuterEdges();
                inSet.findInnerEdges();
                int r = param.spinner[0];
                Tonga.iteration(2);
                inSet.list.forEach(o -> {
                    List<Point> edges = new ArrayList<>();
                    edges.addAll(o.innEdge.getPoints());
                    edges.addAll(o.outEdge.getPoints());
                    edges.forEach(pp -> {
                        for (int xx = -r; xx <= r; xx++) {
                            for (int yy = -r; yy <= r; yy++) {
                                int xl = pp.x + xx, yl = pp.y + yy;
                                int p = width * (yl + o.area.ystart) + (xl + o.area.xstart);
                                try {
                                    if (out32[p] == param.colorARGB[0] && p / width == yl + o.area.ystart
                                            && (!param.toggle[0] || GEO.getDist(pp, new Point(xl, yl)) <= r)) {
                                        out32[p] = in32[width * (pp.y + o.area.ystart) + (pp.x + o.area.xstart)];
                                    }
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    //outside area
                                }
                            }
                        }
                    });
                    Tonga.loader().appendProgress(2. / inSet.list.size());
                });
            }
        };
    }

    public static FilterFast gaussSmoothing() {
        return new FilterFast("Smoothing",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Radius (px)", 2),
                    new ControlReference(SPINNER, "Repeat x times", 5)}) {

            @Override
            protected void processor() {
                ImageData id = inData;
                for (int i = 0; i < param.spinner[1]; i++) {
                    id = Filters.gaussApprox().runSingle(id, param.spinner[0], true);
                    id = Filters.thresholdBright().runSingle(id, 50);
                }
                setOutputBy(id);
            }

            @Override
            protected int iterations(boolean bits) {
                return (int) (param.spinner[1] * 5);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast fuzzyCorrection() {
        return new FilterFast("Fuzzy", new ControlReference[]{
            new ControlReference(SPINNER, "Average object size (px)", 50)}, 6) {

            ImageData tempData;

            @Override
            protected void processor() {
                tempData = Filters.dog().runSingle(inData, param.spinner[0] / 25, param.spinner[0] / 2, true);
                ImageData id = Blender.renderBlend(inData, tempData, Blend.SUBTRACT);
                setOutputBy(id);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast balancedScaling() {
        return new FilterFast("Balanced", new ControlReference[]{
            new ControlReference(SLIDER, new Object[]{0, 1, 100}, "Balancing strength")}, 12) {

            @Override
            protected void processor() {
                Filters.gamma().runTo(inData, outData, 1 - param.sliderScaled[0] * 0.95);
                Filters.scaleDark().runTo(outData);
            }

            @Override
            protected void processor16() {
                Filters.gamma().runTo(inData, outData, 1 - param.sliderScaled[0] * 0.95);
                Filters.scaleDark().runTo(outData);
            }
        };
    }

    public static FilterFast adaptiveThreshold() {
        return new FilterFast("Adaptive Threshold", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SLIDER, new Object[]{0, 10, 50}, "Sensitivity", 10),
            new ControlReference(SPINNER, "Radius", 40),
            new ControlReference(TOGGLE, "Apply average mask", 0),}, 10) {

            ImageData temp, temp2;

            @Override
            protected void processor() {
                temp = Filters.gamma().runSingle(inData, 0.1);
                //prevent corrected bg-coloured areas from jamming the dilation
                double avg = Filters.averageIntensity(inData, temp.pixels32, param.colorARGB[0]);
                temp2 = Filters.gaussApprox().runSingle(temp, 5, true);
                temp2 = Filters.thresholdBright().runSingle(temp2, 1);
                Iterate.pixels(this, (int pos) -> {
                    temp.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE && temp.pixels32[pos] == param.colorARGB[0] ? RGB.argb((int) avg) : temp.pixels32[pos];
                });
                //then dilate the edge
                temp2 = FiltersPass.edgeDilate().runSingle(temp, param.colorARGB[0], param.spinner[0] * 2);
                temp2 = Filters.localThreshold().runSingle(temp2, 1 + param.sliderScaled[0], param.spinner[0]);
                if (param.toggle[0]) {
                    Iterate.pixels(this, (int pos) -> {
                        temp2.pixels32[pos] = inData.pixels32[pos] == param.colorARGB[0] ? COL.WHITE : temp2.pixels32[pos];
                    });
                    Protocol.load(AverageMaskThreshold::new).runSilentTo(null, new ImageData[]{inData, temp2}, temp);
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = inData.pixels32[pos] == param.colorARGB[0]
                                ? param.colorARGB[0]
                                : temp.pixels32[pos] == COL.BLACK || temp2.pixels32[pos] == COL.BLACK
                                        ? COL.BLACK
                                        : COL.WHITE;
                    });
                } else {
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = inData.pixels32[pos] == param.colorARGB[0] ? param.colorARGB[0] : temp2.pixels32[pos];
                    });
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast dogSementing() {
        return new FilterFast("Segmenting", new ControlReference[]{
            new ControlReference(SLIDER, "Sensitivity", 50),
            new ControlReference(SPINNER, "Radius", 50),
            new ControlReference(COMBO, new String[]{"Darkfield/IF", "Brightfield"}, "Type", 0),
            new ControlReference(TOGGLE, "Detect in-between areas", 1)}, 33) {

            ImageData temp, temp2, temp3, temp4;

            @Override
            protected void processor() {
                temp = Filters.gamma().runSingle(inData, 0.35);
                Filters.autoscale().runTo(temp);
                //scaled reverse dog for primary segmentation
                temp2 = Filters.dog().runSingle(temp, Math.min(1, param.spinner[0] / 50), param.spinner[0], param.combo[0] == 0);
                temp3 = Filters.autoscale().runSingle(temp2);
                //get two thresholdings from the primary segmentation
                Filters.thresholdBright().runTo(temp3, temp2, 30 + (100 - param.slider[0]) * 0.3);
                Filters.thresholdBright().runTo(temp3, 10);
                //intensity gradient for secondary segmentation
                temp4 = Filters.maximumDiffEdge().runSingle(temp, 0, 1, true, 6 + (param.slider[0] / 10));
                Filters.connectEdges().runTo(temp4, COL.BLACK, false);
                //merge and smoothen the results to get an acceptable combined result
                Iterate.pixels(this, (int pos) -> {
                    temp2.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE || temp3.pixels32[pos] == COL.WHITE && temp4.pixels32[pos] == COL.BLACK ? COL.WHITE : COL.BLACK;
                });
                FiltersPass.gaussSmoothing().runTo(temp2, 1, 5);
                //remove/classify areas to be in-between based on scaled intensity
                Filters.scaleDark().runTo(temp);
                Protocol.load(DimRemover::new).runSilentTo(null, new ImageData[]{temp2, temp}, temp, COL.BLACK, 0, true, 67.0);
                if (param.toggle[0]) {
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE ? temp.pixels32[pos] == COL.BLACK ? COL.BLACK : COL.GRAY : COL.WHITE;
                    });
                } else {
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE && temp.pixels32[pos] == COL.BLACK ? COL.BLACK : COL.WHITE;
                    });
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast multiLocalThreshold() {
        return new FilterFast("Segmenting", new ControlReference[]{
            new ControlReference(SLIDER, "Tissue scale factor", 40),
            new ControlReference(SPINNER, "Radius", 40),
            new ControlReference(TOGGLE, "Smooth", 1)}, 33) {

            ImageData binar, temp, temp2, temp3;

            @Override
            protected void processor() {
                int tissueScale = param.slider[0];
                int rad = param.spinner[0];
                //get the binary mask
                binar = FiltersPass.adaptiveThreshold().runSingle(inData, COL.dataCornerColour(inData), 10.0, tissueScale);
                //perform dapi
                temp = Blender.renderBlend(binar, inData, Blend.MULTIPLY);
                temp = Filters.blurConditional().runSingle(temp, COL.BLACK, tissueScale / 4, false);
                Filters.autoscale().runTo(temp);
                temp2 = Filters.localThreshold().runSingle(temp, 15, rad);
                temp3 = Blender.renderBlend(binar, temp2, Blend.MINIMUM);
                for (int i = 2; i < 5; i++) {
                    temp2 = Filters.localThreshold().runSingle(temp, 15, rad * i);
                    temp3 = Blender.renderBlend(temp3, temp2, Blend.MINIMUM);
                }
                if (param.toggle[0]) {
                    temp = Filters.connectEdges().runSingle(temp3, COL.BLACK, true);
                    setOutputBy(temp);
                } else {
                    setOutputBy(temp3);
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast avgSementing() {
        return new FilterFast("Segmenting", new ControlReference[]{
            new ControlReference(SLIDER, "Sensitivity", 50),
            new ControlReference(SPINNER, "Radius", 40)}, 33) {

            ImageData temp, temp2, temp3, temp4;

            @Override
            protected void processor() {
                temp = FiltersPass.adaptiveThreshold().runSingle(inData, COL.dataCornerColour(inData), 5.0, param.spinner[0]);
                //get two thresholdings from the primary segmentation
                Filters.thresholdBright().runTo(temp3, temp2, 30 + (100 - param.slider[0]) * 0.3);
                Filters.thresholdBright().runTo(temp3, 10);
                //intensity gradient for secondary segmentation
                temp4 = Filters.maximumDiffEdge().runSingle(temp, 0, 1, true, 6);
                Filters.connectEdges().runTo(temp4, COL.BLACK, false);
                //merge and smoothen the results to get an acceptable combined result
                Iterate.pixels(this, (int pos) -> {
                    temp2.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE || temp3.pixels32[pos] == COL.WHITE && temp4.pixels32[pos] == COL.BLACK ? COL.WHITE : COL.BLACK;
                });
                FiltersPass.gaussSmoothing().runTo(temp2, 1, 5);
                //remove/classify areas to be in-between based on scaled intensity
                Filters.scaleDark().runTo(temp);
                Protocol.load(DimRemover::new).runSilentTo(null, new ImageData[]{temp2, temp}, temp, COL.BLACK, 0, true, 67.0);
                if (param.toggle[0]) {
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE ? temp.pixels32[pos] == COL.BLACK ? COL.BLACK : COL.GRAY : COL.WHITE;
                    });
                } else {
                    Iterate.pixels(this, (int pos) -> {
                        outData.pixels32[pos] = temp2.pixels32[pos] == COL.WHITE && temp.pixels32[pos] == COL.BLACK ? COL.BLACK : COL.WHITE;
                    });
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast maxDiffCorrect() {
        return new FilterFast("Background removal", new ControlReference[]{
            new ControlReference(SPINNER, "Radius", 40)}, 14) {

            ImageData temp;
            int rad;

            @Override
            protected void processor() {
                rad = Math.max(1, 5 - (param.spinner[0] / 10));
                //average
                temp = Filters.maximumDiffEdge().runSingle(inData, 0, rad, true, 20);
                Iterate.pixels(this, (int pos) -> {
                    temp.pixels32[pos] = temp.pixels32[pos] == COL.WHITE ? COL.BLACK : inData.pixels32[pos];
                });
                temp = Filters.blurConditional().runSingle(temp, COL.BLACK, param.spinner[0] / 4, false);
                Protocol.load(ApplyIllumination::new).runSilentTo(null, new ImageData[]{inData, temp}, temp, true, false, true, COL.BLACK, false, null);
                setOutputBy(temp);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast backgroundStainingAvgSubtraction() {
        return new FilterFast("Background removal", new ControlReference[]{
            new ControlReference(TOGGLE, "Red channel", 1),
            new ControlReference(TOGGLE, "Green channel", 1),
            new ControlReference(TOGGLE, "Blue channel", 1),
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Radius", 40),
            new ControlReference(SLIDER, new Object[]{0.5, 2, 150}, "Filter", 50),
            new ControlReference(TOGGLE, "Extract the background", 0)}, 3) {

            ImageData ifthree, ifone, iftwo;
            int rad;

            @Override
            protected void processor() {
                rad = Math.max(1, 5 - (param.spinner[0] / 10));
                //average
                ifone = Filters.separateChannel().runSingle(inData, 0);
                iftwo = Filters.separateChannel().runSingle(inData, 1);
                ifthree = Filters.separateChannel().runSingle(inData, 2);
                if (param.toggle[0]) {
                    correct(ifone);
                }
                if (param.toggle[1]) {
                    correct(iftwo);
                }
                if (param.toggle[2]) {
                    correct(ifthree);
                }
                setOutputBy(Blender.renderBlend(new ImageData[]{ifone, iftwo, ifthree}, Blend.ADD));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }

            private void correct(ImageData channel) {
                double avg = averageGradientIntensity(channel, channel.pixels32, rad, 25, param.colorARGB[0]) * param.sliderScaled[0];
                /*
                ImageData temp = Filters.maximumDiffEdge().runSingle(channel, 0, rad, false, 25);
                Iterate.pixels(this, (int pos) -> {
                    temp.pixels32[pos] = inData.pixels32[pos] == param.colorARGB[0] || (temp.pixels32[pos] & 0xFF) > 25 ? COL.WHITE : channel.pixels32[pos];
                });
                double avg = Filters.averageIntensity(inData, temp.pixels32, COL.WHITE) * param.sliderScaled[0]; */
                if (!param.toggle[3]) {
                    Iterate.pixels(this, (int pos) -> {
                        channel.pixels32[pos] = RGB.argb(
                                (int) Math.max(0x00, (channel.pixels32[pos] >> 16 & 0xFF) - (int) avg),
                                (int) Math.max(0x00, (channel.pixels32[pos] >> 8 & 0xFF) - (int) avg),
                                (int) Math.max(0x00, (channel.pixels32[pos] & 0xFF) - (int) avg), 255);
                    });
                } else {
                    Iterate.pixels(this, (int pos) -> {
                        channel.pixels32[pos] = RGB.argb(
                                (channel.pixels32[pos] >> 16 & 0xFF) - (int) Math.max(0x00, (channel.pixels32[pos] >> 16 & 0xFF) - (int) avg),
                                (channel.pixels32[pos] >> 8 & 0xFF) - (int) Math.max(0x00, (channel.pixels32[pos] >> 8 & 0xFF) - (int) avg),
                                (channel.pixels32[pos] & 0xFF) - (int) Math.max(0x00, (channel.pixels32[pos] & 0xFF) - (int) avg), 255);
                    });
                }
            }
        };
    }

    public static FilterFast backgroundStainingAvgSubtractionShit() {
        return new FilterFast("Background removal", new ControlReference[]{
            new ControlReference(TOGGLE, "Red channel", 1),
            new ControlReference(TOGGLE, "Green channel", 1),
            new ControlReference(TOGGLE, "Blue channel", 1),
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SLIDER, new Object[]{1, 2, 100}, "Filter", 50)}, 2) {

            ImageData ifthree, ifone, iftwo;

            @Override
            protected void processor() {
                //average
                ifone = Filters.separateChannel().runSingle(inData, 0);
                iftwo = Filters.separateChannel().runSingle(inData, 1);
                ifthree = Filters.separateChannel().runSingle(inData, 2);
                if (param.toggle[0]) {
                    correct(ifone);
                }
                if (param.toggle[1]) {
                    correct(iftwo);
                }
                if (param.toggle[2]) {
                    correct(ifthree);
                }
                setOutputBy(Blender.renderBlend(new ImageData[]{ifone, iftwo, ifthree}, Blend.ADD));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }

            private void correct(ImageData channel) {
                double avg = Filters.averageIntensity(inData, channel.pixels32, param.colorARGB[0]) * param.sliderScaled[0];
                Iterate.pixels(this, (int pos) -> {
                    channel.pixels32[pos] = RGB.argb(
                            (int) Math.max(0x00, (channel.pixels32[pos] >> 16 & 0xFF) - (int) avg),
                            (int) Math.max(0x00, (channel.pixels32[pos] >> 8 & 0xFF) - (int) avg),
                            (int) Math.max(0x00, (channel.pixels32[pos] & 0xFF) - (int) avg), 255);
                });
            }
        };
    }

    public static FilterFast backgroundStainingScaling() {
        return new FilterFast("Background removal", new ControlReference[]{
            new ControlReference(TOGGLE, "Red channel", 1),
            new ControlReference(TOGGLE, "Green channel", 1),
            new ControlReference(TOGGLE, "Blue channel", 1)}, 2) {

            ImageData ifchanRed, ifchanGreen, ifchanBlue;

            @Override
            protected void processor() {
                ifchanRed = Filters.separateChannel().runSingle(inData, 0);
                ifchanGreen = Filters.separateChannel().runSingle(inData, 1);
                ifchanBlue = Filters.separateChannel().runSingle(inData, 2);
                if (param.toggle[0]) {
                    int dapipeak = HISTO.getHighestPointIndex(HISTO.getHistogram(ifchanRed.pixels32), true);
                    Filters.cutFilter().runTo(ifchanRed, dapipeak, 255);
                }
                if (param.toggle[1]) {
                    int chan1peak = HISTO.getHighestPointIndex(HISTO.getHistogram(ifchanGreen.pixels32), true);
                    Filters.cutFilter().runTo(ifchanGreen, chan1peak, 255);
                }
                if (param.toggle[2]) {
                    int chan2peak = HISTO.getHighestPointIndex(HISTO.getHistogram(ifchanBlue.pixels32), true);
                    Filters.cutFilter().runTo(ifchanBlue, chan2peak, 255);
                }
                setOutputBy(Blender.renderBlend(new ImageData[]{ifchanRed, ifchanGreen, ifchanBlue}, Blend.ADD));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast getAnnotationMaskInternal() {
        return new FilterFast("Annotation",
                new ControlReference[]{
                    new ControlReference(ANNOTATION, "Which annotation to use"),
                    new ControlReference(TOGGLE, "All", 0, new int[]{0, 0}),
                    new ControlReference(TOGGLE, "Fill shapes", 1),
                    new ControlReference(TOGGLE, "Internal", 0)}) {

            @Override
            protected void processor() {
                List<TongaAnnotation> ta = new ArrayList<>();
                if (param.toggle[0]) {
                    ta = Tonga.getImage().annotations.getAnnotations();
                } else {
                    ta.add(param.annotation[0]);
                }
                ImageData id = param.toggle[2]
                        ? getAnnotationMask(ta, inData)
                        : getAnnotationMaskRender(ta, inData, param.toggle[1]);
                setOutputBy(id);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast getAnnotationMask() {
        return new FilterFast("Annotation",
                new ControlReference[]{
                    new ControlReference(ANNOTATION, "Which annotation to use"),
                    new ControlReference(TOGGLE, "All", 0, new int[]{0, 0})}) {

            @Override
            protected void processor() {
                List<TongaAnnotation> ta = new ArrayList<>();
                if (param.toggle[0]) {
                    ta = Tonga.getImage().annotations.getAnnotations();
                } else {
                    ta.add(param.annotation[0]);
                }
                ImageData id = getAnnotationMask(ta, inData);
                setOutputBy(id);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static ImageData getAnnotationMask(List<TongaAnnotation> ta, ImageData parent) {
        List<ROI> rl = new ArrayList<>();
        for (int a = 0; a < ta.size(); a++) {
            rl.add(new ROI(parent, ta.get(a).toArea(), COL.WHITE));
        }
        ROISet rs = new ROISet(rl, parent.width, parent.height);
        ImageData rid = rs.drawToImageData(true);
        return rid;
    }

    @Deprecated
    public static ImageData getAnnotationMaskRender(List<TongaAnnotation> ta, ImageData parent, boolean fill) {
        ImageData id = TongaAnnotator.renderAsImage(parent.width, parent.height, ta);
        Filters.thresholdBright().runTo(id, 1);
        if (fill) {
            FiltersSet.fillInnerAreas().runTo(id, COL.BLACK, true);
        }
        return id;
    }

    public static FilterFast getNucleiMask() {
        return new FilterFast("Nuclei", noParams) {

            @Override
            protected void processor() {
                ImageData id = Protocol.load(NucleusEdUCounter::new).runSilent(Tonga.getImage(), inData, new Object[]{null, null, false, null, 0})[0];
                setOutputBy(id);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    /* GENERAL SHARED FUNCTIONS */
    public static double averageGradientIntensity(ImageData id, int[] in32, int radius, int threshold, int bgcol) {
        ImageData temp = Filters.maximumDiffEdge().runSingle(id, 0, radius, false, threshold);
        Iterate.pixels(id, (int pos) -> {
            temp.pixels32[pos] = id.pixels32[pos] == bgcol || (temp.pixels32[pos] & 0xFF) > threshold ? COL.WHITE : id.pixels32[pos];
        });
        return Filters.averageIntensity(id, temp.pixels32, COL.WHITE);
    }
}
