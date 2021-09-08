package mainPackage.filters;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import static mainPackage.filters.Filter.bgcol;

public class FilterCrisps {

    public static FilterOld run() {
        return new FilterOld("Even out",bgcol) {

            @Override
            protected void processor(Image sourceImage, WritableImage canvasImage) {
                iteratePixels((PixelReader pxRead, PixelWriter pxWrit, int x, int y) -> {
                    pxWrit.setColor(x, y, pxRead.getColor(x, y));
                }, sourceImage, canvasImage);
                iteratePixels((PixelReader pxRead, PixelWriter pxWrit, int x, int y) -> {
                    Color c = pxRead.getColor(x, y);
                    if (!c.equals(param.color[0])) {
                        consider(pxRead, pxWrit, x, y);
                    }
                }, sourceImage, canvasImage);
            }

            private void consider(PixelReader rdR, PixelWriter wrR, int x, int y) {
                int hit = 0;
                try {
                    if (rdR.getColor(x - 1, y).equals(param.color[0])) {
                        hit++;
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
                try {
                    if (rdR.getColor(x + 1, y).equals(param.color[0])) {
                        hit++;
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
                try {
                    if (rdR.getColor(x, y - 1).equals(param.color[0])) {
                        hit++;
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
                try {
                    if (rdR.getColor(x, y + 1).equals(param.color[0])) {
                        hit++;
                    }
                } catch (IndexOutOfBoundsException ex) {
                }
                if (hit == 3) {
                    wrR.setColor(x, y, param.color[0]);
                }
            }
        };
    }
}
