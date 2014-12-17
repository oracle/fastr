/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
    private static final class ModeException extends IOException {
        private static final long serialVersionUID = 1L;

        private ModeException() {
            // lame but it's what GnuR does
            super("invalid argument");
        }
    }

    private enum OpenMode {
        Lazy(new String[]{""}),
        Read(new String[]{"r", "rt"}),
        Write(new String[]{"w", "wt"}),
        Append(new String[]{"a", "at"}),
        ReadBinary(new String[]{"rb"}),
        WriteBinary(new String[]{"wb"}),
        AppendBinary(new String[]{"ab"}),
        ReadWrite(new String[]{"r+"}),
        ReadWriteBinary(new String[]{"r+b"}),
        ReadWriteTrunc(new String[]{"w+"}),
        ReadWriteTruncBinary(new String[]{"w+b"}),
        ReadAppend(new String[]{"a+"}),
        ReadAppendBinary(new String[]{"a+b"});

        private String[] modeStrings;

        OpenMode(String[] modeStrings) {
            this.modeStrings = modeStrings;
        }

        @TruffleBoundary
        static OpenMode getOpenMode(String modeString) throws IOException {
            for (OpenMode openMode : values()) {
                for (String ms : openMode.modeStrings) {
                    if (ms.equals(modeString)) {
                        return openMode;
                    }
                }
            }
            throw new ModeException();
        }
    }

    // TODO remove invisibility when print for RConnection works
    // TODO implement all open modes
    // TODO implement missing .Internal functions expected by connections.R
    // TODO revisit the use of InputStream for internal use, e.g. in RSerialize
    // TODO Use channels to support blocking/non-blockng mode

    /**
     * Base class for all {@link RConnection} instances. It supports lazy opening, as required by
     * the R spec, through the {@link #theConnection} field, which ultimately holds the actual
     * connection instance when opened. The default implementations of the {@link RConnection}
     * methods check whether the connection is open and if not, call
     * {@link #createDelegateConnection()} and then forward the operation. The result of
     * {@link #createDelegateConnection()} should be a subclass of {@link DelegateRConnection},
     * which subclasses {@link RConnection} directly. A subclass may choose not to use delegation by
     * overriding the default implementations of the methods in this class.
     *
     */
    private abstract static class BaseRConnection extends RConnection {
        private static final RStringVector DEFAULT_CLASSHR = RDataFactory.createStringVector(new String[]{"connection"}, RDataFactory.COMPLETE_VECTOR);

        /**
         * {@code true} is the connection has been opened successfully. N.B. This supports lazy
         * opening, in particular, {@code opened == false} does not mean {@code closed}.
         */
        protected boolean opened;

        /**
         * Returns {@code true} once the {@link #close()} method has been called.
         */
        protected boolean closed;

        /**
         * if {@link #opened} is {@code true} the {@link OpenMode} that this connection is opened
         * in, otherwise {@link OpenMode#Lazy}.
         */
        protected OpenMode mode;
        /**
         * If {@link #opened} is {@code false} and {@link OpenMode} is {@link OpenMode#Lazy}, the
         * mode the connection should eventually be opned in.
         */
        private OpenMode lazyMode;

        private RStringVector classHr;

        /**
         * The actual connection, if delegated.
         */
        private DelegateRConnection theConnection;

        protected BaseRConnection(String modeString) throws IOException {
            this(modeString, OpenMode.Read);
        }

        protected BaseRConnection(String modeString, OpenMode lazyMode) throws IOException {
            this(OpenMode.getOpenMode(modeString), lazyMode);
        }

        protected BaseRConnection(OpenMode mode, OpenMode lazyMode) {
            this.mode = mode;
            this.lazyMode = lazyMode;
        }

        protected void checkOpen() throws IOException {
            if (closed) {
                throw new IOException(RError.Message.INVALID_CONNECTION.message);
            }
            if (!opened) {
                // closed or lazy
                if (mode == OpenMode.Lazy) {
                    mode = lazyMode;
                    createDelegateConnection();
                }
            }
        }

        protected void setDelegate(DelegateRConnection conn, String rClass) {
            this.theConnection = conn;
            setClass(rClass);
            opened = true;
        }

        protected void setClass(String rClass) {
            String[] classes = new String[2];
            classes[0] = rClass;
            classes[1] = "connection";
            this.classHr = RDataFactory.createStringVector(classes, RDataFactory.COMPLETE_VECTOR);
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
        }

        @Override
        /**
         * Must always respond to {@code inherits("connection")} even when not open.
         */
        public RStringVector getClassHierarchy() {
            return classHr != null ? classHr : DEFAULT_CLASSHR;
        }

        /**
         * Subclass-specific creation of the {@link DelegateRConnection} using {@link #mode}. To
         * support both lazy and non-lazy creation, the implementation of this method must
         * explicitly call {@link #setDelegate(DelegateRConnection, String)} if it successfully
         * creates the delegate connection.
         */
        protected abstract void createDelegateConnection() throws IOException;
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

    private static String[] readLinesHelper(BufferedReader bufferedReader, int n) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
            if (n > 0 && lines.size() == n) {
                break;
            }
        }
        String[] result = new String[lines.size()];
        lines.toArray(result);
        return result;
    }

    private static void writeLinesHelper(BufferedWriter bufferedWriter, RAbstractStringVector lines, String sep) throws IOException {
        for (int i = 0; i < lines.getLength(); i++) {
            bufferedWriter.write(lines.getDataAt(i));
            bufferedWriter.write(sep);
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
        public void flush() {
            // nothing to do
        }

        @Override
        public OutputStream getOutputStream() {
            throw RInternalError.shouldNotReachHere();
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
        public InputStream getInputStream() {
            throw RInternalError.shouldNotReachHere();
        }

    }

    /**
     * A special case that does not use delegation as the connection is always open.
     */
    private static class StdinConnection extends BaseRConnection {

        StdinConnection() {
            super(OpenMode.Read, null);
            this.opened = true;
            setClass("terminal");
        }

        @Override
        public boolean isStdin() {
            return true;
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

    }

    private static StdinConnection stdin;

    @RBuiltin(name = "stdin", kind = INTERNAL, parameterNames = {})
    public abstract static class Stdin extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdin() {
            controlVisibility();
            if (stdin == null) {
                stdin = new StdinConnection();
            }
            return stdin;
        }
    }

    /**
     * Base class for all modes of file connections.
     *
     */
    private static class FileRConnection extends BaseRConnection {
        protected final String path;

        protected FileRConnection(String path, String modeString) throws IOException {
            super(modeString);
            this.path = path;
            if (mode != OpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (mode) {
                case Read:
                    delegate = new FileReadTextRConnection(this);
                    break;
                case Write:
                    delegate = new FileWriteTextRConnection(this);
                    break;
                case ReadBinary:
                    delegate = new FileReadBinaryRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + mode);
            }
            setDelegate(delegate, "file");
        }
    }

    private static class FileReadTextRConnection extends DelegateReadRConnection {
        private BufferedInputStream inputStream;
        private BufferedReader bufferedReader;

        FileReadTextRConnection(FileRConnection base) throws IOException {
            super(base);
            inputStream = new BufferedInputStream(new FileInputStream(base.path));
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(bufferedReader, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            bufferedReader.close();
            base.closed = true;

        }
    }

    private static class FileWriteTextRConnection extends DelegateWriteRConnection {
        private BufferedOutputStream outputStream;
        private BufferedWriter bufferedWriter;

        FileWriteTextRConnection(FileRConnection base) throws IOException {
            super(base);
            outputStream = new BufferedOutputStream(new FileOutputStream(base.path));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            writeLinesHelper(bufferedWriter, lines, sep);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public void close() throws IOException {
            bufferedWriter.close();
            base.closed = true;
        }

        @Override
        public void flush() throws IOException {
            bufferedWriter.flush();
        }
    }

    private static class FileReadBinaryRConnection extends DelegateReadRConnection {
        private BufferedInputStream inputStream;
        private InputStreamReader inputReader;

        FileReadBinaryRConnection(FileRConnection base) throws IOException {
            super(base);
            inputStream = new BufferedInputStream(new FileInputStream(base.path));
            inputReader = new InputStreamReader(inputStream, "US-ASCII");
        }

        @TruffleBoundary
        @Override
        public String[] readLinesInternal(int n) throws IOException {
            throw new IOException("TODO");
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            inputReader.close();
            base.closed = true;
        }
    }

    @RBuiltin(name = "file", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding", "raw"})
    public abstract static class File extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object file(RAbstractStringVector description, RAbstractStringVector open, byte blocking, RAbstractStringVector encoding, byte raw) {
            // temporarily return invisible to avoid missing print support
            controlVisibility();
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
    private static class GZIPRConnection extends BaseRConnection {
        protected final String path;

        GZIPRConnection(String path, String modeString) throws IOException {
            super(modeString, OpenMode.ReadBinary);
            this.path = Utils.tildeExpand(path);
            if (mode != OpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (mode) {
                case ReadBinary:
                    delegate = new GZIPInputRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + mode);
            }
            setDelegate(delegate, "gzfile");
        }
    }

    private static class GZIPInputRConnection extends DelegateReadRConnection {
        private GZIPInputStream stream;

        GZIPInputRConnection(GZIPRConnection base) throws IOException {
            super(base);
            stream = new GZIPInputStream(new FileInputStream(base.path), RConnection.GZIP_BUFFER_SIZE);
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            throw new IOException("TODO");
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return stream;
        }

        @Override
        public void close() throws IOException {
            stream.close();
            base.closed = true;
        }

    }

    @RBuiltin(name = "gzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"})
    public abstract static class GZFile extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object gzFile(RAbstractStringVector description, RAbstractStringVector open, RAbstractStringVector encoding, double compression) {
            // temporarily return invisible to avoid missing print support
            controlVisibility();
            try {
                return new GZIPRConnection(description.getDataAt(0), open.getDataAt(0));
            } catch (IOException ex) {
                RError.warning(RError.Message.CANNOT_OPEN_FILE, description.getDataAt(0), ex.getMessage());
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    /**
     * Base class for all modes of gzfile connections.
     */
    private static class TextRConnection extends BaseRConnection {
        @SuppressWarnings("unused") protected String nm;
        protected RAbstractStringVector object;
        @SuppressWarnings("unused") protected REnvironment env;

        protected TextRConnection(String nm, RAbstractStringVector object, REnvironment env, String modeString) throws IOException {
            super(modeString);
            this.nm = nm;
            this.object = object;
            this.env = env;
            if (mode != OpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (mode) {
                case Read:
                    delegate = new TextReadRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + mode);
            }
            setDelegate(delegate, "textConnection");
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
    }

    @RBuiltin(name = "textConnection", kind = INTERNAL, parameterNames = {"nm", "object", "open", "env", "type"})
    public abstract static class TextConnection extends RInvisibleBuiltinNode {
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
     * Base class for socket connections, which are not opened lazily.
     */
    private static class RSocketConnection extends BaseRConnection {
        protected Socket socket;
        protected InputStream inputStream;
        protected OutputStream outputStream;
        protected BufferedReader bufferedReader;
        protected BufferedWriter bufferedWriter;

        protected RSocketConnection(String modeString, @SuppressWarnings("unused") int port) throws IOException {
            super(modeString);
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            throw RInternalError.shouldNotReachHere();
        }

        protected void openStreams() throws IOException {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            checkOpen();
            return readLinesHelper(bufferedReader, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            checkOpen();
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            checkOpen();
            return outputStream;
        }

        @Override
        public void writeLines(RAbstractStringVector lines, String sep) throws IOException {
            checkOpen();
            writeLinesHelper(bufferedWriter, lines, sep);
            flush();
        }

        @Override
        public void flush() throws IOException {
            bufferedWriter.flush();
        }

        @Override
        public void close() throws IOException {
            bufferedReader.close();
            bufferedWriter.close();
            socket.close();
            closed = true;
        }
    }

    private static class RServerSocketConnection extends RSocketConnection {
        private ServerSocket serverSocket;

        RServerSocketConnection(String modeString, int port) throws IOException {
            super(modeString, port);
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            openStreams();
            opened = true;
        }
    }

    private static class RClientSocketConnection extends RSocketConnection {
        RClientSocketConnection(String modeString, String host, int port) throws IOException {
            super(modeString, port);
            socket = new Socket(host, port);
            openStreams();
            opened = true;
        }
    }

    @RBuiltin(name = "socketConnection", kind = INTERNAL, parameterNames = {"host", "port", "server", "blocking", "open", "encoding", "timeout"})
    public abstract static class SocketConnection extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object socketConnection(RAbstractStringVector host, double port, byte server, byte blocking, RAbstractStringVector open, RAbstractStringVector encoding, RAbstractIntVector timeout) {
            return socketConnection(host, (int) port, server, blocking, open, encoding, timeout);
        }

        @SuppressWarnings("unused")
        protected Object socketConnection(RAbstractStringVector host, int port, byte server, byte blocking, RAbstractStringVector open, RAbstractStringVector encoding, RAbstractIntVector timeout) {
            controlVisibility();
            try {
                if (RRuntime.fromLogical(server)) {
                    return new RServerSocketConnection(open.getDataAt(0), port);
                } else {
                    return new RClientSocketConnection(open.getDataAt(0), host.getDataAt(0), port);
                }
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    private static class URLRConnection extends BaseRConnection {
        protected final String urlString;

        protected URLRConnection(String url, String modeString) throws IOException {
            super(modeString);
            this.urlString = url;
            if (mode != OpenMode.Lazy) {
                createDelegateConnection();
            }
        }

        @Override
        protected void createDelegateConnection() throws IOException {
            DelegateRConnection delegate = null;
            switch (mode) {
                case Read:
                    delegate = new URLReadRConnection(this);
                    break;
                default:
                    throw RError.nyi((SourceSection) null, "unimplemented open mode: " + mode);
            }
            setDelegate(delegate, "url");
        }
    }

    private static class URLReadRConnection extends DelegateReadRConnection {

        private InputStream inputStream;
        private BufferedReader bufferedReader;

        protected URLReadRConnection(URLRConnection base) throws MalformedURLException, IOException {
            super(base);
            URL url = new URL(base.urlString);
            inputStream = url.openStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public String[] readLinesInternal(int n) throws IOException {
            return readLinesHelper(bufferedReader, n);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            bufferedReader.close();
            base.closed = true;
        }

    }

    @RBuiltin(name = "url", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding"})
    public abstract static class URLConnection extends RInvisibleBuiltinNode {
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

    private abstract static class CheckIsConnAdapter extends RInvisibleBuiltinNode {
        protected void checkIsConnection(Object con) throws RError {
            if (!(con instanceof RConnection)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_CONNECTION, "con");
            }
        }
    }

    @RBuiltin(name = "open", kind = INTERNAL, parameterNames = {"con", "open", "blocking"})
    public abstract static class Open extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object open(RConnection con, RAbstractStringVector open, @SuppressWarnings("unused") byte blocking) {
            controlVisibility();
            try {
                BaseRConnection baseConn = getBaseConnection(con);
                if (baseConn.closed) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_CONNECTION);
                }
                if (baseConn.opened) {
                    RContext.getInstance().setEvalWarning(RError.Message.ALREADY_OPEN_CONNECTION.message);
                    return RNull.instance;
                }
                baseConn.mode = OpenMode.getOpenMode(open.getDataAt(0));
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

    @RBuiltin(name = "close", kind = INTERNAL, parameterNames = {"con", "type"})
    public abstract static class Close extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object close(RConnection con) {
            controlVisibility();
            try {
                con.close();
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        protected Object open(Object con) {
            checkIsConnection(con);
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE);
        }
    }

    @RBuiltin(name = "readLines", kind = INTERNAL, parameterNames = {"con", "n", "ok", "warn", "encoding", "skipNul"})
    public abstract static class ReadLines extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object readLines(RConnection con, int n, byte ok, @SuppressWarnings("unused") byte warn, @SuppressWarnings("unused") String encoding, @SuppressWarnings("unused") byte skipNul) {
            controlVisibility();
            try {
                String[] lines = con.readLines(n);
                if (n > 0 && lines.length < n && ok == RRuntime.LOGICAL_FALSE) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_FEW_LINES_READ_LINES);
                }
                return RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
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
    public abstract static class WriteLines extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull writeLines(RAbstractStringVector text, RConnection con, RAbstractStringVector sep, @SuppressWarnings("unused") byte useBytes) {
            controlVisibility();
            try {
                con.writeLines(text, sep.getDataAt(0));
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            }
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
    public abstract static class Flush extends RInvisibleBuiltinNode {
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
    public abstract static class PushBack extends RInvisibleBuiltinNode {

        @CreateCast("arguments")
        protected RNode[] castTimesLength(RNode[] arguments) {
            arguments[0] = CastToVectorNodeFactory.create(CastStringNodeFactory.create(arguments[0], false, false, false, false), false, false, false, false);
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
    public abstract static class PushBackClear extends RInvisibleBuiltinNode {

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
    public abstract static class ReadChar extends RBuiltinNode {

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
            return RDataFactory.createEmptyStringVector();
        }

        @Specialization(guards = {"!ncharsEmpty", "!useBytesEmpty"})
        protected RStringVector readChar(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            controlVisibility();
            try {
                return con.readChar(nchars, useBytes.getDataAt(0) == RRuntime.LOGICAL_TRUE);
            } catch (IOException x) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ERROR_READING_CONNECTION, x.getMessage());
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
}
