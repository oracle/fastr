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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import com.oracle.truffle.r.runtime.ProcessOutputManager;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;

public class FifoConnections {

    public static class FifoRConnection extends BaseRConnection {

        private final String path;

        public FifoRConnection(String path, String open, boolean blocking, String encoding) throws IOException {
            super(ConnectionClass.FIFO, open, AbstractOpenMode.Read);
            this.path = path;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new FifoReadRConnection(this, path);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new FifoWriteConnection(this, path);
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
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            return path;
        }
    }

    static class FifoReadRConnection extends DelegateReadRConnection {
        private final RandomAccessFile raf;

        protected FifoReadRConnection(BaseRConnection base, String path) throws FileNotFoundException {
            super(base);
            raf = new RandomAccessFile(path, "r");
        }

        @Override
        public SeekableByteChannel getChannel() {
            return raf.getChannel();
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class FifoWriteConnection extends DelegateWriteRConnection {
        private final RandomAccessFile raf;

        FifoWriteConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            this.raf = createAndOpenFifo(path, "rw");
        }

        @Override
        public SeekableByteChannel getChannel() {
            return raf.getChannel();
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class FifoReadWriteConnection extends DelegateReadWriteRConnection {

        private final RandomAccessFile raf;

        protected FifoReadWriteConnection(BaseRConnection base, String path) throws IOException {
            super(base);
            this.raf = createAndOpenFifo(path, "w");
        }

        @Override
        public SeekableByteChannel getChannel() {
            return raf.getChannel();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

    }

    private static final String MKFIFO_ERROR_FILE_EXISTS = "File exists";

    private static RandomAccessFile createAndOpenFifo(String path, String mode) throws IOException {
        RandomAccessFile pipeFile = null;
        try {
            pipeFile = new RandomAccessFile(path, mode);
        } catch (FileNotFoundException e) {
            // try to create fifo on demand
            createNamedPipe(path);

            // re-try opening
            pipeFile = new RandomAccessFile(path, mode);
        }
        return pipeFile;
    }

    /**
     * Creates a named pipe (FIFO) by invoking {@code mkfifo} in a shell.
     *
     * <b>NOTE:</b> This is a Linux-specific operation and won't work on other OSes.
     *
     * @param path The path to the named pipe.
     * @throws IOException
     */
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

                // ignore if file already exists
                if (!msg.endsWith(MKFIFO_ERROR_FILE_EXISTS)) {
                    throw new IOException(msg);
                }
            }
        } catch (InterruptedException ex) {
            // fall through
        }
    }

}
