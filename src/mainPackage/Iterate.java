package mainPackage;

import mainPackage.filters.Filter;
import mainPackage.morphology.ROI;

/**
 *
 * @author Victoria
 */
public class Iterate {

    public static void pixels(ImageData id, Iterator i) {
        Tonga.iteration();
        for (int y = 0; y < id.height; y++) {
            for (int x = 0; x < id.width; x++) {
                i.iterate(y * id.width + x);
            }
            Tonga.loader().appendProgress(id.height);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    public static void pixels(Filter f, Iterator i) {
        Tonga.iteration();
        for (int y = 0; y < f.height; y++) {
            for (int x = 0; x < f.width; x++) {
                i.iterate(y * f.width + x);
            }
            Tonga.loader().appendProgress(f.height);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    public static void areaPixels(ROI r, Iterator i) {
        for (int y = 0; y < r.area.height; y++) {
            for (int x = 0; x < r.area.width; x++) {
                if (r.area.area[x][y]) {
                    i.iterate(r.originalImage.width * (y + r.area.ystart) + (x + r.area.xstart));
                }
            }
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    public interface Iterator {

        void iterate(int p);

    }
}
