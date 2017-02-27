package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

abstract class DelegateReadWriteNonBlockRConnection extends DelegateRConnection {

    private final ByteBuffer tmp = ByteBuffer.allocate(1);

    protected DelegateReadWriteNonBlockRConnection(BaseRConnection base) {
        super(base);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public int getc() throws IOException {
        tmp.clear();
        int nread = getChannel().read(tmp);
        tmp.rewind();
        return nread > 0 ? tmp.get() : -1;
    }

    @Override
    public String readChar(int nchars, boolean useBytes) throws IOException {
        if (useBytes) {
            return DelegateRConnection.readCharHelper(nchars, getInputStream());
        } else {
            final InputStreamReader isr = new InputStreamReader(getInputStream(), base.getEncoding());
            return DelegateRConnection.readCharHelper(nchars, isr);
        }
    }

    @Override
    public int readBin(ByteBuffer buffer) throws IOException {
        return getChannel().read(buffer);
    }

    @Override
    public byte[] readBinChars() throws IOException {
        return DelegateRConnection.readBinCharsHelper(getInputStream());
    }

    @TruffleBoundary
    @Override
    public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
        return readLinesHelper(n, warn, skipNul);
    }

    @Override
    public void flush() {
        // nothing to do for channels
    }

    @Override
    public OutputStream getOutputStream() {
        throw RInternalError.shouldNotReachHere();
    }

    public abstract ByteChannel getChannel();

    @Override
    @Deprecated
    public InputStream getInputStream() {
        return Channels.newInputStream(getChannel());
    }

    @Override
    public void close() throws IOException {
        getChannel().close();
    }

    @Override
    public void closeAndDestroy() throws IOException {
        base.closed = true;
        close();
    }

    @Override
    public void writeBin(ByteBuffer buffer) throws IOException {
        getChannel().write(buffer);
    }

    @Override
    public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
        DelegateRConnection.writeCharHelper(getChannel(), s, pad, eos);
    }

    @Override
    public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
        DelegateRConnection.writeLinesHelper(getChannel(), lines, sep, base.getEncoding());
    }

    @Override
    public void writeString(String s, boolean nl) throws IOException {
        DelegateRConnection.writeStringHelper(getChannel(), s, nl, base.getEncoding());
    }

}