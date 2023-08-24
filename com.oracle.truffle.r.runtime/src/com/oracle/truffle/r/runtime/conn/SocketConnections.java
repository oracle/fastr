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

import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode.Lazy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.OpenMode;

public class SocketConnections {
    /**
     * Base class for socket connections.
     *
     * While binary operations, e.g. {@code writeBin} are only legal on binary connections, text
     * operations are legal on text and binary connections.
     */
    public static final class RSocketConnection extends BaseRConnection {
        protected final boolean server;
        protected final String host;
        protected final int port;
        protected final int timeout;
        private String description;
        private RCompression.Type cType = RCompression.Type.NONE;

        public RSocketConnection(String modeString, boolean server, String host, int port, boolean blocking, int timeout, String encoding) throws IOException {
            super(ConnectionClass.Socket, modeString, AbstractOpenMode.Read, blocking, encoding);
            this.server = server;
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            this.description = (server ? "<-" : "->") + host + ":" + port;
            openNonLazyConnection();
        }

        @Override
        public boolean canRead() {
            if (cType == RCompression.Type.GZIP) {
                // yes, this means that even if we created a 'read' connection
                // once 'gzcon' was applied, it can't be read anymore
                // see also: gzcon_open() in main/connections.c
                return !canWrite();
            } else {
                // non gz socket connections can always be read
                return true;
            }
        }

        @Override
        public boolean canWrite() {
            // socket connections can always be written
            return true;
        }

        @Override
        @TruffleBoundary
        protected void createDelegateConnection() throws IOException {
            setDelegate(createDelegateConnectionImpl());
        }

        private DelegateRConnection createDelegateConnectionImpl() throws IOException {
            DelegateRConnection delegate;
            if (cType == RCompression.Type.GZIP) {
                delegate = new RClientSocketGZipConnection(this);
            } else if (server) {
                delegate = new RServerSocketConnection(this);
            } else {
                if (isBlocking()) {
                    delegate = new RClientSocketConnection(this);
                } else {
                    delegate = new RClientSocketNonBlockConnection(this);
                }
            }
            return delegate;
        }

        @Override
        public String getSummaryDescription() {
            return description;
        }

        @Override
        @TruffleBoundary
        public void setCompressionType(RCompression.Type cType) throws IOException {
            assert cType == RCompression.Type.GZIP;

            ConnectionSupport.OpenMode openMode = getOpenMode();
            AbstractOpenMode mode = openMode.abstractOpenMode;
            if (mode == Lazy) {
                mode = AbstractOpenMode.getOpenMode(openMode.modeString);
            }
            ConnectionSupport.OpenMode newOpenMode = null;
            switch (mode) {
                case Read:
                    if (!"r".equals(openMode.modeString)) {
                        throw RError.error(RError.SHOW_CALLER, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
                    }
                    newOpenMode = new OpenMode(AbstractOpenMode.ReadBinary);
                    break;
                case Write:
                    if (!"w".equals(openMode.modeString)) {
                        throw RError.error(RError.SHOW_CALLER, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
                    }
                    newOpenMode = new OpenMode(AbstractOpenMode.WriteBinary);
                    break;
                case ReadBinary:
                case WriteBinary:
                    newOpenMode = getOpenMode();
                    break;
                default:
                    throw RError.error(RError.SHOW_CALLER, RError.Message.CAN_USE_ONLY_R_OR_W_CONNECTIONS);
            }
            assert newOpenMode != null;

            this.cType = cType;
            description = new StringBuilder().append("gzcon(").append(description).append(")").toString();
            setDelegate(createDelegateConnectionImpl(), opened, newOpenMode);

        }

        @TruffleBoundary
        public static byte[] select(RSocketConnection[] socketConnections, boolean write, long timeout) throws IOException {
            int op = write ? SelectionKey.OP_WRITE : SelectionKey.OP_READ;

            HashMap<RSocketConnection, SelectionKey> table = new HashMap<>();
            Selector selector = Selector.open();
            for (RSocketConnection con : socketConnections) {
                con.checkOpen();

                SocketChannel sc = (SocketChannel) con.theConnection.getChannel();
                sc.configureBlocking(false);
                table.put(con, sc.register(selector, op));
            }
            int select;
            if (timeout >= 0) {
                select = selector.select(timeout);
            } else {
                select = selector.select();
            }

            byte[] result = new byte[socketConnections.length];
            if (select > 0) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                for (int i = 0; i < result.length; i++) {
                    result[i] = RRuntime.asLogical(selectedKeys.contains(table.get(socketConnections[i])));
                }
            }
            return result;
        }
    }

    private interface RSocketDelegateConection {
        SocketChannel getSocketChannel();
    }

