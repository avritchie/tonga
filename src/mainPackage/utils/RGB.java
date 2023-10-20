package mainPackage.utils;

public class RGB {

    public static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int argbAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | alpha << 24;
    }

    public static int averageColor(int[] colors) {
        int rr = 0, gg = 0, bb = 0, aa = 0;
        double s = (double) colors.length;
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            aa += (color >> 24) & 0xFF;
            rr += (color >> 16) & 0xFF;
            gg += (color >> 8) & 0xFF;
            bb += color & 0xFF;
        }
        return argb((int) (rr / s), (int) (gg / s), (int) (bb / s), (int) (aa / s));
    }

    public static double lightness(int color) {
        //output as 0-1
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return (max + min) / 510.0;
    }

    public static int brightness(int color) {
        //identical to Color.getBrightness(), output as 0-255
        return Math.max(Math.max(color >> 16 & 0xFF, color >> 8 & 0xFF), color & 0xFF);
    }

    public static double distance(int color, int color2) {
        int r1 = (color >> 16) & 0xFF;
        int g1 = (color >> 8) & 0xFF;
        int b1 = color & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        return Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));
    }

    public static double relativeLuminance(int color) {
        //identical to Color.grayscale(), output as 0-1
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
    }

    public static double saturation(int color) {
        //output as 0-1
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return max == 0 ? 0 : (max - min) / (double) max;
    }

    public static double hue(int color) {
        //output as 0-1
        double r = ((color >> 16) & 0xFF) / 255.;
        double g = ((color >> 8) & 0xFF) / 255.;
        double b = (color & 0xFF) / 255.;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        if (max - min == 0) {
            return 0;
        }
        double h = 0;
        if (r >= g && r >= b) {
            h = (g - b) / (max - min);
        }
        if (g >= r && g >= b) {
            h = 2.0 + (b - r) / (max - min);
        }
        if (b >= r && b >= g) {
            h = 4.0 + (r - g) / (max - min);
        }
        if (h < 0) {
            h += 6;
        }
        return h / 6.;
    }

    public static int maxColour(int color) {
        //takes the colour and maximizes it so that the brightness is always 255
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (r == 0 && g == 0 && b == 0) {
            return 0xFF << 16 | 0xFF << 8 | 0xFF | a << 24;
        }
        double f = 255. / Math.max(r, Math.max(g, b));
        return ((int) (r * f)) << 16 | ((int) (g * f)) << 8 | ((int) (b * f)) | a << 24;
    }

    public static int substractFlip(int color, int value) {
        // alle 0 --> 255
        return color - (value << 16 | value << 8 | value);
    }

    public static int substractLimit(int color, int value) {
        // alle 0 --> 0
        return Math.max(0, (color & 0xff) - value)
                | Math.max(0, (color >> 8 & 0xff) - value) << 8
                | Math.max(0, (color >> 16 & 0xff) - value) << 16
                | Math.max(0, (color >> 24 & 0xff) - value) << 24;
    }

    public static int multiplyColor(int color, double factor) {
        //all channels
        return (multiply(color & 0xff, factor))
                | (multiply(color >> 8 & 0xff, factor) << 8)
                | (multiply(color >> 16 & 0xff, factor) << 16)
                | (multiply(color >> 24 & 0xff, factor) << 24);
    }

    public static int multiply(int color, double factor) {
        //single channel
        return (Math.max(0, Math.min(255, (int) (color * factor))));
    }

    public static int lowcut(int c, double upper, double lower) {
        double fs = upper;
        double ms = lower * fs;
        double g = fs - ms;
        double l = RGB.lightness(c);
        if (upper > lower) {
            if (l <= fs) {
                if (l >= ms) {
                    c = (int) ((l - ms) / g * fs * 255);
                } else {
                    c = 0;
                }
            } else {
                c = (int) (fs * 255);
            }
        } else {
            return (int) l * 255;
        }
        return c;
    }

    public static int hicut(int c, double lower, double upper) {
        double fs = lower;
        double ms = fs + ((1 - fs) * upper);
        double g = ms - fs;
        double l = RGB.lightness(c);
        if (upper > lower) {
            if (l >= fs) {
                if (l <= ms) {
                    c = (int) ((((1 - fs) * ((l - fs) / g)) + fs) * 255);
                } else {
                    c = 255;
                }
            } else {
                c = (int) (fs * 255);
            }
        } else {
            return (int) l * 255;
        }
        return c;
    }

    public static int levels(int c, int u, int l) {
        // same as gimp levels, u & l ranges 0-255
        int a = cut((c >> 16) & 0xFF, u, l) << 16
                | cut((c >> 8) & 0xFF, u, l) << 8
                | cut(c & 0xFF, u, l)
                | ((c >> 24) & 0xFF) << 24;
        return a;
    }

    public static int cut(int c, int upper, int lower) {
        if (c <= lower) {
            return 0;
        } else if (c >= upper) {
            return 255;
        } else {
            double range = upper - lower;
            return (int) (((c - lower) / range) * 255);
        }
    }

    public static int argb(int b, int a) {
        return (int) b << 16 | b << 8 | b | a << 24;
    }

    public static int argb(int c) {
        return (int) c << 16 | c << 8 | c | 0xff << 24;
    }

    public static int argb(int r, int g, int b, int a) {
        return (int) r << 16 | g << 8 | b | a << 24;
    }

    public static int argb(int r, int g, int b) {
        return argb(r, g, b, 0xFF);
    }

    public static int argbColored(int b, int c, int a) {
        double rf = (c >> 16 & 0xFF) / 255.;
        double gf = (c >> 8 & 0xFF) / 255.;
        double bf = (c & 0xFF) / 255.;
        return (int) (b * rf) << 16 | (int) (b * gf) << 8 | (int) (b * bf) | a << 24;
    }

    public static int argbColored(int b, int c) {
        return argbColored(b, c, 0xFF);
    }

    public static int rgbc(int b, int c) {
        return argbColored(b, (int) (RGB.maxColour(c)));
    }

    public static int argbc(int b, int c) {
        return argbColored(b, (int) (RGB.maxColour(c)), RGB.alpha(c));
    }

    public static int argb(short intensity, int colour, int max, int min) {
        int bf = colour & 255;
        int gf = colour >> 8 & 255;
        int rf = colour >> 16 & 255;
        double l = max - min == 0 ? 1 : max - min;
        int s = intensity & 65535;
        double v = min > s ? 0 : (s - min) / l;
        v = v > 1 ? 1 : v;
        int b = (int) (v * bf);
        int g = (int) (v * gf);
        int r = (int) (v * rf);
        return -16777216 | r << 16 | g << 8 | b;
    }
}
