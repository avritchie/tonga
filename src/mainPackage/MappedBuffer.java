package mainPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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
    private Mapping mapping;

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
            mapping = new Mapping((MappedByteBuffer) buffer, raf, file);
        } catch (FileNotFoundException ex) {
            Tonga.catchError(ex, "Buffer creation failed.");
        } catch (ClosedChannelException ex) {
            Tonga.catchError(ex, "Interrupted during buffering.");
        } catch (IOException ex) {
            Tonga.catchError(ex, "Buffer creation failed.");
        }
    }

    public Mapping getMapping() {
        return mapping;
    }

    private static String getHash(int identifier) {
        CRC32 crc1 = new CRC32();
        int i = (int) (System.nanoTime() & 0x00000000FFFFFFFFL);
        byte[] b1 = ByteBuffer.allocate(4).putInt(i).array();
        crc1.update(b1);
        CRC32 crc2 = new CRC32();
        int j = new Random().nextInt() + identifier;
        byte[] b2 = ByteBuffer.allocate(4).putInt(j).array();
        crc2.update(b2);
        return Long.toHexString(crc1.getValue()) + Long.toHexString(crc2.getValue());
    }
}
