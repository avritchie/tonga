package mainPackage.filters;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import mainPackage.ImageData;
import mainPackage.PanelCreator.ControlReference;
import mainPackage.Tonga;

public abstract class FilterOld extends Filter {

    public FilterOld(String name, ControlReference[] params) {
        super(name, params);
    }

    public FilterOld(String name, ControlReference[] params, int iter) {
        super(name, params, iter);
    }

    @Override
    protected ImageData handleImage(Object imgs) {
        Image img = ((ImageData) imgs).toFXImage();
        WritableImage out = new WritableImage((int) img.getWidth(), (int) img.getHeight());
        processor(img, out);
        return new ImageData(out);
    }

    @Override
    protected void setDimensions(Object imgs) {
        ImageData imgg = (ImageData) imgs;
        width = imgg.width;
        height = imgg.height;
    }

    protected abstract void processor(Image img, WritableImage out);

    protected void iteratePixels(iterationLogic i, Image sourceImage, WritableImage canvasImage) {
        Tonga.iteration();
        PixelReader spr = sourceImage.getPixelReader();
        PixelWriter cpw = canvasImage.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                i.iterate(spr, cpw, x, y);
            }
            Tonga.loader().appendProgress(height);
            if (Tonga.loader().getTask().isInterrupted()) {
                return;
            }
        }
    }

    protected interface iterationLogic {

        void iterate(PixelReader pxRead, PixelWriter pxWrit, int x, int y);
    }

    @Override
    protected ImageData[] getIDArray(Object o) {
        return ((ImageData[]) o);
    }
}
