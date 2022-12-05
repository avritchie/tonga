package mainPackage.utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import mainPackage.Tonga;
import mainPackage.TongaLayer;

public class HISTO {

    public static int[] getHistogram(BufferedImage imgOriginal) {
        BufferedImage img = new BufferedImage(imgOriginal.getWidth(), imgOriginal.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        img.getGraphics().drawImage(imgOriginal, 0, 0, null);
        int[] hist = new int[256];
        byte[] px = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        int alphaLength = (img.getAlphaRaster() != null) ? 1 : 0;
        int pixels = img.getHeight() * img.getWidth() * (3 + alphaLength);
        for (int i = alphaLength; i < pixels; i += alphaLength) {
            hist[Math.max(Math.max(px[i++] & 0xFF, px[i++] & 0xFF), px[i++] & 0xFF)]++;
        }
        return hist;
    }

    public static int[] getHistogram(Image layer) {
        return getHistogram(SwingFXUtils.fromFXImage(layer, null));
    }

    public static int[] getHistogram(TongaLayer layer) {
        return getHistogram(layer.layerImage);
    }

    public static double getAreaCoverage(int[] histo, int start, int end) {
        // anna histodata ja väli 0-255, ylä ja alaraja
        // antaa prosentin: kuinka suuri osa histogrammista on tällä välillä
        // esim. välillä 0-127 jos kuva on valkoinen ja vain pari mustaa täplää, tulee pieni luku
        // välillä 127-255 tulisi vastaavasti todella korkea luku
        int total = 0, area = 0;
        for (int i = 0; i < 256; i++) {
            total += histo[i];
            if (i > start && i < end) {
                area += histo[i];
            }
        }
        return (double) area / total;
    }

    public static int[] getHistogram(int[] px) {
        // input image as ARGB INT pixel values
        int[] hist = new int[256];
        for (int i = 0; i < px.length; i++) {
            hist[Math.max(Math.max(px[i] >> 16 & 0xFF, px[i] >> 8 & 0xFF), px[i] & 0xFF)]++;
        }
        return hist;
    }

    public static int[] getHistogram(short[] px) {
        // input image as GRAYSCALE SHORT pixel values
        int[] hist = new int[65536];
        for (int i = 0; i < px.length; i++) {
            hist[px[i] & 0xFFFF]++;
        }
        return hist;
    }

    public static int[] getHistogramBWXbit(int[] px, int maxval) {
        // input image as ARGB INT pixel values
        int[] hist = new int[maxval];
        for (int i = 0; i < px.length; i++) {
            hist[px[i] / 2]++;
        }
        return hist;
    }

    public static int[] getScaledHistogram(double[] vA, double min, double max) {
        // input grid with any values and the min/max of the grid
        // will output 0-255 histogram so that min value becomes 0, max becomes 255 (=scaled)
        int[] hist = new int[256];
        int pixels = vA.length;
        double factor = (max - min) / 255.;
        for (int i = 0; i < pixels; i++) {
            double bV = vA[i] - min;
            hist[(int) (bV / factor)]++;
        }
        return hist;
    }

    public static int getHighestPointIndex(int[] histoData, boolean ignoreFirst) {
        int limit = histoData.length;
        // input any grid with any number of points
        int max = Integer.MIN_VALUE;
        int index = -1;
        for (int i = (ignoreFirst ? 1 : 0); i < (ignoreFirst ? limit - 1 : limit); i++) {
            if (histoData[i] > max) {
                max = histoData[i];
                index = i;
            }
        }
        Tonga.log.debug("Histo peak: {}HIGHEST index is {} ", ignoreFirst ? "SECOND " : "", index);
        return index;
    }

    public static int[] getHillHighsAndLow(int[] histoData) {
        // when histo has two highs and a low in-between
        // gives 1st high, low, 2nd high, index coordinates 0-255
        int hFi = -1, hSi = -1, li = -1;
        int prev = 0;
        int limit = histoData[getHighestPointIndex(histoData, false)] / 10;
        boolean goingUp = true;
        for (int i = 0; i < 256; i++) {
            if (histoData[i] < prev && histoData[i] > limit) {
                if (goingUp == true && i > 2 && histoData[i - 1] < histoData[i - 2]) {
                    if (hFi == -1) {
                        hFi = i - 2;
                    } else {
                        hSi = i - 2;
                        break;
                    }
                    goingUp = false;
                }
            } else {
                goingUp = true;
            }
            prev = histoData[i];
        }
        // find the lowest in between
        int lowest = Integer.MAX_VALUE;
        for (int i = hFi; i < hSi; i++) {
            if (histoData[i] < lowest) {
                lowest = histoData[i];
                li = i;
            }
        }
        Tonga.log.debug("Histo hills: LOWEST is {}, HIGHESTS are {} and {}", li, hFi, hSi);
        return new int[]{hFi, hSi, li};
    }

    public static int[] getMinMax(short[] bytes) {
        //gives the lowest and highest values [low,high]
        Tonga.iteration();
        int min = 0xFFFF, max = 0;
        for (int p = 0; p < bytes.length; p++) {
            max = Math.max(bytes[p] & 0xFFFF, max);
            min = Math.min(bytes[p] & 0xFFFF, min);
        }
        return new int[]{min, max};
    }

    public static int[] getMinMax(int[] histoData) {
        return getMinMaxAdaptValue(histoData, 0);
    }

    public static int getSecondMin(int[] histoData) {
        // gives the lowest point that is not zero
        int limit = histoData.length;
        for (int i = 1; i < limit; i++) {
            if (histoData[i] > 0) {
                Tonga.log.debug("Lowest non-zero value at {}", i);
                return i;
            }
        }
        Tonga.log.debug("No non-zero values in the histogram");
        return 0;
    }

    public static int[] getMinMaxAdapt(int[] histoData, double percentage) {
        // gives points to cut for low/hi filter so that "percentage" first is ignored
        // =only when the signal starts to be strong it is considered
        int max = histoData[getHighestPointIndex(histoData, true)];
        int adapt = (int) (max * (percentage / 100.));
        Tonga.log.debug("Adaped autoscaling for {}-sized histogram ({}%): {} limit, highest value is {}", histoData.length, percentage, adapt, max);
        return getMinMaxAdaptValue(histoData, adapt);
    }

    public static int[] getMinMaxAdaptValue(int[] histoData, int value) {
        int limit = histoData.length;
        int up = -1, low = -1;
        for (int i = 0; i < limit; i++) {
            if (histoData[i] > value) {
                low = i;
                break;
            }
        }
        for (int i = limit - 1; i >= 0; i--) {
            if (histoData[i] > value) {
                up = i;
                break;
            }
        }
        if (up == -1 || low == -1) {
            Tonga.log.warn("Autoscale index is negative");
        }
        return new int[]{low, up};
    }
}
