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
import java.nio.charset.Charset;

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

        public PipeRConnection(String command, String open, Charset encoding) throws IOException {
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
        private final InputStream in;

        protected PipeReadRConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            in = p.getInputStream();
        }

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class PipeWriteConnection extends DelegateWriteRConnection {
        private final OutputStream out;

        PipeWriteConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            out = p.getOutputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public boolean isSeekable() {
            return false;
        }
    }

    private static class PipeReadWriteConnection extends DelegateReadWriteRConnection {

        private final InputStream in;
        private final OutputStream out;

        protected PipeReadWriteConnection(BaseRConnection base, String command) throws IOException {
            super(base);
            Process p = PipeConnections.executeAndJoin(command);
            in = p.getInputStream();
            out = p.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

    }

}
