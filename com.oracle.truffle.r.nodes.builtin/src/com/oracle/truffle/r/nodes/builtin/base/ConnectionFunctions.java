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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.equalTo;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalTrue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.ConnectionFunctionsFactory.WriteDataNodeGen;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.HeadPhaseBuilder;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.BaseRConnection;
import com.oracle.truffle.r.runtime.conn.FileConnections.FileRConnection;
import com.oracle.truffle.r.runtime.conn.CompressedConnections.CompressedRConnection;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.SocketConnections.RSocketConnection;
import com.oracle.truffle.r.runtime.conn.TextConnections.TextRConnection;
import com.oracle.truffle.r.runtime.conn.URLConnections.URLRConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
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
        protected RAbstractIntVector stdin() {
            return getStdin().asVector();
        }
    }

    @RBuiltin(name = "stdout", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Stdout extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector stdout() {
            return getStdout().asVector();
        }
    }

    @RBuiltin(name = "stderr", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Stderr extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector stderr() {
            return getStderr().asVector();
        }
    }

    public static final class Casts {
        private static void description(CastBuilder casts) {
            casts.arg("description").mustBe(stringValue()).asStringVector().shouldBe(singleElement(), RError.Message.ARGUMENT_ONLY_FIRST_1, "description").findFirst().notNA();
        }

        private static HeadPhaseBuilder<String> open(CastBuilder casts) {
            return casts.arg("open").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().notNA();
        }

        private static void encoding(CastBuilder casts) {
            casts.arg("encoding").asStringVector().mustBe(singleElement()).findFirst();
        }

        private static void raw(CastBuilder casts) {
            casts.arg("raw").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        private static void blocking(CastBuilder casts) {
            casts.arg("blocking").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        public static void connection(CastBuilder casts) {
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        }

        private static void nchars(CastBuilder casts) {
            casts.arg("nchars").asIntegerVector().mustBe(notEmpty());
        }

        private static void useBytes(CastBuilder casts) {
            casts.arg("useBytes").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        private static void n(CastBuilder casts) {
            casts.arg("n").asIntegerVector().findFirst().mustBe(gte(0));
        }

        private static void size(CastBuilder casts) {
            casts.arg("size").asIntegerVector().findFirst();
        }

        private static void swap(CastBuilder casts) {
            casts.arg("swap").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        private static void method(CastBuilder casts) {
            casts.arg("method").asStringVector().findFirst();
        }
    }

    @RBuiltin(name = "file", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding", "method", "raw"}, behavior = IO)
    public abstract static class File extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.description(casts);
            Casts.open(casts);
            casts.arg("blocking").asLogicalVector().findFirst().mustBe(logicalTrue(), RError.Message.NYI, "non-blocking mode not supported").map(toBoolean());
            Casts.encoding(casts);
            Casts.method(casts);
            Casts.raw(casts);
        }

        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected RAbstractIntVector file(String description, String openArg, boolean blocking, String encoding, String method, boolean raw) {
            String open = openArg;
            // TODO handle http/ftp prefixes and redirect and method
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
                return new FileRConnection(path, open).asVector();
            } catch (IOException ex) {
                RError.warning(this, RError.Message.CANNOT_OPEN_FILE, description, ex.getMessage());
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }

    }

    /*
     * In GNUR R {@code gzfile, bzfile, xzfile} are very versatile on input; they can open
     * uncompressed files, and files compressed by {@code bzip2, xz, lzma}.
     */

    public abstract static class ZZFileAdapter extends RBuiltinNode {
        private final RCompression.Type cType;

        protected ZZFileAdapter(RCompression.Type cType) {
            this.cType = cType;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.description(casts);
            Casts.open(casts);
            Casts.encoding(casts);
            casts.arg("compression").asIntegerVector().findFirst().notNA().mustBe(gte(cType == RCompression.Type.XZ ? -9 : 0).and(lte(9)));
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector zzFile(RAbstractStringVector description, String open, String encoding, int compression) {
            try {
                return new CompressedRConnection(description.getDataAt(0), open, cType, encoding, compression).asVector();
            } catch (IOException ex) {
                throw reportError(description.getDataAt(0), ex);
            }
        }

        private RError reportError(String path, IOException ex) throws RError {
            RError.warning(this, RError.Message.CANNOT_OPEN_FILE, path, ex.getMessage());
            throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
        }
    }

    @RBuiltin(name = "gzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"}, behavior = IO)
    public abstract static class GZFile extends ZZFileAdapter {
        protected GZFile() {
            super(RCompression.Type.GZIP);
        }

    }

    @RBuiltin(name = "bzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"}, behavior = IO)
    public abstract static class BZFile extends ZZFileAdapter {
        protected BZFile() {
            super(RCompression.Type.BZIP2);
        }

    }

    @RBuiltin(name = "xzfile", kind = INTERNAL, parameterNames = {"description", "open", "encoding", "compression"}, behavior = IO)
    public abstract static class XZFile extends ZZFileAdapter {
        protected XZFile() {
            super(RCompression.Type.XZ);
        }

    }

    @RBuiltin(name = "textConnection", kind = INTERNAL, parameterNames = {"description", "text", "open", "env", "encoding"}, behavior = IO)
    public abstract static class TextConnection extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.description(casts);
            // TODO how to have either a RNull or a String/RStringVector and have the latter coerced
            // to a
            // RAbstractStringVector to avoid the explicit handling in the specialization
            casts.arg("text").allowNull().mustBe(stringValue());
            Casts.open(casts).mustBe(equalTo("").or(equalTo("r").or(equalTo("w").or(equalTo("a")))), RError.Message.UNSUPPORTED_MODE);
            casts.arg("env").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
            casts.arg("encoding").asIntegerVector().findFirst().notNA();
        }

        private static RAbstractStringVector forceStringVector(Object text) {
            if (text instanceof String) {
                return RDataFactory.createStringVectorFromScalar((String) text);
            } else {
                return (RAbstractStringVector) text;
            }
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector textConnection(String description, Object text, String open, REnvironment env, @SuppressWarnings("unused") int encoding) {
            RAbstractStringVector object;
            if (open.length() == 0 || open.equals("r")) {
                if (text == RNull.instance) {
                    throw RError.error(this, RError.Message.INVALID_ARGUMENT, "text");
                } else {
                    object = forceStringVector(text);
                }
            } else {
                if (text == RNull.instance) {
                    throw RError.nyi(this, "textConnection: NULL");
                } else {
                    object = forceStringVector(text);
                }
            }
            try {
                return new TextRConnection(description, object, env, open).asVector();
            } catch (IOException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }

    }

    @RBuiltin(name = "textConnectionValue", kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class TextConnectionValue extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("con").defaultError(Message.NOT_A_TEXT_CONNECTION).mustNotBeNull().mustBe(integerValue()).asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object textConnection(int con) {
            RConnection connection = RConnection.fromIndex(con);
            if (connection instanceof TextRConnection) {
                return RDataFactory.createStringVector(((TextRConnection) connection).getValue(), RDataFactory.COMPLETE_VECTOR);
            } else {
                throw RError.error(RError.SHOW_CALLER, Message.NOT_A_TEXT_CONNECTION);
            }
        }
    }

    @RBuiltin(name = "socketConnection", kind = INTERNAL, parameterNames = {"host", "port", "server", "blocking", "open", "encoding", "timeout"}, behavior = IO)
    public abstract static class SocketConnection extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("host").mustBe(stringValue()).asStringVector().findFirst();
            casts.arg("port").asIntegerVector().findFirst().notNA().mustBe(gte(0));
            casts.arg("server").asLogicalVector().findFirst().notNA().map(toBoolean());
            Casts.open(casts);
            Casts.blocking(casts);
            casts.arg("timeout").asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector socketConnection(String host, int port, boolean server, boolean blocking, String open,
                        @SuppressWarnings("unused") RAbstractStringVector encoding, int timeout) {
            try {
                if (server) {
                    return new RSocketConnection(open, true, host, port, blocking, timeout).asVector();
                } else {
                    return new RSocketConnection(open, false, host, port, blocking, timeout).asVector();
                }
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    @RBuiltin(name = "url", kind = INTERNAL, parameterNames = {"description", "open", "blocking", "encoding", "method"}, behavior = IO)
    public abstract static class URLConnection extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.description(casts);
            Casts.open(casts);
            Casts.blocking(casts);
            Casts.encoding(casts);
            Casts.method(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector urlConnection(String url, String open, @SuppressWarnings("unused") boolean blocking, @SuppressWarnings("unused") String encoding,
                        @SuppressWarnings("unused") String method) {
            try {
                return new URLRConnection(url, open).asVector();
            } catch (MalformedURLException ex) {
                throw RError.error(this, RError.Message.UNSUPPORTED_URL_SCHEME);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.CANNOT_OPEN_CONNECTION);
            }
        }
    }

    @RBuiltin(name = "summary.connection", kind = INTERNAL, parameterNames = {"object"}, behavior = IO)
    public abstract static class Summary extends RBuiltinNode {
        private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"description", "class", "mode", "text", "opened", "can read", "can write"},
                        RDataFactory.COMPLETE_VECTOR);

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("object").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RList summary(int object) {
            BaseRConnection baseCon = RConnection.fromIndex(object);
            Object[] data = new Object[NAMES.getLength()];
            data[0] = baseCon.getSummaryDescription();
            data[1] = baseCon.getConnectionClass().getPrintName();
            data[2] = baseCon.getOpenMode().summaryString();
            data[3] = baseCon.getSummaryText();
            data[4] = baseCon.isOpen() ? "opened" : "closed";
            data[5] = baseCon.canRead() ? "yes" : "no";
            data[6] = baseCon.canWrite() ? "yes" : "no";
            return RDataFactory.createList(data, NAMES);
        }
    }

    @RBuiltin(name = "open", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "open", "blocking"}, behavior = IO)
    public abstract static class Open extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            Casts.open(casts);
            Casts.blocking(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object open(int con, String open, @SuppressWarnings("unused") boolean blocking) {
            try {
                BaseRConnection baseConn = getBaseConnection(RConnection.fromIndex(con));
                if (baseConn.isClosed()) {
                    throw RError.error(this, RError.Message.INVALID_CONNECTION);
                }
                if (baseConn.isOpen()) {
                    RError.warning(this, RError.Message.ALREADY_OPEN_CONNECTION);
                    return RNull.instance;
                }
                baseConn.open(open);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

    }

    @RBuiltin(name = "isOpen", kind = INTERNAL, parameterNames = {"con", "rw"}, behavior = IO)
    public abstract static class IsOpen extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            casts.arg("rw").asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RLogicalVector isOpen(int con, int rw) {
            BaseRConnection baseCon = getBaseConnection(RConnection.fromIndex(con));
            boolean result = baseCon.isOpen();
            switch (rw) {
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

    }

    @RBuiltin(name = "close", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "type"}, behavior = IO)
    public abstract static class Close extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            casts.arg("type").asStringVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object close(int con, @SuppressWarnings("unused") String type) {
            BaseRConnection connection = RConnection.fromIndex(con);
            try {
                connection.closeAndDestroy();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "readLines", kind = INTERNAL, parameterNames = {"con", "n", "ok", "warn", "encoding", "skipNul"}, behavior = IO)
    public abstract static class ReadLines extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            casts.arg("n").asIntegerVector().findFirst().notNA();
            casts.arg("ok").asLogicalVector().findFirst().notNA().map(toBoolean());
            casts.arg("warn").asLogicalVector().findFirst().notNA().map(toBoolean());
            Casts.encoding(casts);
            casts.arg("skipNul").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected Object readLines(int con, int n, boolean ok, boolean warn, @SuppressWarnings("unused") String encoding, boolean skipNul) {
            // TODO implement all the arguments
            try (RConnection openConn = RConnection.fromIndex(con).forceOpen("rt")) {
                String[] lines = openConn.readLines(n, warn, skipNul);
                if (n > 0 && lines.length < n && !ok) {
                    throw RError.error(this, RError.Message.TOO_FEW_LINES_READ_LINES);
                }
                return RDataFactory.createStringVector(lines, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            }
        }

    }

    @RBuiltin(name = "writeLines", visibility = OFF, kind = INTERNAL, parameterNames = {"text", "con", "sep", "useBytes"}, behavior = IO)
    public abstract static class WriteLines extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("text").asStringVector().mustBe(instanceOf(RAbstractStringVector.class));
            Casts.connection(casts);
            casts.arg("sep").asStringVector().findFirst();
            Casts.useBytes(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull writeLines(RAbstractStringVector text, int con, String sep, boolean useBytes) {
            try (RConnection openConn = RConnection.fromIndex(con).forceOpen("wt")) {
                openConn.writeLines(text, sep, useBytes);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            }
            return RNull.instance;
        }

    }

    @RBuiltin(name = "flush", visibility = OFF, kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class Flush extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
        }

        @Specialization
        @TruffleBoundary
        protected RNull flush(int con) {
            try {
                RConnection.fromIndex(con).flush();
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_FLUSHING_CONNECTION, x.getMessage());
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "pushBack", visibility = OFF, kind = INTERNAL, parameterNames = {"data", "con", "newLine", "type"}, behavior = IO)
    public abstract static class PushBack extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("data").asStringVector().mustBe(instanceOf(RAbstractStringVector.class));
            Casts.connection(casts);
            casts.arg("newLine").asLogicalVector().findFirst().notNA().map(toBoolean());
            casts.arg("type").asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object pushBack(RAbstractStringVector data, int connection, boolean newLine, @SuppressWarnings("unused") int type) {
            RConnection.fromIndex(connection).pushBack(data, newLine);
            return RNull.instance;
        }

    }

    @RBuiltin(name = "pushBackLength", kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class PushBackLength extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
        }

        @Specialization
        protected int pushBackLength(int connection) {
            return RConnection.fromIndex(connection).pushBackLength();
        }

    }

    @RBuiltin(name = "clearPushBack", visibility = OFF, kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class PushBackClear extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
        }

        @Specialization
        protected RNull pushBackClear(int connection) {
            RConnection.fromIndex(connection).pushBackClear();
            return RNull.instance;
        }

    }

    @RBuiltin(name = "readChar", kind = INTERNAL, parameterNames = {"con", "nchars", "useBytes"}, behavior = IO)
    public abstract static class ReadChar extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            Casts.nchars(casts);
            Casts.useBytes(casts);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "ncharsEmpty(nchars)")
        protected RStringVector readCharNcharsEmpty(int con, RAbstractIntVector nchars, boolean useBytes) {
            return RDataFactory.createEmptyStringVector();
        }

        @Specialization(guards = "!ncharsEmpty(nchars)")
        @TruffleBoundary
        protected RStringVector readChar(int con, RAbstractIntVector nchars, boolean useBytes) {
            try (RConnection openConn = RConnection.fromIndex(con).forceOpen("rb")) {
                String[] data = new String[nchars.getLength()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = openConn.readChar(nchars.getDataAt(i), useBytes);
                }
                return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            }
        }

        boolean ncharsEmpty(RAbstractIntVector nchars) {
            return nchars.getLength() == 0;
        }

    }

    @RBuiltin(name = "writeChar", visibility = OFF, kind = INTERNAL, parameterNames = {"object", "con", "nchars", "sep", "useBytes"}, behavior = IO)
    public abstract static class WriteChar extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("object").asStringVector();
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().mustBe(integerValue().or(rawValue())).mapIf(integerValue(),
                            asIntegerVector().setNext(findFirst().integerElement()));
            Casts.nchars(casts);
            casts.arg("sep").allowNull().mustBe(stringValue());
            Casts.useBytes(casts);
        }

        @TruffleBoundary
        @Specialization
        protected RNull writeChar(RAbstractStringVector object, int con, RAbstractIntVector nchars, RAbstractStringVector sep, boolean useBytes) {
            try (RConnection openConn = RConnection.fromIndex(con).forceOpen("wb")) {
                int length = object.getLength();
                for (int i = 0; i < length; i++) {
                    String s = object.getDataAt(i);
                    int nc = nchars.getDataAt(i % length);
                    int pad = nc - s.length();
                    if (pad > 0) {
                        RError.warning(this, RError.Message.MORE_CHARACTERS);
                    }
                    openConn.writeChar(s, pad, sep.getDataAt(i % length), useBytes);
                }
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected RNull writeChar(RAbstractStringVector object, int con, RAbstractIntVector nchars, RNull sep, boolean useBytes) {
            throw RError.nyi(this, "writeChar(sep=NULL)");
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
    public abstract static class ReadBin extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            // TODO con can be a RAWSXP (not implemented)
            Casts.connection(casts);
            casts.arg("what").asStringVector().findFirst();
            Casts.n(casts);
            Casts.size(casts);
            casts.arg("signed").asLogicalVector().findFirst().notNA().map(toBoolean());
            Casts.swap(casts);
        }

        @Specialization
        @TruffleBoundary
        protected Object readBin(int con, String what, int n, int size, boolean signed, boolean swap) {
            RVector<?> result = null;
            BaseRConnection connection = RConnection.fromIndex(con);
            try (RConnection openConn = connection.forceOpen("rb")) {
                if (getBaseConnection(openConn).getOpenMode().isText()) {
                    throw RError.error(this, RError.Message.ONLY_READ_BINARY_CONNECTION);
                }
                switch (what) {
                    case "int":
                    case "integer":
                        if (size == RRuntime.INT_NA) {
                            size = 4;
                        }
                        if (size == 1 || size == 4) {
                            result = readInteger(connection, n, size, swap, signed);
                        } else {
                            throw RError.nyi(this, "readBin \"int\" size not implemented");
                        }
                        break;
                    case "double":
                    case "numeric":
                        result = readDouble(connection, n, swap);
                        break;
                    case "complex":
                        result = readComplex(connection, n, swap);
                        break;
                    case "character":
                        result = readString(connection, n);
                        break;
                    case "logical":
                        result = readLogical(connection, n, swap);
                        break;
                    case "raw":
                        result = readRaw(connection, n);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            } catch (IOException x) {
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, x.getMessage());
            }
            return result;
        }

        private static RIntVector readInteger(RConnection con, int n, int size, boolean swap, boolean signed) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(n * size);
            int bytesRead = con.readBin(buffer);
            if (bytesRead == 0) {
                return RDataFactory.createEmptyIntVector();
            }
            buffer.flip();
            checkOrder(buffer, swap);
            int nInts = bytesRead / size;
            int[] data = new int[nInts];
            boolean complete = RDataFactory.COMPLETE_VECTOR;
            if (size == 4) {
                IntBuffer intBuffer = buffer.asIntBuffer();
                for (int i = 0; i < nInts; i++) {
                    int d = intBuffer.get();
                    if (RRuntime.isNA(d)) {
                        complete = RDataFactory.INCOMPLETE_VECTOR;
                    }
                    data[i] = d;
                }
            } else if (size == 1) {
                for (int i = 0; i < nInts; i++) {
                    byte b = buffer.get();
                    int d = signed ? b : b & 0xFF;
                    data[i] = d;
                }
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
        @TruffleBoundary
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

    }

    @RBuiltin(name = "writeBin", visibility = OFF, kind = INTERNAL, parameterNames = {"object", "con", "size", "swap", "useBytes"}, behavior = IO)
    public abstract static class WriteBin extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            // TODO atomic, i.e. not RList or RExpression
            casts.arg("object").asVector().mustBe(instanceOf(RAbstractVector.class));
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustBe(integerValue().or(rawValue())).mapIf(integerValue(), asIntegerVector().setNext(findFirst().integerElement()));
            Casts.size(casts);
            Casts.swap(casts);
            Casts.useBytes(casts);
        }

        @TruffleBoundary
        @Specialization
        protected Object writeBin(RAbstractVector object, int con, int size, boolean swap, boolean useBytes,
                        @Cached("create()") WriteDataNode writeData) {
            if (object instanceof RList || object instanceof RExpression) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "object");
            }
            if (object.getLength() > 0) {
                RConnection connection = RConnection.fromIndex(con);
                try (RConnection openConn = connection.forceOpen("wb")) {
                    if (getBaseConnection(openConn).isTextMode()) {
                        throw RError.error(this, RError.Message.ONLY_WRITE_BINARY_CONNECTION);
                    }
                    ByteBuffer buffer = writeData.execute(object, size, swap, useBytes);
                    buffer.flip();
                    connection.writeBin(buffer);
                } catch (IOException x) {
                    throw RError.error(this, RError.Message.ERROR_WRITING_CONNECTION, x.getMessage());
                }
            }
            return RNull.instance;
        }

        @Specialization
        protected RRawVector writeBin(RAbstractVector object, @SuppressWarnings("unused") RAbstractRawVector con, int size, byte swapArg, byte useBytesArg,
                        @Cached("create()") WriteDataNode writeData) {
            boolean swap = RRuntime.fromLogical(swapArg);
            boolean useBytes = RRuntime.fromLogical(useBytesArg);
            ByteBuffer buffer = writeData.execute(object, size, swap, useBytes);
            buffer.flip();
            return RDataFactory.createRawVector(buffer.array());
        }
    }

    @RBuiltin(name = "getConnection", kind = INTERNAL, parameterNames = {"what"}, behavior = IO)
    public abstract static class GetConnection extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("what").asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector getConnection(int what) {
            BaseRConnection con = RContext.getInstance().stateRConnection.getConnection(what, false);
            if (con == null) {
                throw RError.error(this, RError.Message.NO_SUCH_CONNECTION, what);
            } else {
                return con.asVector();
            }
        }
    }

    @RBuiltin(name = "getAllConnections", kind = INTERNAL, parameterNames = {}, behavior = IO)
    public abstract static class GetAllConnections extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RAbstractIntVector getAllConnections() {
            return RContext.getInstance().stateRConnection.getAllConnections();
        }
    }

    @RBuiltin(name = "isSeekable", kind = INTERNAL, parameterNames = "con", behavior = IO)
    public abstract static class IsSeekable extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
        }

        @Specialization
        @TruffleBoundary
        protected byte isSeekable(int con) {
            return RRuntime.asLogical(RConnection.fromIndex(con).isSeekable());
        }
    }

    @RBuiltin(name = "seek", kind = INTERNAL, parameterNames = {"con", "where", "origin", "rw"}, behavior = IO)
    public abstract static class Seek extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            Casts.connection(casts);
            casts.arg("where").asDoubleVector().findFirst();
            casts.arg("origin").asIntegerVector().findFirst();
            casts.arg("rw").asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected int seek(int con, double where, int origin, int rw) {
            /*
             * N.B. 0,1,2 are valid values for "rw"; 1,2,3 are valid values for "origin". We use 0
             * for the NA (enquiry) case.
             */
            long offset = 0;
            if (RRuntime.isNAorNaN(where)) {
                origin = 0;
            } else {
                offset = (long) where;
            }
            try {
                long newOffset = RConnection.fromIndex(con).seek(offset, RConnection.SeekMode.values()[origin], RConnection.SeekRWMode.values()[rw]);
                if (newOffset > Integer.MAX_VALUE) {
                    throw RError.nyi(this, "seek > Integer.MAX_VALUE");
                }
                return (int) newOffset;
            } catch (IOException x) {
                throw RError.error(this, RError.Message.GENERIC, x.getMessage());
            }
        }
    }
}
