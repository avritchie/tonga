package mainPackage.utils;

import static java.lang.Double.NaN;
import java.util.Arrays;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;

public class STAT {

    double[] dataDouble;
    int[] dataInt;
    int size;

    public Double mean;
    public Double median;
    public Double variance;

    public STAT(double[] data) {
        this.dataDouble = data;
        this.dataInt = null;
        size = data.length;
    }

    public STAT(int[] data) {
        this.dataInt = data;
        this.dataDouble = null;
        size = data.length;
    }

    public int getN() {
        return size;
    }

    public double getMax() {
        if (dataDouble == null) {
            return Arrays.stream(dataInt).max().getAsInt();
        } else {
            return Arrays.stream(dataDouble).max().getAsDouble();
        }
    }

    public double getMin() {
        if (dataDouble == null) {
            return Arrays.stream(dataInt).min().getAsInt();
        } else {
            return Arrays.stream(dataDouble).min().getAsDouble();
        }
    }

    public double getMMRatio() {
        //high if a lot of very small objects and yet also very large objects
        double dec = 0;
        if (dataDouble == null) {
            Arrays.sort(dataInt);
            for (int i = 0; i < size / 2; i++) {
                dec += dataInt[size - 1 - i] / (double) dataInt[i];
            }
        } else {
            Arrays.sort(dataDouble);
            for (int i = 0; i < size / 2; i++) {
                dec += dataDouble[size - 1 - i] / dataDouble[i];
            }
        }
        return dec / (size / 2);
    }

    public double getMean() {
        if (mean == null) {
            if (dataDouble == null) {
                mean = Arrays.stream(dataInt).sum() / (double) size;
            } else {
                mean = Arrays.stream(dataDouble).sum() / (double) size;
            }
        }
        return mean;
    }

    public double getFilteredMean(Object filter) {
        if (mean == null) {
            if (dataDouble == null) {
                long fs = Arrays.stream(dataInt).filter((IntPredicate) filter).count();
                mean = Arrays.stream(dataInt).filter((IntPredicate) filter).sum() / ((double) fs);
            } else {
                long fs = Arrays.stream(dataDouble).filter((DoublePredicate) filter).count();
                mean = Arrays.stream(dataDouble).filter((DoublePredicate) filter).sum() / ((double) fs);
            }
        }
        return mean;
    }

    public double getVariance() {
        if (variance == null) {
            double mean = getMean();
            double val = 0;
            double temp = 0;
            if (dataDouble == null) {
                for (int a : dataInt) {
                    val = a - mean;
                    temp += val * val;
                }
            } else {
                for (double a : dataDouble) {
                    val = a - mean;
                    temp += val * val;
                }
            }
            variance = temp / (size - 1);
        }
        return variance;
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public static double decToPerc(double decimal) {
        //decimal to percentage with 3 decimals
        //0.2748827 -> 27.488
        return Math.round(decimal * 100000) / 1000.;
    }

    public static double decToProm(double decimal) {
        //decimal to promille with 4 decimals
        //0.274882795184 -> 274.8828
        return Math.round(decimal * 10000000) / 10000.;
    }

    public double getMedian() {
        if (median == null) {
            if (dataDouble == null) {
                try {
                    Arrays.sort(dataInt);
                    if (dataInt.length % 2 == 0) {
                        median = (dataInt[(dataInt.length / 2) - 1] + dataInt[dataInt.length / 2]) / 2.0;
                    }
                    median = (double) dataInt[dataInt.length / 2];
                } catch (ArrayIndexOutOfBoundsException ex) {
                    median = NaN;
                }
            } else {
                try {
                    Arrays.sort(dataDouble);
                    if (dataDouble.length % 2 == 0) {
                        median = (dataDouble[(dataDouble.length / 2) - 1] + dataDouble[dataDouble.length / 2]) / 2.0;
                    }
                    median = dataDouble[dataDouble.length / 2];
                } catch (ArrayIndexOutOfBoundsException ex) {
                    median = NaN;
                }
            }
        }
        return median;
    }
}
