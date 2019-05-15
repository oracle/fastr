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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RCompression;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode.Lazy;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import java.io.BufferedInputStream;
import java.nio.channels.ByteChannel;

public class RawConnections {

    public static class RawRConnection extends BaseRConnection {

        private final SeekableMemoryByteChannel channel;
        private String description;
        private RCompression.Type cType = RCompression.Type.NONE;

        public RawRConnection(String description, byte[] dataTemp, String open) throws IOException {
            super(ConnectionClass.RAW, open, AbstractOpenMode.Read);
            this.channel = new SeekableMemoryByteChannel(dataTemp);
            this.description = description;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            setDelegate(createDelegateConnectionImpl());
        }

        private DelegateRConnection createDelegateConnectionImpl() throws IOException, RError {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = createReadConection();
                    break;
                case Write:
                case WriteBinary:
                    delegate = createWriteBinaryConection();
                    break;
                case Append:
                    delegate = new RawWriteBinaryConnection(this, channel, true);
                    break;
                case ReadAppend:
                    delegate = new RawReadWriteConnection(this, channel, true);
                    break;
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new RawReadWriteConnection(this, channel, false);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            return delegate;
        }

        @TruffleBoundary
        @Override
        public void setCompressionType(RCompression.Type cType) throws IOException {
            assert cType == RCompression.Type.GZIP;

            ConnectionSupport.OpenMode openMode = getOpenMode();
            AbstractOpenMode mode = openMode.abstractOpenMode;
            if (mode == Lazy) {
                mode = AbstractOpenMode.getOpenMode(openMode.modeString);
            }
            switch (mode) {
                case Append:
                case ReadAppend:
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                case Write:
                    if ("w".equals(openMode.modeString)) {
                        break;
                    }
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
            }

            this.cType = cType;
            description = new StringBuilder().append("gzcon(").append(description).append(")").toString();
            setDelegate(createDelegateConnectionImpl(), opened);
        }

        private DelegateRConnection createReadConection() throws IOException {
            switch (cType) {
                case NONE:
                    return new RawReadRConnection(this, channel);
                case GZIP:
                    return new RawReadGZipRConnection(this, channel);
                default:
                    throw RInternalError.shouldNotReachHere("unsupported compression type. Can be GZIP or NONE");
            }
        }

        private DelegateRConnection createWriteBinaryConection() throws IOException {
            switch (cType) {
                case NONE:
                    return new RawWriteBinaryConnection(this, channel, false);
                case GZIP:
                    return new RawWriteGZipConnection(this, channel);
                default:
                    throw RInternalError.shouldNotReachHere("unsupported compression type. Can be GZIP or NONE");
            }
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }

        @Override
        public boolean isTextMode() {
            return false;
        }

        public byte[] getValue() {
            if (getOpenMode().canWrite() && cType == RCompression.Type.GZIP) {
                try {
                    // force writing of gzip trailer
                    theConnection.close();
                    // new delegate opened for a potentional append
                    // TODO would be better to have it only on demand
                    setDelegate(createDelegateConnectionImpl(), opened);
                } catch (IOException ex) {
                    throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, ex.getMessage());
                }
            }
            return channel.getBuffer();
        }

        private static long seek(SeekableByteChannel channel, long offset, SeekMode seekMode, @SuppressWarnings("unused") SeekRWMode seekRWMode) throws IOException {
            final long origPos = channel.position();
            final long newPos;
            switch (seekMode) {
                case START:
                    newPos = offset;
                    break;
                case CURRENT:
                    newPos = origPos + offset;
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER, "seek mode");
            }
            if (newPos >= 0L && newPos <= channel.size()) {
                channel.position(newPos);
            } else {
                throw RError.error(RError.SHOW_CALLER, RError.Message.SEEK_OUTSITE_RAW_CONNECTION);
            }
            return origPos;
        }
    }

    static class RawReadRConnection extends DelegateReadRConnection {
        private SeekableMemoryByteChannel channel;

        RawReadRConnection(BaseRConnection base, SeekableMemoryByteChannel channel) {
            super(base, 0);
            this.channel = channel;
            try {
                channel.position(0L);
            } catch (IOException e) {
                RInternalError.shouldNotReachHere();
            }
        }

        RawReadRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            // acquire copy from buffer without advancing the cursorskipNul
            final byte[] buffer = channel.getBufferFromCursor();
            int i = 0;
            while (i < buffer.length && buffer[i] != (byte) 0) {
                i++;
            }
            if (i < buffer.length) {
                ByteBuffer readBuf = ByteBuffer.allocate(i + 1);
                channel.read(readBuf);
                return readBuf.array();
            }
            return new byte[0];
        }

        @Override
        public long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }

        @Override
        public SeekableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }
    }

    static class RawReadGZipRConnection extends DelegateReadRConnection {
        private ByteChannel channel;

        RawReadGZipRConnection(BaseRConnection base, SeekableMemoryByteChannel channel) throws IOException {
            super(base, 0);
            try {
                // we have to reset the position
                // gzipstream checks the header
                channel.position(0L);
            } catch (IOException e) {
                RInternalError.shouldNotReachHere();
            }
            this.channel = createGZIPDelegateInputConnection(base, new BufferedInputStream(channel.getInputStream()));
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class RawWriteBinaryConnection extends DelegateWriteRConnection {
        private final SeekableMemoryByteChannel channel;

        RawWriteBinaryConnection(BaseRConnection base, SeekableMemoryByteChannel channel, boolean append) {
            super(base);
            this.channel = Objects.requireNonNull(channel);
            if (!append) {
                try {
                    channel.position(0);
                } catch (IOException e) {
                    RInternalError.shouldNotReachHere();
                }
            }
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }

        @Override
        public SeekableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public void truncate() throws IOException {
            channel.truncate(channel.position());
        }
    }

    private static class RawWriteGZipConnection extends DelegateWriteRConnection {
        private final DelegateRConnection channel;

        RawWriteGZipConnection(BaseRConnection base, SeekableMemoryByteChannel channel) throws IOException {
            super(base);
            this.channel = createGZIPDelegateOutputConnection(base, channel.getOutputStream());
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class RawReadWriteConnection extends DelegateReadWriteRConnection {

        private final SeekableMemoryByteChannel channel;

        protected RawReadWriteConnection(BaseRConnection base, SeekableMemoryByteChannel channel, boolean append) {
            super(base, 0);
            this.channel = channel;
            if (append) {
                try {
                    channel.position(channel.size());
                } catch (IOException e) {
                    RInternalError.shouldNotReachHere();
                }
            }
        }

        @Override
        protected long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }

        @Override
        public SeekableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public void truncate() throws IOException {
            channel.truncate(channel.position());
        }
    }
}
