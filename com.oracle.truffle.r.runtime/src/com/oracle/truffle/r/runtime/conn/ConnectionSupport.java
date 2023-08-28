/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * Basic support classes and methods for the connection implementations.
 */
@SuppressWarnings("this-escape")
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

        private ContextStateImpl() {
            for (int i = 0; i < MAX_CONNECTIONS; i++) {
                allConnections.add(i, null);
            }
        }

        @TruffleBoundary
        public BaseRConnection getConnection(int index, boolean throwError) {
            BaseRConnection conn = null;
            if (index >= 0 && index <= hwm) {
                WeakReference<BaseRConnection> ref = allConnections.get(index);
                if (ref != null) {
                    conn = ref.get();
                }
            }
            if (conn == null && throwError) {
                throw RError.error(RError.SHOW_CALLER, Message.INVALID_CONNECTION);
            }
            return conn;
        }

        private int setStdConnection(int index, BaseRConnection con) {
            assert index >= 0 && index <= 2;
            return setConnection(index, con);
        }

        private int setConnection(int index, BaseRConnection con) {
            assert allConnections.get(index) == null || allConnections.get(index).get() == null;
            allConnections.set(index, new WeakReference<>(con));
            return index;
        }

        public RIntVector getAllConnections() {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i <= hwm; i++) {
                WeakReference<BaseRConnection> ref = allConnections.get(i);
                if (ref != null) {
                    RConnection con = ref.get();
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
            int i = findEmptySlot();
            if (i == -1) {
                // TODO: rewrite to ReferenceQueue
                // We have no way of reclaiming the connection slots than GC...
                System.gc();
                i = findEmptySlot();
            }
            if (i >= 0) {
                return setConnection(i, con);
            } else {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.ALL_CONNECTIONS_IN_USE);
            }
        }

        private int findEmptySlot() {
            for (int i = 3; i < MAX_CONNECTIONS; i++) {
                if (allConnections.get(i) == null || allConnections.get(i).get() == null) {
                    if (i > hwm) {
                        hwm = i;
                    }
                    return i;
                }
            }
            return -1;
        }

        @Override
        public void beforeDispose(RContext context) {
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

        private static void closeAndDestroy(BaseRConnection con) {
            if (!con.closed) {
                try {
                    con.closeAndDestroy();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        public static ContextStateImpl newContextState() {
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
        Write(new String[]{"w", "wt", "wr"}, true, false, true),
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
    public static final class OpenMode {
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

        @Override
        public String toString() {
            return modeString;
        }
    }

    public enum ConnectionClass {
        Terminal("terminal"),
        File("file"),
        GZFile("gzfile"),
        GZCon("gzcon"),
        BZFile("bzfile"),
        XZFile("xzfile"),
        Socket("sockconn"),
        Text("textConnection"),
        URL("url"),
        RAW("rawConnection"),
        Internal("internal"),
        PIPE("pipe"),
        FIFO("fifo"),
        CHANNEL("Java Channel"),
        NATIVE("custom"),
        INVALID("invalid");

        private final String printName;

        ConnectionClass(String printName) {
            this.printName = printName;
        }

        public String getPrintName() {
            return printName;
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

    public static final class InvalidConnection extends BaseRConnection {

        protected InvalidConnection() {
            super(ConnectionClass.INVALID, null);
        }

        public static final InvalidConnection instance = new InvalidConnection();

        private static final int INVALID_DESCRIPTOR = -1;

        @Override
        public String[] readLines(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
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
        public BaseRConnection forceOpen(String modeString) throws IOException {
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
        public int getc() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void writeLines(RStringVector lines, String sep, boolean useBytes) throws IOException {
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

        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public ByteChannel getChannel() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public void truncate() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            throw RInternalError.shouldNotReachHere("INVALID CONNECTION");
        }

        @Override
        public String getSummaryDescription() {
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
    public abstract static class BaseRConnection extends RBaseObject implements RConnection {

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

        private boolean blocking = true;

        /**
         * The encoding to use to read or to write to the connection.
         */
        private Charset encoding;

        private ConnectionClass conClass;

        private LinkedList<String> pushBack;

        /**
         * Indicates that the last line read operation was incomplete.<br>
         * This is only relevant for connections in text and non-blocking mode.
         */
        private boolean incomplete = false;

        /**
         * The constructor for every connection class except {@link StdConnections}.
         *
         * @param conClass the specific class of the connection, e.g, {@link ConnectionClass#File}
         * @param modeString the mode in which the connection should be opened, "" for lazy opening
         * @param defaultModeForLazy the mode to use when this connection is opened implicitly
         * @param blocking Indicates if this connection has been openend in blocking mode.
         * @param encoding The name of the encoding used to read from or to write to the connection
         *            ({@code null} is allowed and sets the character set to
         *            {@code Charset#defaultCharset()}).
         *
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString, AbstractOpenMode defaultModeForLazy, boolean blocking, String encoding) throws IOException, IllegalCharsetNameException {
            this(conClass, new OpenMode(modeString, defaultModeForLazy));
            if (conClass != ConnectionClass.Internal) {
                this.descriptor = getContextStateImpl().setConnection(this);
            }
            this.blocking = blocking;
            if (encoding != null) {
                this.encoding = Charset.forName(ConnectionSupport.convertEncodingName(encoding));
            } else {
                this.encoding = Charset.defaultCharset();
            }
        }

        /**
         * The constructor for every connection class except {@link StdConnections}.
         *
         * @param conClass the specific class of the connection, e.g, {@link ConnectionClass#File}
         * @param modeString the mode in which the connection should be opened, "" for lazy opening
         * @param defaultModeForLazy the mode to use when this connection is opened implicitly
         * @param blocking Indicates if this connection has been openend in blocking mode.
         *
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString, AbstractOpenMode defaultModeForLazy, boolean blocking) throws IOException {
            this(conClass, modeString, defaultModeForLazy, blocking, null);
        }

        /**
         * The constructor for every connection class except {@link StdConnections}.
         *
         * @param conClass the specific class of the connection, e.g, {@link ConnectionClass#File}
         * @param modeString the mode in which the connection should be opened, "" for lazy opening
         * @param defaultModeForLazy the mode to use when this connection is opened implicitly
         * @param encoding The name of the encoding used to read from or to write to the connection.
         *
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString, AbstractOpenMode defaultModeForLazy, String encoding) throws IOException {
            this(conClass, modeString, defaultModeForLazy, true, encoding);
        }

        /**
         * The constructor for every connection class except {@link StdConnections}.
         *
         * @param conClass the specific class of the connection, e.g, {@link ConnectionClass#File}
         * @param modeString the mode in which the connection should be opened, "" for lazy opening
         * @param defaultModeForLazy the mode to use when this connection is opened implicitly
         *
         */
        protected BaseRConnection(ConnectionClass conClass, String modeString, AbstractOpenMode defaultModeForLazy) throws IOException {
            this(conClass, modeString, defaultModeForLazy, true, null);
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
            this.conClass = conClass;
            this.openMode = mode;
        }

        @Override
        public RType getRType() {
            return RType.Connection;
        }

        public ConnectionClass getConnectionClass() {
            return conClass;
        }

        public String getConnectionClassName() {
            return conClass.getPrintName();
        }

        /**
         * {@code gzfile} can open other connection classes, and this isn't known initially.
         */
        public final void updateConnectionClass(ConnectionClass newConClass) {
            this.conClass = newConClass;
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

        public boolean isBlocking() {
            return blocking;
        }

        /**
         * Returns the original encoding string.
         */
        public Charset getEncoding() {
            return encoding;
        }

        @Override
        public BaseRConnection forceOpen(String modeString) throws IOException {
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
            openMode = new OpenMode(modeString == null ? openMode.modeString : modeString);
            if (!isOpen()) {
                createDelegateConnection();
            }
        }

        protected void checkOpen() {
            assert !closed && opened;
        }

        protected void setDelegate(DelegateRConnection conn) {
            setDelegate(conn, true);
        }

        protected void setDelegate(DelegateRConnection conn, boolean opened) {
            setDelegate(conn, opened, null);
        }

        protected void setDelegate(DelegateRConnection conn, boolean opened, OpenMode mode) {
            this.theConnection = conn;
            this.opened = opened;
            if (mode != null) {
                this.openMode = mode;
            }
        }

        protected void setOpenMode(OpenMode mode) {
            assert mode != null;
            this.openMode = mode;
        }

        protected String[] readLinesInternal(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
            checkOpen();
            return theConnection.readLines(n, warn, skipNul);
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
        public ByteChannel getChannel() throws IOException {
            checkOpen();
            return theConnection;
        }

        @Override
        public void writeLines(RStringVector lines, String sep, boolean useBytes) throws IOException {
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

        public long seekInternal(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
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

        public void setCompressionType(@SuppressWarnings("unused") RCompression.Type cType) throws IOException {
            CompilerDirectives.transferToInterpreter();
            throw new IOException("Not yet implemented");
        }

        /**
         * Return the value that is used in the "description" field by {@code summary.connection}.
         */
        public abstract String getSummaryDescription();

        @Override
        public int getDescriptor() {
            return descriptor;
        }

        /**
         * Determines if the sources created by this connection are marked as internal.
         */
        public boolean isInternal() {
            return false;
        }

        public OpenMode getOpenMode() {
            return openMode;
        }

        public boolean isClosed() {
            return closed;
        }

        private String readOneLineWithPushBack(List<String> res, int ind) {
            String s = pushBack.pollLast();
            if (s == null) {
                return null;
            } else {
                String[] lines = TextConnections.splitLines(s, 2);
                if (lines.length == 2) {
                    // we hit end of the line
                    if (lines[1].length() != 0) {
                        // suffix is not empty and needs to be processed later
                        pushBack.push(lines[1]);
                    }
                    assert res.size() == ind;
                    res.add(ind, lines[0]);
                    return null;
                } else {
                    // no end of the line found yet
                    StringBuilder sb = new StringBuilder();
                    do {
                        assert lines.length == 1;
                        sb.append(lines[0]);
                        s = pushBack.pollLast();
                        if (s == null) {
                            break;
                        }

                        lines = TextConnections.splitLines(s, 2);
                        if (lines.length == 2) {
                            // we hit end of the line
                            if (lines[1].length() != 0) {
                                // suffix is not empty and needs to be processed later
                                pushBack.push(lines[1]);
                            }
                            assert res.size() == ind;
                            res.add(ind, sb.append(lines[0]).toString());
                            return null;
                        } // else continue
                    } while (true);
                    return sb.toString();
                }
            }
        }

        /**
         * TODO(fa) This method is subject for refactoring. I modified the original implementation
         * to be capable to handle a negative 'n' parameter. It indicates to read as much lines as
         * available.
         */
        @TruffleBoundary
        private String[] readLinesWithPushBack(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
            // NOTE: 'n' may be negative indicating to read as many lines as available
            final List<String> res;
            if (n >= 0) {
                res = new ArrayList<>(n);
            } else {
                res = new ArrayList<>();
            }

            for (int i = 0; i < n || n < 0; i++) {
                String s = readOneLineWithPushBack(res, i);
                final int remainingLineCount = n >= 0 ? n - i : n;
                if (s == null) {
                    if (i >= res.size() || res.get(i) == null) {
                        // no more push back value

                        String[] resInternal = readLinesInternal(remainingLineCount, warn, skipNul);
                        res.addAll(Arrays.asList(resInternal));
                        pushBack = null;
                        break;
                    }
                    // else res[i] has been filled - move to trying to fill the next one
                } else {
                    // reached the last push back value without reaching and of line
                    assert pushBack.size() == 0;
                    String[] resInternal = readLinesInternal(remainingLineCount, warn, skipNul);
                    res.addAll(i, Arrays.asList(resInternal));
                    if (res.get(i) != null) {
                        res.set(i, s + res.get(i));
                    } else {
                        res.set(i, s);
                    }
                    pushBack = null;
                    break;
                }
            }
            return res.toArray(new String[res.size()]);
        }

        @Override
        public String[] readLines(int n, EnumSet<ReadLineWarning> warn, boolean skipNul) throws IOException {
            if (pushBack == null) {
                return readLinesInternal(n, warn, skipNul);
            } else if (pushBack.size() == 0) {
                pushBack = null;
                return readLinesInternal(n, warn, skipNul);
            } else {
                return readLinesWithPushBack(n, warn, skipNul);
            }
        }

        /**
         * Pushes lines back to the connection.
         */
        @Override
        @TruffleBoundary
        public final void pushBack(RStringVector lines, boolean addNewLine) {
            if (pushBack == null) {
                pushBack = new LinkedList<>();
            }
            for (int i = 0; i < lines.getLength(); i++) {
                String newLine = lines.getDataAt(i);
                if (addNewLine) {
                    newLine = newLine + '\n';
                }
                pushBack.addFirst(newLine);
            }
        }

        /**
         * Return the length of the push back.
         */
        @TruffleBoundary
        public final int pushBackLength() {
            return pushBack == null ? 0 : pushBack.size();
        }

        /**
         * Clears the pushback.
         */
        @TruffleBoundary
        public final void pushBackClear() {
            pushBack = null;
        }

        /**
         * Support for {@code seek} Internal. Also clears push back lines.
         */
        @Override
        public long seek(long offset, SeekMode seekMode, SeekRWMode seekRWMode) throws IOException {
            if (isSeekable()) {
                // discard any push back strings
                pushBackClear();
            }
            // Do not throw error at this position, since the error messages varies depending on the
            // connection.
            return seekInternal(offset, seekMode, seekRWMode);
        }

        @Override
        public void truncate() throws IOException {
            checkOpen();
            if (!closed && opened) {
                theConnection.truncate();
            } else {
                throw RError.error(RError.SHOW_CALLER, RError.Message.TRUNCATE_ONLY_OPEN_CONN);
            }
        }

        /**
         * Returns {@code true} iff the last read operation was blocked or there is unflushed
         * output.
         */
        public boolean isIncomplete() {
            return incomplete;
        }

        protected void setIncomplete(boolean b) {
            this.incomplete = b;
        }

        public final RIntVector asVector() {
            String[] classes = new String[]{ConnectionSupport.getBaseConnection(this).getConnectionClassName(), "connection"};

            RIntVector result = RDataFactory.createIntVector(new int[]{getDescriptor()}, true);

            RStringVector classVector = RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR);
            // it's important to put "this" into the externalptr, so that it doesn't get collected
            RExternalPtr connectionId = RDataFactory.createExternalPtr(null, this, RDataFactory.createSymbol("connection"), RNull.instance);
            DynamicObject attrs = RAttributesLayout.createClassWithConnId(classVector, connectionId);
            result.initAttributes(attrs);
            return result;
        }
    }

    public static BaseRConnection getBaseConnection(RConnection conn) {
        if (conn instanceof BaseRConnection) {
            return (BaseRConnection) conn;
        } else if (conn instanceof DelegateReadRConnection) {
            return ((DelegateRConnection) conn).base;
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    abstract static class BasePathRConnection extends BaseRConnection {
        /** The actual file to open. */
        protected final TruffleFile path;

        /** The description used in the call (required summary output). */
        protected String description;

        protected BasePathRConnection(String description, TruffleFile path, ConnectionClass connectionClass, String modeString, boolean blocking, String encoding) throws IOException {
            this(description, path, connectionClass, modeString, AbstractOpenMode.Read, blocking, encoding);
        }

        protected BasePathRConnection(String description, TruffleFile path, ConnectionClass connectionClass, String modeString, AbstractOpenMode defaultLazyOpenMode, String encoding)
                        throws IOException {
            super(connectionClass, modeString, defaultLazyOpenMode, encoding);
            this.path = path;
            this.description = description;
        }

        protected BasePathRConnection(String description, TruffleFile path, ConnectionClass connectionClass, String modeString, AbstractOpenMode defaultLazyOpenMode, boolean blocking, String encoding)
                        throws IOException {
            super(connectionClass, modeString, defaultLazyOpenMode, blocking, encoding);
            this.path = path;
            this.description = description;
        }

        @Override
        public String getSummaryDescription() {
            // Use 'description' and not 'path' since this may be different, e.g., on temp files.
            return description;
        }
    }

    public static ByteChannel newChannel(InputStream in) {
        final ReadableByteChannel newChannel = Channels.newChannel(in);
        return new ByteChannel() {

            @Override
            public int read(ByteBuffer dst) throws IOException {
                return newChannel.read(dst);
            }

            @Override
            public boolean isOpen() {
                return newChannel.isOpen();
            }

            @Override
            public void close() throws IOException {
                newChannel.close();

            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new IOException("This channel is read-only.");
            }
        };
    }

    public static ByteChannel newChannel(OutputStream out) {
        final WritableByteChannel newChannel = Channels.newChannel(out);
        return new ByteChannel() {

            @Override
            public int read(ByteBuffer dst) throws IOException {
                throw new IOException("This channel is write-only.");
            }

            @Override
            public boolean isOpen() {
                return newChannel.isOpen();
            }

            @Override
            public void close() throws IOException {
                newChannel.close();

            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                return newChannel.write(src);
            }
        };
    }

    /**
     * Converts between character set names of iconv and Java.
     *
     * @param encoding The iconv name of the character set to use.
     * @return The Java name of the character set to use.
     */
    public static String convertEncodingName(String encoding) {
        if (encoding == null || encoding.isEmpty() || "native.enc".equals(encoding)) {
            return Charset.defaultCharset().name();
        }
        return encoding;
    }
}
