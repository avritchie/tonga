package mainPackage.filters;

import mainPackage.MappedImage;
import mainPackage.ImageData;
import mainPackage.Tonga;
import mainPackage.TongaLayer;
import static mainPackage.filters.Filter.noParams;
import mainPackage.morphology.ImageTracer;
import mainPackage.morphology.ROISet;
import mainPackage.utils.COL;

public class Test {

    public static FilterFast test() {
        return new FilterFast("Test", noParams) {
            @Override
            protected void processor() {
                ROISet set = new ImageTracer(inData, COL.BLACK).trace();
                set.getExtendedMasks(15);
                set.findOuterMaskEdges();
                outData.pixels32 = set.drawToImageData().pixels32;
            }

            @Override
            protected void processor16() {
                throw new UnsupportedOperationException("No 16-bit version available");
            }
        };
    }

    public static void run(MappedImage fximage) {
        int width = fximage.getWidth();
        int height = fximage.getHeight();
        int[] in = fximage.getRGB(0, 0, width, height, null, 0, width);
        int[] out = new int[in.length];
        long timeStart = System.nanoTime();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int c = in[y * width + x];
                float f = 1.0f;
                int a = (int) ((c >> 24) & 0xFF);
                int r = (int) (((c >> 16) & 0xFF) * f);
                int g = (int) (((c >> 8) & 0xFF) * f);
                int b = (int) ((c & 0xFF) * f);
                int w = Math.max(Math.max(Math.max(0, r), g), b);
                out[y * width + x] = (a << 24) | (w << 16) | (w << 8) | w;
            }
        }
        Tonga.log.debug("Test timing: {}", System.nanoTime() - timeStart);
        MappedImage newImage = new ImageData(out, width, height).toCachedImage();
        Tonga.injectNewLayer(newImage, "TESTI");
        Tonga.refreshLayerList();
    }
}
