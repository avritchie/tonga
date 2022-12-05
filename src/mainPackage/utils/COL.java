package mainPackage.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import mainPackage.ImageData;
import mainPackage.MappedImage;

public class COL {

    final public static int RED = 0xFFFF0000;
    final public static int LRED = 0xFFFF8080;
    final public static int GREEN = 0xFF00FF00;
    final public static int LGREEN = 0xFF80FF80;
    final public static int BLUE = 0xFF0000FF;
    final public static int LBLUE = 0xFF8080FF;
    final public static int YELLOW = 0xFFFFFF00;
    final public static int LYELLOW = 0xFFFFFFC0;
    final public static int CYAN = 0xFF00FFFF;
    final public static int LCYAN = 0xFFC0FFFF;
    final public static int FUCHSIA = 0xFFFF00FF;
    final public static int LFUCHSIA = 0xFFFFC0FF;
    final public static int BLACK = 0xFF000000;
    final public static int WHITE = 0xFFFFFFFF;
    final public static int GRAY = 0xFF808080;
    final public static int LGRAY = 0xFFC0C0C0;
    final public static int DGRAY = 0xFF404040;
    final public static short UBLACK = (short) 0x0000;
    final public static short UGRAY = (short) 0x8080;
    final public static short UWHITE = (short) 0xFFFF;

    public static int comboColorSelector(int selection, int col) {
        // 0 = RED, 1 = GREEN, 2 = BLUE;
        switch (selection) {
            case 0:
                return (col >> 16) & 0xFF;
            case 1:
                return (col >> 8) & 0xFF;
            case 2:
                return (col) & 0xFF;
        }
        return 0;
    }

    public static Color layerCornerColour(MappedImage lim) {
        Integer[] c = new Integer[4];
        c[0] = lim.getRGB(0, 0);
        c[1] = lim.getRGB(0, lim.getHeight() - 1);
        c[2] = lim.getRGB(lim.getWidth() - 1, 0);
        c[3] = lim.getRGB(lim.getWidth() - 1, lim.getHeight() - 1);
        return new Color(selectMostCommonColour(c), true);
    }

    public static int dataCornerColour(ImageData id) {
        Integer[] c = new Integer[4];
        if (id.bits == 16) {
            c[0] = RGB.argb(id.pixels16[0], id.colour, id.max, id.min);
            c[1] = RGB.argb(id.pixels16[(id.height - 1) * id.width], id.colour, id.max, id.min);
            c[2] = RGB.argb(id.pixels16[(id.width - 1)], id.colour, id.max, id.min);
            c[3] = RGB.argb(id.pixels16[(id.height - 1) * id.width + id.width - 1], id.colour, id.max, id.min);
        } else {
            c[0] = id.pixels32[0];
            c[1] = id.pixels32[(id.height - 1) * id.width];
            c[2] = id.pixels32[(id.width - 1)];
            c[3] = id.pixels32[(id.height - 1) * id.width + id.width - 1];
        }
        return selectMostCommonColour(c);
    }

    private static Integer selectMostCommonColour(Integer[] colours) {
        return Arrays.stream(colours)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().max((o1, o2) -> o1.getValue().compareTo(o2.getValue()))
                .map(Map.Entry::getKey).orElse(null);
    }

    public static javafx.scene.paint.Color awt2FX(Color c) {
        return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
    }

    public static int colorToARGBInt(javafx.scene.paint.Color c) {
        return COL.FX2awt(c).getRGB();
    }

    public static javafx.scene.paint.Color ARGBintToColor(int c) {
        return javafx.scene.paint.Color.rgb(c >> 16 & 255, c >> 8 & 255, c & 255, (c >> 24 & 255) / 255.0);
    }

    public static Color FX2awt(javafx.scene.paint.Color c) {
        return new Color((int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), (int) (c.getOpacity() * 255));
    }

    public static int colorDec(int color, double rc, double gc, double bc) {
        //color deconvolution with 3 factors
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        double rr = 255 - Math.abs(r - (255 * rc));
        double gg = 255 - Math.abs(g - (255 * gc));
        double bb = 255 - Math.abs(b - (255 * bc));
        double c = RGB.saturation(color);
        int nn = (int) ((rr + gg + bb) / 3.0);
        return nn;
    }

