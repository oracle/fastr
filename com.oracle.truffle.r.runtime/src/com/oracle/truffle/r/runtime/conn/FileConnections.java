/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.conn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZ;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RCompression.Type;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode.Lazy;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BasePathRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.DelegateRConnection.CompressedInputRConnection;
import com.oracle.truffle.r.runtime.conn.DelegateRConnection.CompressedOutputRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileConnections {

    /**
     * Base class for all modes of file connections.
     */
    public static class FileRConnection extends BasePathRConnection {
        private final boolean raw;
        private final boolean internal;
        private Type cType = RCompression.Type.NONE;

        public FileRConnection(String description, TruffleFile path, String modeString, boolean blocking, String encoding, boolean raw, boolean internal) throws IOException {
            super(description, path, ConnectionClass.File, modeString, blocking, encoding);
            this.raw = raw;
            this.internal = internal;
            openNonLazyConnection();
        }

        @Override
        public boolean isInternal() {
            return internal;
        }

        @Override
        @TruffleBoundary
        protected void createDelegateConnection() throws IOException {
            setDelegate(createDelegateConnectionImpl());
        }

        private DelegateRConnection createDelegateConnectionImpl() throws IOException {
            return FileConnections.createDelegateConnection(this, cType, raw);
        }

        @TruffleBoundary
        @Override
        public void setCompressionType(Type cType) throws IOException {
            ConnectionSupport.OpenMode openMode = getOpenMode();
            AbstractOpenMode mode = openMode.abstractOpenMode;
            if (mode == Lazy) {
                mode = AbstractOpenMode.getOpenMode(openMode.modeString);
            }
            if (canRead() && canWrite()) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
            }
            ConnectionSupport.OpenMode newOpenMode = null;
            switch (mode) {
                case ReadBinary:
                case WriteBinary:
                    newOpenMode = getOpenMode();
                    break;
                case Read:
                    if (!"r".equals(openMode.modeString)) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
                    }
                    newOpenMode = new ConnectionSupport.OpenMode(AbstractOpenMode.ReadBinary);
                    break;
                case Write:
                    if (!"w".equals(openMode.modeString)) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
                    }
                    newOpenMode = new ConnectionSupport.OpenMode(AbstractOpenMode.WriteBinary);
                    break;
                default:
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
            }
            assert newOpenMode != null;

            String modeString = openMode.modeString;
            if ("w".equals(modeString)) {
                RError.warning(RError.SHOW_CALLER, RError.Message.USING_TEXT_MODE_NOT_WORK_CORRECTLY, "file");
            }
            assert cType == RCompression.Type.GZIP;
            this.cType = cType;
            description = new StringBuilder().append("gzcon(").append(description).append(")").toString();
            if (mode == AbstractOpenMode.ReadBinary || !canRead()) {
                // see createDelegateConnection
                // compressed mode isn't set implicitely for ReadBinary
                setDelegate(createDelegateConnectionImpl(), opened, newOpenMode);
            } else {
                setOpenMode(newOpenMode);
            }
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

        public CompressedRConnection(TruffleFile path, String modeString, Type cType, String encoding, int compression) throws IOException {
            super(path.getPath(), path, mapConnectionClass(cType), modeString, AbstractOpenMode.ReadBinary, encoding);
            this.cType = cType;
            this.compression = compression;
            openNonLazyConnection();
        }

        @Override
        @TruffleBoundary
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
                delegate = new FileReadWriteTextConnection(base, false);
                break;
            case ReadWriteTruncBinary:
                delegate = new FileReadWriteBinaryConnection(base, false);
                break;
            case ReadAppend:
                delegate = new FileReadWriteTextConnection(base, true);
                break;
            case ReadAppendBinary:
                delegate = new FileReadWriteBinaryConnection(base, true);
                break;
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
        return delegate;
    }

    private static DelegateRConnection createXZDelegateConnection(BasePathRConnection base) throws IOException {

        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                return new CompressedInputRConnection(base, new XZInputStream(base.path.newInputStream()));
            case Append:
            case AppendBinary:
                TruffleFile afile = base.path;
                return new CompressedOutputRConnection(base, new XZOutputStream(afile.newOutputStream(StandardOpenOption.APPEND), new LZMA2Options(), XZ.CHECK_CRC32), false);
            case Write:
            case WriteBinary:
                TruffleFile wfile = base.path;
                return new CompressedOutputRConnection(base, new XZOutputStream(wfile.newOutputStream(), new LZMA2Options(), XZ.CHECK_CRC32), false);
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
    }

    private static DelegateRConnection createBZIP2DelegateConnection(BasePathRConnection base) throws IOException {

        switch (base.getOpenMode().abstractOpenMode) {
            case Read:
            case ReadBinary:
                byte[] bzipUdata = RCompression.bzipUncompressFromFile(base.path);
                return new ByteStreamCompressedInputRConnection(base, new ByteArrayInputStream(bzipUdata));
            case Append:
            case AppendBinary:
                return new BZip2OutputRConnection(base, new ByteArrayOutputStream(), true);
            case Write:
            case WriteBinary:
                return new BZip2OutputRConnection(base, new ByteArrayOutputStream(), false);
            default:
                throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
        }
    }

    @TruffleBoundary
    private static DelegateRConnection createDelegateConnection(BasePathRConnection base, RCompression.Type cType, boolean raw) throws IOException {
        AbstractOpenMode openMode = base.getOpenMode().abstractOpenMode;

        /*
         * For input, we check the actual compression type as GNU R is permissive about the claimed
         * type except 'raw' is true.
         */
        final RCompression.Type cTypeActual;
        if (!raw && (openMode == AbstractOpenMode.Read || openMode == AbstractOpenMode.ReadBinary)) {
            RCompression.Type cTypeFound = RCompression.getCompressionType(base.path);
            // For binary reading force file's compression type if compression exists
            // and it conflicts with requested compression type.
            if (cTypeFound != cType && (openMode != AbstractOpenMode.ReadBinary || cType != RCompression.Type.NONE)) {
                cTypeActual = cTypeFound;
                base.updateConnectionClass(mapConnectionClass(cTypeActual));
            } else {
                cTypeActual = cType;
            }
        } else {
            cTypeActual = cType;
        }

        switch (cTypeActual) {
            case NONE:
                return createUncompressedDelegateConnection(base);
            case GZIP:
                switch (base.getOpenMode().abstractOpenMode) {
                    case Read:
                    case ReadBinary:
                        return DelegateRConnection.createGZIPDelegateInputConnection(base, base.path.newInputStream());
                    case Append:
                    case AppendBinary:
                        return DelegateRConnection.createGZIPDelegateOutputConnection(base, base.path.newOutputStream(StandardOpenOption.APPEND));
                    case Write:
                    case WriteBinary:
                        return DelegateRConnection.createGZIPDelegateOutputConnection(base, base.path.newOutputStream());
                    default:
                        throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + base.getOpenMode());
                }
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

        private final SeekableByteChannel channel;

        FileReadBinaryRConnection(BasePathRConnection base) throws IOException {
            super(base);
            channel = base.path.newByteChannel(Collections.singleton(StandardOpenOption.READ));
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return DelegateRConnection.seek(channel, offset, seekMode, seekRWMode, bytesInCache());
        }

        @Override
        public ByteChannel getChannel() {
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

        private final SeekableByteChannel channel;

        FileWriteBinaryConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base, 0);
            Set<OpenOption> opts = new HashSet<>();
            opts.add(StandardOpenOption.WRITE);
            opts.add(StandardOpenOption.CREATE);
            if (append) {
                opts.add(StandardOpenOption.APPEND);
            } else {
                opts.add(StandardOpenOption.TRUNCATE_EXISTING);
            }
            channel = base.path.newByteChannel(opts);
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return DelegateRConnection.seek(channel, offset, seekMode, seekRWMode, bytesInCache());
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        @Override
        public void truncate() throws IOException {
            channel.truncate(channel.position());
        }
    }

    private static class FileReadWriteTextConnection extends DelegateReadWriteRConnection {

        private final SeekableByteChannel channel;
        private long readOffset;
        private long writeOffset;
        private SeekRWMode lastMode = SeekRWMode.READ;

        FileReadWriteTextConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base);
            Set<OpenOption> opts = new HashSet<>();
            opts.add(StandardOpenOption.READ);
            opts.add(StandardOpenOption.WRITE);
            opts.add(StandardOpenOption.CREATE);
            channel = base.path.newByteChannel(opts);
            if (append) {
                writeOffset = channel.size();
            } else {
                channel.truncate(0);
            }
        }

        @Override
        public int getc() throws IOException {
            setReadPosition();
            final int value = super.readInternal();
            if (value != -1) {
                readOffset++;
            }
            return value;
        }

        @Override
        protected void updateReadOffset(int nBytesConsumed) {
            readOffset += nBytesConsumed;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            long result;
            boolean set = true;
            switch (seekMode) {
                case ENQUIRE:
                    set = false;
                    break;
                case START:
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, "seek mode");
            }
            switch (seekRWMode) {
                case LAST:
                    if (lastMode == SeekRWMode.READ) {
                        result = readOffset;
                        if (set) {
                            readOffset = offset;
                        }
                    } else {
                        result = writeOffset;
                        if (set) {
                            writeOffset = offset;
                        }
                    }
                    break;
                case READ:
                    result = readOffset;
                    if (set) {
                        readOffset = offset;
                    }
                    break;
                case WRITE:
                    result = writeOffset;
                    if (set) {
                        writeOffset = offset;
                    }
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, "seek mode");
            }
            return result;
        }

        @Override
        public String[] readLines(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
            setReadPosition();
            return super.readLines(n, warn, skipNul);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            setReadPosition();
            return super.readBin(buffer);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            setReadPosition();
            return super.readChar(nchars, useBytes);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            setReadPosition();
            return super.readBinChars();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            setWritePosition();
            super.writeLines(lines, sep, useBytes);
            writeOffset = channel.position();
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            setWritePosition();
            super.writeBin(buffer);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            setWritePosition();
            super.writeChar(s, pad, eos, useBytes);
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            setWritePosition();
            super.writeString(s, nl);
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        private void setReadPosition() throws IOException {
            if (lastMode != SeekRWMode.READ) {
                channel.position(readOffset);
                lastMode = SeekRWMode.READ;
            }
        }

        private void setWritePosition() throws IOException {
            if (lastMode != SeekRWMode.WRITE) {
                channel.position(writeOffset);
                lastMode = SeekRWMode.WRITE;
            }
        }

        @Override
        public void truncate() throws IOException {
            channel.truncate(writeOffset);
            lastMode = SeekRWMode.WRITE;
            // GnuR also freshly queries the file pointer. It may happen that the file pointer is
            // different as expected.
            writeOffset = channel.position();
        }
    }

    private static class FileReadWriteBinaryConnection extends DelegateReadWriteRConnection {
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

        FileReadWriteBinaryConnection(BasePathRConnection base, boolean append) throws IOException {
            super(base);
            raf = new RandomAccessFile(base.path.getPath(), "rw");
            if (append) {
                writeOffset = raf.length();
            } else {
                raf.setLength(0);
            }
        }

        @Override
        public int getc() throws IOException {
            setReadPosition();
            final int value = raf.read();
            if (value != -1) {
                readOffset++;
            }
            return value;
        }

        @Override
        protected void updateReadOffset(int nBytesConsumed) {
            readOffset += nBytesConsumed;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            long result;
            switch (seekMode) {
                case ENQUIRE:
                case START:
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, "seek mode");
            }
            switch (seekRWMode) {
                case LAST:
                    if (lastMode == SeekRWMode.READ) {
                        result = readOffset;
                        if (seekMode != SeekMode.ENQUIRE) {
                            readOffset = offset;
                        }
                    } else {
                        result = writeOffset;
                        if (seekMode != SeekMode.ENQUIRE) {
                            writeOffset = offset;
                        }
                    }
                    break;
                case READ:
                    result = readOffset;
                    if (seekMode != SeekMode.ENQUIRE) {
                        readOffset = offset;
                    }
                    break;
                case WRITE:
                    result = writeOffset;
                    if (seekMode != SeekMode.ENQUIRE) {
                        writeOffset = offset;
                    }
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
            return result;
        }

        @Override
        public String[] readLines(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
            setReadPosition();
            // the readOffset field is updated from within super.readLines via the overridden
            // updateReadOffset
            return super.readLines(n, warn, skipNul);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            setReadPosition();
            try {
                return super.readBin(buffer);
            } finally {
                readOffset = raf.getFilePointer();
            }
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            setReadPosition();
            try {
                return super.readChar(nchars, useBytes);
            } finally {
                readOffset = raf.getFilePointer();
            }
        }

        @Override
        public byte[] readBinChars() throws IOException {
            setReadPosition();
            try {
                return super.readBinChars();
            } finally {
                readOffset = raf.getFilePointer();
            }
        }

        @TruffleBoundary
        private void setReadPosition() throws IOException {
            if (lastMode != SeekRWMode.READ) {
                raf.seek(readOffset);
                lastMode = SeekRWMode.READ;
            }
        }

        @TruffleBoundary
        private void setWritePosition() throws IOException {
            if (lastMode != SeekRWMode.WRITE) {
                raf.seek(writeOffset);
                lastMode = SeekRWMode.WRITE;
            }
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            setWritePosition();
            super.writeLines(lines, sep, useBytes);
            writeOffset = raf.getFilePointer();
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            setWritePosition();
            super.writeBin(buffer);
            writeOffset = raf.getFilePointer();
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            setWritePosition();
            super.writeChar(s, pad, eos, useBytes);
            writeOffset = raf.getFilePointer();
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            setWritePosition();
            super.writeString(s, nl);
            writeOffset = raf.getFilePointer();
        }

        @Override
        public ByteChannel getChannel() {
            return raf.getChannel();
        }

        @Override
        public void truncate() throws IOException {
            raf.setLength(writeOffset);
            lastMode = SeekRWMode.WRITE;
            // GnuR also freshly queries the file pointer. It may happen that the file pointer is
            // different as expected.
            writeOffset = raf.getFilePointer();
        }
    }

    private static class ByteStreamCompressedInputRConnection extends CompressedInputRConnection {
        ByteStreamCompressedInputRConnection(BasePathRConnection base, ByteArrayInputStream is) {
            super(base, is);
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
