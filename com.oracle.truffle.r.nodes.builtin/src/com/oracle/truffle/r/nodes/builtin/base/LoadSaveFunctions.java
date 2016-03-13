/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.SerializeFunctions.Adapter;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.ops.na.*;

// from src/main/saveload.c

public class LoadSaveFunctions {

    @RBuiltin(name = "loadFromConn2", kind = RBuiltinKind.INTERNAL, parameterNames = {"con", "envir", "verbose"})
    public abstract static class LoadFromConn2 extends RInvisibleBuiltinNode {

        private final NACheck naCheck = NACheck.create();

        @Specialization
        @TruffleBoundary
        protected RStringVector load(RConnection con, REnvironment envir, @SuppressWarnings("unused") RAbstractLogicalVector verbose) {
            controlVisibility();
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
                throw RError.error(this, RError.Message.ERROR_READING_CONNECTION, iox.getMessage());
            } catch (PutException px) {
                throw RError.error(this, px);
            }
        }
    }

    @RBuiltin(name = "load", kind = RBuiltinKind.INTERNAL, parameterNames = {"con", "envir"})
    public abstract static class Load extends RInvisibleBuiltinNode {
        // now deprecated but still used by some packages

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
        protected RStringVector load(RAbstractStringVector fileVec, @SuppressWarnings("unused") REnvironment envir) {
            controlVisibility();
            String path = Utils.tildeExpand(fileVec.getDataAt(0));
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

    @RBuiltin(name = "saveToConn", kind = INTERNAL, parameterNames = {"list", "conn", "ascii", "version", "envir", "eval.promises"})
    public abstract static class SaveToConn extends Adapter {
        private static final String ASCII_HEADER = "RDA2\n";
        private static final String XDR_HEADER = "RDX2\n";

        @Specialization
        protected Object saveToConn(VirtualFrame frame, RAbstractStringVector list, RConnection conn, byte asciiLogical, @SuppressWarnings("unused") RNull version, REnvironment envir,
                        byte evalPromisesLogical) {
            boolean evalPromises = RRuntime.fromLogical(evalPromisesLogical);
            RPairList prev = null;
            Object toSave = RNull.instance;
            for (int i = 0; i < list.getLength(); i++) {
                String varName = list.getDataAt(i);
                Object value = envir.get(varName);
                if (value == null) {
                    throw RError.error(this, RError.Message.UNKNOWN_OBJECT, varName);
                }
                if (value instanceof RPromise && evalPromises) {
                    value = PromiseHelperNode.evaluateSlowPath(frame, (RPromise) value);
                }
                RPairList pl = RDataFactory.createPairList(value);
                pl.setTag(RDataFactory.createSymbol(varName.intern()));
                if (prev == null) {
                    toSave = pl;
                } else {
                    prev.setCdr(pl);
                }
                prev = pl;
            }
            boolean ascii = RRuntime.fromLogical(asciiLogical);
            doSaveConn(toSave, conn, ascii);
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
