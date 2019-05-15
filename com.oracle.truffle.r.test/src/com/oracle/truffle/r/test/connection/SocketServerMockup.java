/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Basic support to test socket connections. Simply start a server or client socket, connect with a
 * respective connection and start writing, reading, etc.
 * 
 * <p>
 * To use from FastR via java interop:
 * </p>
 * <ul>
 * <li>start FastR<br>
 * 
 * <pre>
 * mx R --jvm
 * </pre>
 * 
 * </li>
 * <li>add fastr-unit-tests.jar to classpath<br>
 * 
 * <pre>
 * &gt; java.addToClasspath("{FASTR_REPO}/mxbuild/dists/jdk1.8/fastr-unit-tests.jar")
 * </pre>
 * 
 * </li>
 * <li>get reference to java class<br>
 * 
 * <pre>
 * &gt; ssm &lt;- java.type('com.oracle.truffle.r.test.connection.SocketServerMockup')
 * </pre>
 * 
 * </li>
 * <li>and e.g. start server socket, connect with client connection, ...<br>
 * 
 * <pre>
 * &gt; PORT &lt;- 10003
 * &gt; ssm$startServerSocket(PORT)
 * &gt; socketCon &lt;- socketConnection(host="127.0.0.1", port=PORT, open = "wb")
 * &gt; writeBin(rawVec, socketCon)
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 */
public class SocketServerMockup {

    private static final int DEFAULT_TIMEOUT = 20000;

    public static boolean log = false;

    private static final Map<Integer, ByteArrayOutputStream> CLIENT_SOCKET_DATA = new HashMap<>();

    public static void startClientSocket(int port) {
        startClientSocket(port, DEFAULT_TIMEOUT);
    }

