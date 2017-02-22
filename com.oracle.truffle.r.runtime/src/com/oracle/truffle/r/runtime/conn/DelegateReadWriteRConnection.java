package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

abstract class DelegateReadWriteRConnection extends DelegateRConnection {

    protected DelegateReadWriteRConnection(BaseRConnection base) {
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
        return getInputStream().read();
    }

    @Override
    public String readChar(int nchars, boolean useBytes) throws IOException {
        return ReadWriteHelper.readCharHelper(nchars, getChannel(), useBytes);
    }

    @Override
    public int readBin(ByteBuffer buffer) throws IOException {
        return getChannel().read(buffer);
    }

    @Override
    public byte[] readBinChars() throws IOException {
        return ReadWriteHelper.readBinCharsHelper(getInputStream());
    }

    @TruffleBoundary
    @Override
    public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
        return ReadWriteHelper.readLinesHelper(getInputStream(), n, warn, skipNul, base.getSummaryDescription(), base.getEncoding());
    }

    @Override
    public void flush() {
        // nothing to do for channels
    }

    @Override
    public OutputStream getOutputStream() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public abstract ByteChannel getChannel();

    @Override
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