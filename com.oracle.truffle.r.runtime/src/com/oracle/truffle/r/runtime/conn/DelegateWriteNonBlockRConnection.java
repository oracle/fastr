package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

abstract class DelegateWriteNonBlockRConnection extends DelegateRConnection {

    protected DelegateWriteNonBlockRConnection(BaseRConnection base) {
        super(base);
    }

    @Override
    public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
        throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
    }

    @Override
    public String readChar(int nchars, boolean useBytes) throws IOException {
        throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
    }

    @Override
    public int readBin(ByteBuffer buffer) throws IOException {
        throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
    }

    @Override
    public byte[] readBinChars() throws IOException {
        throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
    }

    @Override
    public int getc() throws IOException {
        throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
    }

    @Override
    public InputStream getInputStream() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public boolean canRead() {
        return false;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public void closeAndDestroy() throws IOException {
        base.closed = true;
        close();
    }

    @Override
    public void flush() throws IOException {
        // channels don't need any flushing
    }

    @Override
    public void close() throws IOException {
        flush();
        getChannel().close();
    }

    public abstract WritableByteChannel getChannel();

    @Override
    @Deprecated
    public OutputStream getOutputStream() throws IOException {
        return Channels.newOutputStream(getChannel());
    }

    @Override
    public void writeBin(ByteBuffer buffer) throws IOException {
        getChannel().write(buffer);
    }

    @Override
    public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
        ReadWriteHelper.writeCharHelper(getChannel(), s, pad, eos);
    }

    @Override
    public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
        ReadWriteHelper.writeLinesHelper(getChannel(), lines, sep, base.getEncoding());
    }

    @Override
    public void writeString(String s, boolean nl) throws IOException {
        ReadWriteHelper.writeStringHelper(getChannel(), s, nl, base.getEncoding());
    }

}