    /**
     * <p>
     * Tries to start in a separate thread a client socket at the given port. If there is no server
     * available until the given timeout a SocketTimeoutException is thrown.
     * </p>
     * <p>
     * Read data are continuously stored and can be accessed via {@link #clientData(int)}.
     * </p>
     * <p>
     * Writing from the mockup side currently isn't possible.
     * </p>
     * 
     * <pre>
     * # init
     * &gt; java.addToClasspath("{FASTR_REPO}/mxbuild/dists/jdk1.8/fastr-unit-tests.jar")
     * &gt; ssm &lt;- java.type('com.oracle.truffle.r.test.connection.SocketServerMockup')
     * 
     * # create client socket 
     * &gt; ssm$startClientSocket(PORT) 
     * # create server connection 
     * &gt; serverSocket &lt;- socketConnection(host="127.0.0.1", port=PORT, server=TRUE, open="wb") 
     * # write data 
     * &gt; writeBin(as.raw(c(1,2,3)), serverSocket) 
     * # access the data 
     * &gt; rv &lt;- as.raw(ssm$clientData(PORT))
     * </pre>
     * 
     * @param port
     * @param timeout
     */
    public static void startClientSocket(int port, int timeout) {
        int tm = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        CLIENT_SOCKET_DATA.put(port, data);
        Thread t = new Thread() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    long time = System.currentTimeMillis();
                    int retry = 0;
                    while (true && (System.currentTimeMillis() - time < tm)) {
                        try {
                            socket = new Socket(InetAddress.getByName("localhost"), port);
                            break;
                        } catch (SocketException e) {
                            try {
                                retry = retry > 10 ? 1 : retry + 1;
                                Thread.sleep(retry * 31);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                    if (socket == null || !socket.isConnected()) {
                        throw new SocketTimeoutException();
                    }

                    log("client socket connected " + socket);
                    int i = -1;
                    InputStream in = socket.getInputStream();
                    while (socket.isConnected() && (i = in.read()) != -1) {
                        log("client socket read: " + i + " " + socket);
                        data.write(i);
                    }
                    log("client socket done " + socket);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };
        t.start();
    }

    /**
     * Returns the data read by a client socket at a given port.
     * 
     * @param port
     * @return read data
     */
    public static byte[] clientData(int port) {
        ByteArrayOutputStream data = CLIENT_SOCKET_DATA.get(port);
        return data != null ? data.toByteArray() : null;
    }

    private static final Map<Integer, ServerImpl> SERVERS = new HashMap<>();

    /**
     * <p>
     * Starts in a separate thread a server socket at the given port. It is possible to connect more
     * then one client socket.
     * </p>
     * <p>
     * If there is more then one client socket connected at the given port then everything that is
     * read from a socket at the server side is immediately written into the other sockets.
     * <p>
     * 
     * <pre>
     * # init
     * &gt; java.addToClasspath("{FASTR_REPO}/mxbuild/dists/jdk1.8/fastr-unit-tests.jar")
     * &gt; ssm &lt;- java.type('com.oracle.truffle.r.test.connection.SocketServerMockup')
     * 
     * # start server socket
     * ssm$startServerSocket(PORT)
     * # create read/write connections
     * socketConWrite &lt;- socketConnection(host="127.0.0.1", port=PORT, open = "wb")
     * socketConRead &lt;- socketConnection(host="127.0.0.1", port=PORT, open = "rb")
     * # write
     * writeBin(as.raw(c(1, 2, 3)), socketConWrite)
     * # read
     * rawVector &lt;- readBin(socketConRead, "raw", 500)
     * </pre>
     * 
     */
    public static void startServerSocket(int port) throws IOException {
        ServerImpl impl = SERVERS.get(port);
        if (impl == null) {
            impl = new ServerImpl(port);
            SERVERS.put(port, impl);
            impl.start();
        }
    }

    /**
     * Closes a server socket at the given port.
     */
    public static void closeServerSocket(int port) throws IOException {
        ServerImpl impl = SERVERS.remove(port);
        if (impl != null) {
            impl.close();
        }
    }

    private static final class ServerImpl extends Thread {

        private final ServerSocket server;
        private final List<Socket> sockets = new ArrayList<>();
        private final Object lock = new Object();
        private boolean closed = false;

        private ServerImpl(int port) throws IOException {
            InetAddress lh = InetAddress.getByName("localhost");
            server = new ServerSocket(port, 1, lh);
        }

        @Override
        public void run() {
            log("started server at port:" + server.getLocalPort());
            try {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    log("new client socket " + socket);
                    synchronized (lock) {
                        sockets.add(socket);
                    }
                    new ReadInputThread(socket).start();
                }
            } catch (SocketException se) {
                if (!closed) {
                    se.printStackTrace();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                log("closed server at port: " + server.getLocalPort());
            }
        }

        private void close() throws IOException {
            closed = true;
            synchronized (lock) {
                Iterator<Socket> it = sockets.iterator();
                if (it.hasNext()) {
                    it.next().close();
                    it.remove();
                }
                server.close();
            }
        }

        private class ReadInputThread extends Thread {
            private final Socket socket;

            ReadInputThread(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                InputStream in = null;
                try {
                    int i = -1;
                    in = socket.getInputStream();
                    while (!server.isClosed() && !socket.isClosed() && (i = in.read()) > -1) {
                        log("read in: " + i + " " + socket);
                        synchronized (lock) {
                            Iterator<Socket> it = sockets.iterator();
                            while (it.hasNext()) {
                                Socket s = it.next();
                                if (!s.isClosed() && s != socket) {
                                    log("write out: " + i + " " + server.getLocalPort() + " " + socket);
                                    try {
                                        s.getOutputStream().write(i);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        if (s.isClosed()) {
                                            it.remove();
                                        }
                                    }
                                } else {
                                    it.remove();
                                }
                            }
                        }
                    }
                } catch (SocketException se) {
                    if (!closed) {
                        se.printStackTrace();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    log("closing " + socket);
                    try {
                        synchronized (lock) {
                            sockets.remove(socket);
                            if (!socket.isClosed()) {
                                socket.close();
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }

    }

    private static void log(String s) {
        if (log) {
            System.out.println(SocketServerMockup.class.getSimpleName() + ": " + s);
        }
    }
}
