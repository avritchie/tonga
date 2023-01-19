package mainPackage.filters;

public class Integral {

    private static int[] integralBounds(int r, int pos, int width, int height) {
        int rn = r + 1;
        int x = pos % width;
        int y = pos / width;
        int maxPos = pos + r + r * width;
        int minPos = pos - rn - rn * width;
        int hrzPos = pos + r - rn * width;
        int vrtPos = pos - rn + r * width;
        int hrzPxls = r * 2 + 1, vrtPxls = hrzPxls;
        //adjust indexes and recalculate window size on bounds
        if (x + r >= width) {
            int xShift = x + r - width + 1;
            maxPos -= xShift;
            hrzPos -= xShift;
            hrzPxls -= xShift;
        }
        if (y + r >= height) {
            int yShift = y + r - height + 1;
            maxPos -= yShift * width;
            vrtPos -= yShift * width;
            vrtPxls -= yShift;
        }
        if (x - r < 0) {
            int xShift = -(x - r);
            minPos += xShift;
            vrtPos += xShift;
            hrzPxls -= xShift;
        }
        if (y - r < 0) {
            int yShift = -(y - r);
            minPos += yShift * width;
            hrzPos += yShift * width;
            vrtPxls -= yShift;
        }
        return new int[]{maxPos, minPos, hrzPos, vrtPos, hrzPxls, vrtPxls};
    }

    protected static double integralMean(int[] integral, int r, int pos, int width, int height) {
        int x = pos % width, y = pos / width;
        int[] bounds = integralBounds(r, pos, width, height);
        int maxPos = bounds[0], minPos = bounds[1];
        int hrzPos = bounds[2], vrtPos = bounds[3];
        int hrzPxls = bounds[4], vrtPxls = bounds[5];
        //compute local mean using the integral
        double pxls = (hrzPxls * vrtPxls);
        return -(integral[maxPos]
                + (x - r > 0 && y - r > 0 ? integral[minPos] : 0)
                - (y - r > 0 ? integral[hrzPos] : 0)
                - (x - r > 0 ? integral[vrtPos] : 0)) / pxls;
    }

    protected static int integralSum(int[] integral, int r, int pos, int width, int height) {
        int x = pos % width, y = pos / width;
        int[] bounds = integralBounds(r, pos, width, height);
        int maxPos = bounds[0], minPos = bounds[1];
        int hrzPos = bounds[2], vrtPos = bounds[3];
        //compute local mean using the integral
        return -(integral[maxPos]
                + (x - r > 0 && y - r > 0 ? integral[minPos] : 0)
                - (y - r > 0 ? integral[hrzPos] : 0)
                - (x - r > 0 ? integral[vrtPos] : 0));
    }

    //long duplicate of the above
    protected static double integralMean(long[] integral, int r, int pos, int width, int height) {
        int x = pos % width, y = pos / width;
        int[] bounds = integralBounds(r, pos, width, height);
        int maxPos = bounds[0], minPos = bounds[1];
        int hrzPos = bounds[2], vrtPos = bounds[3];
        int hrzPxls = bounds[4], vrtPxls = bounds[5];
        //compute local mean using the integral
        double pxls = (hrzPxls * vrtPxls);
        return -(integral[maxPos]
                + (x - r > 0 && y - r > 0 ? integral[minPos] : 0)
                - (y - r > 0 ? integral[hrzPos] : 0)
                - (x - r > 0 ? integral[vrtPos] : 0)) / pxls;
    }

}
