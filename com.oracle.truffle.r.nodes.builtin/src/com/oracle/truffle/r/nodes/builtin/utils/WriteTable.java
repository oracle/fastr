/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.utils;

import java.io.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

//Transcribed from GnuR, library/utils/src/io.c

//Checkstyle: stop
public class WriteTable {
    // @formatter:off
    public static Object execute(RConnection con, Object xx, int nr, int nc, Object rnames, String csep, String ceol, String cna,
                  char cdec, boolean qmethod, boolean[] quoteCol, boolean quoteRn) throws IOException, IllegalArgumentException {
        // @formatter:on
        OutputStream os = con.getOutputStream();
        String tmp = null;
        if (xx instanceof RDataFrame) { /* A data frame */
            RVector xy = ((RDataFrame) xx).getVector();
            RVector x = (RVector) xy.getDataAtAsObject(0);

            /* handle factors internally, check integrity */
            RStringVector[] levels = new RStringVector[nc];
            for (int j = 0; j < nc; j++) {
                RAbstractVector xj = (RAbstractVector) x.getDataAtAsObject(j);
                if (xj.getLength() != nr) {
                    throw new IllegalArgumentException("corrupt data frame -- length of column " + (j + 1) + " does not not match nrows");
                }
                if (isFactor(xj)) {
                    levels[j] = (RStringVector) xj.getAttributes().get("levels");
                }
            }

            for (int i = 0; i < nr; i++) {
                // if (i % 1000 == 999)
                // R_CheckUserInterrupt();
                if (!(rnames instanceof RNull)) {
                    os.write(encodeElement2((RStringVector) rnames, i, quoteRn, qmethod, cdec).getBytes());
                    os.write(csep.getBytes());
                }
                for (int j = 0; j < nc; j++) {
                    RAbstractVector xj = (RAbstractVector) x.getDataAtAsObject(j);
                    if (j > 0)
                        os.write(csep.getBytes());
                    if (isna(xj, i)) {
                        tmp = cna;
                    } else {
                        if (levels[j] != null) {
                            /*
                             * We do not assume factors have integer levels, although they should.
                             */
                            if (xj instanceof RIntVector || xj instanceof RDoubleVector) {
                                tmp = encodeElement2(levels[j], (int) xj.getDataAtAsObject(i) - 1, quoteCol[j], qmethod, cdec);
                            } else {
                                throw new IllegalArgumentException("column " + (j + 1) + " claims to be a factor but does not have numeric codes");
                            }
                        } else {
                            tmp = encodeElement2(xj, i, quoteCol[j], qmethod, cdec);
                        }
                        /* if(cdec) change_dec(tmp, cdec, TYPEOF(xj)); */
                    }
                    os.write(tmp.getBytes());
                }
                os.write(ceol.getBytes());
            }

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
                    os.write(encodeElement2((RStringVector) rnames, i, quoteRn, qmethod, cdec).getBytes());
                    os.write(csep.getBytes());
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

    /* a version of EncodeElement with different escaping of char strings */
    private static String encodeElement2(RAbstractVector x, int indx, boolean quote, boolean qmethod, char cdec) {
        if (indx < 0 || indx >= x.getLength()) {
            throw new IllegalArgumentException("index out of range");
        }
        if (x instanceof RStringVector) {
            RStringVector sx = (RStringVector) x;
            StringBuffer sb = new StringBuffer();
            String p0 = /* translateChar */sx.getDataAt(indx);
            if (!quote)
                return p0;
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
        return encodeElement(x, indx, quote ? '"' : 0, cdec);
    }

    private static boolean isna(RAbstractVector x, int indx) {
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
        throw RInternalError.unimplemented();
    }

    private static boolean isFactor(RAbstractVector v) {
        for (int i = 0; i < v.getClassHierarchy().getLength(); i++) {
            if (v.getClassHierarchy().getDataAt(i).equals("factor")) {
                return true;
            }
        }
        return false;
    }
}
