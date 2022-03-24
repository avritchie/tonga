package mainPackage;

import java.awt.image.DataBuffer;
import java.nio.ByteBuffer;

public class MemoryBuffer extends DataBuffer {

    ByteBuffer buffer;
    private boolean bits;

    public MemoryBuffer(int type, int width, int height, int banks, boolean bits) {
        this(type, width * height, banks, bits);
        this.buffer = ByteBuffer.wrap(new byte[bits ? size * 2 : size * banks]);
    }

    public MemoryBuffer(int type, int size, int banks, boolean bits) {
        super(type, size, banks);
        this.bits = bits;
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
}
