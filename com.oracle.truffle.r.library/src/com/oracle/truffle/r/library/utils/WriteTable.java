/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.utils;

import java.io.IOException;
import java.io.OutputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RClass;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

//Transcribed from GnuR, library/utils/src/io.c

public final class WriteTable extends RExternalBuiltinNode {

    @TruffleBoundary
    private static Object execute(RConnection con, Object xx, int nr, int nc, Object rnames, String csep, String ceol, String cna, char cdec, boolean qmethod, boolean[] quoteCol, boolean quoteRn)
                    throws IOException, IllegalArgumentException {
        OutputStream os = con.getOutputStream();
        String tmp = null;
        if (RClass.DataFrame.isInstanceOf(xx)) {
            executeDataFrame(os, (RVector) xx, nr, nc, rnames, csep, ceol, cna, cdec, qmethod, quoteCol, quoteRn);
        } else { /* A matrix */

            // if (!isVectorAtomic(x))
            // UNIMPLEMENTED_TYPE("write.table, matrix method", x);
            RVector x = (RVector) xx;
            /* quick integrity check */
            if (x.getLength() != nr * nc) {
                throw new IllegalArgumentException("corrupt matrix -- dims not not match length");
            }

            for (int i = 0; i < nr; i++) {
                if (i % 1000 == 999) {
                    // R_CheckUserInterrupt();
                }
                if (!(rnames instanceof RNull)) {
                    os.write(new StringBuffer(encodeElement2((RStringVector) rnames, i, quoteRn, qmethod, cdec)).append(csep).toString().getBytes());
                }
                for (int j = 0; j < nc; j++) {
                    if (j > 0) {
                        os.write(csep.getBytes());
                    }
                    if (isna(x, i + j * nr)) {
                        tmp = cna;
                    } else {
                        tmp = encodeElement2(x, i + j * nr, quoteCol[j], qmethod, cdec);
                        /* if(cdec) change_dec(tmp, cdec, TYPEOF(x)); */
                    }
                    os.write(tmp.getBytes());
                }
                os.write(ceol.getBytes());
            }

        }
        return RNull.instance;
    }

    private static void executeDataFrame(OutputStream os, RVector x, int nr, int nc, Object rnames, String csep, String ceol, String cna, char cdec, boolean qmethod, boolean[] quoteCol,
                    boolean quoteRn)
                    throws IOException {
        String tmp;

        /* handle factors internally, check integrity */
        RStringVector[] levels = new RStringVector[nc];
        for (int j = 0; j < nc; j++) {
            Object xjObj = x.getDataAtAsObject(j);
            if (xjObj instanceof RAbstractContainer) {
                RAbstractContainer xj = (RAbstractContainer) xjObj;
                if (xj.getLength() != nr) {
                    throw new IllegalArgumentException("corrupt data frame -- length of column " + (j + 1) + " does not not match nrows");
                }
                if (isFactor(xj)) {
                    levels[j] = (RStringVector) xj.getAttributes().get("levels");
                }
            } else {
                if (nr != 1) {
                    throw new IllegalArgumentException("corrupt data frame -- length of column " + (j + 1) + " does not not match nrows");
                }
            }
        }

        for (int i = 0; i < nr; i++) {
            // if (i % 1000 == 999)
            // R_CheckUserInterrupt();
            if (!(rnames instanceof RNull)) {
                tmp = new StringBuffer(encodeElement2((RStringVector) rnames, i, quoteRn, qmethod, cdec)).append(csep).toString();
                os.write(tmp.getBytes());
            }
            for (int j = 0; j < nc; j++) {
                Object xjObj = x.getDataAtAsObject(j);
                if (j > 0) {
                    os.write(csep.getBytes());
                }
                if (xjObj instanceof RAbstractContainer) {
                    RAbstractContainer xj = (RAbstractContainer) xjObj;
                    if (isna(xj, i)) {
                        tmp = cna;
                    } else {
                        if (levels[j] != null) {
                            tmp = encodeElement2(levels[j], (int) xj.getDataAtAsObject(i) - 1, quoteCol[j], qmethod, cdec);
                        } else {
                            tmp = encodeElement2((RAbstractVector) xj, i, quoteCol[j], qmethod, cdec);
                        }
                        /* if(cdec) change_dec(tmp, cdec, TYPEOF(xj)); */
                    }
                } else {
                    tmp = encodePrimitiveElement(xjObj, cna, quoteRn, qmethod);
                    /* if(cdec) change_dec(tmp, cdec, TYPEOF(xj)); */
                }
                os.write(tmp.getBytes());
            }
            os.write(ceol.getBytes());
        }
    }

    private static String encodeStringElement(String p0, boolean quote, boolean qmethod) {
        if (!quote) {
            return p0;
        }
        StringBuffer sb = new StringBuffer();
        sb.append('"');
        for (int i = 0; i < p0.length(); i++) {
            char p = p0.charAt(i);
            if (p == '"') {
                sb.append(qmethod ? '\\' : '"');
            }
            sb.append(p);
        }
        sb.append('"');
        return sb.toString();
    }

    /* a version of EncodeElement with different escaping of char strings */
    private static String encodeElement2(RAbstractVector x, int indx, boolean quote, boolean qmethod, char cdec) {
        if (indx < 0 || indx >= x.getLength()) {
            throw new IllegalArgumentException("index out of range");
        }
        if (x instanceof RStringVector) {
            RStringVector sx = (RStringVector) x;
            String p0 = /* translateChar */sx.getDataAt(indx);
            return encodeStringElement(p0, quote, qmethod);
        }
        return encodeElement(x, indx, quote ? '"' : 0, cdec);
    }

