package mainPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.zip.CRC32;

public class MappedBuffer extends MemoryBuffer {

    private static int ID = 0;
    private File file;
    private RandomAccessFile raf;

    public MappedBuffer(int type, int width, int height, int banks, boolean bits) {
        super(type, width * height, banks, bits);
        try {
            file = new File(Tonga.getTempPath());
            while (file.exists()) {
                File nfile = new File(Tonga.getTempPath() + getHash(ID++) + ".bin");
                if (nfile.exists()) {
                    Tonga.log.warn("Hash clash for " + nfile.getName());
                } else {
                    file = nfile;
                }
            }
            raf = new RandomAccessFile(file, "rw");
            buffer = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, bits ? size * 2 : size * banks);
            Tonga.mappedData.add(this);
        } catch (FileNotFoundException ex) {
            Tonga.catchError(ex, "Buffer creation failed.");
        } catch (ClosedChannelException ex) {
            Tonga.catchError(ex, "Interrupted during buffering.");
        } catch (IOException ex) {
            Tonga.catchError(ex, "Buffer creation failed.");
        }
    }

    public File getFile() {
        return file;
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
        clean((MappedByteBuffer) buffer);
        file.delete();
        Tonga.mappedData.remove(this);
        Tonga.log.trace("Freeing " + file.getName());
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

    private static String getHash(int identifier) {
        CRC32 crc = new CRC32();
        crc.update((int) (System.nanoTime() & 0x00000000FFFFFFFFL));
        String s = Long.toHexString(crc.getValue());
        crc.update(new Random().nextInt() + identifier);
        return s + Long.toHexString(crc.getValue());
    }
}
