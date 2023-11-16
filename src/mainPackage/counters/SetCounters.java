package mainPackage.counters;

import mainPackage.utils.STAT;
import mainPackage.morphology.Cell;
import mainPackage.morphology.CellSet;
import mainPackage.morphology.ROI;
import mainPackage.morphology.ROISet;

public class SetCounters {

    public static SetCounter countCellsImage(CellSet set) {
        return new SetCounter("Count cells",
                new String[]{"Image", "Objects", "Avg.Size", "Avg.Rndnss", "<html><b>Cells</b></html>"},
                new String[]{"The name of the image",
                    "The total number of objects in the image",
                    "The average size of the objects in the image in pixels",
                    "The average estimated roundness of the objects in the image",
                    "The estimated number of cells in the image"}) {

            @Override
            protected void processor() {
                //CellSet set = getCellSet(traced, targetImage);
                row[1] = set.objectsCount();
                row[2] = set.avgCornerlessSize();
                row[3] = set.avgCornerlessRoundness();
                row[4] = set.totalCellCount();
            }
        };
    }

    public static SetCounter countCellsSingle(CellSet set) {
        return new SetCounter("Count cells",
                new String[]{"Image", "Object", "Area", "Corners", "<html><b>Cells</b></html>"},
                new String[]{"The name of the image",
                    "The unique id number of the object in the image",
                    "The total area size of the objects in pixels",
                    "The number of concave points in the object",
                    "The estimated number of cells in the object"}) {

            @Override
            protected void processor() {
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
        return new SetCounter("Count objects", new String[]{"Image", "Object", "X", "Y",
            "Area %unit2", "Width %unit", "Height %unit", "Rndnss %"},
                new String[]{"The name of the image",
                    "The unique id number of nucleus in the image",
                    "The X-coordinate pixel of the centroid of this nucleus in the image",
                    "The Y-coordinate pixel of the centroid of this nucleus in the image",
                    "The area size of this nucleus in %unit2",
                    "The width of this nucleus in %unit",
                    "The height of this nucleus in %unit",
                    "An estimation of the roundness of the mask of this nucleus"}) {

            @Override
            protected void processor() {
                //ROISet set = getROISet(traced, targetImage);
                String name = row[0].toString();
                for (int f = 0; f < set.objectsCount(); f++) {
                    ROI obj = set.list.get(f);
                    row[1] = "#" + (f + 1);
                    int[] cent = obj.getCentroid();
                    row[2] = (Integer) cent[0];
                    row[3] = (Integer) cent[1];
                    row[4] = scaleUnit(obj.getSize(), 2);
                    row[5] = scaleUnit(obj.getWidth(), 1);
                    row[6] = scaleUnit(obj.getHeight(), 1);
                    row[7] = STAT.decToPerc(obj.getCircularity());
                    if (f < set.objectsCount() - 1) {
                        row = data.newRow(name);
                    }
                }

            }
        };
    }

    public static SetCounter countObjectsImage(ROISet set) {
        return new SetCounter("Count objects", new String[]{"Image", "Objects",
            "Avg.Size %unit2", "Med.Size %unit2", "Tot.Size %unit2", "Std.Size %unit2"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The average size of the nuclei in the image in %unit2",
                    "The total sum of the sizes of all the nuclei in the image in %unit2",
                    "The standard deviation of the nuclear size in the image in %unit2",
                    "The median nuclear size of the nuclei in the image in %unit2"}) {

            @Override
            protected void processor() {
                //ROISet set = getROISet(traced, targetImage);
                row[1] = set.objectsCount();
                row[2] = scaleUnit(set.statsForTotalSize().getMean(), 2);
                row[3] = scaleUnit(set.statsForTotalSize().getMedian(), 2);
                row[4] = scaleUnit(set.totalAreaSize(), 2);
                row[5] = scaleUnit(set.statsForTotalSize().getStdDev(), 2);
            }
        };
    }

    public static SetCounter countObjectStainsSingle(ROISet set) {
        return new SetCounter("Count staining", new String[]{"Image", "Object", "X", "Y",
            "Area %unit2", "<html><b>Stain %</b></html>", "<html><b>Stain sum</b></html>"},
                new String[]{"The name of the image",
                    "The unique id number of nucleus in the image",
                    "The X-coordinate pixel of the centroid of this nucleus in the image",
                    "The Y-coordinate pixel of the centroid of this nucleus in the image",
                    "The area size of this nucleus in %unit2",
                    "The average relative intensity of this nucleus",
                    "The total relative intensity of this nucleus"}) {

            @Override
            protected void processor() {
                //ROISet set = getROISet(traced, targetImage);
                Rows(set.objectsCount(), (int index) -> {
                    ROI obj = set.list.get(index);
                    row[1] = "#" + (index + 1);
                    int[] cent = obj.getCentroid();
                    row[2] = (Integer) cent[0];
                    row[3] = (Integer) cent[1];
                    row[4] = scaleUnit(obj.getStainSTAT().getN(), 2);
                    row[5] = STAT.decToPerc(obj.getStainAvg());
                    row[6] = (Double) obj.getStainSum();
                });
            }
        };
    }

    public static SetCounter countObjectStainsBGSingle(ROISet set, double bg) {
        return new SetCounter("Count staining", new String[]{"Image", "Object", "X", "Y", "Area %unit2",
            "Stain %", "<html><b>Stain % w/o background</b></html>", "Stain sum", "<html><b>Stain sum w/o background</b></html>"},
                new String[]{"The name of the image",
                    "The unique id number of nucleus in the image",
                    "The X-coordinate pixel of the centroid of this nucleus in the image",
                    "The Y-coordinate pixel of the centroid of this nucleus in the image",
                    "The area size of this nucleus in %unit2",
                    "The average relative intensity of this nucleus",
                    "The average relative intensity of this nucleus with the average background intensity subtracted",
                    "The total relative intensity of this nucleus",
                    "The total relative intensity of this nucleus with the average background intensity subtracted for each pixel"}) {

            @Override
            protected void processor() {
                //int dup = (int) (set.list.get(0).getRawStain() / set.list.get(0).getStain());
                //ROISet set = getROISet(traced, targetImage);
                Rows(set.objectsCount(), (int index) -> {
                    ROI obj = set.list.get(index);
                    row[1] = "#" + (index + 1);
                    int[] cent = obj.getCentroid();
                    row[2] = (Integer) cent[0];
                    row[3] = (Integer) cent[1];
                    row[4] = scaleUnit(obj.getStainSTAT().getN(), 2);
                    row[5] = STAT.decToPerc(obj.getStainAvg());
                    //row[6] = STAT.decToPerc((obj.getStainSum() - (obj.getSize() * bg)) / obj.getSize());
                    row[6] = STAT.decToPerc(obj.getStainAvg(bg));
                    row[7] = obj.getStainSum();
                    //row[7] = obj.getStainSum() - (obj.getSize() * bg);
                    row[8] = obj.getStainSum(bg);
                });
            }
        };
    }

    public static SetCounter countObjectStainsImage(ROISet set) {
        return new SetCounter("Count staining", new String[]{"Image", "Objects",
            "<html><b>Avg.Stain %</b></html>", "Std.Stain %", "Med.Stain %",
            "<html><b>Avg.Stain sum</b></html>", "Std.Stain sum", "Med.Stain sum"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The average relative intensity from all the nuclei in the image",
                    "The standard deviation of the relative intensity measurement between the nuclei",
                    "The median relative intensity from all the nuclei in the image",
                    "The average relative intensity sum from all the nuclei in the image",
                    "The standard deviation of the intensity sum measurement between the nuclei",
                    "The median relative intensity sum from all the nuclei in the image"}) {

            @Override
            protected void processor() {
                STAT stain = set.statsForStainAvg();
                row[1] = set.objectsCount();
                row[2] = STAT.decToPerc(stain.getMean());
                row[3] = STAT.decToPerc(stain.getStdDev());
                row[4] = STAT.decToPerc(stain.getMedian());
                stain = set.statsForStainSum();
                row[5] = stain.getMean();
                row[6] = stain.getStdDev();
                row[7] = stain.getMedian();
            }
        };
    }

    public static SetCounter countObjectStainsBGImage(ROISet set, double bg) {
        return new SetCounter("Count staining", new String[]{"Image", "Objects", "Background %",
            "<html><b>Avg.Stain % w/o background</b></html>", "Std.Stain %", "Med.Stain % w/o background",
            "<html><b>Avg.Stain sum w/o background</b></html>", "Std.Stain sum", "Med.Stain sum w/o background"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The average relative background intensity in the image",
                    "The average relative intensity from all the nuclei in the image with the average background intensity subtracted",
                    "The standard deviation of the relative intensity measurement between the nuclei (with background subtracted)",
                    "The median relative intensity from all the nuclei in the image with the average background intensity subtracted",
                    "The average relative intensity sum from all the nuclei in the image with the average background intensity subtracted",
                    "The standard deviation of the relative intensity sum measurement between the nuclei (with background subtracted)",
                    "The median relative intensity sum from all the nuclei in the image with the average background intensity subtracted"}) {

            @Override
            protected void processor() {
                STAT stain = set.statsForStainAvg(bg);
                row[1] = set.objectsCount();
                row[2] = STAT.decToPerc(bg);
                row[3] = STAT.decToPerc(stain.getMean());
                row[4] = STAT.decToPerc(stain.getStdDev());
                row[5] = STAT.decToPerc(stain.getMedian());
                stain = set.statsForStainSum(bg);
                row[6] = stain.getMean();
                row[7] = stain.getStdDev();
                row[8] = stain.getMedian();
            }
        };
    }

    public static SetCounter countObjectsStainClassSingle(ROISet set, String[] classNames) {
        return new SetCounter("Count staining", new String[]{"Image", "Object", "Class", "X", "Y",
            "Area %unit2", "Value", "<html><b>Stain %</b></html>", "<html><b>Stain sum</b></html>"},
                new String[]{"The name of the image",
                    "The unique id number of nucleus in the image",
                    "The classified class of the nucleus",
                    "The X-coordinate pixel of the centroid of this nucleus in the image",
                    "The Y-coordinate pixel of the centroid of this nucleus in the image",
                    "The area size of this nucleus in %unit2",
                    "The value of this nucleus used for classifying",
                    "The average relative intensity of this nucleus",
                    "The total relative intensity of this nucleus"}) {

            @Override
            protected void processor() {
                //ROISet set = getROISet(traced, targetImage);
                Rows(set.objectsCount(), (int index) -> {
                    ROI obj = set.list.get(index);
                    row[1] = "#" + (index + 1);
                    row[2] = classNames[obj.getClassID() - 1];
                    int[] cent = obj.getCentroid();
                    row[3] = (Integer) cent[0];
                    row[4] = (Integer) cent[1];
                    row[5] = scaleUnit(obj.getStainSTAT().getN(), 2);
                    row[6] = obj.getClassifierValue();
                    row[7] = STAT.decToPerc(obj.getStainAvg());
                    row[8] = obj.getStainSum();
                });
            }
        };
    }

    public static SetCounter countObjectsStainClassImage(ROISet set, String[] classNames) {
        return new SetCounter("Count staining", new String[]{"Image", "Class", "Objects",
            "<html><b>Avg.Stain %</b></html>", "Std.Stain %", "Med.Stain %",
            "<html><b>Avg.Stain sum</b></html>", "Std.Stain sum", "Med.Stain sum"},
                new String[]{"The name of the image",
                    "The classified class of the nuclei",
                    "The total number of recognized nuclei in the image",
                    "The average relative intensity from all the nuclei in the image",
                    "The standard deviation of the relative intensity measurement between the nuclei",
                    "The median relative intensity from all the nuclei in the image",
                    "The average relative intensity sum from all the nuclei in the image",
                    "The standard deviation of the intensity sum measurement between the nuclei",
                    "The median relative intensity sum from all the nuclei in the image"}) {

            @Override
            protected void processor() {
                Rows(set.classCount(), (int index) -> {
                    ROISet nset = set.getOnlyClass(index + 1);
                    STAT stain = nset.statsForStainAvg();
                    row[1] = classNames[index];
                    row[2] = nset.objectsCount();
                    row[3] = STAT.decToPerc(stain.getMean());
                    row[4] = STAT.decToPerc(stain.getStdDev());
                    row[5] = STAT.decToPerc(stain.getMedian());
                    stain = nset.statsForStainSum();
                    row[6] = stain.getMean();
                    row[7] = stain.getStdDev();
                    row[8] = stain.getMedian();
                });
            }
        };
    }

    public static SetCounter countObjectPositive(ROISet set, double d) {
        return new SetCounter("Count positivity", new String[]{"Image", "Objects", "Positive", "<html><b>Ratio %</b></html>"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The number of nuclei classified as positive",
                    "The ratio of positive nuclei out of all detected nuclei"}) {

            @Override
            protected void processor() {
                row[1] = set.objectsCount();
                row[2] = set.objectsCountStainPositive(d);
                row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
            }
        };
    }

    public static SetCounter countObjectPositiveSum(ROISet set, double d) {
        return new SetCounter("Count positivity", new String[]{"Image", "Objects", "Positive", "<html><b>Ratio %</b></html>"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The number of nuclei classified as positive",
                    "The ratio of positive nuclei out of all detected nuclei"}) {

            @Override
            protected void processor() {
                row[1] = set.objectsCount();
                row[2] = set.objectsCountStainSumPositive(d);
                row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
            }
        };
    }

    public static SetCounter countObjectPositiveBG(ROISet set, double bg, double d) {
        return new SetCounter("Count positivity", new String[]{"Image", "Objects", "Positive", "<html><b>Ratio %</b></html>"},
                new String[]{"The name of the image",
                    "The total number of recognized nuclei in the image",
                    "The number of nuclei classified as positive",
                    "The ratio of positive nuclei out of all detected nuclei"}) {

            @Override
            protected void processor() {
                row[1] = set.objectsCount();
                //row[2] = set.objectsCountStainPositive((d - bg) * (1 / (1 - bg)));
                //the threshold re-calculated to adjust for the range when bg removed
                //with 0.5 intensity, 0.1 background, and 50% threshold the range is
                //between 0.1 and 1.0, and thus the 50% threshold will be adjusted to halfway
                //i.e. it will be changed to 55% (>0.55 is positive)
                row[2] = set.objectsCountStainPositive((1 - bg) * d + bg);
                row[3] = STAT.decToPerc(((Integer) row[2]) / (double) ((Integer) row[1]));
            }
        };
    }
}