    public static WritableImage turnArrayToImage(int[] out, int width, int height) {
        BufferedImage newimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        newimg.setRGB(0, 0, width, height, out, 0, width);
        return SwingFXUtils.toFXImage(newimg, null);
    }

    public static javafx.scene.paint.Color blendColor(javafx.scene.paint.Color c0, javafx.scene.paint.Color c1) {
        double r = (c0.getRed() + c1.getRed()) / 2.0;
        double g = (c0.getGreen() + c1.getGreen()) / 2.0;
        double b = (c0.getBlue() + c1.getBlue()) / 2.0;
        return new javafx.scene.paint.Color(r, g, b, 1);
    }

    public static Color blendColor(Color c0, Color c1) {
        double r = (c0.getRed() + c1.getRed()) / 2.0;
        double g = (c0.getGreen() + c1.getGreen()) / 2.0;
        double b = (c0.getBlue() + c1.getBlue()) / 2.0;
        return new Color((int) r, (int) g, (int) b, 255);
    }

    public static int blendColor(int c1, int c2) {
        int r1 = (c1 >> 16) & 255;
        int g1 = (c1 >> 8) & 255;
        int b1 = c1 & 255;
        int r2 = (c2 >> 16) & 255;
        int g2 = (c2 >> 8) & 255;
        int b2 = c2 & 255;
        int rf = (r1 + r2) / 2;
        int gf = (g1 + g2) / 2;
        int bf = (b1 + b2) / 2;
        return RGB.argb(rf, gf, bf);
    }

    public static int blendColorWeighted(int c1, int c2, double ratio) {
        int r1 = (c1 >> 16) & 255;
        int g1 = (c1 >> 8) & 255;
        int b1 = c1 & 255;
        int r2 = (c2 >> 16) & 255;
        int g2 = (c2 >> 8) & 255;
        int b2 = c2 & 255;
        int rf = (int) (r1 * ratio + r2 * (1 - ratio));
        int gf = (int) (g1 * ratio + g2 * (1 - ratio));
        int bf = (int) (b1 * ratio + b2 * (1 - ratio));
        return RGB.argb(rf, gf, bf);
    }

    public static int blendColorAlpha(int c1, int c2) {
        double a1 = ((c1 >> 24) & 255) / 255.;
        int r1 = (c1 >> 16) & 255;
        int g1 = (c1 >> 8) & 255;
        int b1 = c1 & 255;
        double a2 = ((c2 >> 24) & 255) / 255.;
        int r2 = (c2 >> 16) & 255;
        int g2 = (c2 >> 8) & 255;
        int b2 = c2 & 255;
        int af = (int) (255 * (a1 + a2 * (1 - a1)));
        double m1 = a1 * (1 / (a1 + a2) * a1);
        double m2 = a2 * (1 / (a1 + a2) * a2);
        int rf = (int) (r1 * m1 + r2 * m2);
        int gf = (int) (g1 * m1 + g2 * m2);
        int bf = (int) (b1 * m1 + b2 * m2);
        return RGB.argb(rf, gf, bf, af);
    }

    public static int blendColorAlphaAlpha(int c1, int c2, double alpha) {
        double a1 = ((c1 >> 24) & 255) / 255., a2 = ((c2 >> 24) & 255) / 255.;
        int r1 = (c1 >> 16) & 255, r2 = (c2 >> 16) & 255;
        int g1 = (c1 >> 8) & 255, g2 = (c2 >> 8) & 255;
        int b1 = c1 & 255, b2 = c2 & 255;
        double m2 = alpha * a2;
        double m1 = a1 * (1 - m2);
        int af = (int) (255 * (a1 + m2 * (1 - a1)));
        double mf = 1 / (m1 + m2);
        m2 = m2 * mf;
        m1 = m1 * mf;
        int rf = (int) (r1 * m1 + r2 * m2);
        int gf = (int) (g1 * m1 + g2 * m2);
        int bf = (int) (b1 * m1 + b2 * m2);
        return RGB.argb(rf, gf, bf, af);
    }
}
