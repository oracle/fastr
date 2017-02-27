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
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.ConnectionClass;

public class PipeConnections {

    private static Process executeAndJoin(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            // TODO not sure how to handle an interrupted exception at this point
        }
        return p;
    }

    public static class PipeRConnection extends BaseRConnection {

        private final String command;

        public PipeRConnection(String command, String open, String encoding) throws IOException {
            super(ConnectionClass.FIFO, open, AbstractOpenMode.Read, encoding);
            this.command = command;
            openNonLazyConnection();
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            final DelegateRConnection delegate;
            switch (getOpenMode().abstractOpenMode) {
                case Read:
                case ReadBinary:
                    delegate = new PipeReadRConnection(this, command);
                    break;
                case Write:
                case WriteBinary:
                    delegate = new PipeWriteConnection(this, command);
                    break;
                case ReadAppend:
                case ReadWrite:
                case ReadWriteBinary:
                case ReadWriteTrunc:
                case ReadWriteTruncBinary:
                    delegate = new PipeReadWriteConnection(this, command);
                    break;
                default:
                    throw RError.nyi(RError.SHOW_CALLER2, "open mode: " + getOpenMode());
            }
            setDelegate(delegate);
        }

        @Override
        public String getSummaryDescription() {
            return command;
        }
    }

    static class PipeReadRConnection extends DelegateReadRConnection {
        private final ReadableByteChannel channel;

        protected PipeReadRConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            channel = Channels.newChannel(p.getInputStream());
        }

        @Override
        public ReadableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class PipeWriteConnection extends DelegateWriteRConnection {
        private final WritableByteChannel channel;

        PipeWriteConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            channel = Channels.newChannel(p.getOutputStream());
        }

        @Override
        public WritableByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class PipeReadWriteConnection extends DelegateReadWriteRConnection {

        private final RWChannel channel;

        protected PipeReadWriteConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            channel = new RWChannel(p.getInputStream(), p.getOutputStream());
        }

        @Override
        public ByteChannel getChannel() {
            return channel;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

        private static final class RWChannel implements ByteChannel {
            private final ReadableByteChannel rchannel;
            private final WritableByteChannel wchannel;

            RWChannel(InputStream in, OutputStream out) {
                rchannel = Channels.newChannel(in);
                wchannel = Channels.newChannel(out);
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                return rchannel.read(dst);
            }

            @Override
            public boolean isOpen() {
                return rchannel.isOpen() && wchannel.isOpen();
            }

            @Override
            public void close() throws IOException {
                rchannel.close();
                wchannel.close();
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return wchannel.write(src);
            }

        }

    }

}
