/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mainPackage.filters;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import mainPackage.utils.COL;
import mainPackage.Tonga;
import mainPackage.utils.IMG;
import mainPackage.utils.GEO;
import mainPackage.ImageData;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import static mainPackage.PanelCreator.ControlType.*;
import static mainPackage.filters.Filter.noParams;
import mainPackage.morphology.EdgeAnalyzer;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.protocols.NucleusEdUCounter;
import mainPackage.protocols.Protocol;
import mainPackage.utils.DRAW;

/**
 *
 * @author aritchie
 */
public class FiltersPass {

    public static FilterFast fillInnerAreas() {
        return new FilterFast("Area Filler", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(TOGGLE, "Fix broken objects on image edges", 1)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                if (param.toggle[0]) {
                    set.imageEdgeFixer(false);
                }
                set.fillInnerHoles();
                setOutputBy(set.drawToImageData(true));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast fillInnerAreasSizeShape() {
        return new FilterFast("Area Filler", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Remove smaller than (pixels)", 100),
            new ControlReference(SLIDER, "Or rounder than (%)", 90)}, 4) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.imageEdgeFixer(false);
                set.findOuterEdges();
                set.findInnerEdges();
                ImageData origSet = set.drawToImageData(true);
                ROISet innerSet = new ImageTracer(origSet, COL.WHITE).traceInnerObjects(set);
                innerSet.filterOutLargeAndNonRound(param.spinner[0], param.slider[0] / 100.);
                setOutputBy(innerSet.drawToImageData(true));
                Iterate.pixels(this, (int pos) -> {
                    outData.pixels32[pos] = outData.pixels32[pos] == COL.WHITE || origSet.pixels32[pos] == COL.WHITE ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast innerAreas() {
        return new FilterFast("Inner Areas", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.findOuterEdges();
                set.findInnerEdges();
                ImageData origSet = set.drawToImageData(true);
                ROISet innerSet = new ImageTracer(origSet, COL.WHITE).traceInnerObjects(set);
                setOutputBy(innerSet.drawToImageData(true));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast filterObjectSize() {
        return new FilterFast("Size Filter",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Minimum size", 100),
                    new ControlReference(TOGGLE, "Also do for the background", 0, new int[]{3, 1}),
                    new ControlReference(SPINNER, "Background minimum size", 100)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.filterOutSmallObjects((int) param.spinner[0]);
                ImageData drawing = set.drawToImageData();
                if (param.toggle[0]) {
                    drawing = Filters.invert().runSingle(drawing);
                    set = new ImageTracer(drawing, param.color[0]).trace();
                    set.filterOutSmallObjects((int) (param.spinner[1]));
                    drawing = Filters.invert().runSingle(set.drawToImageData());
                }
                setOutputBy(drawing);
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 5 : 2;
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast filterObjectDimension() {
        return new FilterFast("Dimension Filter",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Minimum width or height", 10),
                    new ControlReference(TOGGLE, "Also do for the background", 0, new int[]{3, 1}),
                    new ControlReference(SPINNER, "Background minimum width or height", 10)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.filterOutByDimension((int) param.spinner[0]);
                ImageData drawing = set.drawToImageData();
                if (param.toggle[0]) {
                    drawing = Filters.invert().runSingle(drawing);
                    set = new ImageTracer(drawing, param.color[0]).trace();
                    set.filterOutByDimension((int) (param.spinner[1]));
                    drawing = Filters.invert().runSingle(set.drawToImageData());
                }
                setOutputBy(drawing);
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 5 : 2;
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast filterObjectSizeDimension() {
        return new FilterFast("Size/dimension Filter",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Minimum size", 100),
                    new ControlReference(SPINNER, "Minimum width or height", 10),
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(TOGGLE, "Also do for the background", 0, new int[]{4, 1, 5, 1}),
                    new ControlReference(SPINNER, "Background minimum size", 100),
                    new ControlReference(SPINNER, "Background minimum width or height", 10)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.filterOutSmallObjects((int) param.spinner[0]);
                set.filterOutByDimension((int) param.spinner[1]);
                ImageData drawing = set.drawToImageData();
                if (param.toggle[0]) {
                    drawing = Filters.invert().runSingle(drawing);
                    set = new ImageTracer(drawing, param.color[0]).trace();
                    set.filterOutSmallObjects((int) param.spinner[2]);
                    set.filterOutByDimension((int) (param.spinner[3]));
                    drawing = Filters.invert().runSingle(set.drawToImageData());
                }
                setOutputBy(drawing);
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 5 : 2;
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast filterObjectSizeShape() {
        return new FilterFast("Size/shape filter", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Remove smaller than (pixels)", 100),
            new ControlReference(SLIDER, "If also rounder than (%)", 90)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.filterOutSmallAndRound(param.spinner[0], param.slider[0] / 100.);
                setOutputBy(set.drawToImageData(true));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast filterEdgeTouchers() {
        return new FilterFast("Edge toucher filter", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.removeEdgeTouchers();
                setOutputBy(set.drawToImageData(true));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast fillSmallEdgeHoles() {
        return new FilterFast("Edge filler", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Dilation radius", 1),
            new ControlReference(SPINNER, "Size smaller than (pixels)", 50),
            new ControlReference(TOGGLE, "Pre-remove small objects", 1),
            new ControlReference(TOGGLE, "Return only the filling", 0)}, 4) {
            ImageData mMask, sMask;

            @Override
            protected void processor() {
                if (param.toggle[0]) {
                    mMask = FiltersPass.filterObjectSize().runSingle(inData, COL.BLACK, param.spinner[1], false, 0);
                }
                mMask = FiltersPass.edgeDilate().runSingle(param.toggle[0] ? mMask : inData, param.colorARGB[0], param.spinner[0], false);
                sMask = FiltersPass.fillInnerAreasSizeShape().runSingle(mMask, param.colorARGB[0], param.spinner[1], 80);
                sMask = FiltersRender.blendStack().runSingle(new ImageData[]{mMask, sMask}, 2);
                sMask = FiltersPass.edgeDilate().runSingle(sMask, COL.BLACK, param.spinner[0] + 1, false);
                if (param.toggle[1]) {
                    setOutputBy(sMask);
                } else {
                    Iterate.pixels(inData, (int p) -> {
                        outData.pixels32[p] = inData.pixels32[p] != param.colorARGB[0] || sMask.pixels32[p] == COL.WHITE ? COL.WHITE : COL.BLACK;
                    });
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast connectLineObjects() {
        return new FilterFast("Size/shape Filter", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Maximum distance allowed", 20)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                setOutputBy(set.drawToImageData());
                ArrayList<Point> points = set.connectLongAndThinObjects();
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

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast edgeErode() {
        return new FilterFast("Eroded",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Radius (px)", 2),
                    new ControlReference(TOGGLE, "Diagonal erosion", 0),
                    new ControlReference(TOGGLE, "No border erosion", 1)}) {

            @Override
            protected void processor() {
                boolean diagonal = param.toggle[0];
                boolean border = param.toggle[1];
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                IMG.copyPixels(in32, out32);
                set.findOuterEdges();
                set.findInnerEdges();
                int r = param.spinner[0] - 1;
                Tonga.iteration(2);
                set.list.forEach(o -> {
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
                    Tonga.loader().appendProgress(2. / set.list.size());
                });
            }

            /*
            protected void processor() {
                outData.fill(0xFF000000);
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                int r = param.spinner[0];
                set.list.forEach(o -> {
                    Tonga.iteration();
                    for (int x = 0; x < o.area.width; x++) {
                        for (int y = 0; y < o.area.height; y++) {
                            if (o.area.area[x][y]) {
                                int xr = x + o.area.xstart, yr = y + o.area.ystart;
                                int p = width * yr + xr;
                                int xmn = xr < r ? x - x : x - r;
                                int xmp = xr >= width - r ? x + width - xr - 1 : x + r;
                                int ymn = yr < r ? y - y : y - r;
                                int ymp = yr >= height - r ? y + height - yr - 1 : y + r;
                                try {
                                    if (param.toggle[0]) {
                                        if (o.area.area[xmp][ymp] && o.area.area[xmn][ymn]
                                                && o.area.area[xmn][ymp] && o.area.area[xmp][ymn]) {
                                            out32[p] = inData.pixels32[p];
                                        }
                                    } else {
                                        if (o.area.area[xmp][y] && o.area.area[xmn][y]
                                                && o.area.area[x][ymp] && o.area.area[x][ymn]) {
                                            out32[p] = inData.pixels32[p];
                                        }
                                    }
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                }
                            }
                        }
                    }
                    Tonga.loader().appendProgress(1. / set.list.size());
                });
            }
             */
            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast edgeDilate() {
        return new FilterFast("Dilated",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(SPINNER, "Radius (px)", 2),
                    new ControlReference(TOGGLE, "Circular dilation", 0)}, 3) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                IMG.copyPixels(in32, out32);
                set.findOuterEdges();
                set.findInnerEdges();
                int r = param.spinner[0];
                Tonga.iteration(2);
                set.list.forEach(o -> {
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
                    Tonga.loader().appendProgress(2. / set.list.size());
                });
            }

            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
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
                    id = Filters.gaussApprox().runSingle(id, param.spinner[0]);
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

    public static FilterFast evenIllumination() {
        return new FilterFast("Even", noParams) {

            @Override
            protected void processor() {
                ImageData id = inData;
                id = Filters.gamma().runSingle(id, 0.5);
                //id = Filters.illuminationCorrection().runSingle(id);
                id = Filters.autoscale().runSingle(id);
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

    public static FilterFast getEdgeMask() {
        return new FilterFast("Edges", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.imageEdgeFixer(false);
                set.findOuterEdges();
                set.findInnerEdges();
                setOutputBy(set.drawToImageData());
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast getExtendedMask() {
        return new FilterFast("Edges", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Radius (px)", 5)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.getExtendedMasks(param.spinner[0]);
                set.findOuterMaskEdges();
                setOutputBy(set.drawToImageData());
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast getRadiusOverlap() {
        return new FilterFast("Edges", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Radius (px)", 5)}) {

            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, param.color[0]).trace();
                set.getExtendedMasks(param.spinner[0]);
                int[] vals = set.drawMaskArray();
                Iterate.pixels(inData, (int p) -> {
                    outData.pixels32[p] = vals[p] == COL.GRAY ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }
}
