/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;

import java.io.*;
import java.nio.*;
import java.util.zip.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class GZIPConnections {
    /**
     * Base class for all modes of gzfile connections.
     */
    public static class GZIPRConnection extends BasePathRConnection {
        public GZIPRConnection(String path, String modeString) throws IOException {
            super(path, ConnectionClass.GZFile, modeString);
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    try {
                        delegate = new GZIPInputRConnection(this);
                    } catch (ZipException ex) {
                        delegate = new FileConnections.FileReadTextRConnection(this);
                    }
                    break;
                case Write:
                case WriteBinary:
                    delegate = new GZIPOutputRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }
    }

    private static class GZIPInputRConnection extends DelegateReadRConnection {
        private GZIPInputStream inputStream;

        GZIPInputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            inputStream = new GZIPInputStream(new FileInputStream(base.path), RConnection.GZIP_BUFFER_SIZE);
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
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            inputStream.close();
        }

    }

    private static class GZIPOutputRConnection extends DelegateWriteRConnection {
        private GZIPOutputStream outputStream;

        GZIPOutputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            outputStream = new GZIPOutputStream(new FileOutputStream(base.path), RConnection.GZIP_BUFFER_SIZE);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            flush();
            outputStream.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
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
        public void flush() throws IOException {
            outputStream.flush();
        }

    }

}
