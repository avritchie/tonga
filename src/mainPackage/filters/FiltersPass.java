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
import mainPackage.protocols.NucleusEdUCounter;
import mainPackage.protocols.Protocol;
import mainPackage.utils.DRAW;

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
}
