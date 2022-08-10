package mainPackage.filters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mainPackage.Iterate;
import mainPackage.PanelCreator;
import static mainPackage.PanelCreator.ControlType.*;
import static mainPackage.filters.Filter.bgcol;
import mainPackage.filters.FilterSet.OutputType;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class FiltersSet {

    /* only the filters in this class support the runSet command for returning ROISet 
     any FilterSet placed in FiltersPass only supports runSingle */
    public static FilterSet fillInnerAreas() {
        return new FilterSet("Area Filler", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(TOGGLE, "Fix broken objects on image edges", 1)}, OutputType.PLAINSET) {

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    inSet.imageEdgeFixer(false);
                }
                inSet.fillInnerHoles();
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet fillInnerAreasSizeShape() {
        return new FilterSet("Area Filler", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(SPINNER, "Remove smaller than (pixels)", 100),
            new PanelCreator.ControlReference(SLIDER, "Or rounder than (%)", 90),
            new PanelCreator.ControlReference(TOGGLE, "Size-dependent adjustments", 0, new int[]{4, 1}),
            new PanelCreator.ControlReference(SPINNER, "Target size (pixels)", 100),
            new PanelCreator.ControlReference(TOGGLE, "Less strict on the edges", 1)}, OutputType.PLAINSET) {

            ROISet innerSet;
            List<ROI> origList;

            @Override
            protected void processorSet() {
                origList = new ArrayList<>();
                innerSet = FiltersSet.innerAreas().runSet(inSet, inData, param.colorARGB[0], true);
                origList.addAll(innerSet.list);
                innerSet.filterOutLargeAndNonRound(param.spinner[0], param.slider[0] / 100.);
                if (param.toggle[1]) {
                    innerSet.filterOutLargeEdgeTouchers(param.spinner[0] / 2);
                }
                if (param.toggle[0]) {
                    Iterator<ROI> it = origList.iterator();
                    while (it.hasNext()) {
                        ROI r = it.next();
                        double ratio = r.parent.getSize() / (double) r.getSize() / (r.parent.getSize() / (double) param.spinner[1]);
                        if (ratio < 0.2 && innerSet.list.contains(r)) {
                            innerSet.list.remove(r);
                        }
                    }
                }
                innerSet.parentalMerge();
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet innerAreas() {
        return new FilterSet("Inner Areas", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(TOGGLE, "Include areas on image edges", 1)}, OutputType.PLAINSET) {

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    inSet.annotateCopy();
                    inSet.imageEdgeFixer(false);
                }
                inSet.findOuterEdges();
                inSet.findInnerEdges();
                if (param.toggle[0]) {
                    inSet.copyAnnotate();
                }
                inSet = new ImageTracer(inData, param.colorARGB[0], true).traceInnerObjects(inSet);
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet filterObjectSize() {
        return new FilterSet("Size Filter",
                new PanelCreator.ControlReference[]{
                    new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
                    new PanelCreator.ControlReference(SPINNER, "Minimum size", 100),
                    new PanelCreator.ControlReference(TOGGLE, "Also do for the background", 0, new int[]{3, 1}),
                    new PanelCreator.ControlReference(SPINNER, "Background minimum size", 100)}, OutputType.PLAINSET) {

            ROISet innerSet, origSet;

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    origSet = inSet.copy();
                }
                inSet.filterOutSmallObjects((int) param.spinner[0]);
                if (param.toggle[0]) {
                    origSet.list.removeAll(inSet.list);
                    origSet.drawAppend(inData, param.colorARGB[0]);
                    innerSet = FiltersSet.innerAreas().runSet(inSet, inData, param.colorARGB[0], true);
                    innerSet.filterOutLargeObjects((int) (param.spinner[1]));
                    innerSet.parentalMerge();
                } else {
                    setOutsetBy(inSet);
                }
            }
        };
    }

    public static FilterSet filterObjectDimension() {
        return new FilterSet("Dimension Filter",
                new PanelCreator.ControlReference[]{
                    new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
                    new PanelCreator.ControlReference(SPINNER, "Minimum width or height", 10),
                    new PanelCreator.ControlReference(TOGGLE, "Also do for the background", 0, new int[]{3, 1}),
                    new PanelCreator.ControlReference(SPINNER, "Background minimum width or height", 10)}, OutputType.PLAINSET) {

            ROISet innerSet, origSet;

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    origSet = inSet.copy();
                }
                inSet.filterOutByMinDimension((int) param.spinner[0]);
                if (param.toggle[0]) {
                    origSet.list.removeAll(inSet.list);
                    origSet.drawAppend(inData, param.colorARGB[0]);
                    innerSet = FiltersSet.innerAreas().runSet(inSet, inData, param.colorARGB[0], true);
                    innerSet.filterOutByMaxDimension((int) (param.spinner[1]));
                    innerSet.parentalMerge();
                }
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet filterObjectSizeDimension() {
        return new FilterSet("Size/dimension Filter",
                new PanelCreator.ControlReference[]{
                    new PanelCreator.ControlReference(SPINNER, "Minimum size", 100),
                    new PanelCreator.ControlReference(SPINNER, "Minimum width or height", 10),
                    new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
                    new PanelCreator.ControlReference(TOGGLE, "Also do for the background", 0, new int[]{4, 1, 5, 1}),
                    new PanelCreator.ControlReference(SPINNER, "Background minimum size", 100),
                    new PanelCreator.ControlReference(SPINNER, "Background minimum width or height", 10)}, OutputType.PLAINSET) {

            ROISet sizeSet, dimSet, innerSet, origSet;

            @Override
            protected void processorSet() {
                if (param.toggle[0]) {
                    origSet = inSet.copy();
                }
                inSet.filterOutSmallObjects((int) param.spinner[0]);
                inSet.filterOutByMinDimension((int) param.spinner[1]);
                if (param.toggle[0]) {
                    origSet.list.removeAll(inSet.list);
                    origSet.drawAppend(inData, param.colorARGB[0]);
                    innerSet = FiltersSet.innerAreas().runSet(inSet, inData, param.colorARGB[0], true);
                    sizeSet = innerSet.copy();
                    dimSet = innerSet.copy();
                    sizeSet.filterOutLargeObjects((int) (param.spinner[2]));
                    dimSet.filterOutByMaxDimension((int) (param.spinner[3]));
                    Iterator<ROI> it = innerSet.list.iterator();
                    while (it.hasNext()) {
                        ROI r = it.next();
                        if (!sizeSet.list.contains(r) && !dimSet.list.contains(r)) {
                            it.remove();
                        }
                    }
                    innerSet.parentalMerge();
                }
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet filterObjectSizeShape() {
        return new FilterSet("Size/shape filter", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(SPINNER, "Remove smaller than (pixels)", 100),
            new PanelCreator.ControlReference(SLIDER, "If also rounder than (%)", 90)}, OutputType.PLAINSET) {

            @Override
            protected void processorSet() {
                inSet.filterOutSmallAndRound(param.spinner[0], param.slider[0] / 100.);
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet filterEdgeTouchers() {
        return new FilterSet("Edge toucher filter", bgcol, OutputType.PLAINSET) {

            @Override
            protected void processorSet() {
                inSet.removeEdgeTouchers();
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet getEdgeMask() {
        return new FilterSet("Edges", bgcol, OutputType.EFFECTSET) {

            @Override
            protected void processorSet() {
                inSet.imageEdgeFixer(false);
                inSet.findOuterEdges();
                inSet.findInnerEdges();
                setOutsetBy(inSet);
            }
        };
    }

    public static FilterSet getExtendedMask() {
        return new FilterSet("Edges", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(SPINNER, "Radius (px)", 5),
            new PanelCreator.ControlReference(TOGGLE, "Render overlaps", 0)}, OutputType.OUTDATA) {

            @Override
            protected void processorSet() {
                inSet.getExtendedMasks(param.spinner[0]);
                inSet.findOuterMaskEdges();
                setOutsetBy(inSet);
            }

            @Override
            protected void processor() {
                inSet = new ImageTracer(inData, param.colorARGB[0]).trace();
                processorSet();
                if (param.toggle[0]) {
                    setOutputBy(inSet.drawMaskArray());
                } else {
                    setOutputBy(inSet.drawToImageData());
                }
            }
        };
    }

    public static FilterSet getRadiusOverlap() {
        return new FilterSet("Edges", new PanelCreator.ControlReference[]{
            new PanelCreator.ControlReference(COLOUR, "Background colour", -2),
            new PanelCreator.ControlReference(SPINNER, "Radius (px)", 5)}, OutputType.OUTDATA) {

            @Override
            protected void processorSet() {
                inSet.getExtendedMasks(param.spinner[0]);
                setOutsetBy(inSet);
                int[] vals = inSet.drawMaskArray();
                Iterate.pixels(outData, (int p) -> {
                    outData.pixels32[p] = vals[p] == COL.GRAY ? COL.WHITE : COL.BLACK;
                });
            }
        };
    }
}
