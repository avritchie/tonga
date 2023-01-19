package mainPackage.filters;

import java.util.Arrays;
import java.util.Stack;
import mainPackage.utils.COL;
import mainPackage.utils.IMG;
import mainPackage.utils.GEO;
import mainPackage.Iterate;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.utils.STAT;
import mainPackage.Tonga;
import static mainPackage.PanelCreator.ControlType.*;
import static mainPackage.filters.Filter.limits;
import static mainPackage.filters.Filter.noParams;
import static mainPackage.filters.Filter.radius;
import static mainPackage.filters.Filter.bgcol;
import static mainPackage.filters.Filter.threshold;
import mainPackage.utils.HISTO;
import mainPackage.utils.RGB;

public class Filters {

    public static FilterFast thresholdLight() {
        return new FilterFast("Threshold Lightness", threshold) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.lightness(in32[pos]) >= (param.slider[0] / 100.0) ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast thresholdBiol() {
        return new FilterFast("Threshold Vision", threshold) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.relativeLuminance(in32[pos]) >= (param.slider[0] / 100.0) ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast thresholdBright() {
        return new FilterFast("Threshold Brightness", threshold) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.brightness(in32[pos]) >= (param.slider[0] * 2.55) ? COL.WHITE : COL.BLACK;
                    //c = Color.rgb((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF).getBrightness() >= (filterSlider[0] / 100.0) ? 0xFFFFFF : 0x0;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast thresholdRGB() {
        return new FilterFast("Threshold RGB", threshold) {
            @Override
            protected void processor() {
                double v = param.slider[0] * 2.55;
                Iterate.pixels(this, (int pos) -> {
                    int color = in32[pos];
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    r = r >= v ? 255 : 0;
                    g = g >= v ? 255 : 0;
                    b = b >= v ? 255 : 0;
                    out32[pos] = RGB.argb(r, g, b, 255);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast lcFilter() {
        return new FilterFast("Low-scaled", new ControlReference[]{
            new ControlReference(RANGE, new Integer[]{0, 255}, "Darkest allowed%Brightest allowed")}) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argbc(RGB.lowcut(in32[pos], param.range[1] / 255.0, param.range[0] / 255.0), in32[pos]);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast hcFilter() {
        return new FilterFast("High-scaled", new ControlReference[]{
            new ControlReference(RANGE, new Integer[]{0, 255}, "Darkest allowed%Brightest allowed")}) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argbc(RGB.hicut(in32[pos], param.range[0] / 255.0, param.range[1] / 255.0), in32[pos]);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast crapCleaner() {
        return new FilterFast("Low-filtered",
                new ControlReference[]{new ControlReference(SLIDER, new Object[]{0, 20, 200}, "Filter out the darkest % of pixels")}) {
            @Override
            protected void processor() {
                double v = param.sliderScaled[0] * 2.55;
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.brightness(in32[pos]) < v ? COL.BLACK : in32[pos];
                });
            }

            @Override
            protected void processor16() {
                double v = param.sliderScaled[0] * 655.35;
                Iterate.pixels(this, (int pos) -> {
                    out16[pos] = (in16[pos] & 0xFFFF) < v ? COL.UBLACK : in16[pos];
                });
            }
        };
    }

    public static FilterFast hiCleaner() {
        return new FilterFast("High-filtered",
                new ControlReference[]{new ControlReference(SLIDER, new Object[]{0, 20, 200}, "Filter out the brightest % of pixels")}) {
            @Override
            protected void processor() {
                double v = 255 - param.sliderScaled[0] * 2.55;
                int c = RGB.argb((int) v);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = (RGB.relativeLuminance(in32[pos])) > v ? c : in32[pos];
                });
            }

            @Override
            protected void processor16() {
                double v = 65535 - param.sliderScaled[0] * 655.35;
                short c = (short) v;
                Iterate.pixels(this, (int pos) -> {
                    out16[pos] = (in16[pos] & 0xFFFF) > v ? c : in16[pos];
                });
            }
        };
    }

    public static FilterFast cutFilter() {
        //the one used by the Histogram buttons
        return new FilterFast("Levels", limits) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    // pos,0-255,0-255; HI, LOW
                    out32[pos] = RGB.levels(in32[pos], param.range[1], param.range[0]);
                });
            }

            @Override
            protected void processor16() {
                set16BitScaleRange(param.range[1], param.range[0]);
            }
        };
    }

    public static FilterFast cutFilterAutoLow() {
        return new FilterFast("Low-peaked", noParams) {
            @Override
            protected void processor() {
                int[] histo = HISTO.getHistogram(in32);
                int limit = HISTO.getHighestPointIndex(histo, false);
                Iterate.pixels(this, (int pos) -> {
                    // pos,0-255,0-255
                    out32[pos] = RGB.levels(in32[pos], 255, limit);
                });
            }

            @Override
            protected void processor16() {
                int[] histo = HISTO.getHistogram(in16);
                int limit = HISTO.getHighestPointIndex(histo, false) / 256;
                set16BitScaleRange(255, limit);
            }
        };
    }

    public static FilterFast cutFilterAutoHigh() {
        return new FilterFast("High-peaked", noParams) {
            @Override
            protected void processor() {
                int[] histo = HISTO.getHistogram(in32);
                int limit = HISTO.getHighestPointIndex(histo, false);
                Iterate.pixels(this, (int pos) -> {
                    // pos,0-255,0-255
                    out32[pos] = RGB.levels(in32[pos], limit, 0);
                });
            }

            @Override
            protected void processor16() {
                int[] histo = HISTO.getHistogram(in16);
                int limit = HISTO.getHighestPointIndex(histo, false) / 256;
                set16BitScaleRange(limit, 0);
            }
        };
    }

    public static FilterFast integral() {
        return new FilterFast("Integral", noParams) {

            @Override
            protected void processor() {
                int cnt = 0;
                Tonga.iteration();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pos = y * width + x;
                        int apos = (y - 1) * width + x;
                        int bb = RGB.brightness(in32[pos]);
                        cnt += bb;
                        if (apos > 0) {
                            out32[pos] = out32[apos] - cnt;
                        } else {
                            out32[pos] = 0xFFFFFFFF - cnt;
                        }
                    }
                    cnt = 0;
                    Tonga.loader().appendProgress(height);
                    if (Tonga.loader().getTask().isInterrupted()) {
                        return;
                    }
                }
            }

            @Override
            protected void processor16() {
            }
        };
    }

    public static FilterFast condSum() {
        return new FilterFast("Sum",
                new ControlReference[]{new ControlReference(COLOUR, "Target colour")}) {

            @Override
            protected void processor() {
                int cnt = 0;
                int c = param.colorARGB[0];
                Tonga.iteration();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pos = y * width + x;
                        int apos = (y - 1) * width + x;
                        int bb = in32[pos] != c ? 1 : 0;
                        cnt += bb;
                        if (apos > 0) {
                            out32[pos] = out32[apos] - cnt;
                        } else {
                            out32[pos] = 0xFFFFFFFF - cnt;
                        }
                    }
                    cnt = 0;
                    Tonga.loader().appendProgress(height);
                    if (Tonga.loader().getTask().isInterrupted()) {
                        return;
                    }
                }
            }

            @Override
            protected void processor16() {
            }
        };
    }

    public static FilterFast popColour() {
        return new FilterFast("Separated Colour", new ControlReference[]{
            new ControlReference(COLOUR, "Colour to separate")}) {

            @Override
            protected void processor() {
                double sb = RGB.brightness(param.colorARGB[0]) / 255.;
                double ss = RGB.saturation(param.colorARGB[0]);
                double sh = RGB.hue(param.colorARGB[0]) * 360;
                Iterate.pixels(this, (int pos) -> {
                    double tb = RGB.brightness(in32[pos]) / 255.;
                    double ts = RGB.saturation(in32[pos]);
                    double th = RGB.hue(in32[pos]) * 360;
                    double db = Math.min(1.0, Math.abs(sb - tb));
                    double ds = sb * Math.abs(ss - ts);
                    double dh = sb * (GEO.getDirDifference(sh, th) / 180.);
                    int v = (int) (255 * Math.max(db, Math.max(ds, dh)));
                    out32[pos] = RGB.argb(255 - v);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast alpha() {
        return new FilterFast("Alpha Color",
                new ControlReference[]{new ControlReference(COLOUR, "Colour to turn into the alpha")}) {
            @Override
            protected void processor() {
                double sb = RGB.brightness(param.colorARGB[0]) / 255.;
                double ss = RGB.saturation(param.colorARGB[0]);
                double sh = RGB.hue(param.colorARGB[0]) * 360;
                Iterate.pixels(this, (int pos) -> {
                    double tb = RGB.brightness(in32[pos]) / 255.;
                    double ts = RGB.saturation(in32[pos]);
                    double th = RGB.hue(in32[pos]) * 360;
                    int a = (int) (255 * Math.max(Math.min(1.0, Math.abs(sb - tb)), Math.max(
                            sb * Math.abs(ss - ts), sb * Math.abs(Math.abs(Math.abs(sh - 180) - Math.abs(th - 180)) / 180.))));
                    out32[pos] = RGB.argbAlpha(in32[pos], a);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast alphaDark() {
        return new FilterFast("Alpha Darkness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int color = in32[pos];
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = RGB.brightness(color);
                    out32[pos] = (int) r << 16 | g << 8 | b | a << 24;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast alphaBright() {
        return new FilterFast("Alpha Brightness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int color = in32[pos];
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                    int a = 255 - RGB.brightness(color);
                    out32[pos] = (int) r << 16 | g << 8 | b | a << 24;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast opaque() {
        return new FilterFast("Opaque",
                new ControlReference[]{
                    new ControlReference(TOGGLE, "Replace with a new colour", 0, new int[]{1, 1}),
                    new ControlReference(COLOUR, "Colour to replace with")}) {
            @Override
            protected void processor() {
                if (!param.toggle[0]) {
                    Iterate.pixels(this, (int pos) -> {
                        int color = in32[pos];
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = color & 0xFF;
                        out32[pos] = RGB.argb(r, g, b);
                    });
                } else {
                    Iterate.pixels(this, (int pos) -> {
                        int color = in32[pos];
                        int a = (color >> 24) & 0xFF;
                        out32[pos] = COL.blendColorWeighted(color, param.colorARGB[0], a / 255.);
                    });
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast brightExtreme() {
        return new FilterFast("Extreme Brightness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.maxColour(in32[pos]);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast bwLightness() {
        return new FilterFast("Lightness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb((int) (RGB.lightness(in32[pos]) * 255), (in32[pos] >> 24) & 0xFF);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast bwLuminance() {
        return new FilterFast("Relative Luminance", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb((int) (RGB.relativeLuminance(in32[pos]) * 255));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast bwBrightness() {
        return new FilterFast("Brightness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb(RGB.brightness(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast bwSaturation() {
        return new FilterFast("Saturation", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb((int) (RGB.saturation(in32[pos]) * 255));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast bwHue() {
        return new FilterFast("Hue", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb((int) (RGB.hue(in32[pos]) * 255));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast colorize() {
        return new FilterFast("Colorized",
                new ControlReference[]{new ControlReference(COLOUR, "Target colour")}) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argbColored(RGB.brightness(in32[pos]), param.colorARGB[0], RGB.alpha(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                outData.colour = param.colorARGB[0];
                IMG.copyPixels(in16, out16);
            }
        };
    }

    public static FilterFast multiply() {
        return new FilterFast("Multiply Values",
                new ControlReference[]{new ControlReference(SPINNER, "Of the original (%)", 200)}) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.multiplyColor(in32[pos], param.spinner[0] / 100.);
                });
            }

            @Override
            protected void processor16() {
                Iterate.pixels(this, (int pos) -> {
                    out16[pos] = (short) Math.min(65535, ((in16[pos] & 0xFFFF) * param.spinner[0] / 100.));
                });
            }
        };
    }

    public static FilterFast multiplyColBright() {
        return new FilterFast("Multiply Color & Brightness",
                new ControlReference[]{new ControlReference(SPINNER, "Of the original (%)", 200)}) {
            @Override
            protected void processor() {
                double f = param.spinner[0] / 100.;
                Iterate.pixels(this, (int pos) -> {
                    double h = RGB.hue(in32[pos]);
                    double s = RGB.saturation(in32[pos]) * f;
                    double l = RGB.brightness(in32[pos]) / 255. * f;
                    int c = java.awt.Color.HSBtoRGB((float) h,
                            (float) Math.min(1.0, s),
                            (float) Math.min(1.0, l));
                    out32[pos] = RGB.argbAlpha(c, RGB.alpha(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast hueExtreme() {
        return new FilterFast("Extreme Color", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int c = java.awt.Color.HSBtoRGB((float) RGB.hue(in32[pos]), 1, (float) (RGB.brightness(in32[pos]) / 255.));
                    out32[pos] = RGB.argbAlpha(c, RGB.alpha(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast hueBrightExtreme() {
        return new FilterFast("Extreme Color & Brightness", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int c = java.awt.Color.HSBtoRGB((float) RGB.hue(in32[pos]), 1, 1);
                    out32[pos] = RGB.argbAlpha(c, RGB.alpha(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast meanBrightness() {
        return new FilterFast("Mean Brightness", radius, 6) {
            @Override
            protected void processor() {
                double[] means = (double[]) meanStds(this, param.spinner[0])[1];
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argbc((int) means[pos], in32[pos]);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast stdBrightness() {
        return new FilterFast("STD Edge Detection", radius, 6) {
            @Override
            protected void processor() {
                double[] stds = (double[]) meanStds(this, param.spinner[0])[2];
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argbc((int) stds[pos], in32[pos]);
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast swapColour() {
        return new FilterFast("Colour Swap", new ControlReference[]{
            new ControlReference(COLOUR, "Replace which colour"),
            new ControlReference(COLOUR, "Replace with which colour")}) {

            @Override
            protected void processor() {
                int sourceCol = param.colorARGB[0];
                int finalCol = param.colorARGB[1];
                Iterate.pixels(this, (int pos) -> {
                    int thisCol = in32[pos];
                    out32[pos] = thisCol == sourceCol ? finalCol : thisCol;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast doubleThreshold() {
        return new FilterFast("Double Threshold", new ControlReference[]{
            new ControlReference(RANGE, new Integer[]{0, 255}, "Gray%White")}) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int c = RGB.brightness(in32[pos]);
                    out32[pos] = c >= param.range[1] ? COL.WHITE : c >= param.range[0] ? COL.GRAY : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast localThreshold() {
        return new FilterFast("Local Threshold", new ControlReference[]{
            new ControlReference(SLIDER, "Threshold"),
            new ControlReference(SPINNER, "Radius (px)", 10)}, 2) {
            @Override
            protected void processor() {
                int[] integral = Filters.integral().runSingle(inData).pixels32;
                int r = param.spinner[0];
                Iterate.pixels(this, (int pos) -> {
                    double localMean = Integral.integralMean(integral, r, pos, width, height);
                    out32[pos] = RGB.brightness(in32[pos]) > localMean - param.sliderScaled[0] ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    @Deprecated
    public static FilterFast localSlowThreshold() {
        return new FilterFast("Local Threshold", new ControlReference[]{
            new ControlReference(SLIDER, "Threshold"),
            new ControlReference(SPINNER, "Radius (px)", 10)}, 6) {
            @Override
            protected void processor() {
                Object[] calc = meanStds(this, param.spinner[0]);
                int[] gs = (int[]) calc[0];
                double[] means = (double[]) calc[1];
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = gs[pos] > means[pos] - param.slider[0] ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast niblack() {
        return new FilterFast("Niblack Threshold", new ControlReference[]{
            new ControlReference(SLIDER, "Threshold"),
            new ControlReference(SLIDER, "Adaptation level (k)"),
            new ControlReference(SPINNER, "Radius (px)", 10)}, 2) {
            @Override
            protected void processor() {
                double adapt = param.slider[1] / 100.;
                int r = param.spinner[0];
                int[] integral = new int[width * height];
                long[] powIntegral = new long[width * height];
                //quick integral image version w both single and squared
                int cnt = 0;
                int cnts = 0;
                Tonga.iteration();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pos = y * width + x;
                        int apos = (y - 1) * width + x;
                        int bb = RGB.brightness(in32[pos]);
                        int bs = bb * bb;
                        cnt += bb;
                        cnts += bs;
                        if (apos > 0) {
                            integral[pos] = integral[apos] - cnt;
                            powIntegral[pos] = powIntegral[apos] - cnts;
                        } else {
                            integral[pos] = 0xFFFFFFFF - cnt;
                            powIntegral[pos] = 0xFFFFFFFFFFFFFFFFL - cnts;
                        }
                    }
                    cnt = 0;
                    cnts = 0;
                    Tonga.loader().appendProgress(height);
                    if (Tonga.loader().getTask().isInterrupted()) {
                        return;
                    }
                }
                Iterate.pixels(this, (int pos) -> {
                    double localMean = Integral.integralMean(integral, r, pos, width, height);
                    double powMean = Integral.integralMean(powIntegral, r, pos, width, height);
                    double localStd = Math.sqrt(powMean - localMean * localMean);
                    out32[pos] = RGB.brightness(in32[pos]) > localMean + adapt * localStd - param.sliderScaled[0] ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    @Deprecated
    public static FilterFast niblackSlow() {
        return new FilterFast("Niblack Threshold", new ControlReference[]{
            new ControlReference(SLIDER, "Threshold"),
            new ControlReference(SLIDER, "Adaptation level (k)"),
            new ControlReference(SPINNER, "Radius (px)", 10)}, 6) {
            @Override
            protected void processor() {
                double adapt = param.slider[1] / 100.;
                Object[] calc = meanStds(this, param.spinner[0]);
                int[] gs = (int[]) calc[0];
                double[] means = (double[]) calc[1];
                double[] stds = (double[]) calc[2];
                Iterate.pixels(this, (int pos) -> {
                    // out = value > mean + k * std - offset
                    out32[pos] = gs[pos] > means[pos] + adapt * stds[pos] - param.slider[0] ? COL.WHITE : COL.BLACK;
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    @Deprecated
    private static Object[] meanStdsSlow(FilterFast f, double radd) {
        Tonga.iteration(4);
        int rad = (int) radd;
        int length = f.inData.totalPixels();
        int[] brgharr = new int[length];
        double[] meanarr = new double[length];
        double[] stdarr = new double[length];
        double[] vals = new double[(rad * 2 + 1) * (rad * 2 + 1)];
        int fsize = f.width * f.height;
        //set a value between 0-255 to each pixel
        for (int i = 0; i < length; i++) {
            brgharr[i] = RGB.brightness(f.in32[i]);
        }
        Iterate.pixels(f, 4, (int pos) -> {
            int hits = 0, sum = 0, npos;
            for (int ix = -rad; ix <= rad; ix++) {
                for (int iy = -rad; iy <= rad; iy++) {
                    npos = pos + ix + (iy * f.width);
                    if (npos >= 0 && npos < fsize /*&& p / f.width == i / f.width + iy*/) {
                        vals[hits++] = brgharr[npos];
                        sum += brgharr[npos];
                    }
                }
            }
            STAT stats = new STAT(Arrays.copyOfRange(vals, 0, hits));
            meanarr[pos] = sum / (double) hits;
            stats.mean = meanarr[pos];
            stdarr[pos] = stats.getStdDev();
        });
        return new Object[]{brgharr, meanarr, stdarr};
    }

    @Deprecated
    //this code has an issue on horizontal edges
    private static Object[] meanStds(FilterFast f, double radd) {
        int rad = (int) radd;
        int length = f.inData.totalPixels();
        int[] brgharr = new int[length];
        double[] meanarr = new double[length];
        double[] stdarr = new double[length];
        int rdist = rad * 2 + 1;
        int[] vals = new int[rdist * rdist];
        //set a value between 0-255 to each pixel
        Tonga.iteration();
        for (int i = 0; i < length; i++) {
            brgharr[i] = RGB.brightness(f.in32[i]);
        }
        Tonga.loader().appendProgress(1);
        int minb = rad - 1 + f.width * rad, maxb = length - minb - 1;
        Iterate.pixels(f, 4, (int pos) -> {
            STAT stats;
            if (rad > 4 && pos > minb && pos < maxb) {
                for (int yy = -rad; yy <= rad; yy++) {
                    System.arraycopy(brgharr, pos - rad + yy * f.width, vals, (yy + rad) * rdist, rdist);
                }
                stats = new STAT(vals);
                meanarr[pos] = stats.getMean();
            } else {
                int hits = 0;
                int npos, summ = 0;
                for (int ix = -rad; ix <= rad; ix++) {
                    for (int iy = -rad; iy <= rad; iy++) {
                        npos = pos + ix + (iy * f.width);
                        if (npos >= 0 && npos < length /*&& p / f.width == i / f.width + iy*/) {
                            vals[hits++] = brgharr[npos];
                            summ += brgharr[npos];
                        }
                    }
                }
                stats = new STAT(Arrays.copyOfRange(vals, 0, hits));
                meanarr[pos] = summ / (double) hits;
            }
            stats.mean = meanarr[pos];
            stdarr[pos] = stats.getStdDev();
        });
        return new Object[]{brgharr, meanarr, stdarr};
    }

    public static FilterFast distanceTransform() {
        return new FilterFast("Distance", bgcol) {
            @Override
            protected void processor() {
                int d = Math.max(width, height);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = COL.BLACK;
                    if (in32[pos] != param.colorARGB[0]) {
                        int s = 0, x = pos % width, y = pos / width, p, c;
                        mainloop:
                        while (s <= d) {
                            s++;
                            try {
                                for (int xx = x - s; xx < x + s; xx++) {
                                    for (int yy = y - s; yy < y + s; yy++) {
                                        p = xx + yy * width;
                                        if (in32[p] == param.colorARGB[0]) {
                                            c = (byte) GEO.getDist(x, y, xx, yy);
                                            out32[pos] = RGB.argb(c);
                                            break mainloop;
                                        }
                                    }
                                }
                            } catch (IndexOutOfBoundsException ex) {
                            }
                        }
                    }
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast saturate() {
        return new FilterFast("Max Saturation", noParams, 2) {
            double max, min;

            @Override
            protected void processor() {
                max = 0;
                min = 1;
                Iterate.pixels(this, (int pos) -> {
                    double b = RGB.saturation(in32[pos]);
                    max = Math.max(b, max);
                    min = Math.min(b, min);
                });
                double f = 1. / (max - min);
                Iterate.pixels(this, (int pos) -> {
                    int c = java.awt.Color.HSBtoRGB((float) RGB.hue(in32[pos]),
                            (float) Math.min(1.0, (RGB.saturation(in32[pos]) - min) * f),
                            (float) (RGB.brightness(in32[pos]) / 255.));
                    out32[pos] = RGB.argbAlpha(c, RGB.alpha(in32[pos]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast reduceBrightDepth() {
        return new FilterFast("Reduced Brightness Depth",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Reduce to x levels of brightness", 5)}, 3) {
            int[][] vals;
            int[] boundValues, changePosition, averageValue;
            int max, min, m, cc, mv;

            @Override
            protected void processor() {
                vals = new int[inData.totalPixels()][2];
                min = 255;
                max = 0;
                Iterate.pixels(this, (int pos) -> {
                    vals[pos][0] = RGB.brightness(in32[pos]);
                    vals[pos][1] = pos;
                    max = Math.max(vals[pos][0], max);
                    min = Math.min(vals[pos][0], min);
                });
                Arrays.sort(vals, (int[] a, int[] b) -> Integer.compare(a[0], b[0]));
                double v = (max - min) / (double) param.spinner[0];
                boundValues = new int[param.spinner[0]];
                changePosition = new int[param.spinner[0]];
                for (int i = 1; i <= param.spinner[0]; i++) {
                    boundValues[i - 1] = min + (int) (v * i);
                }
                m = 0;
                cc = 0;
                mv = 0;
                averageValue = new int[param.spinner[0]];
                for (int pos = 0; pos < inData.totalPixels(); pos++) {
                    if (vals[pos][0] > boundValues[m]) {
                        changePosition[m] = pos;
                        averageValue[m] = cc / mv;
                        m++;
                        cc = 0;
                        mv = 0;
                    }
                    cc += vals[pos][0];
                    mv++;
                }
                changePosition[m] = inData.totalPixels();
                averageValue[m] = cc / mv;
                m = 0;
                Iterate.pixels(this, (int pos) -> {
                    if (vals[pos][0] > boundValues[m]) {
                        m++;
                    }
                    int p = vals[pos][1];
                    int c = java.awt.Color.HSBtoRGB((float) RGB.hue(in32[p]),
                            (float) RGB.saturation(in32[p]),
                            (float) (averageValue[m] / 255.));
                    out32[p] = RGB.argbAlpha(c, RGB.alpha(in32[p]));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast autoscale() {
        //the one used by the Histogram buttons
        return new FilterFast("Autoscale", noParams) {
            int max, min;

            @Override
            protected void processor() {
                min = 255;
                max = 0;
                Iterate.pixels(this, (int pos) -> {
                    int b = RGB.brightness(in32[pos]);
                    max = Math.max(b, max);
                    min = Math.min(b, min);
                });
                double f = 255. / (max - min);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.substractLimit(in32[pos], min);
                    out32[pos] = RGB.multiplyColor(out32[pos], f);
                });
            }

            @Override
            protected void processor16() {
                int[] b = HISTO.getMinMax(in16);
                min = b[0];
                max = b[1];
                outData.max = max > outData.max ? outData.max : max;
                outData.min = min < outData.min ? outData.min : min;
                IMG.copyPixels(in16, out16);
            }
        };
    }

    public static FilterFast autoscaleWithAdapt() {
        return new FilterFast("Adapted Autoscale",
                new ControlReference[]{
                    new ControlReference(SLIDER, new Object[]{0, 1, 100}, "Ignore values below % of the median")}) {
            @Override
            protected void processor() {
                int[] histo = HISTO.getHistogram(in32);
                int[] begEnd = HISTO.getMinMaxAdapt(histo, param.sliderScaled[0]);
                int low = begEnd[0];
                int hi = begEnd[1];
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.levels(in32[pos], hi, low);
                });
            }

            @Override
            protected void processor16() {
                int[] histo = HISTO.getHistogram(in16);
                int[] begEnd = HISTO.getMinMaxAdapt(histo, param.sliderScaled[0]);
                set16BitScaleRange(begEnd[0], begEnd[1]);
            }
        };
    }

    public static FilterFast autoscaleWithPixelAdapt() {
        return new FilterFast("Adapted Autoscale",
                new ControlReference[]{
                    new ControlReference(SLIDER, new Object[]{0, 100, 500, 200}, "Ignore values with less than x pixels", 1)}) {
            @Override
            protected void processor() {
                int[] histo = HISTO.getHistogram(in32);
                int[] begEnd = HISTO.getMinMaxAdaptValue(histo, (int) param.sliderScaled[0]);
                int low = begEnd[0];
                int hi = begEnd[1];
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.levels(in32[pos], hi, low);
                });
            }

            @Override
            protected void processor16() {
                int[] histo = HISTO.getHistogram(in16);
                int[] begEnd = HISTO.getMinMaxAdaptValue(histo, (int) param.sliderScaled[0]);
                set16BitScaleRange(begEnd[0], begEnd[1]);
            }
        };
    }

    public static FilterFast scaleDark() {
        return new FilterFast("Autoscale Darkness", noParams) {
            @Override
            protected void processor() {
                int[] histo = HISTO.getHistogram(in32);
                int low = HISTO.getSecondMin(histo);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.levels(in32[pos], histo.length - 1, low);
                });
            }

            @Override
            protected void processor16() {
                int[] histo = HISTO.getHistogram(in16);
                int low = HISTO.getSecondMin(histo);
                set16BitScaleRange(low, histo.length - 1);
            }
        };
    }

    public static FilterFast scaleRGB() {
        return new FilterFast("Scale RGB", new ControlReference[]{
            new ControlReference(SLIDER, "Desired intensity level"),
            new ControlReference(COMBO, new String[]{"Red", "Green", "Blue"}, "Based on which channel", 2),}) {

            double basevalue;
            int areapixels;

            @Override
            protected void processor() {
                basevalue = 0;
                areapixels = 0;
                int bshift = (2 - param.combo[0]) * 16;
                Iterate.pixels(this, (int pos) -> {
                    int col = in32[pos];
                    if (col != COL.BLACK) {
                        areapixels++;
                        basevalue += ((col >> bshift) & 0xFF) / 255.;
                    }
                });
                double intlev = basevalue / (double) areapixels;
                double target = param.slider[0] / 100.;
                int mperc = (int) ((target / intlev) * 100);
                setOutputBy(Filters.multiply().runSingle(inData, mperc));
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast invert() {
        return new FilterFast("Invert", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    int a = (in32[pos] >> 24) & 0xFF;
                    out32[pos] = ((in32[pos] ^ -1) & 0xFFFFFF) | (a << 24);
                });
            }

            @Override
            protected void processor16() {
                Iterate.pixels(this, (int pos) -> {
                    out16[pos] = (short) (0xFFFF - in16[pos]);
                });
                outData.max = 0xFFFF - inData.min;
                outData.min = 0xFFFF - inData.max;
            }
        };
    }

    public static FilterFast massonsDeconvolution() {
        return new FilterFast("Massons Collagen", noParams) {
            @Override
            protected void processor() {
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb(COL.colorDec(in32[pos], 0.09997159, 0.73738605, 0.6680326));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast gamma() {
        return new FilterFast("Gamma", new ControlReference[]{
            new ControlReference(SLIDER, new Object[]{0, 1, 6, 200}, "Gamma to apply (γ)")}) {
            @Override
            protected void processor() {
                double divider = param.sliderScaled[0];
                Iterate.pixels(this, (int pos) -> {
                    int color = in32[pos];
                    out32[pos] = RGB.argb(
                            (int) (255 * (Math.pow(((color >> 16) & 0xFF) / 255., divider))),
                            (int) (255 * (Math.pow(((color >> 8) & 0xFF) / 255., divider))),
                            (int) (255 * (Math.pow((color & 0xFF) / 255., divider))), RGB.alpha(color));
                });
            }

            @Override
            protected void processor16() {
                double divider = param.sliderScaled[0];
                double md = (double) (inData.max - inData.min);
                Iterate.pixels(this, (int pos) -> {
                    out16[pos] = (short) ((md * (Math.pow(((in16[pos] & 0xFFFF) - inData.min) / md, divider))) + inData.min);
                });
            }
        };
    }

    public static FilterFast box() {
        return new FilterFast("Box Blur",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Radius (px)", 5),
                    new ControlReference(TOGGLE, "Process as grayscale (faster)")}) {
            @Override
            protected void processor() {
                outData.pixels32 = new Blur().box(inData, param.spinner[0], param.toggle[0]);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 1 : 2;
            }

        };
    }

    public static FilterFast dog() {
        return new FilterFast("DoG blur",
                new ControlReference[]{
                    new ControlReference(SPINNER, "First radius (px)", 5),
                    new ControlReference(SPINNER, "Second radius (px)", 20),
                    new ControlReference(TOGGLE, "Reversed")}, 5) {
            @Override
            protected void processor() {
                int[] gauss1 = new Blur().gauss(inData, param.spinner[0], true);
                int[] gauss2 = new Blur().gauss(inData, param.spinner[1], true);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb(param.toggle[0]
                            ? Math.max(0, Math.min(255, (gauss2[pos] & 0xFF) - (gauss1[pos] & 0xFF)))
                            : Math.max(0, Math.min(255, (gauss1[pos] & 0xFF) - (gauss2[pos] & 0xFF))));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast dob() {
        return new FilterFast("DoB blur",
                new ControlReference[]{
                    new ControlReference(SPINNER, "First radius (px)", 5),
                    new ControlReference(SPINNER, "Second radius (px)", 20),
                    new ControlReference(TOGGLE, "Reversed")}, 3) {
            @Override
            protected void processor() {
                int[] box1 = new Blur().box(inData, param.spinner[0], true);
                int[] box2 = new Blur().box(inData, param.spinner[1], true);
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb(param.toggle[0]
                            ? Math.max(0, Math.min(255, (box2[pos] & 0xFF) - (box1[pos] & 0xFF)))
                            : Math.max(0, Math.min(255, (box1[pos] & 0xFF) - (box2[pos] & 0xFF))));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast gaussPerfect() {
        return new FilterFast("Gauss Real",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Radius (px)", 5),
                    new ControlReference(TOGGLE, "Process as grayscale (faster)")}) {
            int[] bwIn = null;
            int j, i, rs;
            double r;

            @Override
            protected void processor() {
                r = param.spinner[0];
                rs = (int) Math.ceil(r * 2.57);
                if (param.toggle[0]) {
                    bwIn = new int[inData.totalPixels()];
                    for (int p = 0; p < in32.length; p++) {
                        bwIn[p] = RGB.brightness(in32[p]);
                    }
                }
                Tonga.iteration(iterations(param.toggle[0]));
                Iterate.pixels(this, (int pos) -> {
                    j = pos % width;
                    i = pos / width;
                    if (param.toggle[0]) { // bw
                        int bw = gaussIteration(bwIn, 0);
                        out32[pos] = RGB.argb(bw);
                    } else { //argb
                        int red = gaussIteration(in32, 16);
                        int green = gaussIteration(in32, 8);
                        int blue = gaussIteration(in32, 0);
                        int alpha = gaussIteration(in32, 24);
                        out32[pos] = RGB.argb(red, green, blue, alpha);
                    }
                });
            }

            protected int gaussIteration(int[] in, int bytes) {
                double val = 0, wsum = 0;
                for (int iy = i - rs; iy < i + rs + 1; iy++) {
                    for (int ix = j - rs; ix < j + rs + 1; ix++) {
                        int x = Math.min(width - 1, Math.max(0, ix));
                        int y = Math.min(height - 1, Math.max(0, iy));
                        double dsq = (ix - j) * (ix - j) + (iy - i) * (iy - i);
                        double wght = Math.exp(-dsq / (2 * r * r)) / (Math.PI * 2 * r * r);
                        val += ((in[y * width + x] >> bytes) & 0xFF) * wght;
                        wsum += wght;
                    }
                }
                Tonga.loader().appendProgress(19. / (width * height));
                return (int) Math.round(val / wsum);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 20 : 80;
            }

        };
    }

    public static FilterFast gaussApprox() {
        return new FilterFast("Gauss Approx",
                new ControlReference[]{
                    new ControlReference(SPINNER, "Radius (px)", 5),
                    new ControlReference(TOGGLE, "Process as grayscale (faster)")}) {
            @Override
            protected void processor() {
                outData.pixels32 = new Blur().gauss(inData, param.spinner[0], param.toggle[0]);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }

            @Override
            protected int iterations(boolean bits) {
                return param.toggle[0] ? 2 : 4;
            }

        };
    }

    public static FilterFast blurConditional() {
        return new FilterFast("Conditional blur", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Blur radius (px)", 10),
            new ControlReference(TOGGLE, "Keep the background area")}, 10) {
            int j, i;
            int r, c;

            @Override
            protected void processor() {
                r = param.spinner[0];
                c = param.colorARGB[0];
                Iterate.pixels(this, 10, (int pos) -> {
                    j = pos % width;
                    i = pos / width;
                    if (!param.toggle[0] || in32[i * width + j] != c) {
                        out32[pos] = RGB.argb(blurIteration(in32, in32));
                    } else {
                        out32[pos] = c;
                    }
                });
            }

            protected int blurIteration(int[] in, int[] cond) {
                double val = 0;
                int cc = 0;
                for (int iy = i - r; iy <= i + r; iy++) {
                    for (int ix = j - r; ix <= j + r; ix++) {
                        if (ix >= 0 && ix < width && iy >= 0 && iy < height) {
                            if (GEO.getDist(j, i, ix, iy) <= r) {
                                if (cond[iy * width + ix] != c) {
                                    val += RGB.brightness(in[iy * width + ix]);
                                    cc++;
                                }
                            }
                        }
                    }
                }
                return (int) Math.round(val / cc);
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast blurConditionalFast() {
        return new FilterFast("Conditional blur", new ControlReference[]{
            new ControlReference(COLOUR, "Background colour", -2),
            new ControlReference(SPINNER, "Blur radius (px)", 10),
            new ControlReference(TOGGLE, "Keep the background area")}, 10) {
            int j, i;
            int r, c;

            @Override
            protected void processor() {
                int[] integral = Filters.integral().runSingle(inData).pixels32;
                int[] sum = Filters.condSum().runSingle(inData, param.colorARGB[0]).pixels32;
                int r = param.spinner[0];
                Iterate.pixels(this, (int pos) -> {
                    double localMean = Integral.integralSum(integral, r, pos, width, height);
                    double localSum = Integral.integralSum(sum, r, pos, width, height);
                    out32[pos] = RGB.argb((int) (localMean / localSum));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast illuminationCorrection() {
        return new FilterFast("Lighting correction", noParams, 3) {
            @Override
            protected void processor() {
                int rad = inData.width * inData.height / 10000;
                int[] gauss = new Blur().gauss(inData, rad, false);
                int[] a = Arrays.stream(gauss).map(i -> i >> 24 & 0xFF).toArray();
                int[] r = Arrays.stream(gauss).map(i -> i >> 16 & 0xFF).toArray();
                int[] g = Arrays.stream(gauss).map(i -> i >> 8 & 0xFF).toArray();
                int[] b = Arrays.stream(gauss).map(i -> i & 0xFF).toArray();
                int avga = (int) new STAT(a).getMean();
                int avgr = (int) new STAT(r).getMean();
                int avgg = (int) new STAT(g).getMean();
                int avgb = (int) new STAT(b).getMean();
                Iterate.pixels(this, (int pos) -> {
                    out32[pos] = RGB.argb(
                            Math.min(0xFF, Math.max(0x00, (in32[pos] >> 16 & 0xFF) - (gauss[pos] >> 16 & 0xFF) + avgr)),
                            Math.min(0xFF, Math.max(0x00, (in32[pos] >> 8 & 0xFF) - (gauss[pos] >> 8 & 0xFF) + avgg)),
                            Math.min(0xFF, Math.max(0x00, (in32[pos] & 0xFF) - (gauss[pos] & 0xFF) + avgb)),
                            Math.min(0xFF, Math.max(0x00, (in32[pos] >> 24 & 0xFF) - (gauss[pos] >> 24 & 0xFF) + avga)));
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast connectEdges() {
        return new FilterFast("Connected",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(TOGGLE, "Iterative")}) {

            int c;
            Stack<Integer> stack;

            @Override
            protected void processor() {
                IMG.copyPixels(in32, out32);
                stack = new Stack<>();
                Iterate.pixels(this, (int p) -> {
                    c = out32[p];
                    if (c != param.colorARGB[0]) {
                        isGap(p, 0, -1);
                        isGap(p, 0, 1);
                        isGap(p, -1, 0);
                        isGap(p, 1, 0);
                        isCorner(p, 1, -1);
                        isCorner(p, 1, 1);
                        isCorner(p, -1, 1);
                        isCorner(p, -1, -1);
                    }
                });
                if (param.toggle[0]) {
                    while (!stack.empty()) {
                        int p = stack.pop();
                        c = out32[p];
                        isGap(p, 0, -1);
                        isGap(p, 0, 1);
                        isGap(p, -1, 0);
                        isGap(p, 1, 0);
                        isCorner(p, 1, -1);
                        isCorner(p, 1, 1);
                        isCorner(p, -1, 1);
                        isCorner(p, -1, -1);
                    }
                }
            }

            private void isGap(int p, int x, int y) {
                int np = p + x + y * width;
                int w = p / width;
                try {
                    if (w == (p + x) / width && out32[np] == param.colorARGB[0]
                            && ((w == (p + x * 2) / width && out32[p + x * 2 + y * 2 * width] != param.colorARGB[0])
                            || (((w == (p + x * 3) / width && out32[p + x * 3 + y * 3 * width] != param.colorARGB[0])
                            && ((p + (y + x) * width < out32.length
                            && p + (y + x) * width > -1
                            && out32[p + (x + y) + (y + x) * width] != param.colorARGB[0]
                            && w == (p + x + y) / width)
                            || (p + (y - x) * width < out32.length
                            && p + (y - x) * width > -1
                            && out32[p + (x - y) + (y - x) * width] != param.colorARGB[0]
                            && w == (p + x - y) / width)))))) {
                        out32[np] = c;
                        stack.push(np);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    //edge
                }
            }

            private void isCorner(int p, int x, int y) {
                int np = p + x + y * width;
                int w = p / width;
                try {
                    if ((w == (p + x) / width && out32[np] == param.colorARGB[0])
                            && (w == (p + x * 2) / width && out32[p + x * 2] != param.colorARGB[0])
                            && (out32[p + x] != param.colorARGB[0])
                            && (out32[p + y * 2 * width] != param.colorARGB[0])
                            && (out32[p + y * width] != param.colorARGB[0])) {
                        out32[np] = c;
                        stack.push(np);
                    }
                } catch (IndexOutOfBoundsException ex) {
                    //edge
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast evenEdges() {
        return new FilterFast("Evened", bgcol) {

            @Override
            protected void processor() {
                IMG.copyPixels(in32, out32);
                Iterate.pixels(this, (int p) -> {
                    if (in32[p] != param.colorARGB[0]) {
                        int hit = 0;
                        hit += isOfColor(in32, p, width, 0, 1, param.colorARGB[0]) ? 1 : 0;
                        hit += isOfColor(in32, p, width, 0, -1, param.colorARGB[0]) ? 1 : 0;
                        hit += isOfColor(in32, p, width, 1, 0, param.colorARGB[0]) ? 1 : 0;
                        hit += isOfColor(in32, p, width, -1, 0, param.colorARGB[0]) ? 1 : 0;
                        if (hit == 3) {
                            out32[p] = param.colorARGB[0];
                        }
                    }
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast sharpenEdges() {
        return new FilterFast("Sharpened", bgcol) {

            @Override
            protected void processor() {
                Iterate.pixels(this, (int p) -> {
                    boolean ch = false;
                    if (in32[p] != param.colorARGB[0]) {
                        int hit = 0;
                        hit += isOfColor(in32, p, width, 0, 1, param.colorARGB[0]) ? 1 : 0;
                        hit += isOfColor(in32, p, width, 0, -1, param.colorARGB[0]) ? 1 : 0;
                        if (hit > 1) {
                            ch = true;
                        } else {
                            hit += isOfColor(in32, p, width, 1, 0, param.colorARGB[0]) ? 1 : 0;
                            if (hit > 1) {
                                ch = true;
                            } else {
                                hit += isOfColor(in32, p, width, -1, 0, param.colorARGB[0]) ? 1 : 0;
                                if (hit > 1) {
                                    ch = true;
                                }
                            }
                        }
                    }
                    out32[p] = ch ? param.colorARGB[0] : in32[p];
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }
    
    
    public static FilterFast smoothenCorners() {
        return new FilterFast("Smoothed", bgcol) {
            @Override
            protected void processor() {
                System.arraycopy(in32, 0, out32, 0, inData.totalPixels());
                Iterate.pixels(this, (int pos) -> {
                    int c = in32[pos];
                    if (c != param.colorARGB[0]) {
                        smoothPixel(in32, out32, pos);
                    }
                });
            }

            private void smoothPixel(int[] in, int[] out, int pos) {
                int[][] hits = new int[3][3];
                for (int x = -1; x < 2; x++) {
                    for (int y = -1; y < 2; y++) {
                        try {
                            hits[x + 1][y + 1] = in[pos - x - (y * width)] != param.colorARGB[0] ? 1 : 0;
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            hits[x + 1][y + 1] = 0;
                        }
                    }
                }
                int[] ints = Arrays.stream(hits).map(v -> Arrays.stream(v).sum()).mapToInt(i -> i).toArray();
                int hitnumb = Arrays.stream(ints).sum();
                int emptys = (int) Arrays.stream(ints).filter(i -> i == 0).count();
                int partials = (int) Arrays.stream(ints).filter(i -> i == 2).count();
                if (hitnumb == 4 && emptys == 1 && partials == 2) {
                    out[pos] = param.colorARGB[0];
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    public static FilterFast dotConnectRemove() {
        return new FilterFast("Dot Remover",
                new ControlReference[]{
                    new ControlReference(COLOUR, "Background colour", -2),
                    new ControlReference(TOGGLE, "Iterate until no change")}) {
            boolean nope;
            int[] canvas;

            @Override
            protected void processor() {
                nope = true;
                canvas = in32;
                while (nope) {
                    nope = false;
                    Iterate.pixels(this, (int pos) -> {
                        out32[pos] = canvas[pos];
                        if (canvas[pos] != param.colorARGB[0]) {
                            try {
                                if (canvas[pos - 1] == param.colorARGB[0] && canvas[pos + 1] == param.colorARGB[0]) {
                                    out32[pos] = param.colorARGB[0];
                                    nope = true;
                                }
                            } catch (IndexOutOfBoundsException ex) {
                                //already set
                            }
                            try {
                                if (canvas[pos - width] == param.colorARGB[0] && canvas[pos + width] == param.colorARGB[0]) {
                                    out32[pos] = param.colorARGB[0];
                                    nope = true;
                                }
                            } catch (IndexOutOfBoundsException ex) {
                                //already set
                            }
                        }
                    });
                    canvas = out32;
                    if (!param.toggle[0]) {
                        nope = false;
                    }
                }
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast dotRemove() {
        return new FilterFast("Dot Remover", bgcol) {

            @Override
            protected void processor() {
                Iterate.pixels(this, (int p) -> {
                    out32[p] = in32[p];
                    if (in32[p] != param.colorARGB[0]) {
                        if (isOfColor(in32, p, width, -1, 0, param.colorARGB[0])
                                && isOfColor(in32, p, width, 1, 0, param.colorARGB[0])
                                && isOfColor(in32, p, width, 0, -1, param.colorARGB[0])
                                && isOfColor(in32, p, width, 0, 1, param.colorARGB[0])
                                && isOfColor(in32, p, width, -1, -1, param.colorARGB[0])
                                && isOfColor(in32, p, width, 1, 1, param.colorARGB[0])
                                && isOfColor(in32, p, width, 1, -1, param.colorARGB[0])
                                && isOfColor(in32, p, width, -1, 1, param.colorARGB[0])) {
                            out32[p] = param.colorARGB[0];
                        }
                    }
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static FilterFast maximumDiffEdge() {
        return new FilterFast("Maximum Difference", new ControlReference[]{
            new ControlReference(COMBO, new String[]{"Brightness", "Saturation", "Hue"}, "Calculate the difference in", 0),
            new ControlReference(SPINNER, "Radius (px)", 2),
            new ControlReference(TOGGLE, "Binarize", 0, new int[]{3, 1}),
            new ControlReference(SLIDER, new Object[]{0, 255}, "Minimum difference")}) {
            @Override
            protected void processor() {
                int r = param.spinner[0];
                Tonga.iteration();
                Iterate.pixels(this, (int pos) -> {
                    int[] bs = edgeKernel(in32, pos, width, r);
                    int max = 0, min = 255;
                    switch (param.combo[0]) {
                        case 0:
                            for (int i = 0; i < bs.length; i++) {
                                bs[i] = RGB.brightness(bs[i]);
                                max = Math.max(max, bs[i]);
                                min = Math.min(min, bs[i]);
                            }
                            break;
                        case 1:
                            for (int i = 0; i < bs.length; i++) {
                                bs[i] = (int) (RGB.saturation(bs[i]) * 255);
                                max = Math.max(max, bs[i]);
                                min = Math.min(min, bs[i]);
                            }
                            break;
                        case 2:
                            for (int i = 0; i < bs.length; i++) {
                                bs[i] = (int) (RGB.hue(bs[i]) * 255);
                                max = Math.max(max, bs[i]);
                                min = Math.min(min, bs[i]);
                            }
                            break;
                    }
                    int dif = max - min;
                    if (!param.toggle[0]) {
                        out32[pos] = RGB.argb(dif);
                    } else {
                        int out = dif < param.slider[0] ? COL.BLACK : COL.WHITE;
                        out32[pos] = RGB.argb(out);
                    }
                });
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    /* GENERAL SHARED FUNCTIONS */
    private static boolean isOfColor(int[] in32, int p, int width, int x, int y, int c) {
        int np = p + x + y * width;
        int w = p / width;
        try {
            return (w == (p + x) / width) && np < in32.length && np >= 0
                    && in32[np] == c;
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
    }

    private static int[] edgeKernel(int[] in32, int pos, int width, int r) {
        /*
        collect the neighbouring values
        omitting the 4 furthest edges:
        
          * * * * *
        * * * * * * *
        * * * * * * *
        * * * * * * *
        * * * * * * *
        * * * * * * *
          * * * * *
        
         */
        int b = in32[pos];
        int s = (r * 2 + 1) * (r * 2 + 1) - 4;
        int[] bs = new int[s < 1 ? 1 : s];
        int c = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                if (Math.abs(x) != Math.abs(y) || Math.abs(x) != r) {
                    bs[c] = b;
                    if (pos / width == (pos + x) / width) {
                        try {
                            bs[c] = in32[pos + x + width * y];
                        } catch (IndexOutOfBoundsException ex) {
                            //nothing
                        }
                    }
                    c++;
                }
            }
        }
        return bs;
    }
}
