/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSuicide;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.AbstractOpenMode;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.data.RStringVector;

public class StdConnections {

    private static class Diversion {
        final RConnection conn;
        final boolean closeOnExit;

        Diversion(RConnection conn, boolean closeOnExit) {
            this.conn = conn;
            this.closeOnExit = closeOnExit;
        }
    }

    public static final class ContextStateImpl implements RContext.ContextState {
        private StdinConnection stdin;
        private StdoutConnection stdout;
        private StderrConnection stderr;
        private final Diversion[] diversions = new Diversion[20];
        private int top = -1;
        private StringBuilder outputBuffer;

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }

        /**
         * Allows temporarily redirecting the top level output to the given buffer. Use
         * {@link #resetBuffer()} to revert.
         */
        public void setBuffer(StringBuilder buffer) {
            this.outputBuffer = buffer;
        }

        public void resetBuffer() {
            this.outputBuffer = null;
        }

        @Override
        public RContext.ContextState initialize(RContext context) {
            ConsoleIO console = context.getConsole();
            try {
                stdin = new StdinConnection(console);
                stdout = new StdoutConnection(console);
                stderr = new StderrConnection(console);
            } catch (IOException ex) {
                throw RSuicide.rSuicide("failed to open stdconnections:");
            }
            return this;
        }
    }

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateStdConnections;
    }

    public static BaseRConnection getStdin() {
        return getContextState().stdin;
    }

    public static BaseRConnection getStdout() {
        return getContextState().stdout;
    }

    public static BaseRConnection getStderr() {
        return getContextState().stderr;
    }

    public static boolean pushDivertOut(RConnection conn, boolean closeOnExit) {
        return StdoutConnection.pushDivert(conn, closeOnExit);
    }

    public static void popDivertOut() throws IOException {
        StdoutConnection.popDivert();
    }

    public static void divertErr(RConnection conn) {
        getContextState().stderr.divertErr(conn);
    }

    public static int stdoutDiversions() {
        return StdoutConnection.numDiversions();
    }

    public static int stderrDiversion() {
        return getContextState().stderr.diversionIndex();
    }

    /**
     * Subclasses are special in that they do not use delegation as the connection is always open.
     */
    public abstract static class StdConnection extends BaseRConnection {
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
        public BaseRConnection forceOpen(String modeString) {
            return this;
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

        @Override
        public long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.UNSEEKABLE_CONNECTION);
        }
    }

    private static final class StdinConnection extends StdConnection {

        private final ConsoleIO console;

        StdinConnection(ConsoleIO console) throws IOException {
            super(AbstractOpenMode.Read, 0);
            this.console = console;
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
        public String[] readLinesInternal(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
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

        @Override
        public int getc() throws IOException {
            return console.getStdin().read();
        }
    }

    private abstract static class StdoutputAdapter extends StdConnection {
        protected final ConsoleIO console;

        StdoutputAdapter(int index, ConsoleIO console) throws IOException {
            super(AbstractOpenMode.Write, index);
            this.console = console;
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

        @Override
        public int getc() throws IOException {
            throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
        }
    }

    private static final class StdoutConnection extends StdoutputAdapter {

        StdoutConnection(ConsoleIO console) throws IOException {
            super(1, console);
        }

        static int numDiversions() {
            return getContextState().top + 1;
        }

        @Override
        public String getSummaryDescription() {
            return "stdout";
        }

        @Override
        public void flush() throws IOException {
            ContextStateImpl state = getContextState();
            if (state.top < 0) {
                // no API to flush console
            } else {
                state.diversions[state.top].conn.flush();
            }
        }

        @Override
        public void writeLines(RStringVector lines, String sep, boolean useBytes) throws IOException {
            /*
             * It is more efficient to test for diversion, as this is the most common entry point.
             */
            ContextStateImpl state = getContextState();
            if (state.top < 0) {
                for (int i = 0; i < lines.getLength(); i++) {
                    String line = lines.getDataAt(i);
                    writeString(line, false);
                    writeString(sep, false);
                }
            } else {
                getContextState().diversions[state.top].conn.writeLines(lines, sep, useBytes);
            }
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            ContextStateImpl state = getContextState();
            if (state.top < 0) {
                if (state.outputBuffer != null) {
                    state.outputBuffer.append(s);
                    if (nl) {
                        state.outputBuffer.append('\n');
                    }
                } else {
                    if (nl) {
                        console.println(s);
                    } else {
                        console.print(s);
                    }
                }
            } else {
                getContextState().diversions[state.top].conn.writeString(s, nl);
            }
        }

        static boolean pushDivert(RConnection conn, boolean closeOnExit) {
            ContextStateImpl state = getContextState();
            if (state.top < state.diversions.length - 1) {
                state.top++;
                state.diversions[state.top] = new Diversion(conn, closeOnExit);
            } else {
                return false;
            }
            return true;
        }

        static void popDivert() throws IOException {
            ContextStateImpl state = getContextState();
            if (state.top >= 0) {
                int ctop = state.top;
                state.top--;
                if (state.diversions[ctop].closeOnExit) {
                    state.diversions[ctop].conn.closeAndDestroy();
                }
            }
        }
    }

    private static final class StderrConnection extends StdoutputAdapter {
        RConnection diversion;

        StderrConnection(ConsoleIO console) throws IOException {
            super(2, console);
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
        public void writeLines(RStringVector lines, String sep, boolean useBytes) throws IOException {
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
                    console.printErrorln(s);
                } else {
                    console.printError(s);
                }
            }
        }
    }
}
