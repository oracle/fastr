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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BasePathRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.OpenMode;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class FileConnections {
    public static final int GZIP_BUFFER_SIZE = (2 << 20);

    /**
     * Base class for all modes of file connections.
     */
    public static class FileRConnection extends BasePathRConnection {
        private final boolean raw;

        public FileRConnection(String path, String modeString, boolean blocking, String encoding, boolean raw) throws IOException {
            super(checkTemp(path), ConnectionClass.File, modeString, blocking, encoding);
            this.raw = raw;
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

            DelegateRConnection delegate = FileConnections.createDelegateConnection(this, RCompression.Type.NONE, raw);
            setDelegate(delegate);
        }
    }

    /**
     * Base class for all modes of gzfile/bzfile/xzfile connections. N.B. In GNU R these can read
     * gzip, bzip, lzma and uncompressed files, and this has to be implemented by reading the first
     * few bytes of the file and detecting the type of the file.
     */
    public static class CompressedRConnection extends BasePathRConnection {
        private final RCompression.Type cType;
        @SuppressWarnings("unused") private final int compression; // TODO

        public CompressedRConnection(String path, String modeString, Type cType, String encoding, int compression) throws IOException {
            super(path, mapConnectionClass(cType), modeString, AbstractOpenMode.ReadBinary, encoding);
            this.cType = cType;
            this.compression = compression;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            setDelegate(FileConnections.createDelegateConnection(this, cType, false));

        }

        // @Override
        /**
         * GnuR behavior for lazy connections is odd, e.g. gzfile returns "text", even though the
         * default mode is "rb".
         */
        // public boolean isTextMode() {
        // }
    }

    private static DelegateRConnection createUncompressedDelegateConnection(BasePathRConnection base)
                    throws IOException {

        DelegateRConnection delegate = null;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
                delegate = new FileReadTextRConnection(base);
                break;
            case ReadBinary:
                delegate = new FileReadBinaryRConnection(base);
                break;
            case Write:
                delegate = new FileWriteTextRConnection(base, false);
                break;
            case Append:
                delegate = new FileWriteTextRConnection(base, true);
                break;
            case WriteBinary:
                delegate = new FileWriteBinaryConnection(base, false);
                break;
            case ReadWriteTrunc:
            case ReadWriteTruncBinary:
                delegate = new FileReadWriteConnection(base);
                break;
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
        return delegate;
    }

    private static DelegateRConnection createGZIPDelegateConnection(BasePathRConnection base) throws IOException {

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                return new CompressedInputRConnection(base, new GZIPInputStream(new FileInputStream(base.path), GZIP_BUFFER_SIZE));
            case Append:
            case AppendBinary:
                append = true;
                // intentionally fall through !
            case Write:
            case WriteBinary:
                return new CompressedOutputRConnection(base, new GZIPOutputStream(new FileOutputStream(base.path, append), GZIP_BUFFER_SIZE), true);
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
    }

    private static DelegateRConnection createXZDelegateConnection(BasePathRConnection base) throws IOException {

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                return new CompressedInputRConnection(base, new XZInputStream(new FileInputStream(base.path)));
            case Append:
            case AppendBinary:
                append = true;
                // intentionally fall through !
            case Write:
            case WriteBinary:
                return new CompressedOutputRConnection(base, new XZOutputStream(new FileOutputStream(base.path, append), new LZMA2Options(), XZ.CHECK_CRC32), false);
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
    }

    private static DelegateRConnection createBZIP2DelegateConnection(BasePathRConnection base) throws IOException {

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                byte[] bzipUdata = RCompression.bzipUncompressFromFile(base.path);
                return new ByteStreamCompressedInputRConnection(base, new ByteArrayInputStream(bzipUdata));
            case Append:
            case AppendBinary:
                append = true;
                // intentionally fall through !
            case Write:
            case WriteBinary:
                return new BZip2OutputRConnection(base, new ByteArrayOutputStream(), append);
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
    }

    private static DelegateRConnection createDelegateConnection(BasePathRConnection base, RCompression.Type cType, boolean raw) throws IOException {
        AbstractOpenMode openMode = base.getOpenMode().abstractOpenMode;

        /*
         * For input, we check the actual compression type as GNU R is permissive about the claimed
         * type except 'raw' is true.
         */
        final RCompression.Type cTypeActual;
        if (!raw && (openMode == AbstractOpenMode.Read || openMode == AbstractOpenMode.ReadBinary)) {
            cTypeActual = RCompression.getCompressionType(base.path);
            if (cTypeActual != cType) {
                base.updateConnectionClass(mapConnectionClass(cTypeActual));
            }
        } else {
            cTypeActual = cType;
        }

        switch (cTypeActual) {
            case NONE:
                return createUncompressedDelegateConnection(base);
            case GZIP:
                return createGZIPDelegateConnection(base);
            case XZ:
                return createXZDelegateConnection(base);
            case BZIP2:
                return createBZIP2DelegateConnection(base);
        }
        throw RInternalError.shouldNotReachHere("unsupported compression type");
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

    static class FileReadBinaryRConnection extends DelegateReadRConnection {

        private final FileChannel channel;

        FileReadBinaryRConnection(BasePathRConnection base) throws IOException {
            super(base);
            channel = FileChannel.open(Paths.get(base.path), StandardOpenOption.READ);
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public ReadableByteChannel getChannel() {
            return channel;
        }

    }

    static class FileReadTextRConnection extends FileReadBinaryRConnection {

        FileReadTextRConnection(BasePathRConnection base) throws IOException {
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
    }

    private static class FileWriteTextRConnection extends FileWriteBinaryConnection {

        FileWriteTextRConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base, append);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }
    }

    private static class FileWriteBinaryConnection extends DelegateWriteRConnection {

        private final FileChannel channel;

        FileWriteBinaryConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base);
            List<OpenOption> opts = new ArrayList<>();
            opts.add(StandardOpenOption.WRITE);
            opts.add(StandardOpenOption.CREATE);
            if (append) {
                opts.add(StandardOpenOption.APPEND);
            } else {
                opts.add(StandardOpenOption.TRUNCATE_EXISTING);
            }
            channel = FileChannel.open(Paths.get(base.path), opts.toArray(new OpenOption[opts.size()]));

        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public WritableByteChannel getChannel() {
            return channel;
        }

    }

    private static class FileReadWriteConnection extends DelegateReadWriteRConnection {
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

        FileReadWriteConnection(BasePathRConnection base) throws IOException {
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
        public String[] readLines(int n, boolean warn, boolean skipNul) throws IOException {
            raf.seek(readOffset);
            return super.readLines(n, warn, skipNul);
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            raf.seek(writeOffset);
            byte[] sepData = sep.getBytes(base.getEncoding());
            for (int i = 0; i < lines.getLength(); i++) {
                writeString(lines.getDataAt(i), false);
                raf.write(sepData);
            }
            writeOffset = raf.getFilePointer();
            lastMode = SeekRWMode.WRITE;
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            raf.write(s.getBytes(base.getEncoding()));
            if (nl) {
                raf.write(System.lineSeparator().getBytes(base.getEncoding()));
            }
        }

        @Override
        public ByteChannel getChannel() {
            return raf.getChannel();
        }

    }

    private static class CompressedInputRConnection extends DelegateReadRConnection {
        private final ReadableByteChannel channel;

        protected CompressedInputRConnection(BasePathRConnection base, InputStream is) {
            super(base);
            channel = Channels.newChannel(is);
        }

        @Override
        public ReadableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class ByteStreamCompressedInputRConnection extends CompressedInputRConnection {
        ByteStreamCompressedInputRConnection(BasePathRConnection base, ByteArrayInputStream is) {
            super(base, is);
        }
    }

    private static class CompressedOutputRConnection extends DelegateWriteRConnection {
        protected WritableByteChannel channel;
        private final boolean seekable;
        private long seekPosition = 0L;

        protected CompressedOutputRConnection(BasePathRConnection base, OutputStream os, boolean seekable) {
            super(base);
            this.seekable = seekable;
            this.channel = Channels.newChannel(os);
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            if (seekable) {
                // TODO GZIP is basically seekable; however, the output stream does not allow any
                // seeking
                long oldPos = seekPosition;
                seekPosition = offset;
                return oldPos;
            }
            return super.seek(offset, seekMode, seekRWMode);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
        }

        @Override
        public WritableByteChannel getChannel() {
            return channel;
        }
    }

    private static class BZip2OutputRConnection extends CompressedOutputRConnection {
        private final ByteArrayOutputStream bos;
        private final boolean append;

        BZip2OutputRConnection(BasePathRConnection base, ByteArrayOutputStream os, boolean append) {
            super(base, os, false);
            this.bos = os;
            this.append = append;
        }

        @Override
        public void close() throws IOException {
            flush();
            // Now actually do the compression using sub-process
            byte[] data = bos.toByteArray();
            RCompression.bzipCompressToFile(data, ((BasePathRConnection) base).path, append);
        }
    }
}
