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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.trueValue;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.getBaseConnection;
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.removeFileURLPrefix;
import static com.oracle.truffle.r.runtime.conn.StdConnections.getStderr;
import static com.oracle.truffle.r.runtime.conn.StdConnections.getStdin;
import static com.oracle.truffle.r.runtime.conn.StdConnections.getStdout;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.zip.ZipException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.ConnectionFunctionsFactory.WriteDataNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.FileConnections.FileRConnection;
import com.oracle.truffle.r.runtime.conn.GZIPConnections.GZIPRConnection;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.SocketConnections.RSocketConnection;
import com.oracle.truffle.r.runtime.conn.TextConnections.TextRConnection;
import com.oracle.truffle.r.runtime.conn.URLConnections.URLRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The builtins for connections.
 *
 * The implementations of {@link RConnection} corresponding to the builtins are in
 * {@code com.oracle.truffle.r.runime.conn}.
 *
 */
public abstract class ConnectionFunctions {
    @RBuiltin(name = "stdin", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Stdin extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdin() {
            return getStdin();
        }
    }

    @RBuiltin(name = "stdout", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Stdout extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdout() {
            return getStdout();
        }
    }

    @RBuiltin(name = "stderr", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Stderr extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stderr() {
            return getStderr();
        }
    }

