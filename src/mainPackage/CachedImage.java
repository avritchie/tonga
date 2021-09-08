package mainPackage;

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.zip.CRC32;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

public class CachedImage extends BufferedImage {

    public int bits;
    int colour;
    int min;
    int max;
    int size;

    public CachedImage(int width, int height, boolean bits) {
        super(new ComponentColorModel(ColorSpace.getInstance(bits ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB), !bits, !bits, bits ? 1 : 3, bits ? 1 : 0),
                new CacheRaster(
                        new BandedSampleModel(bits ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE, width, height, bits ? 1 : 4),
                        new CacheBuffer(bits ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE, width, height, bits ? 1 : 4, bits)), !bits, null);
        this.bits = bits ? 16 : 8;
        this.colour = 0xFFFFFFFF;
        this.min = 0;
        this.max = 0xFFFF;
        this.size = width * height * (bits ? 2 : 4);
    }

    public CachedImage(short[] bytes, int width, int height) {
        this(width, height, true);
        this.setShorts(bytes);
    }

    public CachedImage(int[] bytes, int width, int height) {
        this(width, height, false);
        this.setARGB(bytes);
    }

    public CachedImage(ImageData id) {
        this(id.width, id.height, id.bits == 16);
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

    public CachedImage(BufferedImage bi) {
        this(bi.getWidth(), bi.getHeight(), bi.getType() == BufferedImage.TYPE_USHORT_GRAY);
        //source may vary - ensure compatilibty with the bands model
        if (!this.getColorModel().isCompatibleRaster(bi.getRaster())) {
            System.out.println("Incompatible colour model");
            //System.out.println(this.getColorModel());
            //System.out.println(bi.getColorModel());
            bi = forceCorrectFormat(bi);
        }
        //clone contents from bi to cbi
        this.getRaster().setRect(bi.getRaster());
    }

    public CachedImage(File f) {
        this(getBufferedFile(f));
        if (this.bits == 16 && Settings.settingAutoscaleType() != Settings.Autoscale.NONE) {
            TongaRender.setDisplayRange(getShorts(), this);
        }
    }

    private static BufferedImage getBufferedFile(File f) {
        //load and force 4-channel
        BufferedImage bi;
        try {
            bi = getBufferedImage(f);
        } catch (Exception ex) {
            Tonga.catchError(ex, "Direct image loading failed.");
            try {
                bi = getBufferedImageSlow(f);
                return bi;
            } catch (Exception ex1) {
                Tonga.catchError(ex, "Alternative image loading failed.");
            }
            return null;
        }
        return bi;
    }

    private static BufferedImage getBufferedImage(File f) throws IOException {
        ImageInputStream input = ImageIO.createImageInputStream(f);
        ImageReader reader = ImageIO.getImageReaders(input).next();
        reader.setInput(input);
        int height = reader.getHeight(reader.getMinIndex());
        int width = reader.getWidth(reader.getMinIndex());
        ImageReadParam param = reader.getDefaultReadParam();
        ImageTypeSpecifier its = reader.getRawImageType(reader.getMinIndex());
        BufferedImage bi;
        if (its.getBitsPerBand(reader.getMinIndex()) == 16) {
            //System.out.println("The image has 16 bits.");
            param.setDestination(new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY));
            bi = reader.read(reader.getMinIndex(), param);
            return bi;
        } else {
            switch (its.getNumComponents()) {
                case 4:
                    //System.out.println("The image has 4 8-bit channels.");
                    param.setDestination(new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR));
                    bi = reader.read(reader.getMinIndex(), param);
                    return bi;
                case 3:
                    //System.out.println("The image has 3 8-bit channels.");
                    param.setDestination(new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR));
                    bi = reader.read(reader.getMinIndex(), param);
                    return forceCorrectFormat(bi);
                case 1:
                    //System.out.println("The image is an 8-bit grayscale image.");
                    param.setDestination(new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY));
                    bi = reader.read(reader.getMinIndex(), param);
                    return forceCorrectFormat(bi);
            }
        }
        System.out.println("Unsupported image format: " + its.getColorModel());
        return null;
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

    public Image getFXImage() {
        long pt = System.nanoTime(), st;
        byte[] bytes;
        WritableImage wi;
        if (bits == 16) {
            ByteBuffer buffer = ((CacheBuffer) this.getRaster().getDataBuffer()).buffer;
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
            bytes = ((CacheBuffer) this.getRaster().getDataBuffer()).getData();
            st = System.nanoTime();
            PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
            wi = new WritableImage(getWidth(), getHeight());
            wi.getPixelWriter().setPixels(0, 0, getWidth(), getHeight(), format, bytes, 0, getWidth() * 4);
        }
        //System.out.println("Rendering time: (" + (System.nanoTime() - pt) / 1000000 + "+" + (System.nanoTime() - st) / 1000000 + ") "
        //        + (System.nanoTime() - pt + System.nanoTime() - st) / 1000000 + " ms");
        return wi;
    }

    public BufferedImage get8BitCopy() {
        int width = getWidth(), height = getHeight();
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        bi.setRGB(0, 0, width, height, getARGB(), 0, width);
        return bi;
    }

    public final int[] getARGB() {
        ByteBuffer bb = ((CacheBuffer) this.getRaster().getDataBuffer()).buffer;
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
        ByteBuffer bb = ((CacheBuffer) this.getRaster().getDataBuffer()).buffer;
        bb.position(0);
        ShortBuffer ib = bb.asShortBuffer();
        short[] array = new short[ib.remaining()];
        ib.get(array);
        return array;
    }

    public final void setARGB(int[] argb) {
        ByteBuffer bb = ((CacheBuffer) this.getRaster().getDataBuffer()).buffer;
        bb.position(0);
        IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        ib.put(argb);
    }

    public final void setShorts(short[] shorts) {
        ByteBuffer bb = ((CacheBuffer) this.getRaster().getDataBuffer()).buffer;
        bb.position(0);
        ShortBuffer ib = bb.asShortBuffer();
        ib.put(shorts);
    }

    private void setAttributes(ImageData id) {
        this.colour = id.colour;
        this.min = id.min;
        this.max = id.max;
    }

    public CacheBuffer getBuffer() {
        return (CacheBuffer) this.getRaster().getDataBuffer();
    }

    public static class CacheRaster extends WritableRaster {

        public CacheRaster(SampleModel sm, CacheBuffer db) {
            super(sm, db, new Point());
        }
    }

    public static class CacheBuffer extends DataBuffer {

        private static int ID = 0;
        private File file;
        private RandomAccessFile raf;
        private MappedByteBuffer buffer;
        private boolean bits;

        public CacheBuffer(int type, int width, int height, int banks, boolean bits) {
            super(type, width * height, banks);
            this.bits = bits;
            try {
                file = new File(System.getProperty("java.io.tmpdir") + "/Tonga");
                file.mkdir();
                while (file.exists()) {
                    File nfile = new File(file + "/" + getHash(ID++) + ".bin");
                    if (nfile.exists()) {
                        System.out.println("Hash clash for " + nfile.getName());
                    } else {
                        file = nfile;
                    }
                }
                raf = new RandomAccessFile(file, "rw");
                buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, bits ? size * 2 : size * banks);
                Tonga.cachedData.add(this);
            } catch (FileNotFoundException ex) {
                Tonga.catchError(ex, "Buffer creation failed.");
            } catch (ClosedChannelException ex) {
                System.out.println("Interrupted during buffering.");
            } catch (IOException ex) {
                Tonga.catchError(ex, "Buffer creation failed.");
            }
        }

        public File getFile() {
            return file;
        }

        public byte[] getData() {
            buffer.position(0);
            byte[] array = new byte[buffer.remaining()];
            buffer.get(array);
            return array;
        }

        public void setData(byte[] array) {
            buffer.position(0);
            buffer.put(array);
        }

        @Override
        public int getElem(int bank, int i) {
            if (bits) {
                return buffer.getShort(i * 2) & 0xffff;
            } else {
                return buffer.get(i * 4 + (bank != 3 ? Math.abs(bank - 2) : bank)) & 0xff;
            }
        }

        @Override
        public void setElem(int bank, int i, int val) {
            if (bits) {
                buffer.putShort(i * 2, (short) val);
            } else {
                buffer.put(i * 4 + (bank != 3 ? Math.abs(bank - 2) : bank), (byte) val);
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            new Thread() {
                @Override
                public void run() {
                    freeCache();
                }
            }.start();
        }

        protected void freeCache() {
            try {
                raf.close();
            } catch (IOException ex) {
                Tonga.catchError(ex, "RandomAccessFile closing error.");
            }
            clean(buffer);
            file.delete();
            Tonga.cachedData.remove(this);
            System.out.println("Freeing " + file.getName());
        }

        private void clean(MappedByteBuffer b) {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                invokeCleaner.invoke(unsafeField.get(null), b);
            } catch (Exception ex) {
                Tonga.catchError(ex, "MappedByteBuffer cleaning error.");
            }
        }
    }

    private static String getHash(int identifier) {
        CRC32 crc = new CRC32();
        crc.update((int) (System.nanoTime() & 0x00000000FFFFFFFFL));
        String s = Long.toHexString(crc.getValue());
        crc.update(new Random().nextInt() + identifier);
        return s + Long.toHexString(crc.getValue());
    }
}
