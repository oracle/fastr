package com.oracle.truffle.r.runtime.conn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ReadWriteHelper;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class RawConnections {

    public static class RawRConnection extends BaseRConnection {

        private byte[] data;

        public RawRConnection(String description, byte[] dataTemp, String open) throws IOException {
            super(ConnectionClass.RAW, open, AbstractOpenMode.Read);
            this.data = dataTemp;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new RawReadTextRConnection(this, data);
                    break;
                case Write:
                case Append:
                    delegate = new RawWriteBinaryConnection(this);
                    break;
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = null;
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            // TODO Auto-generated method stub
            return null;
        }

        public byte[] getValue() {
            try {
                return ((ByteArrayOutputStream) getOutputStream()).toByteArray();
            } catch (IOException e) {
                throw RInternalError.shouldNotReachHere("Receiving output stream failed");
            }
        }

    }

    static class RawReadTextRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private InputStream inputStream;

        RawReadTextRConnection(BaseRConnection base, byte[] data) {
            super(base);
            this.inputStream = new ByteArrayInputStream(data);
        }

        RawReadTextRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            return readLinesHelper(inputStream, n, warn, skipNul);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public OutputStream getOutputStream() {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NOT_AN_OUTPUT_RAW_CONNECTION);
        }
    }

    private static class RawWriteBinaryConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private final OutputStream outputStream;

        RawWriteBinaryConnection(BaseRConnection base) {
            super(base);
            outputStream = new ByteArrayOutputStream();
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            outputStream.write(buffer.array());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            flush();
            outputStream.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                String line = lines.getDataAt(i);
                outputStream.write(line.getBytes());
                outputStream.write(sep.getBytes());
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            writeStringHelper(outputStream, s, nl);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }
    }

}
