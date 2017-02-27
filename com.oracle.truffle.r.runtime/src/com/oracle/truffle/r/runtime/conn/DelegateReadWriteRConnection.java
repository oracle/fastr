package com.oracle.truffle.r.runtime.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * A blocking connection for reading and writing.
 */
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
        if (useBytes) {
            return DelegateRConnection.readCharHelper(nchars, getInputStream());
        } else {
            final InputStreamReader isr = new InputStreamReader(getInputStream(), base.getEncoding());
            return DelegateRConnection.readCharHelper(nchars, isr);
        }
    }

    @Override
    public int readBin(ByteBuffer buffer) throws IOException {
        return getInputStream().read(buffer.array());
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
    public abstract OutputStream getOutputStream();

    @Override
    public abstract InputStream getInputStream();

    @Override
    public void close() throws IOException {
        getInputStream().close();
        getOutputStream().close();
    }

    @Override
    public void closeAndDestroy() throws IOException {
        base.closed = true;
        close();
    }

    @Override
    public void writeBin(ByteBuffer buffer) throws IOException {
        getOutputStream().write(buffer.array());
    }

    @Override
    public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
        DelegateRConnection.writeCharHelper(getOutputStream(), s, pad, eos);
    }

    @Override
    public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
        DelegateRConnection.writeLinesHelper(getOutputStream(), lines, sep, base.getEncoding());
    }

    @Override
    public void writeString(String s, boolean nl) throws IOException {
        DelegateRConnection.writeStringHelper(getOutputStream(), s, nl, base.getEncoding());
    }

}