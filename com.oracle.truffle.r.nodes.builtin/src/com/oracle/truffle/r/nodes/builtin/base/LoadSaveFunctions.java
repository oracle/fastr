/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.BufferedInputStream;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// from src/main/saveload.c

public class LoadSaveFunctions {

    private static final String ASCII_HEADER2 = "RDA2\n";
    private static final String BINARY_HEADER2 = "RDB2\n";
    private static final String XDR_HEADER2 = "RDX2\n";
    private static final String ASCII_HEADER3 = "RDA3\n";
    private static final String BINARY_HEADER3 = "RDB3\n";
    private static final String XDR_HEADER3 = "RDX3\n";

    private static final int DEFAULT_SAVE_VERSION;
    static {
        String defVersion = System.getenv("R_DEFAULT_SAVE_VERSION");
        if ("2".equals(defVersion) || "3".equals(defVersion)) {
            DEFAULT_SAVE_VERSION = Integer.parseInt(defVersion);
        } else {
            DEFAULT_SAVE_VERSION = 3;
        }
    }

    private static void checkMagicNumber(RConnection openConn, RBuiltinNode node) throws IOException {
        String s = openConn.readChar(5, true);
        if (!(s.equals(ASCII_HEADER2) || s.equals(ASCII_HEADER3) ||
                        s.equals(XDR_HEADER2) || s.equals(XDR_HEADER3) ||
                        s.equals(BINARY_HEADER2) || s.equals(BINARY_HEADER3))) {
            throw node.error(RError.Message.GENERIC, "the input does not start with a magic number compatible with loading from a connection: " + s);
        }
    }

    @RBuiltin(name = "loadFromConn2", visibility = OFF, kind = INTERNAL, parameterNames = {"con", "envir", "verbose"}, behavior = IO)
    public abstract static class LoadFromConn2 extends RBuiltinNode.Arg3 {

        private final NACheck naCheck = NACheck.create();

