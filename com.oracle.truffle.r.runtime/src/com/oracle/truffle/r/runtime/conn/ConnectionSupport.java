/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ref.*;
import java.nio.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Basic support classes and methods for the connection implementations.
 */
public class ConnectionSupport {

    /**
     * The context specific state, which is the set of active connections. We stored these as weak
     * references to detect when they become unused whuch, when detected, results in them being
     * closed if still open.
     */
    public static final class ContextStateImpl implements RContext.ContextState {
        private static final int MAX_CONNECTIONS = 128;
        /**
         * Records all connections. The index in the array is the "descriptor" used in
         * {@code getConnection}. A {@code null} value indicates a slot that is not in use.
         */
        private final ArrayList<WeakReference<BaseRConnection>> allConnections = new ArrayList<>(MAX_CONNECTIONS);

        /**
         * High water mark in {@link #allConnections}.
         */
        private int hwm = 2;

        /**
         * Periodically we can poll the queue and close unused connections as per GnuR.
         */
        private final ReferenceQueue<BaseRConnection> refQueue = new ReferenceQueue<>();

        private ContextStateImpl() {
            for (int i = 0; i < MAX_CONNECTIONS; i++) {
                allConnections.add(i, null);
            }
        }

        public BaseRConnection getConnection(int index) {
            if (index >= 0 && index <= hwm) {
                WeakReference<BaseRConnection> ref = allConnections.get(index);
                if (ref != null) {
                    return ref.get();
                }
            }
            return null;
        }

        private int setStdConnection(int index, BaseRConnection con) {
            assert index >= 0 && index <= 2;
            return setConnection(index, con);
        }

        private int setConnection(int index, BaseRConnection con) {
            assert allConnections.get(index) == null;
            allConnections.set(index, new WeakReference<>(con, refQueue));
            return index;
        }

