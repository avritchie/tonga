package mainPackage;

import mainPackage.filters.Filter;
import mainPackage.morphology.ROI;
import mainPackage.utils.COL;

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
        pixels(f, 1, i);
    }

    public static void pixels(Filter f, double iters, Iterator i) {
        Tonga.iteration((int) iters);
        //the filter is only executed on the given pixels
        if (f.conditional) {
            for (int y = 0; y < f.height; y++) {
                for (int x = 0; x < f.width; x++) {
                    int p = y * f.width + x;
                    if (f.conditionalPixels[p] != f.conditionalColor) {
                        i.iterate(p);
                    }
                }
                Tonga.loader().appendProgress(iters / f.height);
                if (Tonga.loader().getTask().isInterrupted()) {
                    return;
                }
            }
            //or just run the filter normally
        } else {
            for (int y = 0; y < f.height; y++) {
                for (int x = 0; x < f.width; x++) {
                    i.iterate(y * f.width + x);
                }
                Tonga.loader().appendProgress(iters / f.height);
                if (Tonga.loader().getTask().isInterrupted()) {
                    return;
                }
            }
        }
    }

    public static void edgePixels(Filter f, Iterator i) {
        for (int y = 0; y < f.height; y++) {
            for (int x = 0; x < f.width; x += f.width - 1) {
                i.iterate(y * f.width + x);
            }
        }
        for (int y = 0; y < f.height; y += f.height - 1) {
            for (int x = 1; x < f.width - 1; x++) {
                i.iterate(y * f.width + x);
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
