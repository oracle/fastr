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
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Basic support classes and methods for the connection implementations.
 */
public class ConnectionSupport {
    /**
     * Records all connections. The index in the array is the "descriptor" used in
     * {@code getConnection}. Defined at this level so that {code stdin} etc. are initialized before
     * any attempt to return them via, e.g., {@code getConnection}.
     */
    static BaseRConnection[] allConnections = new BaseRConnection[127];

    public static BaseRConnection[] getAllConnections() {
        return allConnections;
    }

    static void setStdConnection(int index, BaseRConnection conn) {
        assert index <= 2;
        allConnections[index] = conn;
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
    public static class OpenMode {
        public final AbstractOpenMode abstractOpenMode;
        public final String modeString;

        public OpenMode(String modeString) throws IOException {
            this(modeString, AbstractOpenMode.getOpenMode(modeString));
        }

        OpenMode(String modeString, AbstractOpenMode mode) {
            this.modeString = modeString;
            this.abstractOpenMode = mode;
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
    }

    enum ConnectionClass {
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
         * if {@link #opened} is {@code true} the {@link OpenMode} that this connection is opened
         * in, otherwise {@link AbstractOpenMode#Lazy}.
         */
        private OpenMode openMode;

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
            this(conClass, new OpenMode(modeString), false);
        }

        /**
         * Primitive constructor that just assigns state. Used by {@link StdConnections} as they are
         * a special case, but should not be used by other connection types.
         */
        protected BaseRConnection(ConnectionClass conClass, OpenMode mode, boolean std) {
            this.openMode = mode;
            String[] classes = new String[2];
            classes[0] = conClass.printName;
            classes[1] = "connection";
            this.classHr = RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR);
            if (!std) {
                registerConnection();
            }
        }

        protected void openNonLazyConnection() throws IOException {
            if (openMode.abstractOpenMode != AbstractOpenMode.Lazy) {
                createDelegateConnection();
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
            if (isActive()) {
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
                if (getOpenMode().abstractOpenMode == AbstractOpenMode.Lazy) {
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
            return getClassHr();
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
         * Return the value that is used in the "text" field by {@code summary.connection}. TODO
         * determine what this really means, e.g., In GnuR {gzfile} on a binary file still reports
         * "text".
         */
        public String getSummaryText() {
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

        public OpenMode getOpenMode() {
            return openMode;
        }

        public RStringVector getClassHr() {
            return classHr;
        }

        public boolean isOpen() {
            return opened;
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

    /**
     * {@code readLines} from an {@link InputStream}. It would be convenient to use a
     * {@link BufferedReader} but mixing binary and text operations, which is a requirement, would
     * then be difficult.
     */
    static String[] readLinesHelper(InputStream in, int n) throws IOException {
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

    static void writeLinesHelper(OutputStream out, RAbstractStringVector lines, String sep) throws IOException {
        for (int i = 0; i < lines.getLength(); i++) {
            out.write(lines.getDataAt(i).getBytes());
            out.write(sep.getBytes());
        }
    }

    static void writeCharHelper(OutputStream out, String s, int pad, String eos) throws IOException {
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

    static void writeBinHelper(ByteBuffer buffer, OutputStream outputStream) throws IOException {
        int n = buffer.remaining();
        byte[] b = new byte[n];
        buffer.get(b);
        outputStream.write(b);
    }

    /**
     * Reads null-terminated character strings from an {@link InputStream}.
     */
    static byte[] readBinCharsHelper(InputStream in) throws IOException {
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

    static int readBinHelper(ByteBuffer buffer, InputStream inputStream) throws IOException {
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

    static String readCharHelper(int nchars, InputStream in, @SuppressWarnings("unused") boolean useBytes) throws IOException {
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

    abstract static class DelegateRConnection extends RConnection {
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

    abstract static class DelegateReadRConnection extends DelegateRConnection {
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

    abstract static class DelegateWriteRConnection extends DelegateRConnection {
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

    }

    abstract static class BasePathRConnection extends BaseRConnection {
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

}
