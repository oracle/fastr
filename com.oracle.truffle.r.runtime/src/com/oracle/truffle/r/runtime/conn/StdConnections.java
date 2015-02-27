/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.*;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class StdConnections {
    /**
     * Subclasses are special in that they do not use delegation as the connection is always open.
     */
    private abstract static class StdConnection extends BaseRConnection {
        StdConnection(OpenMode openMode, int index) {
            super(ConnectionClass.Terminal, openMode, true);
            this.opened = true;
            ConnectionSupport.setStdConnection(index, this);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public OutputStream getOutputStream() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void close() throws IOException {
            throw new IOException(RError.Message.CANNOT_CLOSE_STANDARD_CONNECTIONS.message);
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            // nothing to do, as we override the RConnection methods directly.
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw new IOException("writeBin not implemented for this connection");
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw new IOException("readBin not implemented for this connection");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw new IOException("readBinChars not implemented for this connection");
        }

        @Override
        public boolean forceOpen(String modeString) {
            return true;
        }
    }

    private static class StdinConnection extends StdConnection {

        StdinConnection() {
            super(new OpenMode("r", AbstractOpenMode.Read), 0);
        }

        @Override
        public boolean isStdin() {
            return true;
        }

        @Override
        public String getSummaryDescription() {
            return "stdin";
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return false;
        }

        @Override
        @TruffleBoundary
        public String[] readLinesInternal(int n) throws IOException {
            ConsoleHandler console = RContext.getInstance().getConsoleHandler();
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = console.readLine()) != null) {
                lines.add(line);
                if (n > 0 && lines.size() == n) {
                    break;
                }
            }
            String[] result = new String[lines.size()];
            lines.toArray(result);
            return result;
        }

    }

    private static final StdinConnection stdin = new StdinConnection();

    public static RConnection getStdin() {
        return stdin;
    }

    private static class StdoutConnection extends StdConnection {

        private final boolean isErr;

        StdoutConnection(boolean isErr) {
            super(new OpenMode("w", AbstractOpenMode.Write), isErr ? 2 : 1);
            this.opened = true;
            this.isErr = isErr;
        }

        @Override
        public String getSummaryDescription() {
            return isErr ? "stderr" : "stdout";
        }

        @Override
        public boolean canRead() {
            return false;
        }

        @Override
        public boolean canWrite() {
            return true;
        }

        @Override
        public boolean isStdout() {
            return isErr ? false : true;
        }

        @Override
        public boolean isStderr() {
            return isErr ? true : false;
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
            for (int i = 0; i < lines.getLength(); i++) {
                String line = lines.getDataAt(i);
                if (isErr) {
                    ch.printError(line);
                    ch.printError(sep);
                } else {
                    ch.print(line);
                    ch.print(sep);
                }
            }
        }
    }

    private static final StdoutConnection stdout = new StdoutConnection(false);

    public static RConnection getStdout() {
        return stdout;
    }

    private static final StdoutConnection stderr = new StdoutConnection(true);

    public static RConnection getStderr() {
        return stderr;
    }

}
