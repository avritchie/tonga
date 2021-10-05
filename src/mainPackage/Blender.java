package mainPackage;

import javafx.scene.effect.BlendMode;
import mainPackage.Blender.Blend;
import mainPackage.utils.COL;
import mainPackage.utils.RGB;

public class Blender {

    public static ImageData renderOverlay(ImageData[] layersarray) {
        int[] dim = TongaRender.getMaxDim(layersarray);
        ImageData temp = new ImageData(dim[0], dim[1]);
        for (int y = 0; y < temp.height; y++) {
            for (int x = 0; x < temp.width; x++) {
                for (ImageData la : layersarray) {
                    double alpha = la.alpha ? 1.0 / layersarray.length : 1.0;
                    int c1 = temp.pixels32[y * temp.width + x];
                    int c2 = la.pixels32[y * la.width + x];
                    temp.pixels32[y * temp.width + x] = COL.blendColorAlphaAlpha(c1, c2, alpha);
                }
            }
        }
        return temp;
    }

    public static ImageData renderOverlay2(ImageData[] layersarray) {
        int[] dim = TongaRender.getMaxDim(layersarray);
        ImageData temp = new ImageData(dim[0], dim[1]);
        boolean ass = TongaRender.allSameSize(layersarray);
        boolean onlyalpha = true;
        int ll = 0;
        for (int i = 0; i < layersarray.length; i++) {
            if (!layersarray[i].alpha) {
                ll = i;
                onlyalpha = false;
            }
        }
        int lc = layersarray.length - ll;
        double af = 1. / lc;
        for (int y = 0; y < temp.height; y++) {
            for (int x = 0; x < temp.width; x++) {
                double r = 0;
                double g = 0;
                double b = 0;
                double a = 0;
                for (int i = ll; i < layersarray.length; i++) {
                    int val = layersarray[i].pixels32[y * layersarray[i].width + x];
                    double al = (val >> 24 & 0xFF) / 255.;
                    r += af * al * (val >> 16 & 0xFF);
                    g += af * al * (val >> 8 & 0xFF);
                    b += af * al * (val & 0xFF);
                    a += al;
                }
                double am = lc / a;
                r = am * r;
                g = am * g;
                b = am * b;
                temp.pixels32[y * temp.width + x] = RGB.argb((int) r, (int) g, (int) b,
                        !onlyalpha ? 255 : (int) (255 * (1 - Math.pow(0.5, lc))));
            }
        }
        return temp;
    }

    public static ImageData renderBlend(ImageData img1, ImageData img2, Blend mode) {
        return Blender.renderBlend(new ImageData[]{img1, img2}, mode);
    }

    public static ImageData renderBlend(ImageData img1, ImageData img2) {
        return Blender.renderBlend(new ImageData[]{img1, img2}, Blend.ADD);
    }

    public static ImageData renderBlend(ImageData[] layersarray) {
        return Blender.renderBlend(layersarray, Blend.ADD);
    }

    public static ImageData renderBlend(ImageData[] layersarray, Blend mode) {
        switch (mode) {
            case ADD:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        r += val >> 16 & 0xFF;
                        g += val >> 8 & 0xFF;
                        b += val & 0xFF;
                    }

                    @Override
                    public void get() {
                        r = Math.min(255, r);
                        g = Math.min(255, g);
                        b = Math.min(255, b);
                        a = Math.min(255, a);
                    }
                }.blend(layersarray, false);
            case SUBTRACT:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        r -= val >> 16 & 0xFF;
                        g -= val >> 8 & 0xFF;
                        b -= val & 0xFF;
                    }

                    @Override
                    public void get() {
                        r = Math.max(0, r);
                        g = Math.max(0, g);
                        b = Math.max(0, b);
                    }
                }.blend(layersarray, false);
            case DIFFERENCE:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        r = Math.abs(r - (val >> 16 & 0xFF));
                        g = Math.abs(g - (val >> 8 & 0xFF));
                        b = Math.abs(b - (val & 0xFF));
                    }
                }.blend(layersarray, false);
            case MULTIPLY:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        rd *= (val >> 16 & 0xFF) / 255.;
                        gd *= (val >> 8 & 0xFF) / 255.;
                        bd *= (val & 0xFF) / 255.;
                        ad *= (val >> 24 & 0xFF) / 255.;
                    }
                }.blend(layersarray, true);
            case MAXIMUM:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        r = Math.max(r, val >> 16 & 0xFF);
                        g = Math.max(g, val >> 8 & 0xFF);
                        b = Math.max(b, val & 0xFF);
                        a = Math.max(a, val >> 24 & 0xFF);
                    }
                }.blend(layersarray, false);
            case MINIMUM:
                return new BlendOperation() {
                    @Override
                    public void blend(int val) {
                        r = Math.min(r, val >> 16 & 0xFF);
                        g = Math.min(g, val >> 8 & 0xFF);
                        b = Math.min(b, val & 0xFF);
                        a = Math.min(a, val >> 24 & 0xFF);
                    }
                }.blend(layersarray, false);
        }
        return null;
    }

    private abstract static class BlendOperation {

        int r, g, b, a;
        double rd, gd, bd, ad;

        public abstract void blend(int val);

        private void set(int p, int[] arr, boolean doubles) {
            if (doubles) {
                rd = arr[p] >> 16 & 0xFF;
                gd = arr[p] >> 8 & 0xFF;
                bd = arr[p] & 0xFF;
                ad = arr[p] >> 24 & 0xFF;
            } else {
                r = arr[p] >> 16 & 0xFF;
                g = arr[p] >> 8 & 0xFF;
                b = arr[p] & 0xFF;
                a = arr[p] >> 24 & 0xFF;
            }
        }

        public void get() {
        }

        private int ret(boolean doubles) {
            get();
            return doubles ? RGB.argb((int) rd, (int) gd, (int) bd, (int) ad) : RGB.argb(r, g, b, a);
        }

        public ImageData blend(ImageData[] layersarray, boolean doubles) {
            int[] dim = TongaRender.getMaxDim(layersarray);
            ImageData temp = new ImageData(dim[0], dim[1]);
            boolean ass = TongaRender.allSameSize(layersarray);
            if (ass) {
                for (int y = 0; y < temp.height; y++) {
                    for (int x = 0; x < temp.width; x++) {
                        int p = y * temp.width + x;
                        set(p, layersarray[0].pixels32, doubles);
                        for (int i = 1; i < layersarray.length; i++) {
                            blend(layersarray[i].pixels32[p]);
                        }
                        temp.pixels32[p] = ret(doubles);
                    }
                }
            } else {
                for (int y = 0; y < temp.height; y++) {
                    for (int x = 0; x < temp.width; x++) {
                        set(y * layersarray[0].width + x, layersarray[0].pixels32, doubles);
                        for (int i = 1; i < layersarray.length; i++) {
                            blend(layersarray[i].pixels32[y * layersarray[i].width + x]);
                        }
                        temp.pixels32[y * temp.width + x] = ret(doubles);
                    }
                }
            }
            return temp;
        }
    }

    public enum Blend {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIFFERENCE,
        MAXIMUM,
        MINIMUM
    }

    public static Blend modeBridge(BlendMode bm) {
        switch (bm) {
            case ADD:
                return Blend.ADD;
            case MULTIPLY:
                return Blend.MULTIPLY;
            case DIFFERENCE:
                return Blend.DIFFERENCE;
            case LIGHTEN:
                return Blend.MAXIMUM;
            case DARKEN:
                return Blend.MINIMUM;
        }
        return Blend.ADD;
    }
}
