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
import static com.oracle.truffle.r.runtime.conn.ConnectionSupport.*;
import static com.oracle.truffle.r.runtime.conn.StdConnections.*;
import static com.oracle.truffle.r.runtime.conn.FileConnections.*;
import static com.oracle.truffle.r.runtime.conn.SocketConnections.*;
import static com.oracle.truffle.r.runtime.conn.TextConnections.*;
import static com.oracle.truffle.r.runtime.conn.GZIPConnections.*;
import static com.oracle.truffle.r.runtime.conn.URLConnections.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * The builtins for connections.
 *
 * The implementations of {@link RConnection} corresponding to the builtins are in
 * {@code com.oracle.truffle.r.runime.conn}.
 *
 */
public abstract class ConnectionFunctions {
    @RBuiltin(name = "stdin", kind = INTERNAL, parameterNames = {})
    public abstract static class Stdin extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdin() {
            controlVisibility();
            return getStdin();
        }
    }

    @RBuiltin(name = "stdout", kind = INTERNAL, parameterNames = {})
    public abstract static class Stdout extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stdout() {
            controlVisibility();
            return getStdout();
        }
    }

    @RBuiltin(name = "stderr", kind = INTERNAL, parameterNames = {})
    public abstract static class Stderr extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RConnection stderr() {
            controlVisibility();
            return getStderr();
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
            data[1] = baseCon.getClassHr().getDataAt(0);
            data[2] = baseCon.getOpenMode().modeString;
            data[3] = baseCon.getSummaryText();
            data[4] = baseCon.isClosed() || !baseCon.isOpen() ? "closed" : "opened";
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
                if (baseConn.isClosed()) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_CONNECTION);
                }
                if (baseConn.isOpen()) {
                    RContext.getInstance().setEvalWarning(RError.Message.ALREADY_OPEN_CONNECTION.message);
                    return RNull.instance;
                }
                baseConn.forceOpen(open.getDataAt(0));
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
            boolean result = !baseCon.isClosed() && baseCon.isOpen();
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
                if (getBaseConnection(con).getOpenMode().isText()) {
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
            BaseRConnection[] allConnections = getAllConnections();
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
