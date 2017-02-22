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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
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
     *
     */
    public static class FileRConnection extends BasePathRConnection {

        public FileRConnection(String path, String modeString, Charset encoding) throws IOException {
            super(checkTemp(path), ConnectionClass.File, modeString, encoding);
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

            // TODO (non-)blocking

            DelegateRConnection delegate = FileConnections.createDelegateConnection(this, RCompression.Type.NONE);
            setDelegate(delegate);
        }
    }

    private static DelegateRConnection createUncompressedDelegateConnection(BasePathRConnection base)
                    throws IOException {

        // TODO (non-)blocking

        DelegateRConnection delegate = null;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
                delegate = new FileReadTextRConnection(base, true);
                break;
            case Write:
                delegate = new FileWriteTextRConnection(base, false);
                break;
            case Append:
                delegate = new FileWriteTextRConnection(base, true);
                break;
            case ReadBinary:
                delegate = new FileReadBinaryRConnection(base);
                break;
            case WriteBinary:
                delegate = new FileWriteBinaryConnection(base, false, true);
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

        // TODO (non-)blocking

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                return new CompressedInputRConnection(base, new GZIPInputStream(new FileInputStream(base.path), GZIP_BUFFER_SIZE), true);
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

        // TODO (non-)blocking

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                return new CompressedInputRConnection(base, new XZInputStream(new FileInputStream(base.path)), false);
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

        // TODO (non-)blocking

        boolean append = false;
        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                byte[] bzipUdata = RCompression.bzipUncompressFromFile(base.path);
                return new ByteStreamCompressedInputRConnection(base, new ByteArrayInputStream(bzipUdata), false);
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

    private static DelegateRConnection createDelegateConnection(BasePathRConnection base, RCompression.Type cType) throws IOException {
        AbstractOpenMode openMode = base.getOpenMode().abstractOpenMode;

        /*
         * For input, we check the actual compression type as GNU R is permissive about the claimed
         * type.
         */
        final RCompression.Type cTypeActual;
        if (openMode == AbstractOpenMode.Read || openMode == AbstractOpenMode.ReadBinary) {
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

    static class FileReadTextRConnection extends DelegateReadRConnection {

        private final ReadableByteChannel channel;

        FileReadTextRConnection(BasePathRConnection base, boolean blocking) throws IOException {
            super(base);

            // FIXME: since streams are used to open the file, this is BLOCKING by default !

            // can be compressed - check for it
            RCompression.Type cType = RCompression.getCompressionType(base.path);
            switch (cType) {
                case NONE:
                    channel = Files.newByteChannel(Paths.get(base.path), StandardOpenOption.READ);
                    break;
                case GZIP:
                    channel = Channels.newChannel(new GZIPInputStream(new FileInputStream(base.path), GZIP_BUFFER_SIZE));
                    break;
                case BZIP2:
                    // no in Java support, so go via byte array
                    byte[] bzipUdata = RCompression.bzipUncompressFromFile(base.path);
                    channel = new SeekableMemoryByteChannel(bzipUdata);
                    break;
                case XZ:
                    channel = Channels.newChannel(new XZInputStream(new FileInputStream(base.path)));
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

        @Override
        public ReadableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            // TODO Auto-generated method stub
            return false;
        }

    }

    private static class FileWriteTextRConnection extends DelegateWriteRConnection {

        private final SeekableByteChannel channel;

        /**
         * Opens a file for writing.
         *
         * @param base The base connection.
         * @param append If {@code true}, the file cursor is positioned at the end and write
         *            operations append to the file. If {@code false}, the file will be truncated.
         * @throws IOException
         */
        FileWriteTextRConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base);
            List<OpenOption> opts = new ArrayList<>();
            opts.add(StandardOpenOption.WRITE);
            opts.add(StandardOpenOption.CREATE);
            if (append) {
                opts.add(StandardOpenOption.APPEND);
            } else {
                opts.add(StandardOpenOption.TRUNCATE_EXISTING);
            }

            channel = Files.newByteChannel(Paths.get(base.path), opts.toArray(new OpenOption[opts.size()]));
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            ReadWriteHelper.writeCharHelper(getChannel(), s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            ReadWriteHelper.writeStringHelper(getChannel(), s, nl, base.getEncoding());
        }

        @Override
        public SeekableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

    }

    static class FileReadBinaryRConnection extends DelegateReadRConnection {
        private final FileInputStream inputStream;

        FileReadBinaryRConnection(BasePathRConnection base) throws IOException {
            super(base);
            inputStream = new FileInputStream(base.path);
        }

        @Override
        public InputStream getInputStream() {
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
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
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

        @Override
        public ReadableByteChannel getChannel() {
            return inputStream.getChannel();
        }
    }

    private static class FileWriteBinaryConnection extends DelegateWriteRConnection {

        private final SeekableByteChannel channel;

        FileWriteBinaryConnection(BasePathRConnection base, boolean append, boolean blocking) throws IOException {
            super(base);

            List<OpenOption> args = new LinkedList<>();
            args.add(StandardOpenOption.WRITE);
            args.add(StandardOpenOption.CREATE);
            if (append) {
                args.add(StandardOpenOption.APPEND);
            } else {
                args.add(StandardOpenOption.TRUNCATE_EXISTING);
            }

            channel = Files.newByteChannel(Paths.get(base.path), args.toArray(new OpenOption[args.size()]));
        }

        @Override
        public WritableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return true;
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
            return super.readLines(n, warn, skipNul);
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
                writeString(lines.getDataAt(i), false);
                raf.write(sepData);
            }
            writeOffset = raf.getFilePointer();
            lastMode = SeekRWMode.WRITE;
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            raf.write(s.getBytes());
            if (nl) {
                raf.writeBytes(System.lineSeparator());
            }
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

        @Override
        public ByteChannel getChannel() {
            return raf.getChannel();
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

        public CompressedRConnection(String path, String modeString, Type cType, Charset encoding, int compression) throws IOException {
            super(path, mapConnectionClass(cType), modeString, AbstractOpenMode.ReadBinary, encoding);
            this.cType = cType;
            this.compression = compression;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            setDelegate(FileConnections.createDelegateConnection(this, cType));

        }

        // @Override
        /**
         * GnuR behavior for lazy connections is odd, e.g. gzfile returns "text", even though the
         * default mode is "rb".
         */
        // public boolean isTextMode() {
        // }
    }

    private static class CompressedInputRConnection extends DelegateReadRConnection {
        private final InputStream inputStream;
        private final boolean seekable;

        protected CompressedInputRConnection(BasePathRConnection base, InputStream is, boolean seekable) {
            super(base);
            this.inputStream = is;
            this.seekable = seekable;
        }

        @Override
        public ReadableByteChannel getChannel() {
            return Channels.newChannel(inputStream);
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            if (seekable) {
                // TODO
            }
            return super.seekInternal(offset, seekMode, seekRWMode);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
        }
    }

    private static class ByteStreamCompressedInputRConnection extends CompressedInputRConnection {
        ByteStreamCompressedInputRConnection(BasePathRConnection base, ByteArrayInputStream is, boolean seekable) {
            super(base, is, seekable);
        }
    }

    private static class CompressedOutputRConnection extends DelegateWriteRConnection {
        protected OutputStream outputStream;
        private final WritableByteChannel channel;
        private final boolean seekable;
        private long seekPosition = 0L;

        protected CompressedOutputRConnection(BasePathRConnection base, OutputStream os, boolean seekable) {
            super(base);
            this.outputStream = os;
            this.channel = Channels.newChannel(os);
            this.seekable = seekable;
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
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public WritableByteChannel getChannel() {
            return channel;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            if (seekable) {
                // TODO GZIP is basically seekable; however, the output stream does not allow any
                // seeking
                long oldPos = seekPosition;
                seekPosition = offset;
                return oldPos;
            }
            return super.seekInternal(offset, seekMode, seekRWMode);
        }

        @Override
        public boolean isSeekable() {
            return seekable;
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
            outputStream.close();
            // Now actually do the compression using sub-process
            byte[] data = bos.toByteArray();
            RCompression.bzipCompressToFile(data, ((BasePathRConnection) base).path, append);
        }
    }
}
