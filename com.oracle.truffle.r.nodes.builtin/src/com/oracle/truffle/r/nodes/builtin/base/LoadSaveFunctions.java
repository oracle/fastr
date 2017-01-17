/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.SerializeFunctions.Adapter;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// from src/main/saveload.c

public class LoadSaveFunctions {

    @RBuiltin(name = "loadFromConn2", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "envir", "verbose"}, behavior = IO)
    public abstract static class LoadFromConn2 extends RBuiltinNode {

        private final NACheck naCheck = NACheck.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
            casts.arg("envir").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
            casts.arg("verbose").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector load(int conIndex, REnvironment envir, @SuppressWarnings("unused") boolean verbose) {
            RConnection con = RConnection.fromIndex(conIndex);
            try (RConnection openConn = con.forceOpen("r")) {
                String s = openConn.readChar(5, true);
                if (s.equals("RDA2\n") || s.equals("RDB2\n") || s.equals("RDX2\n")) {
                    Object o = RSerialize.unserialize(con);
                    if (o == RNull.instance) {
                        return RDataFactory.createEmptyStringVector();
                    }
                    if (!(o instanceof RPairList)) {
                        throw RError.error(this, RError.Message.GENERIC, "loaded data is not in pair list form");
                    }
                    RPairList vars = (RPairList) o;

                    String[] data = new String[vars.getLength()];
                    int i = 0;
                    naCheck.enable(true);
                    while (true) {
                        String tag = vars.getTag().toString();
                        data[i] = tag;
                        naCheck.check(tag);

                        envir.put(tag, vars.car());

                        if (vars.cdr() == null || vars.cdr() == RNull.instance) {
                            break;
                        }
                        vars = (RPairList) vars.cdr();
                        i++;
                    }

                    return RDataFactory.createStringVector(data, naCheck.neverSeenNA());

                } else {
                    throw RError.error(this, RError.Message.GENERIC, "the input does not start with a magic number compatible with loading from a connection");
                }
            } catch (IOException iox) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.ERROR_READING_CONNECTION, iox.getMessage());
            } catch (PutException px) {
                throw RError.error(RError.SHOW_CALLER, px);
            }
        }
    }

    @RBuiltin(name = "load", visibility = OFF, kind = INTERNAL, parameterNames = {"file", "envir"}, behavior = IO)
    public abstract static class Load extends RBuiltinNode {
        // now deprecated but still used by some packages

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("file").mustBe(stringValue()).asStringVector().mustBe(notEmpty(), RError.Message.FIRST_ARGUMENT_NOT_FILENAME).findFirst();
            casts.arg("envir").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
        }

        private static final int R_MAGIC_EMPTY = 999;
        private static final int R_MAGIC_CORRUPT = 998;
        private static final int R_MAGIC_TOONEW = 997;
        private static final int R_MAGIC_ASCII_V1 = 1001;
        private static final int R_MAGIC_BINARY_V1 = 1002;
        private static final int R_MAGIC_XDR_V1 = 1003;
        private static final int R_MAGIC_ASCII_V2 = 2001;
        private static final int R_MAGIC_BINARY_V2 = 2002;
        private static final int R_MAGIC_XDR_V2 = 2003;

        @Specialization
        @TruffleBoundary
        protected RStringVector load(String pathIn, @SuppressWarnings("unused") REnvironment envir) {
            String path = Utils.tildeExpand(pathIn);
            try (BufferedInputStream bs = new BufferedInputStream(new FileInputStream(path))) {
                int magic = readMagic(bs);
                switch (magic) {
                    case R_MAGIC_EMPTY:
                        throw RError.error(this, RError.Message.MAGIC_EMPTY);
                    case R_MAGIC_TOONEW:
                        throw RError.error(this, RError.Message.MAGIC_TOONEW);
                    case R_MAGIC_CORRUPT:
                        throw RError.error(this, RError.Message.MAGIC_CORRUPT);
                    default:

                }
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.FILE_OPEN_ERROR);
            }
            throw RError.nyi(this, "load");
        }

        private static int readMagic(BufferedInputStream bs) throws IOException {
            byte[] buf = new byte[5];
            int count = bs.read(buf, 0, 5);
            if (count != 5) {
                if (count == 0) {
                    return R_MAGIC_EMPTY;
                } else {
                    return R_MAGIC_CORRUPT;
                }
            }
            String magic = new String(buf);
            switch (magic) {
                case "RDA1\n":
                    return R_MAGIC_ASCII_V1;
                case "RDB1\n":
                    return R_MAGIC_BINARY_V1;
                case "RDX1\n":
                    return R_MAGIC_XDR_V1;
                case "RDA2\n":
                    return R_MAGIC_ASCII_V2;
                case "RDB2\n":
                    return R_MAGIC_BINARY_V2;
                case "RDX2\n":
                    return R_MAGIC_XDR_V2;
                default:
                    if (buf[0] == 'R' && buf[1] == 'D') {
                        return R_MAGIC_TOONEW;
                    } else {
                        return R_MAGIC_CORRUPT;
                    }
            }
        }
    }

    @RBuiltin(name = "saveToConn", visibility = OFF, kind = INTERNAL, parameterNames = {"list", "con", "ascii", "version", "environment", "eval.promises"}, behavior = IO)
    public abstract static class SaveToConn extends Adapter {
        private static final String ASCII_HEADER = "RDA2\n";
        private static final String XDR_HEADER = "RDX2\n";

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("list").mustBe(stringValue()).asStringVector().mustBe(notEmpty(), RError.Message.FIRST_ARGUMENT_NOT_CHARVEC).findFirst();
            ConnectionFunctions.Casts.connection(casts);
            casts.arg("ascii").mustBe(logicalValue(), RError.Message.ASCII_NOT_LOGICAL);
            casts.arg("version").allowNull().mustBe(integerValue());
            casts.arg("environment").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
            casts.arg("eval.promises").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @Specialization
        protected Object saveToConn(VirtualFrame frame, RAbstractStringVector list, int con, byte asciiLogical, @SuppressWarnings("unused") RNull version, REnvironment envir, boolean evalPromises,
                        @Cached("new()") PromiseCheckHelperNode promiseHelper) {
            RPairList prev = null;
            Object toSave = RNull.instance;
            for (int i = 0; i < list.getLength(); i++) {
                String varName = list.getDataAt(i);
                Object value = envir.get(varName);
                if (value == null) {
                    throw RError.error(this, RError.Message.UNKNOWN_OBJECT, varName);
                }
                if (evalPromises) {
                    value = promiseHelper.checkEvaluate(frame, value);
                }
                RPairList pl = RDataFactory.createPairList(value);
                pl.setTag(RDataFactory.createSymbol(Utils.intern(varName)));
                if (prev == null) {
                    toSave = pl;
                } else {
                    prev.setCdr(pl);
                }
                prev = pl;
            }
            boolean ascii = RRuntime.fromLogical(asciiLogical);
            doSaveConn(toSave, RConnection.fromIndex(con), ascii);
            return RNull.instance;
        }

        @TruffleBoundary
        private void doSaveConn(Object toSave, RConnection conn, boolean ascii) {
            try (RConnection openConn = conn.forceOpen(ascii ? "wt" : "wb")) {
                if (!openConn.canWrite()) {
                    throw RError.error(this, RError.Message.CONNECTION_NOT_OPEN_WRITE);
                }
                if (!ascii && openConn.isTextMode()) {
                    throw RError.error(this, RError.Message.CONN_XDR);
                }
                openConn.writeChar(ascii ? ASCII_HEADER : XDR_HEADER, 0, "", false);
                RSerialize.serialize(openConn, toSave, ascii ? RSerialize.ASCII : RSerialize.XDR, RSerialize.DEFAULT_VERSION, null);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object saveToConn(Object list, Object con, Object ascii, Object version, Object envir, Object evaPromises) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }
}