    private static String encodePrimitiveElement(Object o, String cna, boolean quote, boolean qmethod) {
        if (o instanceof Integer) {
            int v = (int) o;
            return RRuntime.isNA(v) ? cna : RRuntime.intToStringNoCheck(v);
        } else if (o instanceof Double) {
            double v = (double) o;
            return RRuntime.isNA(v) ? cna : RRuntime.doubleToStringNoCheck(v);
        } else if (o instanceof Byte) {
            byte v = (byte) o;
            return RRuntime.isNA(v) ? cna : RRuntime.logicalToStringNoCheck(v);
        } else if (o instanceof String) {
            String v = (String) o;
            return RRuntime.isNA(v) ? cna : encodeStringElement(v, quote, qmethod);
        } else if (o instanceof Double) {
            RComplex v = (RComplex) o;
            return RRuntime.isNA(v) ? cna : RRuntime.complexToStringNoCheck(v);
        } else if (o instanceof RRaw) {
            RRaw v = (RRaw) o;
            return RRuntime.rawToString(v);
        }
        throw RInternalError.unimplemented();
    }

    private static boolean isna(RAbstractContainer x, int indx) {
        if (x instanceof RLogicalVector) {
            return RRuntime.isNA(((RLogicalVector) x).getDataAt(indx));
        } else if (x instanceof RDoubleVector) {
            return RRuntime.isNA(((RDoubleVector) x).getDataAt(indx));
        } else if (x instanceof RIntVector) {
            return RRuntime.isNA(((RIntVector) x).getDataAt(indx));
        } else if (x instanceof RStringVector) {
            return RRuntime.isNA(((RStringVector) x).getDataAt(indx));
        } else if (x instanceof RComplexVector) {
            RComplexVector cvec = (RComplexVector) x;
            RComplex c = cvec.getDataAt(indx);
            return c.isNA();
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private static String encodeElement(Object x, int indx, char quote, char dec) {
        if (x instanceof RAbstractDoubleVector) {
            RAbstractDoubleVector v = (RAbstractDoubleVector) x;
            return RRuntime.doubleToString(v.getDataAt(indx));
        }
        if (x instanceof RAbstractIntVector) {
            RAbstractIntVector v = (RAbstractIntVector) x;
            return RRuntime.intToString(v.getDataAt(indx));
        }
        if (x instanceof RAbstractLogicalVector) {
            RAbstractLogicalVector v = (RAbstractLogicalVector) x;
            return RRuntime.logicalToString(v.getDataAt(indx));
        }
        if (x instanceof RAbstractComplexVector) {
            RAbstractComplexVector v = (RAbstractComplexVector) x;
            return RRuntime.complexToString(v.getDataAt(indx));
        }
        if (x instanceof RAbstractRawVector) {
            RAbstractRawVector v = (RAbstractRawVector) x;
            return RRuntime.rawToString(v.getDataAt(indx));
        }
        throw RInternalError.unimplemented();
    }

    private static boolean isFactor(RAbstractContainer v) {
        for (int i = 0; i < v.getClassHierarchy().getLength(); i++) {
            if (v.getClassHierarchy().getDataAt(i).equals("factor")) {
                return true;
            }
        }
        return false;
    }

    private void invalidArgument(String name) throws RError {
        errorProfile.enter();
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, name);
    }

    // Transcribed from GnuR, library/utils/src/io.c
    @Override
    public Object call(RArgsValuesAndNames args) {
        Object[] argValues = args.getArguments();
        Object conArg = argValues[1];
        RConnection conn;
        if (!(conArg instanceof RConnection)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.GENERIC, "'file' is not a connection");
        } else {
            conn = (RConnection) conArg;
        }
        // TODO check connection writeable

        int nr = castInt(castVector(argValues[2]));
        int nc = castInt(castVector(argValues[3]));
        Object rnamesArg = argValues[4];
        Object sepArg = argValues[5];
        Object eolArg = argValues[6];
        Object naArg = argValues[7];
        Object decArg = argValues[8];
        Object quoteArg = argValues[9];
        byte qmethod = castLogical(castVector(argValues[10]));

        String csep;
        String ceol;
        String cna;
        String cdec;

        if (nr == RRuntime.INT_NA) {
            invalidArgument("nr");
        }
        if (nc == RRuntime.INT_NA) {
            invalidArgument("nc");
        }
        if (!(rnamesArg instanceof RNull) && isString(rnamesArg) == null) {
            invalidArgument("rnames");
        }
        if ((csep = isString(sepArg)) == null) {
            invalidArgument("sep");
        }
        if ((ceol = isString(eolArg)) == null) {
            invalidArgument("eol");
        }
        if ((cna = isString(naArg)) == null) {
            invalidArgument("na");
        }
        if ((cdec = isString(decArg)) == null) {
            invalidArgument("dec");
        }
        if (qmethod == RRuntime.LOGICAL_NA) {
            invalidArgument("qmethod");
        }
        if (cdec.length() != 1) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.GENERIC, "'dec' must be a single character");
        }
        boolean[] quoteCol = new boolean[nc];
        boolean quoteRn = false;
        RAbstractIntVector quote = (RAbstractIntVector) castVector(quoteArg);
        for (int i = 0; i < quote.getLength(); i++) {
            int qi = quote.getDataAt(i);
            if (qi == 0) {
                quoteRn = true;
            }
            if (qi > 0) {
                quoteCol[qi - 1] = true;
            }
        }
        try (RConnection openConn = conn.forceOpen("wt")) {
            execute(openConn, argValues[0], nr, nc, rnamesArg, csep, ceol, cna, cdec.charAt(0), RRuntime.fromLogical(qmethod), quoteCol, quoteRn);
        } catch (IOException | IllegalArgumentException ex) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }
        return RNull.instance;
    }
}
