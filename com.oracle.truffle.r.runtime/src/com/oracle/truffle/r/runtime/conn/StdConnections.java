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
import com.oracle.truffle.r.runtime.RContext.ContextState;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class StdConnections implements RContext.StateFactory {

    private static class ContextStateImpl implements RContext.ContextState {
        private final StdinConnection stdin;
        private final StdoutConnection stdout;
        private final StderrConnection stderr;

        ContextStateImpl(StdinConnection stdin, StdoutConnection stdout, StderrConnection stderr) {
            this.stdin = stdin;
            this.stdout = stdout;
            this.stderr = stderr;
        }

    }

    public ContextState newContext(RContext context, Object... objects) {
        ConsoleHandler consoleHandler = (ConsoleHandler) objects[0];
        try {
            return new ContextStateImpl(new StdinConnection(), new StdoutConnection(consoleHandler), new StderrConnection(consoleHandler));
        } catch (IOException ex) {
            throw Utils.fail("failed to open stdconnections:");
        }
    }

    private static ContextStateImpl getContextState() {
        return (ContextStateImpl) RContext.getContextState(RContext.ClassStateKind.StdConnections);
    }

    public static RConnection getStdin() {
        return getContextState().stdin;
    }

    public static RConnection getStdout() {
        return getContextState().stdout;
    }

    public static RConnection getStderr() {
        return getContextState().stderr;
    }

    public static boolean pushDivertOut(RConnection conn, boolean closeOnExit) {
        return getContextState().stdout.pushDivert(conn, closeOnExit);
    }

    public static void popDivertOut() throws IOException {
        getContextState().stdout.popDivert();
    }

    public static void divertErr(RConnection conn) {
        getContextState().stderr.divertErr(conn);
    }

    public static int stdoutDiversions() {
        return getContextState().stdout.numDiversions();
    }

    public static int stderrDiversion() {
        return getContextState().stderr.diversionIndex();
    }

    /**
     * Subclasses are special in that they do not use delegation as the connection is always open.
     */
    private abstract static class StdConnection extends BaseRConnection {
        StdConnection(AbstractOpenMode openMode, int index) throws IOException {
            super(openMode, index);
            this.opened = true;
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
        public void closeAndDestroy() throws IOException {
            throw new IOException(RError.Message.CANNOT_CLOSE_STANDARD_CONNECTIONS.message);
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            // nothing to do, as we override the RConnection methods directly.
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw new IOException("'write' not enabled for this connection");
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw new IOException("'write' not enabled for this connection");
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
        public RConnection forceOpen(String modeString) {
            return this;
        }
    }

    private static class StdinConnection extends StdConnection {

        StdinConnection() throws IOException {
            super(AbstractOpenMode.Read, 0);
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

    private abstract static class StdoutputAdapter extends StdConnection {
        protected final ConsoleHandler consoleHandler;

        StdoutputAdapter(int index, ConsoleHandler consoleHandler) throws IOException {
            super(AbstractOpenMode.Write, index);
            this.consoleHandler = consoleHandler;
            this.opened = true;
        }

        @Override
        public boolean canRead() {
            return false;
        }

        @Override
        public boolean canWrite() {
            return true;
        }

    }

    private static class StdoutConnection extends StdoutputAdapter {

        private static class Diversion {
            final RConnection conn;
            final boolean closeOnExit;

            Diversion(RConnection conn, boolean closeOnExit) {
                this.conn = conn;
                this.closeOnExit = closeOnExit;
            }

        }

        private static final Diversion[] diversions = new Diversion[20];

        private static int top = -1;

        StdoutConnection(ConsoleHandler consoleHandler) throws IOException {
            super(1, consoleHandler);
        }

        int numDiversions() {
            return top + 1;
        }

        @Override
        public String getSummaryDescription() {
            return "stdout";
        }

        @Override
        public void flush() throws IOException {
            if (top < 0) {
                // no API to flush console
            } else {
                diversions[top].conn.flush();
            }
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            /*
             * It is more efficient to test for diversion, as this is the most common entry point.
             */
            if (top < 0) {
                for (int i = 0; i < lines.getLength(); i++) {
                    String line = lines.getDataAt(i);
                    writeString(line, false);
                    writeString(sep, false);
                }
            } else {
                diversions[top].conn.writeLines(lines, sep, useBytes);
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            if (top < 0) {
                if (nl) {
                    consoleHandler.println(s);
                } else {
                    consoleHandler.print(s);
                }
            } else {
                diversions[top].conn.writeString(s, nl);
            }
        }

        boolean pushDivert(RConnection conn, boolean closeOnExit) {
            if (top < diversions.length - 1) {
                top++;
                diversions[top] = new Diversion(conn, closeOnExit);
            } else {
                return false;
            }
            return true;
        }

        void popDivert() throws IOException {
            if (top >= 0) {
                int ctop = top;
                top--;
                if (diversions[ctop].closeOnExit) {
                    diversions[ctop].conn.closeAndDestroy();
                }
            }
        }
    }

    private static class StderrConnection extends StdoutputAdapter {
        RConnection diversion;

        StderrConnection(ConsoleHandler consoleHandler) throws IOException {
            super(2, consoleHandler);
        }

        void divertErr(RConnection conn) {
            if (conn == getContextState().stderr) {
                diversion = null;
            } else {
                diversion = conn;
            }
        }

        int diversionIndex() {
            return diversion == null ? 2 : diversion.getDescriptor();
        }

        @Override
        public String getSummaryDescription() {
            return "stderr";
        }

        @Override
        public void flush() throws IOException {
            if (diversion == null) {
                // no API to flush console
            } else {
                diversion.flush();
            }
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            /*
             * It is more efficient to test for diversion, as this is the most common entry point.
             */
            if (diversion == null) {
                for (int i = 0; i < lines.getLength(); i++) {
                    String line = lines.getDataAt(i);
                    writeString(line, false);
                    writeString(sep, false);
                }
            } else {
                diversion.writeLines(lines, sep, useBytes);
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            if (diversion != null) {
                diversion.writeString(s, nl);
            } else {
                if (nl) {
                    consoleHandler.printErrorln(s);
                } else {
                    consoleHandler.printError(s);
                }
            }
        }

    }

}
