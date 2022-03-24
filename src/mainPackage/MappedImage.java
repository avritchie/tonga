package mainPackage;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.NoSuchElementException;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

public class MappedImage extends BufferedImage {

    public int bits;
    int colour;
    int size;
    int min;
    int max;
    private boolean mapped;

    private MappedImage(int width, int height, boolean bits, DataBuffer db) {
        super(getColor(bits), new WrappedRaster(getSample(width, height, bits), db), !bits, null);
        this.bits = bits ? 16 : 8;
        this.colour = 0xFFFFFFFF;
        this.min = 0;
        this.max = 0xFFFF;
        this.size = width * height * (bits ? 2 : 4);
    }

    public MappedImage(int width, int height, boolean bits, boolean mapping) {
        this(width, height, bits, (mapping && Settings.settingMemoryMapping())
                ? new MappedBuffer(bits ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE, width, height, bits ? 1 : 4, bits)
                : new MemoryBuffer(bits ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE, width, height, bits ? 1 : 4, bits));
        this.mapped = mapping && Settings.settingMemoryMapping();
        Tonga.log.trace("{} image created.", mapping ? "Mapped" : "Cached");
    }

    /*public CachedImage(int width, int height, boolean bits) {
        this(width, height, bits, new DataBufferByte(width * height * (bits ? 2 : 1), bits ? 1 : 4));
    }
     */
    public MappedImage(short[] bytes, int width, int height) {
        this(width, height, true, true);
        this.setShorts(bytes);
    }

    public MappedImage(int[] bytes, int width, int height) {
        this(width, height, false, true);
        this.setARGB(bytes);
    }

    public MappedImage(ImageData id, boolean mapping) {
        this(id.width, id.height, id.bits == 16, mapping);
        if (id.pixels32 == null && id.pixels16 == null) {
            id.setPixels();
        }
        if (id.bits == 8) {
            this.setARGB(id.pixels32);
        } else {
            this.setShorts(id.pixels16);
            this.setAttributes(id);
        }
    }

    public MappedImage(BufferedImage bi) {
        this(bi.getWidth(), bi.getHeight(), bi.getType() == BufferedImage.TYPE_USHORT_GRAY, true);
        //source may vary - ensure compatilibty with the bands model
        if (!this.getColorModel().isCompatibleRaster(bi.getRaster())) {
            Tonga.log.debug("Incompatible colour model");
            Tonga.log.debug("Current: {}", this.getColorModel());
            Tonga.log.debug("New: {}", bi.getColorModel());
            bi = forceCorrectFormat(bi);
        }
        //clone contents from bi to cbi
        this.getRaster().setRect(bi.getRaster());
    }

    public MappedImage(File f) {
        this(getBufferedFile(f));
        if (this.bits == 16 && Settings.settingAutoscaleType() != Settings.Autoscale.NONE) {
            TongaRender.setDisplayRange(getShorts(), this);
        }
    }

    private static ColorModel getColor(boolean bits) {
        ColorSpace colorSpace = ColorSpace.getInstance(bits ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB);
        return new ComponentColorModel(colorSpace, !bits, !bits, bits ? 1 : 3, bits ? 1 : 0);
    }

    private static SampleModel getSample(int width, int height, boolean bits) {
        return new BandedSampleModel(bits ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE, width, height, bits ? 1 : 4);
    }

    private static BufferedImage getBufferedFile(File f) {
        //load and force 4-channel
        BufferedImage bi;
        try {
            bi = getBufferedImage(f);
        } catch (IOException ex) {
            Tonga.catchError(ex, "Direct image loading failed.");
            try {
                bi = getBufferedImageSlow(f);
                return bi;
            } catch (IOException ex1) {
                Tonga.catchError(ex, "Alternative image loading failed.");
            }
            return null;
        }
        return bi;
    }

    private static BufferedImage getBufferedImage(File f) throws IOException {
        ImageInputStream input;
        ImageReader reader;
        ImageTypeSpecifier its;
        try {
            input = ImageIO.createImageInputStream(f);
            reader = ImageIO.getImageReaders(input).next();
            reader.setInput(input);
            its = reader.getRawImageType(reader.getMinIndex());
            Tonga.log.debug("Colour type of the image is {}", its.getColorModel().getClass().getSimpleName().replace("ColorModel", ""));
            BufferedImage bi;
            if (its.getBitsPerBand(reader.getMinIndex()) == 16) {
                Tonga.log.debug("The image has 16 bits.");
                bi = readBufferedImageWithReader(BufferedImage.TYPE_USHORT_GRAY, reader);
                return bi;
            } else {
                switch (its.getSampleModel().getNumBands()) {
                    case 4:
                        Tonga.log.debug("The image has 4 8-bit channels.");
                        bi = readBufferedImageWithReader(BufferedImage.TYPE_4BYTE_ABGR, reader);
                        return bi;
                    case 3:
                        Tonga.log.debug("The image has 3 8-bit channels.");
                        bi = readBufferedImageWithReader(BufferedImage.TYPE_3BYTE_BGR, reader);
                        return forceCorrectFormat(bi);
                    case 1:
                        if (its.getColorModel().getClass().equals(IndexColorModel.class)) {
                            Tonga.log.debug("The image is an 8-bit indexed image.");
                            bi = readBufferedImageWithReader(BufferedImage.TYPE_BYTE_GRAY, reader);
                            return forceIndexedFormat(bi, (IndexColorModel) its.getColorModel());
                        } else {
                            Tonga.log.debug("The image is an 8-bit grayscale image.");
                            bi = readBufferedImageWithReader(BufferedImage.TYPE_BYTE_GRAY, reader);
                            return forceCorrectFormat(bi);
                        }
                }
            }
            Tonga.log.warn("Unsupported color format: {}", its.getColorModel());
            return null;
        } catch (NoSuchElementException ex) {
            //no available reader for this format
            Tonga.log.warn("Unsupported image format: {}", f.toString());
            return null;
        }
    }

