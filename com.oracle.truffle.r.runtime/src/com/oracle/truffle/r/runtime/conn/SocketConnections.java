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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;

public class SocketConnections {
    /**
     * Base class for socket connections.
     *
     * While binary operations, e.g. {@code writeBin} are only legal on binary connections, text
     * operations are legal on text and binary connections.
     */
    public static class RSocketConnection extends BaseRConnection {
        protected final boolean server;
        protected final String host;
        protected final int port;
        protected final int timeout;

        public RSocketConnection(String modeString, boolean server, String host, int port, boolean blocking, int timeout) throws IOException {
            super(ConnectionClass.Socket, modeString, AbstractOpenMode.Read, blocking);
            this.server = server;
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = server ? new RServerSocketConnection(this) : new RClientSocketConnection(this);
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            return (server ? "<-" : "->") + host + ":" + port;
        }
    }

    private abstract static class RSocketReadWriteConnection extends DelegateReadWriteRConnection {
        private Socket socket;
        private SocketChannel socketChannel;
        protected OutputStream outputStream;
        protected final RSocketConnection thisBase;

        protected RSocketReadWriteConnection(RSocketConnection base) {
            super(base);
            this.thisBase = base;
        }

        protected void openStreams(Socket socketArg) throws IOException {
            this.socket = socketArg;
            this.socketChannel = socket.getChannel();
            if (thisBase.isBlocking()) {
                // Java (int) timeouts do not meet the POSIX standard of 31 days
                long millisTimeout = ((long) thisBase.timeout) * 1000;
                if (millisTimeout > Integer.MAX_VALUE) {
                    millisTimeout = Integer.MAX_VALUE;
                }
                socket.setSoTimeout((int) millisTimeout);
            } else {
                socketChannel.configureBlocking(false);
            }
            outputStream = socket.getOutputStream();
        }

        @Override
        public void flush() {
            // TODO add "throws"
            try {
                outputStream.flush();
            } catch (IOException e) {
                RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public void close() throws IOException {
            // TODO(fa) I'm not sure if socket.close() is the same as channel.close()
            socket.close();
        }

        @Override
        public ByteChannel getChannel() {
            return socketChannel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class RServerSocketConnection extends RSocketReadWriteConnection {
        private final Socket connectionSocket;

        RServerSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            InetSocketAddress addr = new InetSocketAddress(base.port);
            ServerSocket serverSocket = new ServerSocket();
            // we expect only one connection per-server socket; furthermore, we need to accommodate
            // for multiple connections being established locally on the same server port;
            // consequently, we close the server socket at the end of the constructor and allow
            // address reuse to be able to open the next connection after the current one closes
            serverSocket.setReuseAddress(true);
            serverSocket.bind(addr);
            connectionSocket = serverSocket.accept();
            openStreams(connectionSocket);
            serverSocket.close();
        }

        @Override
        public void close() throws IOException {
            super.close();
            connectionSocket.close();
        }
    }

    private static class RClientSocketConnection extends RSocketReadWriteConnection {

        RClientSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(base.host, base.port));
            openStreams(socketChannel.socket());
        }
    }
}
