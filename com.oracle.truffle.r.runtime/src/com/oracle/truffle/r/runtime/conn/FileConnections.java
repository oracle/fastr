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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class FileConnections {
    /**
     * Base class for all modes of file connections.
     *
     */
    public static class FileRConnection extends BasePathRConnection {

        public FileRConnection(String path, String modeString) throws IOException {
            super(path, ConnectionClass.File, modeString);
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                    delegate = new FileReadTextRConnection(this);
                    break;
                case Write:
                    delegate = new FileWriteTextRConnection(this, false);
                    break;
                case Append:
                    delegate = new FileWriteTextRConnection(this, true);
                    break;
                case ReadBinary:
                    delegate = new FileReadBinaryRConnection(this);
                    break;
                case WriteBinary:
                    delegate = new FileWriteBinaryConnection(this, false);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }
    }

    static class FileReadTextRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private BufferedInputStream inputStream;

        FileReadTextRConnection(BasePathRConnection base) throws IOException {
            super(base);
            inputStream = new BufferedInputStream(new FileInputStream(base.path));
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.error(RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
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

    }

    private static class FileWriteTextRConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private BufferedOutputStream outputStream;

        FileWriteTextRConnection(FileRConnection base, boolean append) throws IOException {
            super(base);
            outputStream = new BufferedOutputStream(new FileOutputStream(base.path, append));
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            writeLinesHelper(outputStream, lines, sep);
            flush();
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            writeStringHelper(outputStream, s, nl);
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
            outputStream.close();
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }
    }

    private static class FileReadBinaryRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private FileInputStream inputStream;

        FileReadBinaryRConnection(FileRConnection base) throws IOException {
            super(base);
            inputStream = new FileInputStream(base.path);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return inputStream.getChannel().read(buffer);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            return readBinCharsHelper(inputStream);
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
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

    private static class FileWriteBinaryConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private FileOutputStream outputStream;

        FileWriteBinaryConnection(FileRConnection base, boolean append) throws IOException {
            super(base);
            outputStream = new FileOutputStream(base.path, append);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            outputStream.getChannel().write(buffer);
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
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
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
