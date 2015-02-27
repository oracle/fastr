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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public abstract class ConnectionFunctions {
    /**
     * Records all connections. The index in the array is the "descriptor" used in
     * {@code getConnection}. Defined at this level so that {@link #stdin} etc. are initialized
     * before any attempt to return them via, e.g., {@code getConnection}.
     */
    private static BaseRConnection[] allConnections = new BaseRConnection[127];

    private static final class ModeException extends IOException {
        private static final long serialVersionUID = 1L;

        private ModeException() {
            // lame but it's what GnuR does
            super("invalid argument");
        }
    }

    /**
     * Abstracts from the fact that some modes can be specified with multiple text strings. N.B.
     * includes the {@code Lazy} mode for lazy opening support, which is requested in R by "" as the
     * open argument.
     */
    private enum AbstractOpenMode {
        Lazy(new String[]{""}, false, true, true),
        Read(new String[]{"r", "rt"}, true, true, false),
        Write(new String[]{"w", "wt"}, true, false, true),
        Append(new String[]{"a", "at"}, true, false, true),
        ReadBinary(new String[]{"rb"}, false, true, false),
        WriteBinary(new String[]{"wb"}, false, false, true),
        AppendBinary(new String[]{"ab"}, false, false, true),
        ReadWrite(new String[]{"r+"}, true, true, true),
        ReadWriteBinary(new String[]{"r+b"}, false, true, true),
        ReadWriteTrunc(new String[]{"w+"}, true, true, true),
        ReadWriteTruncBinary(new String[]{"w+b"}, false, true, true),
        ReadAppend(new String[]{"a+"}, true, true, true),
        ReadAppendBinary(new String[]{"a+b"}, false, true, true);

        private String[] modeStrings;
        public final boolean isText;
        public final boolean readable;
        public final boolean writeable;

        AbstractOpenMode(String[] modeStrings, boolean isText, boolean readable, boolean writeable) {
            this.modeStrings = modeStrings;
            this.isText = isText;
            this.readable = readable;
            this.writeable = writeable;
        }

        @TruffleBoundary
        static AbstractOpenMode getOpenMode(String modeString) throws IOException {
            for (AbstractOpenMode openMode : values()) {
                for (String ms : openMode.modeStrings) {
                    if (ms.equals(modeString)) {
                        return openMode;
                    }
                }
            }
            throw new ModeException();
        }
    }

    /**
     * The class used to designate an actual open mode in {@link BaseRConnection}. Records the
     * actual mode string that was used, which is needed by {@code summary}. E.g., for
     * {@link AbstractOpenMode#Read}, {@link #modeString} could be "r" or "rt".
     *
     */
    private static class OpenMode {
        final AbstractOpenMode abstractOpenMode;
        final String modeString;

        OpenMode(String modeString) throws IOException {
            this(modeString, AbstractOpenMode.getOpenMode(modeString));
        }

        OpenMode(String modeString, AbstractOpenMode mode) {
            this.modeString = modeString;
            this.abstractOpenMode = mode;
        }

        boolean isText() {
            return abstractOpenMode.isText;
        }

        boolean canRead() {
            return abstractOpenMode.readable;
        }

        boolean canWrite() {
            return abstractOpenMode.writeable;
        }
    }

    private enum ConnectionClass {
        Terminal("terminal"),
        File("file"),
        GZFile("gzfile"),
        Socket("socketconn"),
        Text("textConnection"),
        URL("url");

        final String printName;

        ConnectionClass(String printName) {
            this.printName = printName;
        }

    }

    // TODO implement all open modes
    // TODO implement missing .Internal functions expected by connections.R
    // TODO revisit the use of InputStream for internal use, e.g. in RSerialize
    // TODO Use channels to support blocking/non-blockng mode

    /**
     * Class that holds common state for all {@link RConnection} instances. It supports lazy
     * opening, as required by the R spec, through the {@link #theConnection} field, which
     * ultimately holds the actual connection instance when opened. The default implementations of
     * the {@link RConnection} methods check whether the connection is open and if not, call
     * {@link #createDelegateConnection()} and then forward the operation. The result of
     * {@link #createDelegateConnection()} should be a subclass of {@link DelegateRConnection},
     * which subclasses {@link RConnection} directly. A subclass may choose not to use delegation by
     * overriding the default implementations of the methods in this class. A
     * {@link DelegateRConnection} can get to its {@link BaseRConnection} through the
     * {@link #getBaseConnection} method.
     *
     * N.B. There is subtle but important distinction between an explicit {@code close} on a
     * connection and the implicit open/close that occurs when a function such as {@code readChar}
     * is invoked on an unopened connection. The former destroys the connection and attempts to use
     * it subsequently will throw an error. The latter will open/close the connection (internally)
     * and this can be repeated indefinitely.
     */
    private abstract static class BaseRConnection extends RConnection {

        /**
         * {@code true} is the connection has been opened successfully. N.B. This supports lazy
         * opening, in particular, {@code opened == false} does not mean {@code closed}.
         */
        protected boolean opened;

        /**
         * Returns {@code true} once the {code R close} method has been called.
         */
        protected boolean closed;

        /**
         * if {@link #opened} is {@code true} the {@link OpenMode} that this connection is opened
         * in, otherwise {@link AbstractOpenMode#Lazy}.
         */
        protected OpenMode openMode;

        private final RStringVector classHr;

        /**
         * The actual connection, if delegated.
         */
        protected DelegateRConnection theConnection;

        private int descriptor;

        /**
         * The constructor to use for a connection class whose default open mode is not "read", but
         * specified explicitly by "lazyMode".
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString) throws IOException {
            this(conClass, new OpenMode(modeString));
        }

        /**
         * Primitive constructor that just assigns state. Used by {@link StdConnection}s as they are
         * a special case but should not be used by other connection types.
         */
        protected BaseRConnection(ConnectionClass conClass, OpenMode mode) {
            this.openMode = mode;
            String[] classes = new String[2];
            classes[0] = conClass.printName;
            classes[1] = "connection";
            this.classHr = RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR);
            if (!isStdin() && !isStdout() && !isStderr()) {
                registerConnection();
            }
        }

        /**
         * A connection is "active" if it has been opened and not yet closed.
         */
        protected boolean isActive() {
            return !closed && opened;
        }

        /**
         * Return value for "can read" for {@code summary.connection}.
         */
        @Override
        public boolean canRead() {
            if (isActive()) {
                return openMode.canRead();
            } else {
                // Might think to check the lazy open mode, but GnuR doesn't
                return true;
            }
        }

        /**
         * Return value for "can read" for {@code summary.connection}.
         */
        @Override
        public boolean canWrite() {
            if (isActive()) {
                return openMode.canWrite();
            } else {
                // Might think to check the lazy open mode, but GnuR doesn't
                return true;
            }
        }

        /**
         * Is this a text mode connection. N.B. The connection may not be opened yet!
         */
        @Override
        public boolean isTextMode() {
            return openMode.isText();
        }

        private void registerConnection() {
            for (int i = 3; i < allConnections.length; i++) {
                if (allConnections[i] == null) {
                    allConnections[i] = this;
                    descriptor = i;
                    return;
                }
            }
            throw RError.error(RError.Message.ALL_CONNECTIONS_IN_USE);
        }

        @Override
        public boolean forceOpen(String modeString) throws IOException {
            boolean ret = opened;
            if (closed) {
                throw new IOException(RError.Message.INVALID_CONNECTION.message);
            }
            if (!opened) {
                // internal closed or lazy
                if (openMode.abstractOpenMode == AbstractOpenMode.Lazy) {
                    // modeString may override the default
                    openMode = new OpenMode(modeString);
                }
                createDelegateConnection();
            }
            return ret;
        }

        protected void checkOpen() {
            assert !closed && opened;
        }

        protected void setDelegate(DelegateRConnection conn) {
            this.theConnection = conn;
            opened = true;
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            checkOpen();
            return theConnection.readLinesInternal(n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            checkOpen();
            return theConnection.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            checkOpen();
            return theConnection.getOutputStream();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            checkOpen();
            theConnection.writeLines(lines, sep);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            checkOpen();
            return theConnection.readChar(nchars, useBytes);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            checkOpen();
            theConnection.writeChar(s, pad, eos, useBytes);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            checkOpen();
            theConnection.writeBin(buffer);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            checkOpen();
            return theConnection.readBin(buffer);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            checkOpen();
            return theConnection.readBinChars();
        }

        @Override
        public void flush() throws IOException {
            checkOpen();
            theConnection.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            if (theConnection != null) {
                theConnection.close();
            }
            assert allConnections[this.descriptor] != null;
            allConnections[this.descriptor] = null;
        }

        @Override
        public void internalClose() throws IOException {
            opened = false;
            if (theConnection != null) {
                DelegateRConnection tc = theConnection;
                theConnection = null;
                tc.internalClose();
            }
        }

        @Override
        public RStringVector getClassHierarchy() {
            return classHr;
        }

        /**
         * Subclass-specific creation of the {@link DelegateRConnection} using {@link #openMode}. To
         * support both lazy and non-lazy creation, the implementation of this method must
         * explicitly call {@link #setDelegate(DelegateRConnection)} if it successfully creates the
         * delegate connection.
         */
        protected abstract void createDelegateConnection() throws IOException;

        /**
         * Return the value that is used in the "description" field by {@code summary.connection}.
         */
        protected abstract String getSummaryDescription();

        /**
         * Return the value that is used in the "text" field by {@code summary.connection}. TODO
         * determine what this really means, e.g., In GnuR {gzfile} on a binary file still reports
         * "text".
         */
        protected String getSummaryText() {
            return "text";
        }

        @Override
        public int getDescriptor() {
            return descriptor;
        }

        public static BaseRConnection getConnection(int descriptor) {
            if (descriptor >= allConnections.length || allConnections[descriptor] == null) {
                throw RError.error(RError.Message.INVALID_CONNECTION);
            }
            return allConnections[descriptor];
        }

        public static RIntVector getAllConnections() {
            int resLen = 0;
            for (int i = 0; i < allConnections.length; i++) {
                if (allConnections[i] != null) {
                    resLen++;
                }
            }
            int[] data = new int[resLen];
            int ind = 0;
            for (int i = 0; i < allConnections.length; i++) {
                if (allConnections[i] != null) {
                    data[ind++] = i;
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private static BaseRConnection getBaseConnection(RConnection conn) {
        if (conn instanceof BaseRConnection) {
            return (BaseRConnection) conn;
        } else if (conn instanceof DelegateReadRConnection) {
            return ((DelegateReadRConnection) conn).base;
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    /**
     * {@code readLines} from an {@link InputStream}. It would be convenient to use a
     * {@link BufferedReader} but mixing binary and text operations, which is a requirement, would
     * then be difficult.
     */
    private static String[] readLinesHelper(InputStream in, int n) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        int totalRead = 0;
        byte[] buffer = new byte[64];
        int pushBack = 0;
        while (true) {
            int ch;
            if (pushBack != 0) {
                ch = pushBack;
                pushBack = 0;
            } else {
                ch = in.read();
            }
            boolean lineEnd = false;
            if (ch < 0) {
                // N.B. This means data may be discarded if no line-end
                break;
            }
            if (ch == '\n') {
                lineEnd = true;
            } else if (ch == '\r') {
                lineEnd = true;
                ch = in.read();
                if (ch == '\n') {
                    // swallow the trailing lf
                } else {
                    pushBack = ch;
                }
            }
            if (lineEnd) {
                lines.add(new String(buffer, 0, totalRead));
                if (n > 0 && lines.size() == n) {
                    break;
                }
                totalRead = 0;
            } else {
                buffer = checkBuffer(buffer, totalRead);
                buffer[totalRead++] = (byte) (ch & 0xFF);
            }
        }
        String[] result = new String[lines.size()];
        lines.toArray(result);
        return result;
    }

    private static void writeLinesHelper(OutputStream out, RAbstractStringVector lines, String sep) throws IOException {
        for (int i = 0; i < lines.getLength(); i++) {
            out.write(lines.getDataAt(i).getBytes());
            out.write(sep.getBytes());
        }
    }

    private static void writeCharHelper(OutputStream out, String s, int pad, String eos) throws IOException {
        out.write(s.getBytes());
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                out.write(0);
            }
        }
        if (eos != null && eos.length() > 0) {
            out.write(eos.getBytes());
        }
    }

    private static void writeBinHelper(ByteBuffer buffer, OutputStream outputStream) throws IOException {
        int n = buffer.remaining();
        byte[] b = new byte[n];
        buffer.get(b);
        outputStream.write(b);
    }

    /**
     * Reads null-terminated character strings from an {@link InputStream}.
     */
    private static byte[] readBinCharsHelper(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0) {
            return null;
        }
        int totalRead = 0;
        byte[] buffer = new byte[64];
        while (true) {
            buffer = checkBuffer(buffer, totalRead);
            buffer[totalRead++] = (byte) (ch & 0xFF);
            if (ch == 0) {
                break;
            }
            ch = in.read();
        }
        return buffer;
    }

    private static int readBinHelper(ByteBuffer buffer, InputStream inputStream) throws IOException {
        int bytesToRead = buffer.remaining();
        byte[] b = new byte[bytesToRead];
        int totalRead = 0;
        int thisRead = 0;
        while ((totalRead < bytesToRead) && ((thisRead = inputStream.read(b, totalRead, bytesToRead - totalRead)) > 0)) {
            totalRead += thisRead;
        }
        buffer.put(b, 0, totalRead);
        return totalRead;
    }

    private static String readCharHelper(int nchars, InputStream in, @SuppressWarnings("unused") boolean useBytes) throws IOException {
        byte[] bytes = new byte[nchars];
        in.read(bytes);
        int j = 0;
        for (; j < bytes.length; j++) {
            // strings end at 0
            if (bytes[j] == 0) {
                break;
            }
        }
        return new String(bytes, 0, j);
    }

    private static byte[] checkBuffer(byte[] buffer, int n) {
        if (n > buffer.length - 1) {
            byte[] newBuffer = new byte[buffer.length + buffer.length / 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            return newBuffer;
        } else {
            return buffer;
        }

    }

    private abstract static class DelegateRConnection extends RConnection {
        protected BaseRConnection base;

        DelegateRConnection(BaseRConnection base) {
            this.base = base;
        }

        @Override
        public RStringVector getClassHierarchy() {
            return base.getClassHierarchy();
        }

        @Override
        public int getDescriptor() {
            return base.getDescriptor();
        }

        @Override
        public boolean isTextMode() {
            return base.isTextMode();
        }

        @Override
        public boolean forceOpen(String modeString) throws IOException {
            return base.forceOpen(modeString);
        }

    }

    private abstract static class DelegateReadRConnection extends DelegateRConnection {
        protected DelegateReadRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void flush() {
            // nothing to do
        }

        @Override
        public OutputStream getOutputStream() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return false;
        }
    }

    private abstract static class DelegateWriteRConnection extends DelegateRConnection {
        protected DelegateWriteRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw new IOException(RError.Message.CANNOT_READ_CONNECTION.message);
        }

        @Override
        public InputStream getInputStream() {
            throw RInternalError.shouldNotReachHere();
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

    private abstract static class DelegateReadWriteRConnection extends DelegateRConnection {
        protected DelegateReadWriteRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public boolean canWrite() {
            return true;
        }

    }

    /**
     * Subclasses are special in that they do not use delegation as the connection is always open.
     */
    private abstract static class StdConnection extends BaseRConnection {
        StdConnection(OpenMode openMode) {
            super(ConnectionClass.Terminal, openMode);
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
            super(new OpenMode("r", AbstractOpenMode.Read));
            allConnections[0] = this;
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

    @RBuiltin(name = "stdin", kind = INTERNAL, parameterNames = {})
    public abstract static class Stdin extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdin() {
            controlVisibility();
            return stdin;
        }
    }

    private static class StdoutConnection extends StdConnection {

        private final boolean isErr;

        StdoutConnection(boolean isErr) {
            super(new OpenMode("w", AbstractOpenMode.Write));
            this.opened = true;
            this.isErr = isErr;
            allConnections[isErr ? 2 : 1] = this;
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

    @RBuiltin(name = "stdout", kind = INTERNAL, parameterNames = {})
    public abstract static class Stdout extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdout() {
            controlVisibility();
            return stdout;
        }
    }

    private static final StdoutConnection stderr = new StdoutConnection(true);

    @RBuiltin(name = "stderr", kind = INTERNAL, parameterNames = {})
    public abstract static class Stderr extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stderr() {
            controlVisibility();
            return stderr;
        }
    }

    private abstract static class BasePathRConnection extends BaseRConnection {
        protected final String path;

        protected BasePathRConnection(String path, ConnectionClass connectionClass, String modeString) throws IOException {
            super(connectionClass, modeString);
            this.path = Utils.tildeExpand(path);
        }

        @Override
        public String getSummaryDescription() {
            return path;
        }

    }

    /**
     * Base class for all modes of file connections.
     *
     */
    private static class FileRConnection extends BasePathRConnection {

        protected FileRConnection(String path, String modeString) throws IOException {
            super(path, ConnectionClass.File, modeString);
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (openMode.abstractOpenMode) {
                case Read:
                    delegate = new FileReadTextRConnection(this);
                    break;
                case Write:
                    delegate = new FileWriteTextRConnection(this, false);
                    break;
                case Append:
                    delegate = new FileWriteTextRConnection(this, true);
                    break;
                case ReadBinary:
                    delegate = new FileReadBinaryRConnection(this);
                    break;
                case WriteBinary:
                    delegate = new FileWriteBinaryConnection(this, false);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + openMode);
            }
            setDelegate(delegate);
        }
    }

    private static class FileReadTextRConnection extends DelegateReadRConnection {
        private BufferedInputStream inputStream;

        FileReadTextRConnection(BasePathRConnection base) throws IOException {
            super(base);
            inputStream = new BufferedInputStream(new FileInputStream(base.path));
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.error(RError.Message.ONLY_READ_BINARY_CONNECTION);
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            inputStream.close();
        }

    }

    private static class FileWriteTextRConnection extends DelegateWriteRConnection {
        private BufferedOutputStream outputStream;

        FileWriteTextRConnection(FileRConnection base, boolean append) throws IOException {
            super(base);
            outputStream = new BufferedOutputStream(new FileOutputStream(base.path, append));
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RError.error(RError.Message.ONLY_WRITE_BINARY_CONNECTION);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            writeLinesHelper(outputStream, lines, sep);
            flush();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            outputStream.close();
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }
    }

    private static class FileReadBinaryRConnection extends DelegateReadRConnection {
        private FileInputStream inputStream;

        FileReadBinaryRConnection(FileRConnection base) throws IOException {
            super(base);
            inputStream = new FileInputStream(base.path);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return inputStream.getChannel().read(buffer);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            return readBinCharsHelper(inputStream);
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            inputStream.close();
        }

    }

    private static class FileWriteBinaryConnection extends DelegateWriteRConnection {
        private FileOutputStream outputStream;

        FileWriteBinaryConnection(FileRConnection base, boolean append) throws IOException {
            super(base);
            outputStream = new FileOutputStream(base.path, append);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            outputStream.getChannel().write(buffer);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            flush();
            outputStream.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                String line = lines.getDataAt(i);
                outputStream.write(line.getBytes());
                outputStream.write(sep.getBytes());
            }
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

    }

    @RBuiltin(name = "file", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding", "raw"})
    public abstract static class File extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object file(RAbstractStringVector description, RAbstractStringVector open, byte blocking, RAbstractStringVector encoding, byte raw) {
            controlVisibility();
            if (!RRuntime.fromLogical(blocking)) {
                throw RError.nyi(getEncapsulatingSourceSection(), " non-blocking mode not supported");
            }
            try {
                return new FileRConnection(description.getDataAt(0), open.getDataAt(0));
            } catch (IOException ex) {
                RError.warning(RError.Message.CANNOT_OPEN_FILE, description.getDataAt(0), ex.getMessage());
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object file(Object description, Object open, Object blocking, Object encoding, Object raw) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    /**
     * Base class for all modes of gzfile connections.
     */
    private static class GZIPRConnection extends BasePathRConnection {
        GZIPRConnection(String path, String modeString) throws IOException {
            super(path, ConnectionClass.GZFile, modeString);
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (openMode.abstractOpenMode) {
                case Read:
                case ReadBinary:
                    try {
                        delegate = new GZIPInputRConnection(this);
                    } catch (ZipException ex) {
                        delegate = new FileReadTextRConnection(this);
                    }
                    break;
                case Write:
                case WriteBinary:
                    delegate = new GZIPOutputRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + openMode);
            }
            setDelegate(delegate);
        }
    }

    private static class GZIPInputRConnection extends DelegateReadRConnection {
        private GZIPInputStream inputStream;

        GZIPInputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            inputStream = new GZIPInputStream(new FileInputStream(base.path), RConnection.GZIP_BUFFER_SIZE);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return readBinHelper(buffer, inputStream);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            return readBinCharsHelper(inputStream);
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            inputStream.close();
        }

    }

    private static class GZIPOutputRConnection extends DelegateWriteRConnection {
        private GZIPOutputStream outputStream;

        GZIPOutputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            outputStream = new GZIPOutputStream(new FileOutputStream(base.path), RConnection.GZIP_BUFFER_SIZE);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            flush();
            outputStream.close();
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            writeLinesHelper(outputStream, lines, sep);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            writeBinHelper(buffer, outputStream);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

    }

    /**
     * {@code gzfile} is very versatile (unfortunately); it can open uncompressed files, and files
     * compressed by {@code bzip2, xz, lzma}. Currently we only support {@code gzip} and
     * uncompressed.
     */
    @RBuiltin(name = "gzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"})
    public abstract static class GZFile extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object gzFile(RAbstractStringVector description, RAbstractStringVector open, RAbstractStringVector encoding, double compression) {
            controlVisibility();
            try {
                return new GZIPRConnection(description.getDataAt(0), open.getDataAt(0));
            } catch (ZipException ex) {
                // wasn't a gzip file, try uncompressed text
                try {
                    return new FileRConnection(description.getDataAt(0), "r");
                } catch (IOException ex1) {
                    throw reportError(description.getDataAt(0), ex1);
                }
            } catch (IOException ex) {
                throw reportError(description.getDataAt(0), ex);
            }
        }

        private RError reportError(String path, IOException ex) throws RError {
            RError.warning(RError.Message.CANNOT_OPEN_FILE, path, ex.getMessage());
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
        }
    }

    private static class TextRConnection extends BaseRConnection {
        protected String nm;
        protected RAbstractStringVector object;
        @SuppressWarnings("unused") protected REnvironment env;

        protected TextRConnection(String nm, RAbstractStringVector object, REnvironment env, String modeString) throws IOException {
            super(ConnectionClass.Text, modeString);
            this.nm = nm;
            this.object = object;
            this.env = env;
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        public String getSummaryDescription() {
            return nm;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (openMode.abstractOpenMode) {
                case Read:
                    delegate = new TextReadRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + openMode);
            }
            setDelegate(delegate);
        }
    }

    private static class TextReadRConnection extends DelegateReadRConnection {
        private String[] lines;
        private int index;

        TextReadRConnection(TextRConnection base) {
            super(base);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < base.object.getLength(); i++) {
                sb.append(base.object.getDataAt(i));
                // vector elements are implicitly terminated with a newline
                sb.append('\n');
            }
            lines = sb.toString().split("\\n");
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            int nleft = lines.length - index;
            int nlines = nleft;
            if (n > 0) {
                nlines = n > nleft ? nleft : n;
            }

            String[] result = new String[nlines];
            System.arraycopy(lines, index, result, 0, nlines);
            index += nlines;
            return result;
        }

        @SuppressWarnings("hiding")
        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
        }

        @Override
        public void internalClose() {
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, " readBin on text connection");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.nyi(null, " readBinChars on text connection");
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw RError.nyi(null, " readChar on text connection");
        }

    }

    @RBuiltin(name = "textConnection", kind = INTERNAL, parameterNames = {"nm", "object", "open", "env", "type"})
    public abstract static class TextConnection extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object textConnection(RAbstractStringVector nm, RAbstractStringVector object, RAbstractStringVector open, REnvironment env, @SuppressWarnings("unused") RIntVector encoding) {
            controlVisibility();
            try {
                return new TextRConnection(nm.getDataAt(0), object, env, open.getDataAt(0));
            } catch (IOException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    /**
     * Base class for socket connections.
     *
     * While binary operations, e.g. {@code writeBin} are only legal on binary connections, text
     * operations are legal on text and binary connections.
     *
     * TODO Non-blocking support.
     */
    private static class RSocketConnection extends BaseRConnection {
        protected final boolean server;
        protected final String host;
        protected final int port;
        protected final boolean blocking;
        protected final int timeout;

        protected RSocketConnection(String modeString, boolean server, String host, int port, boolean blocking, int timeout) throws IOException {
            super(ConnectionClass.Socket, modeString);
            this.server = server;
            this.host = host;
            this.port = port;
            this.blocking = blocking;
            this.timeout = timeout;
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
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
        protected InputStream inputStream;
        protected OutputStream outputStream;
        protected final RSocketConnection thisBase;

        protected RSocketReadWriteConnection(RSocketConnection base) {
            super(base);
            this.thisBase = base;
        }

        protected void openStreams(Socket socketArg) throws IOException {
            this.socket = socketArg;
            this.socketChannel = socket.getChannel();
            if (thisBase.blocking) {
                socket.setSoTimeout(thisBase.timeout * 1000);
            } else {
                socketChannel.configureBlocking(false);
            }
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            writeLinesHelper(outputStream, lines, sep);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            writeBinHelper(buffer, outputStream);
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            return readBinHelper(buffer, inputStream);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            writeCharHelper(outputStream, s, pad, eos);
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

        @Override
        public byte[] readBinChars() throws IOException {
            return readBinCharsHelper(inputStream);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            socket.close();
        }

    }

    private static class RServerSocketConnection extends RSocketReadWriteConnection {
        private ServerSocket serverSocket;

        RServerSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            InetSocketAddress addr = new InetSocketAddress(base.port);
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(addr);
            serverSocket = serverSocketChannel.socket();
            openStreams(serverSocket.accept());
        }

        @Override
        public void internalClose() throws IOException {
            super.internalClose();
            serverSocket.close();
        }
    }

    private static class RClientSocketConnection extends RSocketReadWriteConnection {

        RClientSocketConnection(RSocketConnection base) throws IOException {
            super(base);
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(base.host, base.port));
            openStreams(socketChannel.socket());
        }

    }

    @RBuiltin(name = "socketConnection", kind = INTERNAL, parameterNames = {"host", "port", "server", "blocking", "open", "encoding", "timeout"})
    public abstract static class SocketConnection extends RBuiltinNode {
        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[1] = CastIntegerNodeGen.create(arguments[1], false, false, false);
            arguments[6] = CastIntegerNodeGen.create(arguments[6], false, false, false);
            return arguments;
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected Object socketConnection(RAbstractStringVector host, RAbstractIntVector portVec, byte server, byte blocking, RAbstractStringVector open, RAbstractStringVector encoding, int timeout) {
            int port = portVec.getDataAt(0);
            String modeString = open.getDataAt(0);
            try {
                if (RRuntime.fromLogical(server)) {
                    return new RSocketConnection(modeString, true, host.getDataAt(0), port, RRuntime.fromLogical(blocking), timeout);
                } else {
                    return new RSocketConnection(modeString, false, host.getDataAt(0), port, RRuntime.fromLogical(blocking), timeout);
                }
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    private static class URLRConnection extends BaseRConnection {
        protected final String urlString;

        protected URLRConnection(String url, String modeString) throws IOException {
            super(ConnectionClass.URL, modeString);
            this.urlString = url;
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        public String getSummaryDescription() {
            return urlString;
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (openMode.abstractOpenMode) {
                case Read:
                    delegate = new URLReadRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + openMode);
            }
            setDelegate(delegate);
        }
    }

    private static class URLReadRConnection extends DelegateReadRConnection {

        private BufferedInputStream inputStream;

        protected URLReadRConnection(URLRConnection base) throws MalformedURLException, IOException {
            super(base);
            URL url = new URL(base.urlString);
            inputStream = new BufferedInputStream(url.openStream());
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RError.nyi(null, " readBin on URL");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RError.nyi(null, " readBinChars on URL");
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(inputStream, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            base.closed = true;
            internalClose();
        }

        @Override
        public void internalClose() throws IOException {
            inputStream.close();
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            return readCharHelper(nchars, inputStream, useBytes);
        }

    }

    @RBuiltin(name = "url", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding"})
    public abstract static class URLConnection extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object urlConnection(RAbstractStringVector url, RAbstractStringVector open, @SuppressWarnings("unused") byte blocking, @SuppressWarnings("unused") RAbstractStringVector encoding) {
            controlVisibility();
            try {
                return new URLRConnection(url.getDataAt(0), open.getDataAt(0));
            } catch (MalformedURLException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNSUPPORTED_URL_SCHEME);
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    private abstract static class CheckIsConnAdapter extends RBuiltinNode {
        protected RConnection checkIsConnection(Object con) throws RError {
            if (!(con instanceof RConnection)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_CONNECTION, "con");
            } else {
                return (RConnection) con;
            }
        }
    }

    @RBuiltin(name = "summary.connection", kind = INTERNAL, parameterNames = {"object}"})
    public abstract static class Summary extends CheckIsConnAdapter {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"description", "class", "mode", "text", "opened", "can read", "can write"},
                        RDataFactory.COMPLETE_VECTOR);

        @Specialization
        @TruffleBoundary
        protected RList summary(Object object) {
            BaseRConnection baseCon;
            if (object instanceof Integer) {
                baseCon = BaseRConnection.getConnection((int) object);
            } else if (object instanceof Double) {
                baseCon = BaseRConnection.getConnection((int) Math.floor((Double) object));

            } else {
                RConnection con = checkIsConnection(object);
                baseCon = getBaseConnection(con);
            }
            Object[] data = new Object[NAMES.getLength()];
            data[0] = baseCon.getSummaryDescription();
            data[1] = baseCon.classHr.getDataAt(0);
            data[2] = baseCon.openMode.modeString;
            data[3] = baseCon.getSummaryText();
            data[4] = baseCon.closed || !baseCon.opened ? "closed" : "opened";
            data[5] = baseCon.canRead() ? "yes" : "no";
            data[6] = baseCon.canWrite() ? "yes" : "no";
            return RDataFactory.createList(data, NAMES);
        }
    }

    @RBuiltin(name = "open", kind = INTERNAL, parameterNames = {"con", "open", "blocking"})
    public abstract static class Open extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object open(RConnection con, RAbstractStringVector open, @SuppressWarnings("unused") byte blocking) {
            forceVisibility(false);
            try {
                BaseRConnection baseConn = getBaseConnection(con);
                if (baseConn.closed) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_CONNECTION);
                }
                if (baseConn.opened) {
                    RContext.getInstance().setEvalWarning(RError.Message.ALREADY_OPEN_CONNECTION.message);
                    return RNull.instance;
                }
                baseConn.openMode = new OpenMode(open.getDataAt(0));
                baseConn.createDelegateConnection();
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object open(Object con, Object open, Object blocking) {
            checkIsConnection(con);
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
        }
    }

    @RBuiltin(name = "isOpen", kind = INTERNAL, parameterNames = {"con", "rw"})
    public abstract static class IsOpen extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected RLogicalVector isOpen(RConnection con, RAbstractIntVector rw) {
            controlVisibility();
            BaseRConnection baseCon = getBaseConnection(con);
            boolean result = !baseCon.closed && baseCon.opened;
            switch (rw.getDataAt(0)) {
                case 0:
                    break;
                case 1:
                    result &= baseCon.canRead();
                    break;
                case 2:
                    result &= baseCon.canWrite();
                    break;
                default:
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_VALUE, "rw");
            }
            return RDataFactory.createLogicalVectorFromScalar(result);
        }

        @Fallback
        @TruffleBoundary
        protected Object isOpen(Object con, @SuppressWarnings("unused") Object rw) {
            checkIsConnection(con);
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
        }

    }

    @RBuiltin(name = "close", kind = INTERNAL, parameterNames = {"con", "type"})
    public abstract static class Close extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object close(RConnection con) {
            forceVisibility(false);
            try {
                con.close();
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        protected Object close(Object con) {
            checkIsConnection(con);
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
        }
    }

    /**
     * This is inherited by the {@code readXX/writeXXX} builtins that are required to "close" a
     * connection that they opened. It does not destroy the connection.
     */
    private abstract static class InternalCloseHelper extends RBuiltinNode {
        protected void internalClose(RConnection con) throws RError {
            try {
                BaseRConnection baseConn = getBaseConnection(con);
                baseConn.internalClose();
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
        }

    }

    @RBuiltin(name = "readLines", kind = INTERNAL, parameterNames = {"con", "n", "ok", "warn", "encoding", "skipNul"})
    public abstract static class ReadLines extends InternalCloseHelper {
        @Specialization
        @TruffleBoundary
        protected Object readLines(RConnection con, int n, byte ok, @SuppressWarnings("unused") byte warn, @SuppressWarnings("unused") String encoding, @SuppressWarnings("unused") byte skipNul) {
            // TODO implement all the arguments
            controlVisibility();
            boolean wasOpen = true;
            try {
                wasOpen = con.forceOpen("rt");
                String[] lines = con.readLines(n);
                if (n > 0 && lines.length < n && ok == RRuntime.LOGICAL_FALSE) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_FEW_LINES_READ_LINES);
                }
                return RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            } finally {
                if (!wasOpen) {
                    internalClose(con);
                }
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object readLines(RConnection con, double n, byte ok, byte warn, String encoding, byte skipNul) {
            return readLines(con, (int) n, ok, warn, encoding, skipNul);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object readLines(Object con, Object n, Object ok, Object warn, Object encoding, Object skipNul) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "writeLines", kind = INTERNAL, parameterNames = {"text", "con", "sep", "useBytes"})
    public abstract static class WriteLines extends InternalCloseHelper {
        @Specialization
        @TruffleBoundary
        protected RNull writeLines(RAbstractStringVector text, RConnection con, RAbstractStringVector sep, @SuppressWarnings("unused") byte useBytes) {
            boolean wasOpen = true;
            try {
                wasOpen = con.forceOpen("wt");
                con.writeLines(text, sep.getDataAt(0));
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            } finally {
                if (!wasOpen) {
                    internalClose(con);
                }
            }
            forceVisibility(false);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected RNull writeLines(Object text, Object con, Object sep, Object useBytes) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "flush", kind = INTERNAL, parameterNames = {"con"})
    public abstract static class Flush extends RBuiltinNode {
        @TruffleBoundary
        @Specialization
        protected RNull flush(RConnection con) {
            controlVisibility();
            try {
                con.flush();
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_FLUSHING_CONNECTION, x.getMessage());
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "pushBack", kind = INTERNAL, parameterNames = {"data", "connection", "newLine", "type"})
    public abstract static class PushBack extends RBuiltinNode {

        @CreateCast("arguments")
        protected RNode[] castTimesLength(RNode[] arguments) {
            arguments[0] = CastToVectorNodeGen.create(CastStringNodeGen.create(arguments[0], false, false, false, false), false, false, false, false);
            return arguments;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull pushBack(RAbstractStringVector data, RConnection connection, RAbstractLogicalVector newLine, RAbstractIntVector type) {
            controlVisibility();
            if (newLine.getLength() == 0) {
                throw RError.error(RError.Message.INVALID_ARGUMENT, "newLine");
            }
            connection.pushBack(data, newLine.getDataAt(0) == RRuntime.LOGICAL_TRUE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object pushBack(RAbstractStringVector data, RConnection connection, RNull newLine, RAbstractIntVector type) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_ARGUMENT, "newLine");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!newLineIsLogical")
        protected Object pushBack(RAbstractStringVector data, RConnection connection, RAbstractVector newLine, RAbstractIntVector type) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_ARGUMENT, "newLine");
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object pushBack(Object data, Object connection, Object newLine, Object encoding) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_CONNECTION);
        }

        @SuppressWarnings("unused")
        protected boolean newLineIsLogical(RAbstractStringVector data, RConnection connection, RAbstractVector newLine) {
            return newLine.getElementClass() == RLogical.class;
        }

    }

    @RBuiltin(name = "pushBackLength", kind = INTERNAL, parameterNames = {"connection"})
    public abstract static class PushBackLength extends RBuiltinNode {

        @Specialization
        protected int pushBackLength(RConnection connection) {
            controlVisibility();
            return connection.pushBackLength();
        }

        @Fallback
        protected Object pushBacklLength(@SuppressWarnings("unused") Object connection) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_CONNECTION);
        }

    }

    @RBuiltin(name = "clearPushBack", kind = INTERNAL, parameterNames = {"connection"})
    public abstract static class PushBackClear extends RBuiltinNode {

        @Specialization
        protected RNull pushBackClear(RConnection connection) {
            controlVisibility();
            connection.pushBackClear();
            return RNull.instance;
        }

        @Fallback
        protected Object pushBackClear(@SuppressWarnings("unused") Object connection) {
            controlVisibility();
            throw RError.error(RError.Message.INVALID_CONNECTION);
        }

    }

    @RBuiltin(name = "readChar", kind = INTERNAL, parameterNames = {"con", "nchars", "useBytes"})
    public abstract static class ReadChar extends InternalCloseHelper {

        @SuppressWarnings("unused")
        @Specialization(guards = "ncharsEmpty")
        protected RStringVector readCharNcharsEmpty(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            controlVisibility();
            return RDataFactory.createEmptyStringVector();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "useBytesEmpty")
        protected RStringVector readCharUseBytesEmpty(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "useBytes");
        }

        @Specialization(guards = {"!ncharsEmpty", "!useBytesEmpty"})
        @TruffleBoundary
        protected RStringVector readChar(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            controlVisibility();
            boolean wasOpen = true;
            try {
                wasOpen = con.forceOpen("rb");
                String[] data = new String[nchars.getLength()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = con.readChar(nchars.getDataAt(i), useBytes.getDataAt(0) == RRuntime.LOGICAL_TRUE);
                }
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            } finally {
                if (!wasOpen) {
                    internalClose(con);
                }
            }
        }

        boolean ncharsEmpty(@SuppressWarnings("unused") RConnection con, RAbstractIntVector nchars) {
            return nchars.getLength() == 0;
        }

        @SuppressWarnings("unused")
        boolean useBytesEmpty(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            return useBytes.getLength() == 0;
        }

    }

    @RBuiltin(name = "writeChar", kind = INTERNAL, parameterNames = {"object", "con", "nchars", "eos", "useBytes"})
    public abstract static class WriteChar extends InternalCloseHelper {
        @TruffleBoundary
        @Specialization
        protected RNull writeChar(RAbstractStringVector object, RConnection con, RAbstractIntVector nchars, RAbstractStringVector eos, byte useBytes) {
            controlVisibility();
            boolean wasOpen = true;
            try {
                wasOpen = con.forceOpen("wb");
                int length = object.getLength();
                for (int i = 0; i < length; i++) {
                    String s = object.getDataAt(i);
                    int nc = nchars.getDataAt(i % length);
                    int pad = nc - s.length();
                    if (pad > 0) {
                        RContext.getInstance().setEvalWarning(RError.Message.MORE_CHARACTERS.message);
                    }
                    con.writeChar(s, pad, eos.getDataAt(i % length), RRuntime.fromLogical(useBytes));
                }
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            } finally {
                if (!wasOpen) {
                    internalClose(con);
                }
            }
            forceVisibility(false);
            return RNull.instance;
        }
    }

    /**
     * Sets the order of the given {@code ByteBuffer} from value of {@code swap}. The value of
     * {@code swap} will be {@code true} iff the original requested order was the opposite of the
     * native byte order. Perhaps surprisingly, the default order of a {@code ByteBuffer} is
     * {@code ByteOrder.BIG_ENDIAN}, NOT the native byte order.
     */
    private static ByteBuffer checkOrder(ByteBuffer buffer, boolean swap) {
        ByteOrder nb = ByteOrder.nativeOrder();
        if (swap) {
            nb = nb == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        }
        return buffer.order(nb);
    }

    @RBuiltin(name = "readBin", kind = INTERNAL, parameterNames = {"con", "what", "n", "size", "signed", "swap"})
    public abstract static class ReadBin extends InternalCloseHelper {
        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[2] = CastIntegerNodeGen.create(arguments[2], false, false, false);
            return arguments;
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected Object readBin(RConnection con, RAbstractStringVector whatVec, RAbstractIntVector nVec, int size, byte signedArg, byte swapArg) {
            boolean swap = RRuntime.fromLogical(swapArg);
            boolean wasOpen = true;
            RVector result = null;
            int n = nVec.getDataAt(0);
            try {
                if (getBaseConnection(con).openMode.isText()) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.ONLY_READ_BINARY_CONNECTION);
                }
                wasOpen = con.forceOpen("rb");
                String what = whatVec.getDataAt(0);
                switch (what) {
                    case "int":
                    case "integer":
                        result = readInteger(con, n, swap);
                        break;
                    case "double":
                    case "numeric":
                        result = readDouble(con, n, swap);
                        break;
                    case "complex":
                        result = readComplex(con, n, swap);
                        break;
                    case "character":
                        result = readString(con, n);
                        break;
                    case "logical":
                        result = readLogical(con, n, swap);
                        break;
                    case "raw":
                        result = readRaw(con, n);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            } finally {
                if (!wasOpen) {
                    internalClose(con);
                }
            }
            return result;
        }

        private static RIntVector readInteger(RConnection con, int n, boolean swap) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n * 4);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            buffer.flip();
            IntBuffer intBuffer = checkOrder(buffer, swap).asIntBuffer();
            int nInts = bytesRead / 4;
            int[] data = new int[nInts];
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < nInts; i++) {
                int d = intBuffer.get();
                if (RRuntime.isNA(d)) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                }
                data[i] = d;
            }
            return RDataFactory.createIntVector(data, complete);
        }

        private static RDoubleVector readDouble(RConnection con, int n, boolean swap) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n * 8);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyDoubleVector();
            }
            buffer.flip();
            DoubleBuffer doubleBuffer = checkOrder(buffer, swap).asDoubleBuffer();
            int nDoubles = bytesRead / 8;
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            double[] data = new double[nDoubles];
            for (int i = 0; i < nDoubles; i++) {
                double d = doubleBuffer.get();
                if (RRuntime.isNA(d)) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                }
                data[i] = d;
            }
            return RDataFactory.createDoubleVector(data, complete);
        }

        private static RComplexVector readComplex(RConnection con, int n, boolean swap) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n * 16);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyComplexVector();
            }
            buffer.flip();
            DoubleBuffer doubleBuffer = checkOrder(buffer, swap).asDoubleBuffer();
            int nComplex = bytesRead / 16;
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            double[] data = new double[nComplex * 2];
            for (int i = 0; i < nComplex; i++) {
                double re = doubleBuffer.get();
                double im = doubleBuffer.get();
                if (RRuntime.isNA(re) || RRuntime.isNA(im)) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                }
                data[2 * i] = re;
                data[2 * i + 1] = im;
            }
            return RDataFactory.createComplexVector(data, complete);
        }

        private static RStringVector readString(RConnection con, int n) throws IOException {
            ArrayList<String> strings = new ArrayList<>(n);
            int s = 0;
            while (s < n) {
                byte[] chars = con.readBinChars();
                if (chars == null) {
                    break;
                }
                int npos = 0;
                while (chars[npos] != 0) {
                    npos++;
                }
                strings.add(new String(chars, 0, npos));
                s++;
            }
            // There is no special encoding for NA_character_
            String[] stringData = new String[s];
            strings.toArray(stringData);
            return RDataFactory.createStringVector(stringData, RDataFactory.COMPLETE_VECTOR);
        }

        private static RRawVector readRaw(RConnection con, int n) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyRawVector();
            }
            buffer.flip();
            byte[] data = new byte[bytesRead];
            buffer.get(data);
            return RDataFactory.createRawVector(data);
        }

        private static RLogicalVector readLogical(RConnection con, int n, boolean swap) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n * 4);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyLogicalVector();
            }
            buffer.flip();
            IntBuffer intBuffer = checkOrder(buffer, swap).asIntBuffer();
            int nInts = bytesRead / 4;
            byte[] data = new byte[nInts];
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            for (int i = 0; i < nInts; i++) {
                int value = intBuffer.get();
                data[i] = value == RRuntime.INT_NA ? RRuntime.LOGICAL_NA : value == 1 ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
                if (value == RRuntime.INT_NA) {
                    complete = RDataFactory.INCOMPLETE_VECTOR;
                }
            }
            return RDataFactory.createLogicalVector(data, complete);
        }
    }

    @RBuiltin(name = "writeBin", kind = INTERNAL, parameterNames = {"object", "con", "size", "swap", "useBytes"})
    public abstract static class WriteBin extends InternalCloseHelper {
        @TruffleBoundary
        @Specialization
        protected Object writeBin(RAbstractVector object, RConnection con, int size, byte swapArg, byte useBytesArg) {
            boolean swap = RRuntime.fromLogical(swapArg);
            boolean wasOpen = true;
            boolean useBytes = RRuntime.fromLogical(useBytesArg);
            if (object.getLength() > 0) {
                try {
                    if (getBaseConnection(con).isTextMode()) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ONLY_WRITE_BINARY_CONNECTION);
                    }
                    wasOpen = con.forceOpen("wb");
                    if (object instanceof RAbstractIntVector) {
                        writeInteger((RAbstractIntVector) object, con, size, swap);
                    } else if (object instanceof RAbstractDoubleVector) {
                        writeDouble((RAbstractDoubleVector) object, con, size, swap);
                    } else if (object instanceof RAbstractComplexVector) {
                        writeComplex((RAbstractComplexVector) object, con, size, swap);
                    } else if (object instanceof RAbstractStringVector) {
                        writeString((RAbstractStringVector) object, con, size, swap, useBytes);
                    } else if (object instanceof RAbstractStringVector) {
                        writeLogical((RAbstractLogicalVector) object, con, size, swap);
                    } else if (object instanceof RRawVector) {
                        writeRaw((RAbstractRawVector) object, con, size, swap);
                    } else {
                        throw RError.nyi(getEncapsulatingSourceSection(), " vector type");
                    }
                } catch (IOException x) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
                } finally {
                    if (!wasOpen) {
                        internalClose(con);
                    }
                }
            }
            forceVisibility(false);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        private static void writeInteger(RAbstractIntVector object, RConnection con, int size, boolean swap) throws IOException {
            int length = object.getLength();
            ByteBuffer buffer = ByteBuffer.allocate(4 * length);
            IntBuffer intBuffer = checkOrder(buffer, swap).asIntBuffer();
            for (int i = 0; i < length; i++) {
                int value = object.getDataAt(i);
                intBuffer.put(value);
            }
            // The underlying buffer offset/position is independent of IntBuffer, so no flip
            con.writeBin(buffer);
        }

        @SuppressWarnings("unused")
        private static void writeDouble(RAbstractDoubleVector object, RConnection con, int size, boolean swap) throws IOException {
            int length = object.getLength();
            ByteBuffer buffer = ByteBuffer.allocate(8 * length);
            DoubleBuffer doubleBuffer = checkOrder(buffer, swap).asDoubleBuffer();
            for (int i = 0; i < length; i++) {
                double value = object.getDataAt(i);
                doubleBuffer.put(value);
            }
            // The underlying buffer offset/position is independent of DoubleBuffer, so no flip
            con.writeBin(buffer);
        }

        @SuppressWarnings("unused")
        private static void writeComplex(RAbstractComplexVector object, RConnection con, int size, boolean swap) throws IOException {
            int length = object.getLength();
            ByteBuffer buffer = ByteBuffer.allocate(16 * length);
            DoubleBuffer doubleBuffer = checkOrder(buffer, swap).asDoubleBuffer();
            for (int i = 0; i < length; i++) {
                RComplex complex = object.getDataAt(i);
                double re = complex.getRealPart();
                double im = complex.getImaginaryPart();
                doubleBuffer.put(re);
                doubleBuffer.put(im);
            }
            // The underlying buffer offset/position is independent of DoubleBuffer, so no flip
            con.writeBin(buffer);
        }

        @SuppressWarnings("unused")
        private static void writeString(RAbstractStringVector object, RConnection con, int size, boolean swap, boolean useBytes) throws IOException {
            int length = object.getLength();
            byte[][] data = new byte[length][];
            int totalLength = 0;
            for (int i = 0; i < length; i++) {
                String s = object.getDataAt(i);
                // There is no special encoding for NA_character_
                data[i] = s.getBytes();
                totalLength = totalLength + data[i].length + 1; // zero pad
            }

            ByteBuffer buffer = ByteBuffer.allocate(totalLength);
            for (int i = 0; i < length; i++) {
                buffer.put(data[i]);
                buffer.put((byte) 0);
            }
            buffer.flip();
            con.writeBin(buffer);
        }

        @SuppressWarnings("unused")
        private static void writeLogical(RAbstractLogicalVector object, RConnection con, int size, boolean swap) throws IOException {
            // encoded as ints, with FALSE=0, TRUE=1, NA=Integer_NA_
            int length = object.getLength();
            ByteBuffer buffer = ByteBuffer.allocate(length * 4);
            IntBuffer intBuffer = checkOrder(buffer, swap).asIntBuffer();
            for (int i = 0; i < length; i++) {
                byte value = object.getDataAt(i);
                int encoded = RRuntime.isNA(value) ? RRuntime.INT_NA : value == RRuntime.LOGICAL_FALSE ? 0 : 1;
                intBuffer.put(encoded);
            }
            con.writeBin(buffer);

        }

        @SuppressWarnings("unused")
        private static void writeRaw(RAbstractRawVector object, RConnection con, int size, boolean swap) throws IOException {
            int length = object.getLength();
            ByteBuffer buffer = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                RRaw value = object.getDataAt(i);
                buffer.put(value.getValue());
            }
            buffer.flip();
            con.writeBin(buffer);

        }
    }

    @RBuiltin(name = "getConnection", kind = INTERNAL, parameterNames = {"what"})
    public abstract static class GetConnection extends RBuiltinNode {
        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[0] = CastIntegerNodeGen.create(arguments[0], false, false, false);
            return arguments;
        }

        @Specialization
        @TruffleBoundary
        protected RConnection getConnection(int what) {
            controlVisibility();
            boolean error = what < 0 || what >= allConnections.length || allConnections[what] == null;
            if (error) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_CONNECTION, what);
            } else {
                return allConnections[what];
            }
        }
    }

    @RBuiltin(name = "getAllConnections", kind = INTERNAL, parameterNames = {})
    public abstract static class GetAllConnections extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RIntVector getAllConnections() {
            controlVisibility();
            return BaseRConnection.getAllConnections();
        }
    }

}