        public RIntVector getAllConnections() {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i <= hwm; i++) {
                WeakReference<BaseRConnection> ref = allConnections.get(i);
                if (ref != null) {
                    BaseRConnection con = ref.get();
                    if (con != null) {
                        list.add(i);
                    }
                }
            }
            int[] data = new int[list.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = list.get(i);
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        private void destroyConnection(int index) {
            allConnections.get(index).clear();
            allConnections.set(index, null);
        }

        private int setConnection(BaseRConnection con) {
            collectUnusedConnections();
            for (int i = 3; i < MAX_CONNECTIONS; i++) {
                if (allConnections.get(i) == null) {
                    if (i > hwm) {
                        hwm = i;
                    }
                    return setConnection(i, con);
                }
            }
            throw RError.error(RError.SHOW_CALLER2, RError.Message.ALL_CONNECTIONS_IN_USE);
        }

        @Override
        public void beforeDestroy(RContext context) {
            // close all open connections
            for (int i = 3; i <= hwm; i++) {
                WeakReference<BaseRConnection> ref = allConnections.get(i);
                if (ref != null) {
                    BaseRConnection con = ref.get();
                    if (con != null) {
                        closeAndDestroy(con);
                    }
                    ref.clear();
                }
            }
        }

        private void collectUnusedConnections() {
            while (true) {
                Reference<? extends BaseRConnection> ref = refQueue.poll();
                if (ref == null) {
                    return;
                }
                BaseRConnection con = ref.get();
                if (con instanceof TextConnections.TextRConnection) {
                    RError.warning(RError.SHOW_CALLER2, RError.Message.UNUSED_TEXTCONN, con.descriptor, ((TextConnections.TextRConnection) con).description);
                }
                if (con != null) {
                    int index = con.descriptor;
                    closeAndDestroy(con);
                    allConnections.set(index, null);
                }
            }
        }

        private static void closeAndDestroy(BaseRConnection con) {
            if (!con.closed) {
                try {
                    con.closeAndDestroy();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        public static ContextStateImpl newContext(@SuppressWarnings("unused") RContext context) {
            return new ContextStateImpl();
        }
    }

    private static ContextStateImpl getContextStateImpl() {
        return RContext.getInstance().stateRConnection;
    }

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
    enum AbstractOpenMode {
        Lazy(new String[]{""}, true, true, true),
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

        public final String[] modeStrings;
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
    public static class OpenMode {
        public final AbstractOpenMode abstractOpenMode;
        /**
         * When {@link #abstractOpenMode} is {@code Lazy}, this is used to store the default open
         * mode, otherwise it is the actual string passed to the open.
         */
        public final String modeString;

        /**
         * For connections other than {@link StdConnections}.
         */

        public OpenMode(String modeString, AbstractOpenMode defaultModeForLazy) throws IOException {
            this.abstractOpenMode = AbstractOpenMode.getOpenMode(modeString);
            this.modeString = abstractOpenMode == AbstractOpenMode.Lazy ? defaultModeForLazy.modeStrings[0] : modeString;
        }

        /**
         * For {@link StdConnections}.
         */
        OpenMode(AbstractOpenMode mode) throws IOException {
            this(mode.modeStrings[0], mode);
        }

        /**
         * Only used when a lazy connection is actually opened.
         */
        OpenMode(String modeString) throws IOException {
            // defaultModeForLazy will not be used
            this(modeString, null);
        }

        public boolean isText() {
            return abstractOpenMode.isText;
        }

        boolean canRead() {
            return abstractOpenMode.readable;
        }

        boolean canWrite() {
            return abstractOpenMode.writeable;
        }

        public String summaryString() {
            /*
             * FIXME this is not entirely consistent with GnuR, which sometimes displays "" for
             * unopened connections instead of the default mode. E.g. file("foo") and
             * socketConnection(open="", port=n) differ.
             */
            return modeString;
        }
    }

    enum ConnectionClass {
        Terminal("terminal"),
        File("file"),
        GZFile("gzfile"),
        Socket("sockconn"),
        Text("textConnection"),
        URL("url"),
        Internal("internal");

        final String printName;

        ConnectionClass(String printName) {
            this.printName = printName;
        }

    }

    public static final String FILE_URL_PREFIX = "file://";

    public static String removeFileURLPrefix(String path) {
        if (path.startsWith(FILE_URL_PREFIX)) {
            return path.replace(FILE_URL_PREFIX, "");
        } else {
            return path;
        }
    }

    /**
     * In GnuR a connection is just an numeric vector and can be created by setting the class, even
     * if the result isn't actually a valid connection, e.g.
     * {@code structure(2, class=c("terminal","connection"))}.
     */
    public static RConnection fromVector(RVector vector, @SuppressWarnings("unused") RStringVector classAttr) {
        int index = RRuntime.asInteger(vector);
        RConnection result = RContext.getInstance().stateRConnection.getConnection(index);
        if (result == null) {
            // non-existent connection, still legal according to GnuR
            // we cannot throw an error here as this can be used by parallel packages - do it lazily
            return InvalidConnection.instance;
        }
        return result;
    }

    // TODO implement all open modes

    public static class InvalidConnection extends RConnection {

        public static final InvalidConnection instance = new InvalidConnection();

        private static final int INVALID_DESCRIPTOR = -1;

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public String[] readLines(int n, boolean warn, boolean skipNul) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void closeAndDestroy() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public boolean canRead() {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public boolean canWrite() {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public RConnection forceOpen(String modeString) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void close() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public boolean isSeekable() {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public int getc() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void flush() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public int getDescriptor() {
            return INVALID_DESCRIPTOR;
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public String readChar(int nchars, boolean useBytes) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public int readBin(ByteBuffer buffer) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public byte[] readBinChars() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public boolean isTextMode() {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public boolean isOpen() {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

    }

    /**
     * Class that holds common state for all {@link RConnection} instances. It supports lazy
     * opening, as required by the R spec, through the {@link #theConnection} field, which
     * ultimately holds the actual connection instance when opened. Any builtin that uses a
     * connection passed in as argument must call {@link RConnection#forceOpen} to check whether the
     * connection is already open. If not already open {@link #forceOpen} will call
     * {@link #createDelegateConnection()} to create the actual connection. The result of
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
    public abstract static class BaseRConnection extends RConnection {

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
         * Supports the auto-close operation for connections opened just for a single operation.
         */
        private boolean tempOpened;

        /**
         * if {@link #opened} is {@code true} the {@link OpenMode} that this connection is opened
         * in, otherwise {@link AbstractOpenMode#Lazy}.
         */
        private OpenMode openMode;

        /**
         * The actual connection, if delegated.
         */
        protected DelegateRConnection theConnection;

        /**
         * An integer used to identify the connection to the {@code getAllConnections} builtin.
         */
        private int descriptor;

        /**
         * The constructor for every connection class except {@link StdConnections}.
         *
         * @param conClass the specific class of the connection, e.g, {@link ConnectionClass#File}
         * @param modeString the mode in which the connection should be opened, "" for lazy opening
         * @param defaultModeForLazy the mode to use when this connection is opened implicitly
         *
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString, AbstractOpenMode defaultModeForLazy) throws IOException {
            this(conClass, new OpenMode(modeString, defaultModeForLazy));
            if (conClass != ConnectionClass.Internal) {
                this.descriptor = getContextStateImpl().setConnection(this);
            }
        }

        /**
         * The constructor for {@link StdConnections}.
         */
        protected BaseRConnection(AbstractOpenMode mode, int index) throws IOException {
            this(ConnectionClass.Terminal, new OpenMode(mode));
            this.descriptor = getContextStateImpl().setStdConnection(index, this);
        }

        /**
         * Primitive constructor that just assigns state.
         */
        private BaseRConnection(ConnectionClass conClass, OpenMode mode) {
            this.openMode = mode;
            String[] classes = new String[2];
            classes[0] = conClass.printName;
            classes[1] = "connection";
            initAttributes().put(RRuntime.CLASS_ATTR_KEY, RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR));
            // For GnuR compatibility we define the "conn_id" attribute
            getAttributes().put("conn_id", RDataFactory.createExternalPtr(0, RDataFactory.createSymbol("connection")));
        }

        protected void openNonLazyConnection() throws IOException {
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        /**
         * A connection is "active" if it has been opened and not yet closed.
         */
        @Override
        public boolean isOpen() {
            return !closed && opened;
        }

        /**
         * Return value for "can read" for {@code summary.connection}.
         */
        @Override
        public boolean canRead() {
            if (isOpen()) {
                return getOpenMode().canRead();
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
            if (isOpen()) {
                return getOpenMode().canWrite();
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
            return getOpenMode().isText();
        }

        @Override
        public RConnection forceOpen(String modeString) throws IOException {
            if (closed) {
                throw new IOException(RError.Message.INVALID_CONNECTION.message);
            }
            if (!opened) {
                tempOpened = true;
                // internal closed or lazy
                if (openMode.abstractOpenMode == AbstractOpenMode.Lazy) {
                    // modeString may override the default
                    openMode = new OpenMode(modeString == null ? openMode.modeString : modeString);
                }
                createDelegateConnection();
            }
            return this;
        }

        /**
         * This is used exclusively by the {@code open} builtin.
         */
        public void open(String modeString) throws IOException {
            if (openMode.abstractOpenMode == AbstractOpenMode.Lazy) {
                // modeString may override the default
                openMode = new OpenMode(modeString == null ? openMode.modeString : modeString);
            }
            createDelegateConnection();
        }

        protected void checkOpen() {
            assert !closed && opened;
        }

        protected void setDelegate(DelegateRConnection conn) {
            this.theConnection = conn;
            opened = true;
        }

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
            checkOpen();
            return theConnection.readLinesInternal(n, warn, skipNul);
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
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            checkOpen();
            theConnection.writeLines(lines, sep, useBytes);
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            checkOpen();
            theConnection.writeString(s, nl);
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
        public void closeAndDestroy() throws IOException {
            closed = true;
            if (theConnection != null) {
                theConnection.closeAndDestroy();
            }
            getContextStateImpl().destroyConnection(this.descriptor);
        }

        @Override
        public void close() throws IOException {
            if (tempOpened) {
                tempOpened = false;
                opened = false;
                if (theConnection != null) {
                    DelegateRConnection tc = theConnection;
                    theConnection = null;
                    tc.close();
                }
            }
        }

        @Override
        public int getc() throws IOException {
            checkOpen();
            return theConnection.getc();
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            checkOpen();
            return theConnection.seek(offset, seekMode, seekRWMode);
        }

        @Override
        public boolean isSeekable() {
            checkOpen();
            return theConnection.isSeekable();
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
        public abstract String getSummaryDescription();

        /**
         * Return the value that is used in the "text" field by {@code summary.connection}.
         */
        public String getSummaryText() {
            return isTextMode() ? "text" : "binary";
        }

        @Override
        public int getDescriptor() {
            return descriptor;
        }

        public OpenMode getOpenMode() {
            return openMode;
        }

        public boolean isClosed() {
            return closed;
        }

    }

    public static BaseRConnection getBaseConnection(RConnection conn) {
        if (conn instanceof BaseRConnection) {
            return (BaseRConnection) conn;
        } else if (conn instanceof DelegateReadRConnection) {
            return ((DelegateReadRConnection) conn).base;
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    interface ReadWriteHelper {

        /**
         * {@code readLines} from an {@link InputStream}. It would be convenient to use a
         * {@link BufferedReader} but mixing binary and text operations, which is a requirement,
         * would then be difficult.
         *
         * @param warn TODO
         * @param skipNul TODO
         */
        default String[] readLinesHelper(InputStream in, int n, boolean warn, boolean skipNul) throws IOException {
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
                    if (totalRead > 0) {
                        /*
                         * TODO GnuR says keep data and output a warning if blocking, otherwise
                         * silently push back. FastR doesn't support non-blocking yet, so we keep
                         * the data. Some refactoring is needed to be able to reliably access the
                         * "name" for the warning.
                         */
                        lines.add(new String(buffer, 0, totalRead));
                        if (warn) {
                            RError.warning(RError.SHOW_CALLER2, RError.Message.INCOMPLETE_FINAL_LINE, "TODO: connection path");
                        }
                    }
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

        default void writeLinesHelper(OutputStream out, RAbstractStringVector lines, String sep) throws IOException {
            for (int i = 0; i < lines.getLength(); i++) {
                out.write(lines.getDataAt(i).getBytes());
                out.write(sep.getBytes());
            }
        }

        default void writeStringHelper(OutputStream out, String s, boolean nl) throws IOException {
            out.write(s.getBytes());
            if (nl) {
                out.write('\n');
            }
        }

        default void writeCharHelper(OutputStream out, String s, int pad, String eos) throws IOException {
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

        default void writeBinHelper(ByteBuffer buffer, OutputStream outputStream) throws IOException {
            int n = buffer.remaining();
            byte[] b = new byte[n];
            buffer.get(b);
            outputStream.write(b);
        }

        /**
         * Reads null-terminated character strings from an {@link InputStream}.
         */
        default byte[] readBinCharsHelper(InputStream in) throws IOException {
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

        default int readBinHelper(ByteBuffer buffer, InputStream inputStream) throws IOException {
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

        default String readCharHelper(int nchars, InputStream in, @SuppressWarnings("unused") boolean useBytes) throws IOException {
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

    abstract static class DelegateRConnection extends RConnection {
        protected BaseRConnection base;

        DelegateRConnection(BaseRConnection base) {
            this.base = base;
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
        public boolean isOpen() {
            return base.isOpen();
        }

        @Override
        public RConnection forceOpen(String modeString) throws IOException {
            return base.forceOpen(modeString);
        }

        @Override
        public boolean isSeekable() {
            return false;
        }

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.UNSEEKABLE_CONNECTION);
        }

    }

    abstract static class DelegateReadRConnection extends DelegateRConnection {
        protected DelegateReadRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep, boolean useBytes) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void writeChar(String s, int pad, String eos, boolean useBytes) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void writeString(String s, boolean nl) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public void writeBin(ByteBuffer buffer) throws IOException {
            throw new IOException(RError.Message.CANNOT_WRITE_CONNECTION.message);
        }

        @Override
        public int getc() throws IOException {
            return getInputStream().read();
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

    abstract static class DelegateWriteRConnection extends DelegateRConnection {
        protected DelegateWriteRConnection(BaseRConnection base) {
            super(base);
        }

        @Override
        public String[] readLinesInternal(int n, boolean warn, boolean skipNul) throws IOException {
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
        public int getc() throws IOException {
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

    abstract static class DelegateReadWriteRConnection extends DelegateRConnection {
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

        @Override
        public int getc() throws IOException {
            return getInputStream().read();
        }

    }

    abstract static class BasePathRConnection extends BaseRConnection {
        protected final String path;

        protected BasePathRConnection(String path, ConnectionClass connectionClass, String modeString) throws IOException {
            this(path, connectionClass, modeString, AbstractOpenMode.Read);
        }

        protected BasePathRConnection(String path, ConnectionClass connectionClass, String modeString, AbstractOpenMode defaultLazyOpenMode) throws IOException {
            super(connectionClass, modeString, defaultLazyOpenMode);
            this.path = Utils.tildeExpand(path);
        }

        @Override
        public String getSummaryDescription() {
            return path;
        }

    }

}
