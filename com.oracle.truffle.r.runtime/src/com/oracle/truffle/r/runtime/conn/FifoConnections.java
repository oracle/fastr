/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.context.RContext;

public class FifoConnections {

    public static final class FifoRConnection extends BaseRConnection {

        private final String path;

        public FifoRConnection(String path, String open, boolean blocking, String encoding) throws IOException {
            super(ConnectionClass.FIFO, open, AbstractOpenMode.Read, blocking, encoding);
            this.path = path;
            openNonLazyConnection();
        }

        @Override
        @TruffleBoundary
        protected void createDelegateConnection() throws IOException {
            final DelegateRConnection delegate;
            if (isBlocking()) {
                delegate = createBlockingDelegate();
            } else {
                delegate = createNonBlockingDelegate();
            }
            setDelegate(delegate);
        }

        private DelegateRConnection createBlockingDelegate() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new FifoReadRConnection(this, path);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new FifoWriteConnection(this, path);
                    break;
                case ReadAppend:
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new FifoReadWriteConnection(this, path);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            return delegate;
        }

        private DelegateRConnection createNonBlockingDelegate() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new FifoReadNonBlockingRConnection(this, path);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new FifoWriteNonBlockingRConnection(this, path);
                    break;
                case ReadAppend:
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new FifoReadWriteNonBlockingRConnection(this, path);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            return delegate;
        }

        @Override
        public String getSummaryDescription() {
            return path;
        }
    }

    static class FifoReadRConnection extends DelegateReadRConnection {
        private final SeekableByteChannel channel;

        protected FifoReadRConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            channel = RContext.getInstance().getSafeTruffleFile(path).newByteChannel(Collections.singleton(StandardOpenOption.READ));
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

    private static class FifoWriteConnection extends DelegateWriteRConnection {
        private final SeekableByteChannel channel;

        FifoWriteConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            this.channel = createAndOpenFifo(path);
        }

        @Override
        public SeekableByteChannel getChannel() {
            return channel;
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class FifoReadWriteConnection extends DelegateReadWriteRConnection {

        private final SeekableByteChannel channel;

        protected FifoReadWriteConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            this.channel = createAndOpenFifo(path);
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }
    }

    static class FifoReadNonBlockingRConnection extends DelegateReadRConnection {
        private final SeekableByteChannel channel;

        protected FifoReadNonBlockingRConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            channel = RContext.getInstance().getSafeTruffleFile(path).newByteChannel(Collections.singleton(StandardOpenOption.READ));
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

    private static class FifoWriteNonBlockingRConnection extends DelegateWriteRConnection {
        private final SeekableByteChannel channel;

        FifoWriteNonBlockingRConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            channel = createAndOpenNonBlockingFifo(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
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

    private static class FifoReadWriteNonBlockingRConnection extends DelegateReadWriteRConnection {
        private final SeekableByteChannel channel;

        FifoReadWriteNonBlockingRConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            channel = createAndOpenNonBlockingFifo(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
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

    private static final String MKFIFO_ERROR_FILE_EXISTS = "File exists";

    private static SeekableByteChannel createAndOpenFifo(String path) throws IOException {
        TruffleFile truffleFile = RContext.getInstance().getSafeTruffleFile(path);
        if (!truffleFile.exists()) {
            // try to create fifo on demand
            createNamedPipe(path);
        }
        return truffleFile.newByteChannel(EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ));
    }

    private static SeekableByteChannel createAndOpenNonBlockingFifo(String path, OpenOption... openOptions) throws IOException {
        TruffleFile truffleFile = RContext.getInstance().getSafeTruffleFile(path);
        if (!truffleFile.exists()) {
            // try to create fifo on demand
            createNamedPipe(path);
        }
        return truffleFile.newByteChannel(new HashSet<>(Arrays.asList(openOptions)));
    }

    /**
     * Creates a named pipe (FIFO) by invoking {@code mkfifo} in a shell.
     *
     * <b>NOTE:</b> This is a Linux-specific operation and won't work on other OSes.
     *
     * @param path The path to the named pipe.
     * @throws IOException
     */
    @TruffleBoundary
    private static void createNamedPipe(String path) throws IOException {

        String[] command = new String[]{"mkfifo", path};
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        InputStream is = p.getInputStream();
        ProcessOutputManager.OutputThreadVariable readThread = new ProcessOutputManager.OutputThreadVariable(command[0], is);
        readThread.start();
        try {
            int rc = p.waitFor();
            if (rc == 0) {
                readThread.join();
                String msg = new String(Arrays.copyOf(readThread.getData(), readThread.getTotalRead()));

                // if the message is not empty and it is not the "file exists" error, then throw
                // error
                if (!msg.isEmpty() && !msg.endsWith(MKFIFO_ERROR_FILE_EXISTS)) {
                    throw new IOException(msg);
                }
            }
        } catch (InterruptedException ex) {
            // fall through
        }
    }
}
