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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

//Transcribed from GnuR, library/utils/src/io.c

//Checkstyle: stop
public class WriteTable {
    private static class WtInfo {
        boolean wasopen;
        RConnection con;
        int savedigits;
    }

    // @formatter: off
    public Object execute(RConnection con, Object xx, int nr, int nc, Object rnames, String csep,
                          String ceol, String cna, char cdec, byte qmethod, boolean[] quoteCol, boolean quoteRn) {
    // @formatter: on
        if (xx instanceof RDataFrame) { /* A data frame */

            /* handle factors internally, check integrity */
            levels = (SEXP *) R_alloc(nc, sizeof(SEXP));
            for (int j = 0; j < nc; j++) {
                xj = VECTOR_ELT(x, j);
                if (LENGTH(xj) != nr)
                    error(
                            _(
                                    "corrupt data frame -- length of column %d does not not match nrows"),
                            j + 1);
                if (inherits(xj, "factor")) {
                    levels[j] = getAttrib(xj, R_LevelsSymbol);
                } else
                    levels[j] = R_NilValue;
            }

            for (int i = 0; i < nr; i++) {
                if (i % 1000 == 999)
                    R_CheckUserInterrupt();
                if (!isNull(rnames))
                    Rconn_printf(con, "%s%s",
                            EncodeElement2(rnames, i, quote_rn, qmethod, &strBuf,
                                    cdec), csep);
                for (int j = 0; j < nc; j++) {
                    xj = VECTOR_ELT(x, j);
                    if (j > 0)
                        Rconn_printf(con, "%s", csep);
                    if (isna(xj, i))
                        tmp = cna;
                    else {
                        if (!isNull(levels[j])) {
                            /* We do not assume factors have integer levels,
                             although they should. */
                            if (TYPEOF(xj) == INTSXP)
                                tmp = EncodeElement2(levels[j], INTEGER(xj)[i] - 1,
                                        quote_col[j], qmethod, &strBuf, cdec);
                            else if (TYPEOF(xj) == REALSXP)
                                tmp = EncodeElement2(levels[j],
                                        (int) (REAL(xj)[i] - 1), quote_col[j],
                                        qmethod, &strBuf, cdec);
                            else
                                error(
                                        _(
                                                "column %s claims to be a factor but does not have numeric codes"),
                                        j + 1);
                        } else {
                            tmp = EncodeElement2(xj, i, quote_col[j], qmethod,
                                    &strBuf, cdec);
                        }
                        /* if(cdec) change_dec(tmp, cdec, TYPEOF(xj)); */
                    }
                    Rconn_printf(con, "%s", tmp);
                }
                Rconn_printf(con, "%s", ceol);
            }

        } else { /* A matrix */

            //if (!isVectorAtomic(x))
            //    UNIMPLEMENTED_TYPE("write.table, matrix method", x);
            RVector x = (RVector) xx;
            /* quick integrity check */
            if (x.getLength() != nr * nc) {
                throw new IllegalArgumentException("corrupt matrix -- dims not not match length");

            for (int i = 0; i < nr; i++) {
                if (i % 1000 == 999) {
                    // R_CheckUserInterrupt();
                }
                if (!(rnames instanceof RNull)) {
                    Rconn_printf(con, "%s%s",
                            EncodeElement2(rnames, i, quote_rn, qmethod, &strBuf,
                                    cdec), csep);
                }
                for (int j = 0; j < nc; j++) {
                    if (j > 0) {
                        Rconn_printf(con, "%s", csep);
                    }
                    if (isna(x, i + j * nr)) {
                        tmp = cna;
                    } else {
                        tmp = EncodeElement2(x, i + j * nr, quote_col[j], qmethod,
                                &strBuf, cdec);
                        /* if(cdec) change_dec(tmp, cdec, TYPEOF(x)); */
                    }
                    Rconn_printf(con, "%s", tmp);
                }
                Rconn_printf(con, "%s", ceol);
            }

        }
        return RNull.instance;

    }
}
