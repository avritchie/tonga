package mainPackage.utils;

import java.awt.Point;
import mainPackage.ImageData;

public class DRAW {

    public static void redDot(ImageData drawhere, int x, int y) {
        int w = drawhere.width;
        int h = drawhere.height;
        drawhere.pixels32[y * w + x] = COL.WHITE;
        if (x > 0) {
            drawhere.pixels32[y * w + x - 1] = COL.RED;
        }
        if (w - x > 1) {
            drawhere.pixels32[y * w + x + 1] = COL.RED;
        }
        if (y > 0) {
            drawhere.pixels32[(y - 1) * w + x] = COL.RED;
        }
        if (h - y > 1) {
            drawhere.pixels32[(y + 1) * w + x] = COL.RED;
        }
    }

    public abstract static class lineDrawer {

        public boolean keepOnGoing;
        public Object returnData;

        public abstract void action(int x, int y);

        public Object drawLine(Point cur, Point comp) {
            keepOnGoing = true;
            int x1 = cur.x, x2 = comp.x, y1 = cur.y, y2 = comp.y;
            int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
            int dx2 = 2 * dx, dy2 = 2 * dy;
            int ix = x1 < x2 ? 1 : -1, iy = y1 < y2 ? 1 : -1;
            int d = 0, x = x1, y = y1;
            boolean mode = dx >= dy;
            while (keepOnGoing) {
                action(x, y);
                if (mode) {
                    if (x == x2) {
                        break;
                    }
                    x += ix;
                    d += dy2;
                    if (d > dx) {
                        y += iy;
                        d -= dx2;
                    }
                } else {
                    if (y == y2) {
                        break;
                    }
                    y += iy;
                    d += dx2;
                    if (d > dy) {
                        x += ix;
                        d -= dy2;
                    }
                }
            }
            return returnData();
        }

        public void abortLineDrawing() {
            keepOnGoing = false;
        }

        public Object returnData() {
            return null;
        }
    }
}