    private abstract static class RSocketReadWriteConnection extends DelegateReadWriteRConnection implements RSocketDelegateConection {
        private Socket socket;
        private SocketChannel channel;
        protected final RSocketConnection thisBase;

        protected RSocketReadWriteConnection(RSocketConnection base) {
            super(base, 0);
            this.thisBase = base;
        }

        protected void openStreams(SocketChannel socketArg) throws IOException {
            channel = socketArg;
            socket = socketArg.socket();
            if (thisBase.isBlocking()) {
                channel.configureBlocking(true);
                // Java (int) timeouts do not meet the POSIX standard of 31 days
                long millisTimeout = ((long) thisBase.timeout) * 1000;
                if (millisTimeout > Integer.MAX_VALUE) {
                    millisTimeout = Integer.MAX_VALUE;
                }
                socket.setSoTimeout((int) millisTimeout);
            } else {
                channel.configureBlocking(false);
            }
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        @Override
        public SocketChannel getSocketChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private abstract static class RSocketGZipConnection extends DelegateReadWriteRConnection {
        private Socket socket;
        private ByteChannel channel;

        protected RSocketGZipConnection(RSocketConnection base) {
            super(base, 0);
        }

        protected void openStreams(SocketChannel socketArg) throws IOException {
            socket = socketArg.socket();
            if (!socketArg.isBlocking()) {
                // have to block with GZIPOutputStream
                socketArg.configureBlocking(true);
            }
            GZIPOutputStream gzipOS = new GZIPOutputStream(socket.getOutputStream());
            WritableByteChannel writableChannel = Channels.newChannel(gzipOS);
            channel = new ByteChannel() {
                @Override
                public int read(ByteBuffer dst) throws IOException {
                    // read not possible for gzcon-ed socket connections
                    // see also #canRead()
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.CANNOT_READ_CONNECTION);
                }

                @Override
                public boolean isOpen() {
                    return writableChannel.isOpen();
                }

                @Override
                public void close() throws IOException {
                    writableChannel.close();
                }

                @Override
                public int write(ByteBuffer src) throws IOException {
                    return writableChannel.write(src);
                }
            };
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

    private abstract static class RSocketReadWriteNonBlockConnection extends DelegateReadWriteRConnection implements RSocketDelegateConection {
        private Socket socket;
        private SocketChannel socketChannel;

        protected RSocketReadWriteNonBlockConnection(RSocketConnection base) {
            super(base, 0);
        }

        protected void openStreams(Socket socketArg) throws IOException {
            this.socket = socketArg;
            this.socketChannel = socket.getChannel();
            socketChannel.configureBlocking(false);
        }

        @Override
        public void close() throws IOException {
            socketChannel.close();
            socket.close();
        }

        @Override
        public ByteChannel getChannel() {
            return socketChannel;
        }

        @Override
        public SocketChannel getSocketChannel() {
            return socketChannel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class RServerSocketConnection extends RSocketReadWriteConnection implements RSocketDelegateConection {
        private final SocketChannel connectionSocket;

        RServerSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            InetSocketAddress addr = new InetSocketAddress(base.port);
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

            // we expect only one connection per-server socket; furthermore, we need to accommodate
            // for multiple connections being established locally on the same server port;
            // consequently, we close the server socket at the end of the constructor and allow
            // address reuse to be able to open the next connection after the current one closes
            serverSocketChannel.socket().setReuseAddress(true);
            serverSocketChannel.socket().bind(addr);
            connectionSocket = serverSocketChannel.accept();
            openStreams(connectionSocket);
            serverSocketChannel.close();
        }

        @Override
        public void close() throws IOException {
            super.close();
            connectionSocket.close();
        }

        @Override
        public ByteChannel getChannel() {
            return connectionSocket;
        }

        @Override
        public SocketChannel getSocketChannel() {
            return connectionSocket;
        }

    }

    private static class RClientSocketConnection extends RSocketReadWriteConnection {

        RClientSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(base.host, base.port));
            openStreams(socketChannel);
        }
    }

    private static class RClientSocketNonBlockConnection extends RSocketReadWriteNonBlockConnection {

        RClientSocketNonBlockConnection(RSocketConnection base) throws IOException {
            super(base);
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(base.host, base.port));
            openStreams(socketChannel.socket());
        }
    }

    private static class RClientSocketGZipConnection extends RSocketGZipConnection {
        RClientSocketGZipConnection(RSocketConnection base) throws IOException {
            super(base);
            assert base.theConnection instanceof RSocketDelegateConection : "can make gzcon only from a RSocketDelegateConection";
            SocketChannel socketChannel = ((RSocketDelegateConection) base.theConnection).getSocketChannel();
            openStreams(socketChannel);
        }
    }
}
