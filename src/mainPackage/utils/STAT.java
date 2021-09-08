package mainPackage.utils;

import static java.lang.Double.NaN;
import java.util.Arrays;

public class STAT {

    double[] dataDouble;
    int[] dataInt;
    int size;

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
        double ret = 0;
        double fact = 1. / size;
        if (dataDouble == null) {
            for (int a : dataInt) {
                ret += a * fact;
            }
        } else {
            for (double a : dataDouble) {
                ret += a * fact;
            }
        }
        return ret;
    }

    public double getVariance() {
        double mean = getMean();
        double temp = 0;
        if (dataDouble == null) {
            for (int a : dataInt) {
                temp += (a - mean) * (a - mean);
            }
        } else {
            for (double a : dataDouble) {
                temp += (a - mean) * (a - mean);
            }
        }
        return temp / (size - 1);
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
        if (dataDouble == null) {
            try {
                Arrays.sort(dataInt);
                if (dataInt.length % 2 == 0) {
                    return (dataInt[(dataInt.length / 2) - 1] + dataInt[dataInt.length / 2]) / 2.0;
                }
                return dataInt[dataInt.length / 2];
            } catch (ArrayIndexOutOfBoundsException ex) {
                return NaN;
            }
        } else {
            try {
                Arrays.sort(dataDouble);
                if (dataDouble.length % 2 == 0) {
                    return (dataDouble[(dataDouble.length / 2) - 1] + dataDouble[dataDouble.length / 2]) / 2.0;
                }
                return dataDouble[dataDouble.length / 2];
            } catch (ArrayIndexOutOfBoundsException ex) {
                return NaN;
            }
        }
    }
}
