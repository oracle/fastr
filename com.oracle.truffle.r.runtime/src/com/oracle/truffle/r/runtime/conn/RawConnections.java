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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateReadWriteRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.DelegateWriteRConnection;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

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

        public static long seek(SeekableMemoryByteChannel channel, long offset, SeekMode seekMode, @SuppressWarnings("unused") SeekRWMode seekRWMode) throws IOException {
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

    static class RawReadRConnection extends DelegateReadRConnection implements ReadWriteHelper {
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
        public int readBin(ByteBuffer buffer) throws IOException {
            return channel.read(buffer);
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

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            return readLinesHelper(channel.getInputStream(), n, warn, skipNul);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, channel.getInputStream(), useBytes);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return channel.getInputStream();
        }

        @Override
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public OutputStream getOutputStream() {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.NOT_AN_OUTPUT_RAW_CONNECTION);
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }
    }

    private static class RawWriteBinaryConnection extends DelegateWriteRConnection implements ReadWriteHelper {
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
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(channel.getOutputStream(), s, pad, eos);
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
        public void closeAndDestroy() throws IOException {
            base.closed = true;
            close();
        }

        @Override
        public void close() throws IOException {
            flush();
            channel.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                String line = lines.getDataAt(i);
                channel.write(line.getBytes());
                channel.write(sep.getBytes());
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            writeStringHelper(channel.getOutputStream(), s, nl);
        }

        @Override
        public void flush() throws IOException {
            // nothing to do
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }
    }

    private static class RawReadWriteConnection extends DelegateReadWriteRConnection implements ReadWriteHelper {

        private final SeekableMemoryByteChannel channel;

        protected RawReadWriteConnection(BaseRConnection base, SeekableMemoryByteChannel channel, boolean append) {
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
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return channel.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return channel.getOutputStream();
        }

        @Override
        public void closeAndDestroy() throws IOException {
            close();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                channel.write(lines.getDataAt(i).getBytes());
                channel.write(sep.getBytes());
            }
        }

        @Override
        public void flush() throws IOException {
            // nothing to do
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            channel.write(s.getBytes());
            if (nl) {
                channel.write(System.lineSeparator().getBytes());
            }
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(channel.getOutputStream(), s, pad, eos);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, channel.getInputStream(), useBytes);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            channel.write(buffer);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return channel.read(buffer);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RInternalError.unimplemented();
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            return RawRConnection.seek(channel, offset, seekMode, seekRWMode);
        }

    }

}