    private static BufferedImage readBufferedImageWithReader(int type, ImageReader reader) throws IOException {
        ImageReadParam param = reader.getDefaultReadParam();
        int readerInd = reader.getMinIndex();
        int height = reader.getHeight(readerInd);
        int width = reader.getWidth(readerInd);
        param.setDestination(new BufferedImage(width, height, type));
        return reader.read(readerInd, param);
    }

    private static BufferedImage getBufferedImageSlow(File f) throws IOException {
        BufferedImage raw = ImageIO.read(f);
        return forceCorrectFormat(raw);
    }

    private static BufferedImage forceCorrectFormat(BufferedImage raw) {
        int width = raw.getWidth(), height = raw.getHeight();
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        bi.getGraphics().drawImage(raw, 0, 0, null);
        return bi;
    }

    private static BufferedImage forceIndexedFormat(BufferedImage raw, IndexColorModel icm) {
        try {
            int width = raw.getWidth(), height = raw.getHeight();
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            DataBufferByte nind = ((DataBufferByte) bi.getRaster().getDataBuffer());
            DataBufferByte rind = ((DataBufferByte) raw.getRaster().getDataBuffer());
            for (int p = 0; p < width * height; p++) {
                int ip = p * 4;
                int ci = rind.getElem(p);
                nind.setElem(ip, icm.getAlpha(ci));
                nind.setElem(ip + 1, icm.getBlue(ci));
                nind.setElem(ip + 2, icm.getGreen(ci));
                nind.setElem(ip + 3, icm.getRed(ci));
            }
            return bi;
        } catch (ClassCastException ex) {
            Tonga.log.warn("Unsupported color format: {}", icm);
            return null;
        }
    }

    public Image getFXImage() {
        long pt = System.nanoTime(), st;
        byte[] bytes;
        WritableImage wi;
        if (bits == 16) {
            ByteBuffer buffer = ((MemoryBuffer) this.getRaster().getDataBuffer()).buffer;
            buffer.position(0);
            bytes = new byte[buffer.remaining() * 2];
            int rem = buffer.remaining() / 2, s, b;
            double v, l = max - min == 0 ? 1 : max - min;
            int bf = (colour & 0xFF), gf = (colour >> 8 & 0xFF), rf = (colour >> 16 & 0xFF);
            for (int i = 0; i < rem; i++) {
                b = i * 4;
                s = buffer.getShort() & 0xFFFF;
                v = min > s ? 0 : (s - min) / l;
                v = v > 1 ? 1 : v;
                bytes[b] = (byte) (v * bf);
                bytes[b + 1] = (byte) (v * gf);
                bytes[b + 2] = (byte) (v * rf);
                bytes[b + 3] = (byte) 0xFF;
            }
            st = System.nanoTime();
            PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraPreInstance();
            wi = new WritableImage(new PixelBuffer<>(getWidth(), getHeight(), ByteBuffer.wrap(bytes), format));
        } else {
            bytes = ((MemoryBuffer) this.getRaster().getDataBuffer()).getData();
            st = System.nanoTime();
            PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
            wi = new WritableImage(getWidth(), getHeight());
            wi.getPixelWriter().setPixels(0, 0, getWidth(), getHeight(), format, bytes, 0, getWidth() * 4);
        }
        Tonga.log.trace("Rendering time: ({} + {}) ({}) ms",
                (System.nanoTime() - pt) / 1000000,
                (System.nanoTime() - st) / 1000000,
                (System.nanoTime() - pt + System.nanoTime() - st) / 1000000);
        return wi;
    }

    public BufferedImage get8BitCopy() {
        int width = getWidth(), height = getHeight();
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        bi.setRGB(0, 0, width, height, getARGB(), 0, width);
        return bi;
    }

    public final int[] getARGB() {
        ByteBuffer bb = getByteBuffer();
        bb.position(0);
        if (bits == 16) {
            ShortBuffer sb = bb.asShortBuffer();
            sb.position(0);
            short[] array = new short[sb.remaining()];
            sb.get(array);
            return TongaRender.shortToARGB(array, colour, max, min);
        } else {
            IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
            int[] array = new int[ib.remaining()];
            ib.get(array);
            return array;
        }
    }

    public final short[] getShorts() {
        ByteBuffer bb = getByteBuffer();
        bb.position(0);
        ShortBuffer ib = bb.asShortBuffer();
        short[] array = new short[ib.remaining()];
        ib.get(array);
        return array;
    }

    public final void setARGB(int[] argb) {
        ByteBuffer bb = getByteBuffer();
        bb.position(0);
        IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        ib.put(argb);
    }

    public final void setShorts(short[] shorts) {
        ByteBuffer bb = getByteBuffer();
        bb.position(0);
        ShortBuffer ib = bb.asShortBuffer();
        ib.put(shorts);
    }

    protected final void setAttributes(ImageData id) {
        this.colour = id.colour;
        this.min = id.min;
        this.max = id.max;
    }

    public MemoryBuffer getBuffer() {
        return (MemoryBuffer) this.getRaster().getDataBuffer();
    }

    public ByteBuffer getByteBuffer() {
        return getBuffer().buffer;
    }

    boolean isMapped() {
        return mapped;
    }

    public static class WrappedRaster extends WritableRaster {

        public WrappedRaster(SampleModel sm, DataBuffer db) {
            super(sm, db, new Point());
        }
    }
}
