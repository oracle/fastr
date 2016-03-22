/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.conn;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BasePathRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ReadWriteHelper;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class GZIPConnections {
    public static final int GZIP_BUFFER_SIZE = (2 << 20);

    /**
     * Base class for all modes of gzfile connections. N.B. gzfile is defined to be able to read
     * gzip, bzip, lzma and uncompressed files, which has to be implemented by reading the first few
     * bytes of the file and detecting the type of the file.
     */
    public static class GZIPRConnection extends BasePathRConnection {
        public GZIPRConnection(String path, String modeString) throws IOException {
            super(path, ConnectionClass.GZFile, modeString, AbstractOpenMode.ReadBinary);
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    RCompression.Type cType = RCompression.Type.NONE;
                    try (InputStream is = new FileInputStream(path)) {
                        byte[] buf = new byte[5];
                        int count = is.read(buf);
                        if (count == 5) {
                            cType = RCompression.Type.decodeBuf(buf);
                        }
                    }
                    switch (cType) {
                        case NONE:
                            delegate = new FileConnections.FileReadTextRConnection(this);
                            break;
                        case GZIP:
                            delegate = new GZIPInputRConnection(this);
                            break;
                        case LZMA:
                            /*
                             * no lzma support in Java. For now we use RCompression to a byte array
                             * and return a ByteArrayInputStream on that.
                             */
                            byte[] lzmaUdata = RCompression.lzmaUncompressFromFile(path);
                            delegate = new ByteGZipInputRConnection(this, new ByteArrayInputStream(lzmaUdata));
                            break;
                        case BZIP2:
                            // ditto
                            byte[] bzipUdata = RCompression.bzipUncompressFromFile(path);
                            delegate = new ByteGZipInputRConnection(this, new ByteArrayInputStream(bzipUdata));
                    }
                    break;
                case Write:
                case WriteBinary:
                    delegate = new GZIPOutputRConnection(this);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }

        // @Override
        /**
         * GnuR behavior for lazy connections is odd, e.g. gzfile returns "text", even though the
         * default mode is "rb".
         */
        // public boolean isTextMode() {
        // }
    }

    private static class GZIPInputRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private InputStream inputStream;

        GZIPInputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            inputStream = new GZIPInputStream(new FileInputStream(base.path), GZIP_BUFFER_SIZE);
        }

        protected GZIPInputRConnection(GZIPRConnection base, InputStream is) {
            super(base);
            this.inputStream = is;
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return readBinHelper(buffer, inputStream);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            return readBinCharsHelper(inputStream);
        }

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            return readLinesHelper(inputStream, n, warn, skipNul);
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
    }

    private static class ByteGZipInputRConnection extends GZIPInputRConnection {
        ByteGZipInputRConnection(GZIPRConnection base, ByteArrayInputStream is) {
            super(base, is);
        }
    }

    private static class GZIPOutputRConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private GZIPOutputStream outputStream;

        GZIPOutputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            outputStream = new GZIPOutputStream(new FileOutputStream(base.path), GZIP_BUFFER_SIZE);
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
            writeLinesHelper(outputStream, lines, sep);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            writeBinHelper(buffer, outputStream);
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
