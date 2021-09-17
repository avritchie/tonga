package mainPackage.morphology;

import java.awt.Point;
import mainPackage.TongaLayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import mainPackage.CachedImage;
import mainPackage.ImageData;
import mainPackage.Tonga;
import mainPackage.utils.COL;

public class ImageTracer {

    private ImageData image;
    private int bgColor;
    private boolean[] assigned;

    interface checkPosition {

        void check(int x, int y);
    }

    interface evaluatePixel {

        boolean evaluate(int p);
    }

    public ImageTracer(TongaLayer img, Color bg) {
        this(img.layerImage, bg);
    }

    public ImageTracer(Image img, Color bg) {
        this(new ImageData(img), bg);
    }

    public ImageTracer(CachedImage img, Color bg) {
        this(new ImageData(img), bg);
    }

    public ImageTracer(ImageData img, Color bg) {
        this(img, COL.colorToARGBInt(bg));
    }

    public ImageTracer(ImageData img, int bg) {
        this.image = img;
        this.bgColor = bg;
        this.assigned = new boolean[image.totalPixels()];
    }

    public ROI traceSingleObjectAtPoint(int x, int y) {
        int p = image.width * y + x;
        if (image.pixels32[p] != bgColor) {
            Area area = traceArea(image.pixels32, bgColor, image.width, image.height, p);
            return new ROI(image, area);
        } else {
            Tonga.log.trace("Object not found at position {}.{}", x, y);
            return null;
        }
    }

    public ROISet traceInnerObjects(ROISet set) {
        List<ROI> foundObjects = new ArrayList<>();
        set.list.forEach(roi -> {
            Area outArea = roi.getOutArea();
            roi.innEdge.list.forEach(pnt -> {
                int i = findClosestNonAreaPixel(pnt, roi.area, outArea);
                if (i != -1 && !assigned[i]) {
                    Area area = traceArea(image.pixels32, bgColor, image.width, image.height, i);
                    foundObjects.add(new ROI(image, area));
                    markAreaAsAssigned(area);
                }
            });
        });
        return new ROISet(foundObjects, image.width, image.height);
    }

    public ROISet traceObjectsTouchingEdgeOf(ROI roi) {
        Tonga.iteration();
        List<ROI> foundObjects = new ArrayList<>();
        roi.outEdge.list.forEach(pnt -> {
            int i = image.width * pnt.y + pnt.x;
            if (image.pixels32[i] != (bgColor) && !assigned[i]) {
                Area area = traceArea(image.pixels32, bgColor, image.width, image.height, i);
                foundObjects.add(new ROI(image, area));
                markAreaAsAssigned(area);
            }
            if (i % image.width == 0) {
                Tonga.loader().appendProgress(1. / image.totalPixels() * image.width);
            }
        });
        return new ROISet(foundObjects, image.width, image.height);
    }

    public ROISet trace() {
        Tonga.iteration();
        List<ROI> foundObjects = new ArrayList<>();
        for (int i = 0; i < image.totalPixels(); i++) {
            if (image.pixels32[i] != (bgColor) && !assigned[i]) {
                Area area = traceArea(image.pixels32, bgColor, image.width, image.height, i);
                foundObjects.add(new ROI(image, area));
                markAreaAsAssigned(area);
            }
            if (i % image.width == 0) {
                Tonga.loader().appendProgress(1. / image.totalPixels() * image.width);
            }
        }
        return new ROISet(foundObjects, image.width, image.height);
    }

    private void markAreaAsAssigned(Area area) {
        for (int x = 0; x < area.area.length; x++) {
            for (int y = 0; y < area.area[0].length; y++) {
                int p = image.width * (area.ystart + y) + (area.xstart + x);
                if (assigned[p] || area.area[x][y]) {
                    assigned[p] = true;
                }
            }
        }
    }

    private int findClosestNonAreaPixel(Point pnt, Area areaIn, Area areaOut) {
        for (int xx = -1; xx <= 1; xx++) {
            for (int yy = -1; yy <= 1; yy++) {
                try {
                    if (!areaIn.area[pnt.x + xx][pnt.y + yy] && !areaOut.area[pnt.x + xx][pnt.y + yy]) {
                        return image.width * (areaIn.ystart + pnt.y + yy) + (areaIn.xstart + pnt.x + xx);
                    }
                } catch (ArrayIndexOutOfBoundsException ex) {
                }
            }
        }
        return -1;
    }

    protected static Area traceFalseArea(boolean[][] pixels, int width, int height, int startPosition) {
        evaluatePixel evaluate = (int p) -> {
            return pixels[p % width][p / width] == false;
        };
        return traceArea(evaluate, width, height, startPosition);
    }

    protected static Area traceArea(int[] pixels, int color, int width, int height, int startPosition) {
        evaluatePixel evaluate = (int p) -> {
            return pixels[p] != color;
        };
        return traceArea(evaluate, width, height, startPosition);
    }

    private static Area traceArea(evaluatePixel evaluate, int width, int height, int startPosition) {
        // alustus
        Stack<Integer> recursiveStack = new Stack<>();
        int maxX = 0, minX = width, maxY = 0, minY = height;
        boolean[][] area = new boolean[width][height];
        boolean[] assigned = new boolean[width * height];
        recursiveStack.push(startPosition);
        assigned[startPosition] = true;
        // tee arean sisältävä array
        while (!recursiveStack.isEmpty()) {
            int n = recursiveStack.pop();
            int x = n % width, y = n / width;
            area[x][y] = true;
            maxX = Math.max(maxX, x);
            minX = Math.min(minX, x);
            maxY = Math.max(maxY, y);
            minY = Math.min(minY, y);
            checkPosition check = (int xx, int yy) -> {
                int p = width * yy + xx;
                try {
                    if (!assigned[p] && !area[xx][yy] && evaluate.evaluate(p)) {
                        recursiveStack.push(p);
                        assigned[p] = true;
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
            };
            check.check(x + 1, y);
            check.check(x - 1, y);
            check.check(x, y + 1);
            check.check(x, y - 1);
        }
        // pakkaa objektiksi
        boolean[][] finalArea = new boolean[maxX - minX + 1][maxY - minY + 1];
        for (int j = minX; j <= maxX; j++) {
            finalArea[j - minX] = Arrays.copyOfRange(area[j], minY, maxY + 1);
        }
        return new Area(finalArea, minX, minY, startPosition % width, startPosition / width);
    }
}
