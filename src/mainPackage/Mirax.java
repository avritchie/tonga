package mainPackage;

import MRXS.ExtractThread;
import MRXS.MRXSLevel;
import MRXS.MRXSSlide;
import java.util.List;
import static mainPackage.protocols.Processor.applyOperator;
import mainPackage.utils.COL;
import ome.units.UNITS;
import ome.units.quantity.Length;

public final class Mirax {

    public static ImageData renderPreview(MRXSSlide slide, ImageData thumbnail) {
        try {
            //render the preview merged
            int previewLevel = findLevel(slide, thumbnail.width, thumbnail.height);
            MRXSLevel level = new MRXSLevel(slide, previewLevel, 0);
            double[] scaling = level.getScaling();
            Length scale = new Length((scaling[0] + scaling[1]) / 2, UNITS.MICROMETER);
            int bgColor = level.getBgColor();
            int repBgColor = bgColor == COL.WHITE ? COL.WHITE : COL.BLACK;
            //log info
            Tonga.log.info("The level of the thumbnail is {}", previewLevel);
            Tonga.log.info("The scaling of the thumbnail is {}", scale.value().doubleValue());
            String path = Tonga.getTempPath();
            ExtractThread et = level.extractMergedTiles(0, path);
            et.get();
            List<Exception> except = et.exceptionList;
            if (!except.isEmpty()) {
                for (Exception ex : except) {
                    Tonga.catchError(ex, "Failed to render the preview.");
                }
                return null;
            }
            ImageData preview = IO.getImageDataFromFile(path + slide.slideName + "_level" + previewLevel + "_channel0.png");
            preview.ref.scale = scale;
            applyOperator(preview, preview, p -> preview.pixels32[p] == bgColor ? repBgColor : preview.pixels32[p]);
            return preview;
        } catch (Exception ex) {
            Tonga.catchError(ex, "Failed to render the preview.");
            return null;
        }
    }

    public static int findLevel(MRXSSlide sl, int width, int height) {
        int zls = sl.getZoomLevelCount();
        try {
            for (int z = zls - 1; z >= 0; z--) {
                MRXSLevel lv = new MRXSLevel(sl, z, 0);
                int[] res = lv.getResolution();
                if (res[0] >= width && res[1] >= height) {
                    return z;
                }
            }
        } catch (Exception ex) {
            Tonga.catchError(ex);
        }
        Tonga.catchError("Cannot find a zoom level matching the imported image.");
        return 0;
    }
}
