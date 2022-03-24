package mainPackage;

import java.awt.image.BufferedImage;
import java.util.Objects;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class TongaLayer {

    public String layerName;
    public MappedImage layerImage;
    public boolean isGhost;
    public int width;
    public int height;
    public String path;
    public boolean isPointer;

    public TongaLayer(MappedImage image) {
        this(image, null);
    }

    public TongaLayer(Image image) {
        this(SwingFXUtils.fromFXImage(image, null), null);
    }

    public TongaLayer(Image image, String name) {
        this(SwingFXUtils.fromFXImage(image, null), name);
    }

    public TongaLayer(BufferedImage image) {
        this(new MappedImage(image), null);
    }

    public TongaLayer(BufferedImage image, String name) {
        this(new MappedImage(image), name);
    }

    public TongaLayer(MappedImage image, String name) {
        layerName = name;
        layerImage = image;
        isGhost = false;
        isPointer = false;
        width = (int) image.getWidth();
        height = (int) image.getHeight();
    }

    public TongaLayer(String pointer, String name) {
        layerName = name;
        path = pointer;
        isGhost = false;
        isPointer = true;
    }

    public TongaLayer(ImageData id) {
        this(id.toCachedImage(), id.name);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.layerName);
        hash = 71 * hash + Objects.hashCode(this.layerImage);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TongaLayer other = (TongaLayer) obj;
        if (!Objects.equals(this.layerName, other.layerName)) {
            return false;
        }
        return Objects.equals(this.layerImage, other.layerImage);
    }

    @Override
    public String toString() {
        return layerName;
    }

    protected String description() {
        StringBuilder desc = new StringBuilder();
        desc.append(layerName);
        desc.append("  |  ");
        if (isPointer) {
            desc.append(path);
        } else {
            desc.append(layerImage.bits).append("-bit  |  ").append(width).append("x").append(height).append(" px  |  ");
            desc.append(String.format("%.2f", layerImage.size / 1048576.).replace(",", ".")).append(" MB");
        }
        return desc.toString();
    }
}
