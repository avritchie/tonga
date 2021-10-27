package mainPackage.counters;

import mainPackage.utils.STAT;
import mainPackage.morphology.Cell;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;

public class SetCounters {

    public static SetCounter countCellsImage(CellSet set) {
        return new SetCounter("Count cells", new String[]{"Image", "Objects", "Avg.Size", "Avg.Rndnss", "<html><b>Cells</b></html>"}) {

            @Override
            protected void processor(Object[] row) {
                //CellSet set = getCellSet(traced, targetImage);
                row[1] = set.objectsCount();
                row[2] = set.avgCornerlessSize();
                row[3] = set.avgCornerlessRoundness();
                row[4] = set.totalCellCount();
            }
        };
    }

    public static SetCounter countCellsSingle(CellSet set) {
        return new SetCounter("Count cells", new String[]{"Image", "Object", "Area", "Corners", "<html><b>Cells</b></html>"}) {

            @Override
            protected void processor(Object[] row) {
                //CellSet set = getCellSet(traced, targetImage);
                String name = row[0].toString();
                for (int f = 0; f < set.objectsCount(); f++) {
                    Cell obj = (Cell) set.list.get(f);
                    row[1] = "#" + (f + 1);
                    row[2] = (Integer) obj.getSize();
                    row[3] = (Integer) obj.getCornerCount();
                    row[4] = (Integer) obj.getCellEstimate();
                    if (f < set.objectsCount() - 1) {
                        row = data.newRow(name);
                    }
                }

            }
        };
    }

    public static SetCounter countObjectsSingle(ROISet set) {
        return new SetCounter("Count objects", new String[]{"Image", "Object", "Area", "Width", "Height", "Rndnss %"}) {

            @Override
            protected void processor(Object[] row) {
                //ROISet set = getROISet(traced, targetImage);
                String name = row[0].toString();
                for (int f = 0; f < set.objectsCount(); f++) {
                    ROI obj = set.list.get(f);
                    row[1] = "#" + (f + 1);
                    row[2] = (Integer) obj.getSize();
                    row[3] = (Integer) obj.getWidth();
                    row[4] = (Integer) obj.getHeight();
                    row[5] = STAT.decToPerc(obj.getCircularity());
                    if (f < set.objectsCount() - 1) {
                        row = data.newRow(name);
                    }
                }

            }
        };
    }

    public static SetCounter countObjectsImage(ROISet set) {
        return new SetCounter("Count objects", new String[]{"Image", "Objects", "Avg.Size", "Tot.Size", "Std.Size", "Med.Size"}) {

            @Override
            protected void processor(Object[] row) {
                //ROISet set = getROISet(traced, targetImage);
                row[1] = set.objectsCount();
                row[2] = set.statsForTotalSize().getMean();
                row[3] = set.totalAreaSize();
                row[4] = set.statsForTotalSize().getStdDev();
                row[5] = set.statsForTotalSize().getMedian();
            }
        };
    }

    public static SetCounter countObjectStainsSingle(ROISet set) {
        return new SetCounter("Count staining", new String[]{"Image", "Object", "Area", "<html><b>Stain %</b></html>", "<html><b>Stain sum</b></html>"}) {

            @Override
            protected void processor(Object[] row) {
                //ROISet set = getROISet(traced, targetImage);
                String name = row[0].toString();
                for (int f = 0; f < set.objectsCount(); f++) {
                    ROI obj = set.list.get(f);
                    row[1] = "#" + (f + 1);
                    row[2] = obj.getStainSTAT().getN();
                    row[3] = STAT.decToPerc(obj.getStainAvg());
                    row[4] = obj.getStain();
                    if (f < set.objectsCount() - 1) {
                        row = data.newRow(name);
                    }
                }

            }
        };
    }

    public static SetCounter countObjectStainsBGSingle(ROISet set, double bg) {
        return new SetCounter("Count staining", new String[]{"Image", "Object", "Area",
            "Stain %", "<html><b>Stain % w/o background</b></html>", "Stain sum", "Stain sum w/o background"}) {

            @Override
            protected void processor(Object[] row) {
                //int dup = (int) (set.list.get(0).getRawStain() / set.list.get(0).getStain());
                //ROISet set = getROISet(traced, targetImage);
                String name = row[0].toString();
                for (int f = 0; f < set.objectsCount(); f++) {
                    ROI obj = set.list.get(f);
                    row[1] = "#" + (f + 1);
                    row[2] = obj.getStainSTAT().getN();
                    row[3] = STAT.decToPerc(obj.getStainAvg());
                    row[4] = STAT.decToPerc((obj.getStain() - (obj.getSize() * bg)) / obj.getSize());
                    row[5] = obj.getStain();
                    row[6] = obj.getStain() - (obj.getSize() * bg);
                    if (f < set.objectsCount() - 1) {
                        row = data.newRow(name);
                    }
                }

            }
        };
    }

    public static SetCounter countObjectStainsImage(ROISet set) {
        return new SetCounter("Count staining", new String[]{"Image", "Objects", "Avg.Stain", "Std.Stain", "Med.Stain"}) {

            @Override
            protected void processor(Object[] row) {
                STAT stain = set.statsForStain();
                row[1] = set.objectsCount();
                row[2] = stain.getMean();
                row[3] = stain.getStdDev();
                row[4] = stain.getMedian();
            }
        };
    }

    public static SetCounter countObjectStainsBGImage(ROISet set, double bg) {
        return new SetCounter("Count staining", new String[]{"Image", "Objects", "Background", "Avg.Stain w/o background", "Std.Stain", "Med.Stain w/o background"}) {

            @Override
            protected void processor(Object[] row) {
                STAT stain = set.statsForStain();
                row[1] = set.objectsCount();
                row[2] = bg;
                row[3] = stain.getMean() - bg;
                row[4] = stain.getStdDev();
                row[5] = stain.getMedian() - bg;
            }
        };
    }

    public static SetCounter countObjectPositive(ROISet set, double d) {
        return new SetCounter("Count positivity", new String[]{"Image", "Cells", "Positive", "<html><b>Ratio %</b></html>"}) {

            @Override
            protected void processor(Object[] row) {
                row[1] = set.objectsCount();
                row[2] = set.objectsCountStainPositive(d);
                row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
            }
        };
    }

    public static SetCounter countObjectPositiveBG(ROISet set, double bg, double d) {
        return new SetCounter("Count positivity", new String[]{"Image", "Cells", "Positive", "<html><b>Ratio %</b></html>"}) {

            @Override
            protected void processor(Object[] row) {
                row[1] = set.objectsCount();
                row[2] = set.objectsCountStainPositive((d - bg) * (1 / (1 - bg)));
                row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
            }
        };
    }
}
