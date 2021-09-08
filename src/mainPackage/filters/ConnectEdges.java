package mainPackage.filters;

import java.awt.Point;
import java.util.Stack;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import static mainPackage.filters.Filter.bgcol;

public class ConnectEdges {

    public static FilterOld run() {
        return new FilterOld("Parsed",bgcol) {
            Stack<Point> recursiveStack;
            Color gC;
            PixelReader rdR;
            PixelWriter wrR;

            @Override
            protected void processor(Image sourceImage, WritableImage canvasImage) {
                recursiveStack = new Stack<>();
                rdR = canvasImage.getPixelReader();
                wrR = canvasImage.getPixelWriter();
                iteratePixels((PixelReader pxRead, PixelWriter pxWrit, int x, int y) -> {
                    pxWrit.setColor(x, y, pxRead.getColor(x, y));
                }, sourceImage, canvasImage);
                iteratePixels((PixelReader pxRead, PixelWriter pxWrit, int x, int y) -> {
                if (!rdR.getColor(x, y).equals(param.color[0])) {
                    gC = rdR.getColor(x, y);
                    parseGap(rdR, wrR, x, y, 0, -1);
                    parseGap(rdR, wrR, x, y, 0, 1);
                    parseGap(rdR, wrR, x, y, -1, 0);
                    parseGap(rdR, wrR, x, y, 1, 0);
                    parseCorner(rdR, wrR, x, y);
                }
                }, sourceImage, canvasImage);
            }

            private void parseGap(PixelReader r, PixelWriter w, int x, int y, int xx, int yy) {
                try {
                    if (!r.getColor(x, y).equals(param.color[0])) {
                        if (r.getColor(x + xx, y + yy).equals(param.color[0])) {
                            if ((!r.getColor(x + xx * 2, y + yy * 2).equals(param.color[0])) || ((!r.getColor(x + xx * 3, y + yy * 3).equals(param.color[0])
                                    && (!r.getColor(x + (xx == 0 ? 1 : xx), y + (yy == 0 ? 1 : yy)).equals(param.color[0])
                                    || !r.getColor(x + (xx == 0 ? -1 : xx), y + (yy == 0 ? -1 : yy)).equals(param.color[0]))))) {
                                w.setColor(x + xx, y + yy, gC);
                                recursiveStack.push(new Point(x - xx, y - yy));
                                recursiveStack.push(new Point(x + xx, y + yy));
                                recursiveStack.push(new Point(x, y));
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
            }

            private void parseCorner(PixelReader r, PixelWriter w, int x, int y) {
                try { // UPRIGHT
                    if (r.getColor(x + 1, y - 1).equals(param.color[0])
                            && (!r.getColor(x + 2, y).equals(param.color[0]) && !r.getColor(x + 1, y).equals(param.color[0])
                            && (!r.getColor(x, y - 2).equals(param.color[0]) && !r.getColor(x, y - 1).equals(param.color[0])))) {
                        w.setColor(x + 1, y - 1, gC);
                        recursiveStack.push(new Point(x + 1, y - 1));
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
                try { // UPLEFT
                    if (r.getColor(x - 1, y - 1).equals(param.color[0])
                            && (!r.getColor(x - 2, y).equals(param.color[0]) && !r.getColor(x - 1, y).equals(param.color[0])
                            && (!r.getColor(x, y - 2).equals(param.color[0]) && !r.getColor(x, y - 1).equals(param.color[0])))) {
                        w.setColor(x - 1, y - 1, gC);
                        recursiveStack.push(new Point(x - 1, y - 1));
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
                try { // DOWNRIGHT
                    if (r.getColor(x + 1, y + 1).equals(param.color[0])
                            && (!r.getColor(x + 2, y).equals(param.color[0]) && !r.getColor(x + 1, y).equals(param.color[0])
                            && (!r.getColor(x, y + 2).equals(param.color[0]) && !r.getColor(x, y + 1).equals(param.color[0])))) {
                        w.setColor(x + 1, y + 1, gC);
                        recursiveStack.push(new Point(x + 1, y + 1));
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
                try { // DOWNLEFT
                    if (r.getColor(x - 1, y + 1).equals(param.color[0])
                            && (!r.getColor(x - 2, y).equals(param.color[0]) && !r.getColor(x - 1, y).equals(param.color[0])
                            && (!r.getColor(x, y + 2).equals(param.color[0]) && !r.getColor(x, y + 1).equals(param.color[0])))) {
                        w.setColor(x - 1, y + 1, gC);
                        recursiveStack.push(new Point(x - 1, y + 1));
                    }
                } catch (IndexOutOfBoundsException exp) {
                }
            }
        };
    }
}
