package mainPackage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class Mapping {

    private final MappedByteBuffer buffer;
    private final RandomAccessFile raf;
    private final File file;
    private WeakReference wr;

    public Mapping(MappedByteBuffer buffer, RandomAccessFile raf, File file) {
        this.buffer = buffer;
        this.raf = raf;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public WeakReference getRefer() {
        return wr;
    }

    public void refer(WeakReference wr) {
        this.wr = wr;
    }

    private static void unmapBuffer(MappedByteBuffer mbb) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
            invokeCleaner.invoke(unsafeField.get(null), mbb);
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            Tonga.catchError(ex, "MappedByteBuffer cleaning error.");
        }
    }

    public void unmap() {
        try {
            unmapBuffer(buffer);
        } finally {
            try {
                raf.close();
                boolean ye = file.delete();
                Tonga.log.trace("{}", ye);
            } catch (IOException ex) {
                Tonga.catchError(ex, "RandomAccessFile closing error.");
            }
        }
        Tonga.log.trace("Freeing {}", file.getName());
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
