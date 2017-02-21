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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;

public class RawConnections {

    public static class RawRConnection extends BaseRConnection {

        private final SeekableMemoryByteChannel channel;
        private final String description;

        public RawRConnection(String description, byte[] dataTemp, String open) throws IOException {
            super(ConnectionClass.RAW, open, AbstractOpenMode.Read);
            this.channel = new SeekableMemoryByteChannel(dataTemp);
            this.description = description;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new RawReadRConnection(this, channel);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new RawWriteBinaryConnection(this, channel, false);
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
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }

        @Override
        public String getSummaryText() {
            return "binary";
        }

        public byte[] getValue() {
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
            super(base);
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
            // acquire copy from buffer without advancing the cursor
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
        public void writeBin(ByteBuffer buffer) throws IOException {
            channel.write(buffer);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return channel.getOutputStream();
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
    }

    private static class RawReadWriteConnection extends DelegateReadWriteRConnection {

        private final SeekableMemoryByteChannel channel;

        protected RawReadWriteConnection(BaseRConnection base, SeekableMemoryByteChannel channel, boolean append) {
            super(base);
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

    }

}