    @RBuiltin(name = "file", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding", "raw"}, behavior = IO)
    public abstract static class File extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("description").mustBe(stringValue()).asStringVector().shouldBe(singleElement(), RError.Message.ARGUMENT_ONLY_FIRST_1, "description").findFirst().notNA();

            casts.arg("open").mustBe(stringValue()).asStringVector().findFirst().notNA();

            casts.arg("blocking").asLogicalVector().findFirst().map(toBoolean()).mustBe(trueValue(), RError.Message.NYI, "non-blocking mode not supported");

            casts.arg("encoding").asStringVector().findFirst();

            casts.arg("raw").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object file(String description, String openArg, boolean blocking, String encoding, boolean raw) {
            String open = openArg;
            // TODO handle http/ftp prefixes and redirect
            String path = removeFileURLPrefix(description);
            if (path.length() == 0) {
                // special case, temp file opened in "w+" or "w+b" only
                if (open.length() == 0) {
                    open = "w+";
                } else {
                    if (!(open.equals("w+") || open.equals("w+b"))) {
                        open = "w+";
                        RError.warning(this, RError.Message.FILE_OPEN_TMP);
                    }
                }
            }
            try {
                return new FileRConnection(path, open);
            } catch (IOException ex) {
                RError.warning(this, RError.Message.CANNOT_OPEN_FILE, description, ex.getMessage());
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object file(Object description, Object open, Object blocking, Object encoding, Object raw) {
            throw RError.error(this, RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    /**
     * {@code gzfile} is very versatile (unfortunately); it can open uncompressed files, and files
     * compressed by {@code bzip2, xz, lzma}. Currently we only support {@code gzip} and
     * uncompressed.
     */
    @RBuiltin(name = "gzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"}, behavior = IO)
    public abstract static class GZFile extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected Object gzFile(RAbstractStringVector description, RAbstractStringVector open, RAbstractStringVector encoding, double compression) {
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
            RError.warning(this, RError.Message.CANNOT_OPEN_FILE, path, ex.getMessage());
            throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
        }
    }

    @RBuiltin(name = "textConnection", kind = INTERNAL, parameterNames = {"nm", "object", "open", "env", "type"}, behavior = IO)
    public abstract static class TextConnection extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object textConnection(RAbstractStringVector nm, RAbstractStringVector object, RAbstractStringVector open, REnvironment env, @SuppressWarnings("unused") RAbstractIntVector encoding) {
            if (nm.getLength() != 1) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "description");
            }
            // TODO more error checking as per GnuR
            try {
                return new TextRConnection(nm.getDataAt(0), object, env, open.getDataAt(0));
            } catch (IOException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object textConnection(Object nm, Object object, Object open, Object env, Object encoding) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "textConnectionValue", kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class TextConnectionValue extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object textConnection(RConnection conn) {
            if (conn instanceof TextRConnection) {
                return RDataFactory.createStringVector(((TextRConnection) conn).getValue(), RDataFactory.COMPLETE_VECTOR);
            } else {
                throw RError.error(this, RError.Message.NOT_A_TEXT_CONNECTION);
            }
        }
    }

    @RBuiltin(name = "socketConnection", kind = INTERNAL, parameterNames = {"host", "port", "server", "blocking", "open", "encoding", "timeout"}, behavior = IO)
    public abstract static class SocketConnection extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(1);
            casts.toInteger(6);
        }

        @Specialization
        @TruffleBoundary
        protected Object socketConnection(RAbstractStringVector host, RAbstractIntVector portVec, byte server, byte blocking, RAbstractStringVector open,
                        @SuppressWarnings("unused") RAbstractStringVector encoding, RAbstractIntVector timeoutVec) {
            int port = portVec.getDataAt(0);
            String modeString = open.getDataAt(0);
            int timeout = timeoutVec.getDataAt(0);
            try {
                if (RRuntime.fromLogical(server)) {
                    return new RSocketConnection(modeString, true, host.getDataAt(0), port, RRuntime.fromLogical(blocking), timeout);
                } else {
                    return new RSocketConnection(modeString, false, host.getDataAt(0), port, RRuntime.fromLogical(blocking), timeout);
                }
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    @RBuiltin(name = "url", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding"}, behavior = IO)
    public abstract static class URLConnection extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object urlConnection(RAbstractStringVector url, RAbstractStringVector open, @SuppressWarnings("unused") byte blocking, @SuppressWarnings("unused") RAbstractStringVector encoding) {
            try {
                return new URLRConnection(url.getDataAt(0), open.getDataAt(0));
            } catch (MalformedURLException ex) {
                throw RError.error(this, RError.Message.UNSUPPORTED_URL_SCHEME);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    private abstract static class CheckIsConnAdapter extends RBuiltinNode {
        protected RConnection checkIsConnection(Object con) throws RError {
            if (!(con instanceof RConnection)) {
                throw RError.error(this, RError.Message.NOT_CONNECTION, "con");
            } else {
                return (RConnection) con;
            }
        }
    }

    @RBuiltin(name = "summary.connection", kind = INTERNAL, parameterNames = {"object}"}, behavior = IO)
    public abstract static class Summary extends CheckIsConnAdapter {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"description", "class", "mode", "text", "opened", "can read", "can write"},
                        RDataFactory.COMPLETE_VECTOR);

        @Specialization
        @TruffleBoundary
        protected RList summary(Object object) {
            BaseRConnection baseCon;
            if (object instanceof Integer) {
                baseCon = RContext.getInstance().stateRConnection.getConnection((int) object);
            } else if (object instanceof Double) {
                baseCon = RContext.getInstance().stateRConnection.getConnection((int) Math.floor((Double) object));
            } else {
                RConnection con = checkIsConnection(object);
                baseCon = getBaseConnection(con);
            }
            Object[] data = new Object[NAMES.getLength()];
            data[0] = baseCon.getSummaryDescription();
            data[1] = baseCon.getClassHierarchy().getDataAt(0);
            data[2] = baseCon.getOpenMode().summaryString();
            data[3] = baseCon.getSummaryText();
            data[4] = baseCon.isOpen() ? "opened" : "closed";
            data[5] = baseCon.canRead() ? "yes" : "no";
            data[6] = baseCon.canWrite() ? "yes" : "no";
            return RDataFactory.createList(data, NAMES);
        }
    }

    @RBuiltin(name = "open", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "open", "blocking"}, behavior = IO)
    public abstract static class Open extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object open(RConnection con, RAbstractStringVector open, @SuppressWarnings("unused") byte blocking) {
            RContext.getInstance().setVisible(false);
            try {
                BaseRConnection baseConn = getBaseConnection(con);
                if (baseConn.isClosed()) {
                    throw RError.error(this, RError.Message.INVALID_CONNECTION);
                }
                if (baseConn.isOpen()) {
                    RError.warning(this, RError.Message.ALREADY_OPEN_CONNECTION);
                    return RNull.instance;
                }
                baseConn.open(open.getDataAt(0));
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object open(Object con, Object open, Object blocking) {
            checkIsConnection(con);
            throw RError.error(this, RError.Message.INVALID_ARG_TYPE);
        }
    }

    @RBuiltin(name = "isOpen", kind = INTERNAL, parameterNames = {"con", "rw"}, behavior = IO)
    public abstract static class IsOpen extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected RLogicalVector isOpen(RConnection con, RAbstractIntVector rw) {
            BaseRConnection baseCon = getBaseConnection(con);
            boolean result = baseCon.isOpen();
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
                    throw RError.error(this, RError.Message.UNKNOWN_VALUE, "rw");
            }
            return RDataFactory.createLogicalVectorFromScalar(result);
        }

        @Fallback
        @TruffleBoundary
        protected Object isOpen(Object con, @SuppressWarnings("unused") Object rw) {
            checkIsConnection(con);
            throw RError.error(this, RError.Message.INVALID_ARG_TYPE);
        }
    }

    @RBuiltin(name = "close", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "type"}, behavior = IO)
    public abstract static class Close extends CheckIsConnAdapter {
        @Specialization
        @TruffleBoundary
        protected Object close(RConnection con) {
            RContext.getInstance().setVisible(false);
            try {
                con.closeAndDestroy();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

        @Fallback
        @TruffleBoundary
        protected Object close(Object con) {
            checkIsConnection(con);
            throw RError.error(this, RError.Message.INVALID_ARG_TYPE);
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
                baseConn.close();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }
    }

    @RBuiltin(name = "readLines", kind = INTERNAL, parameterNames = {"con", "n", "ok", "warn", "encoding", "skipNul"}, behavior = IO)
    public abstract static class ReadLines extends InternalCloseHelper {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toLogical(2);
            casts.toLogical(3);
            casts.toLogical(5);
        }

        @Specialization
        @TruffleBoundary
        protected Object readLines(RConnection con, int n, byte ok, byte warn, @SuppressWarnings("unused") String encoding, byte skipNul) {
            // TODO implement all the arguments
            try (RConnection openConn = con.forceOpen("rt")) {
                String[] lines = openConn.readLines(n, RRuntime.fromLogical(warn), RRuntime.fromLogical(skipNul));
                if (n > 0 && lines.length < n && ok == RRuntime.LOGICAL_FALSE) {
                    throw RError.error(this, RError.Message.TOO_FEW_LINES_READ_LINES);
                }
                return RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
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
            throw RError.error(this, RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "writeLines", visibility = OFF, kind = INTERNAL, parameterNames = {"text", "con", "sep", "useBytes"}, behavior = IO)
    public abstract static class WriteLines extends InternalCloseHelper {
        @Specialization
        @TruffleBoundary
        protected RNull writeLines(RAbstractStringVector text, RConnection con, RAbstractStringVector sep, byte useBytes) {
            try (RConnection openConn = con.forceOpen("wt")) {
                openConn.writeLines(text, sep.getDataAt(0), RRuntime.fromLogical(useBytes));
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            }
            RContext.getInstance().setVisible(false);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected RNull writeLines(Object text, Object con, Object sep, Object useBytes) {
            throw RError.error(this, RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "flush", visibility = OFF, kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class Flush extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull flush(RConnection con) {
            try {
                con.flush();
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_FLUSHING_CONNECTION, x.getMessage());
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "pushBack", visibility = OFF, kind = INTERNAL, parameterNames = {"data", "connection", "newLine", "type"}, behavior = IO)
    public abstract static class PushBack extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toCharacter(0);
        }

        @Specialization
        @TruffleBoundary
        protected RNull pushBack(RAbstractStringVector data, RConnection connection, RAbstractLogicalVector newLine, @SuppressWarnings("unused") RAbstractIntVector type) {
            if (newLine.getLength() == 0) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "newLine");
            }
            connection.pushBack(data, newLine.getDataAt(0) == RRuntime.LOGICAL_TRUE);
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected Object pushBack(RAbstractStringVector data, RConnection connection, RNull newLine, RAbstractIntVector type) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "newLine");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!newLineIsLogical(newLine)")
        @TruffleBoundary
        protected Object pushBack(RAbstractStringVector data, RConnection connection, RAbstractVector newLine, RAbstractIntVector type) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "newLine");
        }

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object pushBack(Object data, Object connection, Object newLine, Object encoding) {
            throw RError.error(this, RError.Message.INVALID_CONNECTION);
        }

        protected boolean newLineIsLogical(RAbstractVector newLine) {
            return newLine.getElementClass() == RLogical.class;
        }
    }

    @RBuiltin(name = "pushBackLength", kind = INTERNAL, parameterNames = {"connection"}, behavior = IO)
    public abstract static class PushBackLength extends RBuiltinNode {

        @Specialization
        protected int pushBackLength(RConnection connection) {
            return connection.pushBackLength();
        }

        @Fallback
        protected Object pushBacklLength(@SuppressWarnings("unused") Object connection) {
            throw RError.error(this, RError.Message.INVALID_CONNECTION);
        }
    }

    @RBuiltin(name = "clearPushBack", visibility = OFF, kind = INTERNAL, parameterNames = {"connection"}, behavior = IO)
    public abstract static class PushBackClear extends RBuiltinNode {

        @Specialization
        protected RNull pushBackClear(RConnection connection) {
            connection.pushBackClear();
            return RNull.instance;
        }

        @Fallback
        protected Object pushBackClear(@SuppressWarnings("unused") Object connection) {
            throw RError.error(this, RError.Message.INVALID_CONNECTION);
        }
    }

    @RBuiltin(name = "readChar", kind = INTERNAL, parameterNames = {"con", "nchars", "useBytes"}, behavior = IO)
    public abstract static class ReadChar extends InternalCloseHelper {

        @SuppressWarnings("unused")
        @Specialization(guards = "ncharsEmpty(nchars)")
        protected RStringVector readCharNcharsEmpty(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            return RDataFactory.createEmptyStringVector();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "useBytesEmpty(useBytes)")
        protected RStringVector readCharUseBytesEmpty(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "useBytes");
        }

        @Specialization(guards = {"!ncharsEmpty(nchars)", "!useBytesEmpty(useBytes)"})
        @TruffleBoundary
        protected RStringVector readChar(RConnection con, RAbstractIntVector nchars, RAbstractLogicalVector useBytes) {
            try (RConnection openConn = con.forceOpen("rb")) {
                String[] data = new String[nchars.getLength()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = openConn.readChar(nchars.getDataAt(i), useBytes.getDataAt(0) == RRuntime.LOGICAL_TRUE);
                }
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            }
        }

        boolean ncharsEmpty(RAbstractIntVector nchars) {
            return nchars.getLength() == 0;
        }

        boolean useBytesEmpty(RAbstractLogicalVector useBytes) {
            return useBytes.getLength() == 0;
        }
    }

    @RBuiltin(name = "writeChar", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"object", "con", "nchars", "eos", "useBytes"}, behavior = IO)
    public abstract static class WriteChar extends InternalCloseHelper {
        @TruffleBoundary
        @Specialization
        protected RNull writeChar(RAbstractStringVector object, RConnection con, RAbstractIntVector nchars, RAbstractStringVector eos, byte useBytes) {
            try (RConnection openConn = con.forceOpen("wb")) {
                int length = object.getLength();
                for (int i = 0; i < length; i++) {
                    String s = object.getDataAt(i);
                    int nc = nchars.getDataAt(i % length);
                    int pad = nc - s.length();
                    if (pad > 0) {
                        RError.warning(this, RError.Message.MORE_CHARACTERS);
                    }
                    openConn.writeChar(s, pad, eos.getDataAt(i % length), RRuntime.fromLogical(useBytes));
                }
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            }
            RContext.getInstance().setVisible(false);
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

    @RBuiltin(name = "readBin", kind = INTERNAL, parameterNames = {"con", "what", "n", "size", "signed", "swap"}, behavior = IO)
    public abstract static class ReadBin extends InternalCloseHelper {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(2);
        }

        @Specialization
        @TruffleBoundary
        protected Object readBin(RConnection con, RAbstractStringVector whatVec, RAbstractIntVector nVec, @SuppressWarnings("unused") int size, @SuppressWarnings("unused") byte signedArg,
                        byte swapArg) {
            boolean swap = RRuntime.fromLogical(swapArg);
            RVector result = null;
            int n = nVec.getDataAt(0);
            try (RConnection openConn = con.forceOpen("rb")) {
                if (getBaseConnection(openConn).getOpenMode().isText()) {
                    throw RError.error(this, RError.Message.ONLY_READ_BINARY_CONNECTION);
                }
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
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
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

    @TypeSystemReference(RTypes.class)
    public abstract static class WriteDataNode extends RBaseNode {

        public abstract ByteBuffer execute(Object value, int size, boolean swap, boolean useBytes);

        public static WriteDataNode create() {
            return WriteDataNodeGen.create();
        }

        private static ByteBuffer allocate(int capacity, boolean swap) {
            ByteBuffer buffer = ByteBuffer.allocate(capacity);
            checkOrder(buffer, swap);
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeInteger(RAbstractIntVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            int length = object.getLength();
            ByteBuffer buffer = allocate(4 * length, swap);
            for (int i = 0; i < length; i++) {
                int value = object.getDataAt(i);
                buffer.putInt(value);
            }
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeDouble(RAbstractDoubleVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            int length = object.getLength();
            ByteBuffer buffer = allocate(8 * length, swap);
            for (int i = 0; i < length; i++) {
                double value = object.getDataAt(i);
                buffer.putDouble(value);
            }
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeComplex(RAbstractComplexVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            int length = object.getLength();
            ByteBuffer buffer = allocate(16 * length, swap);
            for (int i = 0; i < length; i++) {
                RComplex complex = object.getDataAt(i);
                double re = complex.getRealPart();
                double im = complex.getImaginaryPart();
                buffer.putDouble(re);
                buffer.putDouble(im);
            }
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeString(RAbstractStringVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            int length = object.getLength();
            byte[][] data = new byte[length][];
            int totalLength = 0;
            for (int i = 0; i < length; i++) {
                String s = object.getDataAt(i);
                // There is no special encoding for NA_character_
                data[i] = s.getBytes();
                totalLength = totalLength + data[i].length + 1; // zero pad
            }

            ByteBuffer buffer = allocate(totalLength, swap);
            for (int i = 0; i < length; i++) {
                buffer.put(data[i]);
                buffer.put((byte) 0);
            }
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeLogical(RAbstractLogicalVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            // encoded as ints, with FALSE=0, TRUE=1, NA=Integer_NA_
            int length = object.getLength();
            ByteBuffer buffer = allocate(4 * length, swap);
            for (int i = 0; i < length; i++) {
                byte value = object.getDataAt(i);
                int encoded = RRuntime.isNA(value) ? RRuntime.INT_NA : value == RRuntime.LOGICAL_FALSE ? 0 : 1;
                buffer.putInt(encoded);
            }
            return buffer;
        }

        @Specialization
        protected ByteBuffer writeRaw(RAbstractRawVector object, @SuppressWarnings("unused") int size, boolean swap, @SuppressWarnings("unused") boolean useBytes) {
            int length = object.getLength();
            ByteBuffer buffer = allocate(length, swap);
            for (int i = 0; i < length; i++) {
                RRaw value = object.getDataAt(i);
                buffer.put(value.getValue());
            }
            return buffer;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected ByteBuffer fallback(Object value, int size, boolean swap, boolean useBytes) {
            throw RError.nyi(this, "vector type");
        }
    }

    @RBuiltin(name = "writeBin", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"object", "con", "size", "swap", "useBytes"}, behavior = IO)
    public abstract static class WriteBin extends InternalCloseHelper {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.firstIntegerWithError(2, null, null);
        }

        @TruffleBoundary
        @Specialization
        protected Object writeBin(RAbstractVector object, RConnection con, int size, byte swapArg, byte useBytesArg, //
                        @Cached("create()") WriteDataNode writeData) {
            boolean swap = RRuntime.fromLogical(swapArg);
            boolean useBytes = RRuntime.fromLogical(useBytesArg);
            if (object.getLength() > 0) {
                try (RConnection openConn = con.forceOpen("wb")) {
                    if (getBaseConnection(openConn).isTextMode()) {
                        throw RError.error(this, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
                    }
                    ByteBuffer buffer = writeData.execute(object, size, swap, useBytes);
                    buffer.flip();
                    con.writeBin(buffer);
                } catch (IOException x) {
                    throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
                }
            }
            RContext.getInstance().setVisible(false);
            return RNull.instance;
        }

        @Specialization
        protected RRawVector writeBin(RAbstractVector object, @SuppressWarnings("unused") RAbstractRawVector con, int size, byte swapArg, byte useBytesArg, //
                        @Cached("create()") WriteDataNode writeData) {
            boolean swap = RRuntime.fromLogical(swapArg);
            boolean useBytes = RRuntime.fromLogical(useBytesArg);
            ByteBuffer buffer = writeData.execute(object, size, swap, useBytes);
            buffer.flip();
            RContext.getInstance().setVisible(false);
            return RDataFactory.createRawVector(buffer.array());
        }
    }

    @RBuiltin(name = "getConnection", kind = INTERNAL, parameterNames = {"what"}, behavior = IO)
    public abstract static class GetConnection extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toInteger(0);
        }

        @Specialization
        @TruffleBoundary
        protected RConnection getConnection(int what) {
            BaseRConnection con = RContext.getInstance().stateRConnection.getConnection(what);
            if (con == null) {
                throw RError.error(this, RError.Message.NO_SUCH_CONNECTION, what);
            } else {
                return con;
            }
        }
    }

    @RBuiltin(name = "getAllConnections", kind = INTERNAL, parameterNames = {}, behavior = IO)
    public abstract static class GetAllConnections extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RIntVector getAllConnections() {
            return RContext.getInstance().stateRConnection.getAllConnections();
        }
    }

    @RBuiltin(name = "isSeekable", kind = INTERNAL, parameterNames = "con", behavior = IO)
    public abstract static class IsSeekable extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected byte isSeekable(RConnection con) {
            return RRuntime.asLogical(con.isSeekable());
        }
    }

    @RBuiltin(name = "seek", kind = INTERNAL, parameterNames = {"con", "where", "origin", "rw"}, behavior = IO)
    public abstract static class Seek extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected long seek(RConnection con, RAbstractDoubleVector where, RAbstractIntVector origin, RAbstractIntVector rw) {
            long offset = (long) where.getDataAt(0);
            try {
                return con.seek(offset, RConnection.SeekMode.values()[origin.getDataAt(0)], RConnection.SeekRWMode.values()[rw.getDataAt(0)]);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.GENERIC, x.getMessage());
            }
        }
    }
}
