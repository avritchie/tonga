package mainPackage;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import mainPackage.utils.IMG;

public class ImageData {

    public short[] pixels16;
    public int[] pixels32;
    public final int width;
    public final int height;
    public int bits;
    public int colour;
    public int min;
    public int max;
    public boolean alpha; //false = normal, true = ghost
    public String name;
    public final MappedImage ref;

    public ImageData(int[] bytes, int width, int height) {
        this(bytes, width, height, null);
    }

    public ImageData(int[] bytes, int width, int height, String name) {
        this.pixels32 = bytes;
        this.pixels16 = null;
        this.width = width;
        this.height = height;
        this.bits = 8;
        this.colour = 0;
        this.min = 0;
        this.max = 0;
        this.name = name;
        this.ref = null;
        Tonga.log.trace("Allocated a new int ImageData of {} bytes by {}", bytes.length * 4, Threader.getCaller(5));
    }

    public ImageData(short[] bytes, int width, int height) {
        this(bytes, width, height, null);
    }

    public ImageData(short[] bytes, int width, int height, String name) {
        this.pixels32 = null;
        this.pixels16 = bytes;
        this.width = width;
        this.height = height;
        this.bits = 16;
        this.colour = 0xFFFFFFFF;
        this.min = 0;
        this.max = 0xFFFF;
        this.name = null;
        this.ref = null;
        Tonga.log.trace("Allocated a new short ImageData of {} bytes by {}", bytes.length * 2, Threader.getCaller(5));
    }

    public ImageData(int width, int height) {
        this(new int[width * height], width, height);
    }

    public ImageData(int width, int height, String name) {
        this(new int[width * height], width, height, name);
    }

    public ImageData(Image img) {
        this(new MappedImage(SwingFXUtils.fromFXImage(img, null)));
    }

    public ImageData(TongaLayer layer) {
        this(layer.layerImage);
        this.alpha = layer.isGhost;
    }

    public ImageData(MappedImage img) {
        this(img, null);
    }

    public ImageData(Image img, String name) {
        this(new MappedImage(SwingFXUtils.fromFXImage(img, null)), name);
    }

    public ImageData(MappedImage img, String name) {
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.bits = img.bits;
        this.name = name;
        this.ref = img;
        this.colour = img.colour;
        this.min = img.min;
        this.max = img.max;
        setPixels();
    }

    public ImageData(ImageData img) {
        //creates a copy
        this.width = img.width;
        this.height = img.height;
        this.bits = img.bits;
        this.name = img.name;
        this.ref = null;
        this.colour = img.colour;
        this.min = img.min;
        this.max = img.max;
        this.pixels16 = img.pixels16 == null ? null : IMG.copyArray(img.pixels16);
        this.pixels32 = img.pixels32 == null ? null : IMG.copyArray(img.pixels32);
    }

    public int totalPixels() {
        return width * height;
    }

    public void name(String n) {
        this.name = n;
    }

    public MappedImage toCachedImage() {
        if (ref != null) {
            return ref;
        } else {
            String caller = Threader.getCaller(3);
            if (!caller.equals("injectAsLayers")) {
                Tonga.log.warn("Mapping of internal image data by {}", caller);
            }
            return new MappedImage(this, true);
        }
    }

    public MappedImage toStreamedImage() {
        if (ref != null) {
            return ref;
        } else {
            return new MappedImage(this, false);
        }
    }

    public Image toFXImage() {
        return toStreamedImage().getFXImage();
    }

    public TongaLayer toLayer() {
        return new TongaLayer(this);
    }

    public static TongaLayer[] convertToLayers(ImageData[] inputLayers) {
        TongaLayer[] layers = new TongaLayer[inputLayers.length];
        for (int i = 0; i < inputLayers.length; i++) {
            layers[i] = inputLayers[i].toLayer();
        }
        return layers;
    }

    public static ImageData[] convertToImageData(Object[] layers) {
        // makes Image[] or CachedImage[] to ImageData[]
        ImageData[] idlayers = new ImageData[layers.length];
        for (int i = 0; i < layers.length; i++) {
            if (layers[i].getClass() == Image.class) {
                idlayers[i] = new ImageData((Image) layers[i]);
            }
            if (layers[i].getClass() == MappedImage.class) {
                idlayers[i] = new ImageData((MappedImage) layers[i]);
            }
        }
        return idlayers;
    }

    public void setAttributes(ImageData id) {
        this.colour = id.colour;
        this.min = id.min;
        this.max = id.max;
    }

    public final void setPixels() {
        if (pixels32 == null && pixels16 == null) {
            pixels32 = bits == 8 ? ref.getARGB() : null;
            pixels16 = bits == 16 ? ref.getShorts() : null;
        }
    }

    public void set8BitPixels() {
        if (pixels32 == null) {
            pixels32 = TongaRender.shortToARGB(pixels16, colour, max, min);
        }
    }

    public void fill(int v) {
        //fills the whole image with a value fast
        if (bits == 16) {
            for (int i = 0; i < width; i++) {
                pixels16[i] = (short) v;
            }
        } else {
            for (int i = 0; i < width; i++) {
                pixels32[i] = v;
            }
        }
        Object array = bits == 16 ? pixels16 : pixels32;
        for (int i = 1; i < height; i++) {
            System.arraycopy(array, 0, array, width * i, width);
        }
    }

    public ImageData copy() {
        return new ImageData(this);
    }
}
