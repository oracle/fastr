/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZ;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RCompression.Type;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BasePathRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ReadWriteHelper;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class CompressedConnections {
    public static final int GZIP_BUFFER_SIZE = (2 << 20);

    /**
     * Base class for all modes of gzfile/bzfile/xzfile connections. N.B. In GNU R these can read
     * gzip, bzip, lzma and uncompressed files, and this has to be implemented by reading the first
     * few bytes of the file and detecting the type of the file.
     */
    public static class CompressedRConnection extends BasePathRConnection {
        private final RCompression.Type cType;
        @SuppressWarnings("unused") private final String encoding; // TODO
        @SuppressWarnings("unused") private final int compression; // TODO

        public CompressedRConnection(String path, String modeString, Type cType, String encoding, int compression) throws IOException {
            super(path, mapConnectionClass(cType), modeString, AbstractOpenMode.ReadBinary);
            this.cType = cType;
            this.encoding = encoding;
            this.compression = compression;
            openNonLazyConnection();
        }

        private static ConnectionClass mapConnectionClass(RCompression.Type cType) {
            switch (cType) {
                case NONE:
                    return ConnectionClass.File;
                case GZIP:
                    return ConnectionClass.GZFile;
                case BZIP2:
                    return ConnectionClass.BZFile;
                case XZ:
                    return ConnectionClass.XZFile;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            AbstractOpenMode openMode = getOpenMode().abstractOpenMode;
            switch (openMode) {
                case Read:
                case ReadBinary:
                    /*
                     * For input, we check the actual compression type as GNU R is permissive about
                     * the claimed type.
                     */
                    RCompression.Type cTypeActual = RCompression.getCompressionType(path);
                    if (cTypeActual != cType) {
                        updateConnectionClass(mapConnectionClass(cTypeActual));
                    }
                    switch (cTypeActual) {
                        case NONE:
                            if (openMode == AbstractOpenMode.ReadBinary) {
                                delegate = new FileConnections.FileReadBinaryRConnection(this);
                            } else {
                                delegate = new FileConnections.FileReadTextRConnection(this);
                            }
                            break;
                        case GZIP:
                            delegate = new CompressedInputRConnection(this, new GZIPInputStream(new FileInputStream(path), GZIP_BUFFER_SIZE));
                            break;
                        case XZ:
                            delegate = new CompressedInputRConnection(this, new XZInputStream(new FileInputStream(path)));
                            break;
                        case BZIP2:
                            // no in Java support, so go via byte array
                            byte[] bzipUdata = RCompression.bzipUncompressFromFile(path);
                            delegate = new ByteStreamCompressedInputRConnection(this, new ByteArrayInputStream(bzipUdata));
                    }
                    break;

                case Append:
                case AppendBinary:
                case Write:
                case WriteBinary: {
                    boolean append = openMode == AbstractOpenMode.Append || openMode == AbstractOpenMode.AppendBinary;
                    switch (cType) {
                        case GZIP:
                            delegate = new CompressedOutputRConnection(this, new GZIPOutputStream(new FileOutputStream(path, append), GZIP_BUFFER_SIZE));
                            break;
                        case BZIP2:
                            delegate = new BZip2OutputRConnection(this, new ByteArrayOutputStream(), append);
                            break;
                        case XZ:
                            delegate = new CompressedOutputRConnection(this, new XZOutputStream(new FileOutputStream(path, append), new LZMA2Options(), XZ.CHECK_CRC32));
                            break;
                    }
                    break;
                }
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

    private static class CompressedInputRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private final InputStream inputStream;

        protected CompressedInputRConnection(CompressedRConnection base, InputStream is) {
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

    private static class ByteStreamCompressedInputRConnection extends CompressedInputRConnection {
        ByteStreamCompressedInputRConnection(CompressedRConnection base, ByteArrayInputStream is) {
            super(base, is);
        }
    }

    private static class CompressedOutputRConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        protected OutputStream outputStream;

        protected CompressedOutputRConnection(CompressedRConnection base, OutputStream os) {
            super(base);
            this.outputStream = os;
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

    private static class BZip2OutputRConnection extends CompressedOutputRConnection {
        private final ByteArrayOutputStream bos;
        private final boolean append;

        BZip2OutputRConnection(CompressedRConnection base, ByteArrayOutputStream os, boolean append) {
            super(base, os);
            this.bos = os;
            this.append = append;
        }

        @Override
        public void close() throws IOException {
            flush();
            outputStream.close();
            // Now actually do the compression using sub-process
            byte[] data = bos.toByteArray();
            RCompression.bzipCompressToFile(data, ((BasePathRConnection) base).path, append);
        }
    }

}
