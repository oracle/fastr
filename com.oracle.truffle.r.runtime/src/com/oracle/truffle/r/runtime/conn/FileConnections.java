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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

import org.tukaani.xz.XZInputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BasePathRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.OpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ReadWriteHelper;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class FileConnections {
    /**
     * Base class for all modes of file connections.
     *
     */
    public static class FileRConnection extends BasePathRConnection {

        public FileRConnection(String path, String modeString) throws IOException {
            super(checkTemp(path), ConnectionClass.File, modeString);
            openNonLazyConnection();
        }

        private static String checkTemp(String path) {
            if (path.length() == 0) {
                return TempPathName.createNonExistingFilePath("Rf", TempPathName.tempDirPath(), "");
            } else {
                return path;
            }
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
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new FileReadWriteConnection(this);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }
    }

    static class FileReadTextRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private InputStream inputStream;

        FileReadTextRConnection(BasePathRConnection base) throws IOException {
            super(base);
            // can be compressed - check for it
            RCompression.Type cType = RCompression.getCompressionType(base.path);
            switch (cType) {
                case NONE:
                    inputStream = new BufferedInputStream(new FileInputStream(base.path));
                    break;
                case GZIP:
                    inputStream = new GZIPInputStream(new FileInputStream(base.path), CompressedConnections.GZIP_BUFFER_SIZE);
                    break;
                case BZIP2:
                    // no in Java support, so go via byte array
                    byte[] bzipUdata = RCompression.bzipUncompressFromFile(base.path);
                    inputStream = new ByteArrayInputStream(bzipUdata);
                    break;
                case XZ:
                    inputStream = new XZInputStream(new FileInputStream(base.path));
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "compression type: " + cType.name());
            }
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
    }

    private static class FileWriteTextRConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private final BufferedOutputStream outputStream;

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
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
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

    static class FileReadBinaryRConnection extends DelegateReadRConnection implements ReadWriteHelper {
        private final FileInputStream inputStream;

        FileReadBinaryRConnection(BasePathRConnection base) throws IOException {
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

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            long position = inputStream.getChannel().position();
            switch (seekMode) {
                case ENQUIRE:
                    break;
                case CURRENT:
                    if (offset != 0) {
                        inputStream.getChannel().position(position + offset);
                    }
                    break;
                case START:
                    inputStream.getChannel().position(offset);
                    break;
                case END:
                    throw RInternalError.unimplemented();

            }
            return position;
        }
    }

    private static class FileWriteBinaryConnection extends DelegateWriteRConnection implements ReadWriteHelper {
        private final FileOutputStream outputStream;

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

    private static class FileReadWriteConnection extends DelegateReadWriteRConnection implements ReadWriteHelper {
        /*
         * This is a minimal implementation to support one specific use in package installation.
         *
         * N.B. R mandates separate "position" offsets for reading and writing (pain). This code is
         * pessimistic and assumes interleaved reads and writes, so does a lot of probably redundant
         * seeking. It could be optimized.
         */
        private final RandomAccessFile raf;
        private long readOffset;
        private long writeOffset;
        private SeekRWMode lastMode = SeekRWMode.READ;
        private final RAFInputStream inputStream;

        /**
         * Allows an {@link RandomAccessFile} to appear to be an {@link InputStream}.
         *
         */
        private class RAFInputStream extends InputStream {
            @Override
            public int read() throws IOException {
                return FileReadWriteConnection.this.getc();
            }
        }

        FileReadWriteConnection(FileRConnection base) throws IOException {
            super(base);
            OpenMode openMode = base.getOpenMode();
            String rafMode = null;
            switch (openMode.abstractOpenMode) {
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    rafMode = "rw";
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
            raf = new RandomAccessFile(base.path, rafMode);
            inputStream = new RAFInputStream();
        }

        @Override
        public int getc() throws IOException {
            raf.seek(readOffset);
            int value = raf.read();
            if (value != -1) {
                readOffset++;
            }
            return value;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            long result = raf.getFilePointer();
            switch (seekMode) {
                case ENQUIRE:
                    return result;
                case START:
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, "seek mode");
            }
            switch (seekRWMode) {
                case LAST:
                    if (lastMode == SeekRWMode.READ) {
                        readOffset = offset;
                    } else {
                        writeOffset = offset;
                    }
                    break;
                case READ:
                    readOffset = offset;
                    break;
                case WRITE:
                    writeOffset = offset;
                    break;
            }
            return result;
        }

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            raf.seek(readOffset);
            return readLinesHelper(inputStream, n, warn, skipNul);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            // TODO encodings
            raf.seek(writeOffset);
            byte[] sepData = sep.getBytes();
            for (int i = 0; i < lines.getLength(); i++) {
                byte[] data = lines.getDataAt(i).getBytes();
                raf.write(data);
                raf.write(sepData);
            }
            writeOffset = raf.getFilePointer();
            lastMode = SeekRWMode.WRITE;
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RInternalError.unimplemented();
        }
    }
}
