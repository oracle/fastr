package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Objects;

import com.oracle.truffle.r.runtime.RInternalError;

/**
 * A resizable byte channel backed by a byte array.
 */
public class SeekableMemoryByteChannel implements SeekableByteChannel {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /** current cursor position */
    private long position = 0L;

    /** position of the last available byte (exclusive) */
    private long endPos = 0L;

    private long offset = 0L;
    private byte[] buf;
    private boolean open = true;

    public SeekableMemoryByteChannel(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("'initialCapacity' must not be negative");
        }
        buf = new byte[initialCapacity];
    }

    /**
     * Find next power of two (see Hacker's Delight).
     * 
     * @param n the number
     */
    private static int clp2(final int n) {
        int x = n;
        x = x - 1;
        x = x | (x >> 1);
        x = x | (x >> 2);
        x = x | (x >> 4);
        x = x | (x >> 8);
        x = x | (x >> 16);
        return x + 1;
    }

    public byte[] getBufferFromCursor() {
        return Arrays.copyOfRange(buf, (int) (position - offset), (int) (endPos - offset));
    }

    public byte[] getBuffer() {
        return Arrays.copyOfRange(buf, (int) offset, (int) (endPos - offset));
    }

    public SeekableMemoryByteChannel() {
        this(32);
    }

    public SeekableMemoryByteChannel(byte[] initialBuffer) {
        this(clp2(Objects.requireNonNull(initialBuffer).length));
        assert buf.length >= initialBuffer.length;
        endPos = initialBuffer.length;
        position = initialBuffer.length;
        System.arraycopy(initialBuffer, 0, buf, 0, initialBuffer.length);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        Objects.requireNonNull(dst);
        RInternalError.guarantee(dst.remaining() <= Integer.MAX_VALUE);
        final int nCanRead = (int) Math.min(dst.remaining(), size());
        dst.put(buf, (int) (position - offset), nCanRead);
        position += nCanRead;
        return nCanRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        Objects.requireNonNull(src);
        final int len = src.remaining();
        ensureCapacity((int) (position - offset + len));
        src.get(buf, (int) (position - offset), len);
        position += len;
        endPos = Math.max(position, endPos);
        return len;
    }

    public int write(byte[] data) throws IOException {
        if (!open) {
            throw new ClosedChannelException();
        }
        Objects.requireNonNull(data);
        ensureCapacity((int) (position - offset + data.length));
        System.arraycopy(data, 0, buf, (int) (position - offset), data.length);
        position += data.length;
        endPos = Math.max(position, endPos);
        return data.length;
    }

    public void write(int b) throws IOException {
        // this is probably bad, but duplicating code is also bad and this case is probably
        // rarely used
        write(new byte[]{(byte) b});
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);

    }

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public long size() {
        return endPos - offset;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IllegalArgumentException("newPosition must not be negative");
        }
        position = newPosition + offset;
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        // avoid security leak by nulling previous data
        Arrays.fill(buf, (byte) 0);

        offset = 0;
        position = 0;
        endPos = 0;
        return this;
    }

    @Override
    public void close() throws IOException {
        open = false;
    }

    public InputStream getInputStream() {
        return new SyncIS();
    }

    private class SyncIS extends InputStream {

        @Override
        public int read() throws IOException {
            return SeekableMemoryByteChannel.this.read();
        }

    }

    public OutputStream getOutputStream() {
        return new SyncOS();
    }

    public int read() throws IOException {
        final ByteBuffer bf = ByteBuffer.allocate(1);
        read(bf);
        return bf.get();
    }

    private class SyncOS extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            SeekableMemoryByteChannel.this.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            SeekableMemoryByteChannel.this.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            SeekableMemoryByteChannel.this.write(ByteBuffer.wrap(b, off, len));
        }
    }

}