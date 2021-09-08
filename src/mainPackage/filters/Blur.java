package mainPackage.filters;

import java.util.function.Consumer;
import mainPackage.ImageData;
import mainPackage.Tonga;
import mainPackage.utils.RGB;

/*
modified from original code by Ivan Kutskir 
original code licenced under the MIT License
 */
public class Blur {

    ImageData id;
    int[] bxs;
    int w;
    int h;
    double r;
    double progressSteps;
    int progressMax;

    public int[] gauss(ImageData imgd, double radius, boolean bw) {
        id = imgd;
        w = id.width;
        h = id.height;
        r = radius;
        progressSteps = bw ? 2. : 4.;
        bxs = boxes(r, 3);
        Tonga.iteration((int) progressSteps);
        int[] out = new int[id.totalPixels()];
        if (bw) { // bw
            progressMax = 2 * 3;
            blurImage(this::gauss, out);
            return out;
        } else { //argb
            progressMax = 2 * 3 * 4;
            blurArray(this::gauss, out, 16); // red
            blurArray(this::gauss, out, 8); // green
            blurArray(this::gauss, out, 0); // blue
            blurArray(this::gauss, out, 24); // alpha
            return out;
        }
    }

    public int[] box(ImageData imgd, double radius, boolean bw) {
        id = imgd;
        w = id.width;
        h = id.height;
        r = radius;
        progressSteps = bw ? 1. : 2.;
        Tonga.iteration((int) progressSteps);
        int[] out = new int[id.totalPixels()];
        if (bw) { // bw
            progressMax = 2;
            blurImage(this::box, out);
            return out;
        } else { //argb
            progressMax = 2 * 4;
            blurArray(this::box, out, 16); // red
            blurArray(this::box, out, 8); // green
            blurArray(this::box, out, 0); // blue
            blurArray(this::box, out, 24); // alpha
            return out;
        }
    }

    private void blurImage(Consumer<int[][]> blur, int[] out) {
        int[] in = new int[id.totalPixels()];
        for (int i = 0; i < id.pixels32.length; i++) {
            in[i] = RGB.brightness(id.pixels32[i]);
        }
        blur.accept(new int[][]{in, out});
        for (int i = 0; i < id.pixels32.length; i++) {
            out[i] = RGB.argb(out[i]);
        }

    }

    private void blurArray(Consumer<int[][]> blur, int[] out, int bytes) {
        int[] lout = new int[id.totalPixels()];
        int[] in = new int[id.totalPixels()];
        for (int i = 0; i < id.pixels32.length; i++) {
            in[i] = (id.pixels32[i] >> bytes) & 0xFF; // red
        }
        blur.accept(new int[][]{in, lout});
        for (int i = 0; i < id.pixels32.length; i++) {
            out[i] = out[i] | lout[i] << bytes;
        }
    }

    private void gauss(int[][] arrays) {
        int[] in = arrays[0], out = arrays[1];
        boxBlur(in, out, (bxs[0] - 1) / 2);
        boxBlur(out, in, (bxs[1] - 1) / 2);
        boxBlur(in, out, (bxs[2] - 1) / 2);
    }

    private void box(int[][] arrays) {
        int[] in = arrays[0], out = arrays[1];
        boxBlur(in, out, (int) r);
    }

    private int[] boxes(double sigma, int n) {
        double wIdeal = Math.sqrt((12 * sigma * sigma / n) + 1);
        int wl = (int) Math.floor(wIdeal);
        if (wl % 2 == 0) {
            wl--;
        }
        int wu = wl + 2;
        double mIdeal = (12 * sigma * sigma - n * wl * wl - 4 * n * wl - 3 * n) / (-4 * wl - 4);
        int m = (int) Math.round(mIdeal);
        int sizes[] = new int[n];
        for (int i = 0; i < n; i++) {
            sizes[i] = i < m ? wl : wu;
        }
        return sizes;
    }

    private void boxBlur(int[] in, int[] out, int r) {
        System.arraycopy(in, 0, out, 0, in.length);
        boxBlurVertical(out, in, r);
        boxBlurHorizontal(in, out, r);
    }

    private void boxBlurVertical(int[] in, int[] out, int r) {
        double rad = 1. / (r + r + 1);
        for (int i = 0; i < h; i++) {
            int ti = i * w, li = ti, ri = ti + r;
            int fv = in[ti], lv = in[ti + w - 1], val = (r + 1) * fv;
            for (int j = 0; j < r; j++) {
                if (ti + j < in.length) {
                    val += in[ti + j];
                }
            }
            for (int j = 0; j <= r; j++) {
                if (ri < in.length) {
                    val += in[ri++] - fv;
                }
                if (ti < out.length) {
                    out[ti++] = (int) Math.round(val * rad);
                }
            }
            for (int j = r + 1; j < w - r; j++) {
                val += in[ri++] - in[li++];
                out[ti++] = (int) Math.round(val * rad);
            }
            for (int j = w - r; j < w; j++) {
                if (li < in.length) {
                    val += lv - in[li++];
                }
                if (ti < out.length) {
                    out[ti++] = (int) Math.round(val * rad);
                }
            }
        }
        Tonga.loader().appendProgress(progressSteps / progressMax);
        //Tonga.accessLoader().loaderProgress(++progressSteps, progressMax, 4);
    }

    private void boxBlurHorizontal(int[] in, int[] out, int r) {
        double rad = 1. / (r + r + 1);
        for (int i = 0; i < w; i++) {
            int ti = i, li = ti, ri = ti + r * w;
            int fv = in[ti], lv = in[ti + w * (h - 1)], val = (r + 1) * fv;
            for (int j = 0; j < r; j++) {
                int ji = ti + j * w;
                if (ji < in.length) {
                    val += in[ji];
                }
            }
            for (int j = 0; j <= r; j++) {
                if (ri < in.length) {
                    val += in[ri] - fv;
                }
                if (ti < out.length) {
                    out[ti] = (int) Math.round(val * rad);
                }
                ri += w;
                ti += w;
            }
            for (int j = r + 1; j < h - r; j++) {
                val += in[ri] - in[li];
                out[ti] = (int) Math.round(val * rad);
                li += w;
                ri += w;
                ti += w;
            }
            for (int j = h - r; j < h; j++) {
                if (li < in.length) {
                    val += lv - in[li];
                }
                if (ti < out.length) {
                    out[ti] = (int) Math.round(val * rad);
                }
                li += w;
                ti += w;
            }
        }
        Tonga.loader().appendProgress(progressSteps / progressMax);
        //Tonga.accessLoader().loaderProgress(++progressSteps, progressMax, 4);
    }
}
