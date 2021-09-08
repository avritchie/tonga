package mainPackage.utils;

import java.util.Arrays;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import mainPackage.CachedImage;
import mainPackage.ImageData;
import mainPackage.Tonga;

public class IMG {

    public static int[] copyArray(int[] original) {
        // DO NOT USE TO COPY THE ORIGINAL IMAGE TO THE DESTINATION CANVAS - DOES NOT WORK
        return Arrays.copyOf(original, original.length);
    }

    public static short[] copyArray(short[] original) {
        // DO NOT USE TO COPY THE ORIGINAL IMAGE TO THE DESTINATION CANVAS - DOES NOT WORK
        return Arrays.copyOf(original, original.length);
    }

    public static void copyPixels(int[] original, int[] destination) {
        System.arraycopy(original, 0, destination, 0, original.length);
    }

    public static void copyPixels(short[] original, short[] destination) {
        System.arraycopy(original, 0, destination, 0, original.length);
    }

    public static void fillArray(int[] array, int width, int height, int value) {
        for (int i = 0; i < width; i++) {
            array[i] = value;
        }
        for (int i = 1; i < height; i++) {
            System.arraycopy(array, 0, array, width * i, width);
        }
    }

    public static WritableImage copyImage(Image original) {
        return new WritableImage(original.getPixelReader(), (int) original.getWidth(), (int) original.getHeight());
    }

    public static void copyImage(ImageData original, WritableImage destination) {
        copyImage(original.toImage().getFXImage(), destination);
    }

    public static void copyImage(ImageData[] original, WritableImage destination) {
        copyImage(original[0], destination);
    }

    public static void copyImage(CachedImage original, WritableImage destination) {
        copyImage(original.getFXImage(), destination);
    }

    public static void copyImage(Image original, WritableImage destination) {
        Tonga.iteration();
        PixelWriter pxw = destination.getPixelWriter();
        PixelReader pxr = original.getPixelReader();
        for (int x = 0; x < original.getWidth(); x++) {
            Tonga.loader().appendProgress(1. / original.getWidth());
            for (int y = 0; y < original.getHeight(); y++) {
                pxw.setArgb(x, y, pxr.getArgb(x, y));
            }
        }
    }

    public static Image[] datasToImages(ImageData[] img) {
        Image[] imgs = new Image[img.length];
        for (int i = 0; i < img.length; i++) {
            imgs[i] = img[i].toFXImage();
        }
        return imgs;
    }

    public static CachedImage[] imagesToCaches(Image[] img) {
        CachedImage[] imgs = new CachedImage[img.length];
        for (int i = 0; i < img.length; i++) {
            imgs[i] = new CachedImage(SwingFXUtils.fromFXImage(img[i], null));
        }
        return imgs;
    }

}