        static {
            Casts casts = new Casts(LoadFromConn2.class);
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
            casts.arg("envir").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
            casts.arg("verbose").asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector load(int conIndex, REnvironment envir, @SuppressWarnings("unused") boolean verbose) {
            RConnection con = RConnection.fromIndex(conIndex);
            try (RConnection openConn = con.forceOpen("r")) {
                checkMagicNumber(openConn, this);
                Object o = RSerialize.unserialize(con);
                if (o == RNull.instance) {
                    return RDataFactory.createEmptyStringVector();
                }
                if (!((o instanceof RPairList && !((RPairList) o).isLanguage()))) {
                    throw error(RError.Message.GENERIC, "loaded data is not in pair list form");
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
            } catch (IOException iox) {
                throw error(RError.Message.ERROR_READING_CONNECTION, iox.getMessage());
            } catch (PutException px) {
                throw error(px);
            }
        }
    }

    @RBuiltin(name = "loadInfoFromConn2", visibility = ON, kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class LoadInfoFromConn2 extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(LoadInfoFromConn2.class);
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RList load(int conIndex) {
            RConnection con = RConnection.fromIndex(conIndex);
            try (RConnection openConn = con.forceOpen("r")) {
                checkMagicNumber(openConn, this);
                return RSerialize.unserializeInfo(con).toVector();
            } catch (IOException iox) {
                throw error(RError.Message.ERROR_READING_CONNECTION, iox.getMessage());
            }
        }
    }

    @RBuiltin(name = "load", visibility = OFF, kind = INTERNAL, parameterNames = {"file", "envir"}, behavior = IO)
    public abstract static class Load extends RBuiltinNode.Arg2 {
        // now deprecated but still used by some packages

        static {
            Casts casts = new Casts(Load.class);
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
        private static final int R_MAGIC_ASCII_V3 = 3001;
        private static final int R_MAGIC_BINARY_V3 = 3002;
        private static final int R_MAGIC_XDR_V3 = 3003;

        @Specialization
        @TruffleBoundary
        protected RStringVector load(String pathIn, @SuppressWarnings("unused") REnvironment envir) {
            try (BufferedInputStream bs = new BufferedInputStream(FileSystemUtils.getSafeTruffleFile(RContext.getInstance().getEnv(), Utils.tildeExpand(pathIn)).newInputStream())) {
                int magic = readMagic(bs);
                switch (magic) {
                    case R_MAGIC_EMPTY:
                        throw error(RError.Message.MAGIC_EMPTY);
                    case R_MAGIC_TOONEW:
                        throw error(RError.Message.MAGIC_TOONEW);
                    case R_MAGIC_CORRUPT:
                        throw error(RError.Message.MAGIC_CORRUPT);
                    default:

                }
            } catch (IOException ex) {
                throw error(RError.Message.FILE_OPEN_ERROR);
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
                case "RDA3\n":
                    return R_MAGIC_ASCII_V3;
                case "RDB3\n":
                    return R_MAGIC_BINARY_V3;
                case "RDX3\n":
                    return R_MAGIC_XDR_V3;
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
    public abstract static class SaveToConn extends RBuiltinNode.Arg6 {

        private final ConditionProfile valueNullProfile = ConditionProfile.createBinaryProfile();

        static {
            Casts casts = new Casts(SaveToConn.class);
            casts.arg("list").mustBe(stringValue()).asStringVector();
            ConnectionFunctions.CastsHelper.connection(casts);
            casts.arg("ascii").mustBe(logicalValue(), RError.Message.ASCII_NOT_LOGICAL).asLogicalVector().findFirst().map(toBoolean());
            casts.arg("version").allowNull().asIntegerVector().findFirstOrNull();
            casts.arg("environment").mustNotBeNull(RError.Message.USE_NULL_ENV_DEFUNCT).mustBe(instanceOf(REnvironment.class));
            casts.arg("eval.promises").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization
        protected Object saveToConn(VirtualFrame frame, RAbstractStringVector list, int con, boolean ascii, @SuppressWarnings("unused") RNull version, REnvironment envir, boolean evalPromises,
                        @Cached("new()") PromiseHelperNode promiseHelper) {
            return saveToConn(list, envir, evalPromises, promiseHelper, frame, con, ascii, DEFAULT_SAVE_VERSION);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object saveToConn(VirtualFrame frame, RAbstractStringVector list, int con, boolean ascii, int version, REnvironment envir, boolean evalPromises,
                        @Cached("new()") PromiseHelperNode promiseHelper) {
            return saveToConn(list, envir, evalPromises, promiseHelper, frame, con, ascii, version);
        }

        private Object saveToConn(RAbstractStringVector list, REnvironment envir, boolean evalPromises, PromiseHelperNode promiseHelper, VirtualFrame frame, int con, boolean ascii, int version)
                        throws RError {
            RPairList prev = null;
            Object toSave = RNull.instance;
            for (int i = 0; i < list.getLength(); i++) {
                String varName = list.getDataAt(i);
                Object value = null;
                REnvironment env = envir;
                while (env != REnvironment.emptyEnv()) {
                    value = env.get(varName);
                    if (value != null) {
                        if (value instanceof RPromise) {
                            value = evalPromises
                                            ? promiseHelper.evaluate(frame, (RPromise) value)
                                            : ((RPromise) value).getRawValue();
                        }
                        break;
                    }
                    env = env.getParent();
                }
                if (valueNullProfile.profile(value == null)) {
                    throw error(RError.Message.UNKNOWN_OBJECT, varName);
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
            doSaveConn(toSave, RConnection.fromIndex(con), ascii, version);
            return RNull.instance;
        }

        @TruffleBoundary
        private void doSaveConn(Object toSave, RConnection conn, boolean ascii, int version) {
            try (RConnection openConn = conn.forceOpen(ascii ? "wt" : "wb")) {
                if (!openConn.canWrite()) {
                    throw error(RError.Message.CONNECTION_NOT_OPEN_WRITE);
                }
                if (!ascii && openConn.isTextMode()) {
                    throw error(RError.Message.CONN_XDR);
                }

                String header;
                switch (version) {
                    case 2:
                        header = ascii ? ASCII_HEADER2 : XDR_HEADER2;
                        break;
                    case 3:
                        header = ascii ? ASCII_HEADER3 : XDR_HEADER3;
                        break;
                    default:
                        throw RInternalError.unimplemented("save version " + version);
                }
                openConn.writeChar(header, 0, null, false);
                RSerialize.serialize(RContext.getInstance(), openConn, toSave, ascii ? RSerialize.ASCII : RSerialize.XDR, version, null);
            } catch (IOException ex) {
                throw error(RError.Message.GENERIC, ex.getMessage());
            }
        }
    }
}
