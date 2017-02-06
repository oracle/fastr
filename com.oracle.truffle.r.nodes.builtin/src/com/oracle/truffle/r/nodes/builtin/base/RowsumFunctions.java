/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2015,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// Translated from main/unique.c

// TODO rowsum_df
public class RowsumFunctions {

    @RBuiltin(name = "rowsum_matrix", kind = INTERNAL, parameterNames = {"x", "g", "uniqueg", "snarm", "rn"}, behavior = PURE)
    public abstract static class Rowsum extends RBuiltinNode {

        private final ConditionProfile typeProfile = ConditionProfile.createBinaryProfile();
        private final NACheck na = NACheck.create();

        static {
            Casts casts = new Casts(Rowsum.class);
            casts.arg("x").mustBe(integerValue().or(doubleValue()), RError.Message.ROWSUM_NON_NUMERIC);

            casts.arg("g").asVector();

            casts.arg("uniqueg").asVector();

            casts.arg("snarm").asLogicalVector().findFirst().notNA(RError.Message.INVALID_LOGICAL).map(toBoolean());

            casts.arg("rn").mustBe(stringValue(), RError.Message.ROWSUM_NAMES_NOT_CHAR).asStringVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object rowsum(RAbstractVector xv, RAbstractVector g, RAbstractVector uniqueg, boolean narm, RAbstractStringVector rn) {
            int p = xv.isMatrix() ? xv.getDimensions()[1] : 1;
            int n = g.getLength();
            int ng = uniqueg.getLength();
            HashMap<Object, Integer> table = new HashMap<>();
            for (int i = 0; i < ng; i++) {
                // uniqueg has no duplicates (by definition)
                table.put(uniqueg.getDataAtAsObject(i), i);
            }
            int[] matches = new int[n];
            for (int i = 0; i < n; i++) {
                Integer hi = table.get(g.getDataAtAsObject(i));
                matches[i] = xv == null ? 0 : hi + 1;
            }
            int offset = 0;
            int offsetg = 0;

            boolean isInt = xv instanceof RIntVector;
            RVector<?> result;
            na.enable(xv);
            boolean complete = xv.isComplete();

            if (typeProfile.profile(isInt)) {
                RAbstractIntVector xi = (RAbstractIntVector) xv;
                int[] ansi = new int[ng * p];
                for (int i = 0; i < p; i++) {
                    for (int j = 0; j < n; j++) {
                        int midx = matches[j] - 1 + offsetg;
                        int itmp = ansi[midx];
                        if (na.check(xi.getDataAt(j + offset))) {
                            if (!narm) {
                                ansi[midx] = RRuntime.INT_NA;
                                complete = RDataFactory.INCOMPLETE_VECTOR;
                            }
                        } else if (!na.check(itmp)) {
                            long dtmp = itmp;
                            int jtmp = xi.getDataAt(j + offset);
                            dtmp += jtmp;
                            if (dtmp < Integer.MIN_VALUE || dtmp > Integer.MAX_VALUE) {
                                itmp = RRuntime.INT_NA;
                                complete = RDataFactory.INCOMPLETE_VECTOR;
                            } else {
                                itmp += jtmp;
                            }
                            ansi[midx] = itmp;
                        }
                    }
                    offset += n;
                    offsetg += ng;
                }
                result = RDataFactory.createIntVector(ansi, complete, new int[]{ng, p});
            } else {
                RAbstractDoubleVector xd = (RAbstractDoubleVector) xv;
                double[] ansd = new double[ng * p];
                for (int i = 0; i < p; i++) {
                    for (int j = 0; j < n; j++) {
                        int midx = matches[j] - 1 + offsetg;
                        double dtmp = xd.getDataAt(j + offset);
                        if (!narm || !Double.isNaN(dtmp)) {
                            ansd[midx] += dtmp;
                        }
                    }
                    offset += n;
                    offsetg += ng;
                }
                result = RDataFactory.createDoubleVector(ansd, complete, new int[]{ng, p});
            }
            Object[] dimNamesData = new Object[2];
            dimNamesData[0] = rn;
            RList dn2 = xv.materialize().getDimNames();
            if (dn2 != null && dn2.getLength() >= 2 && dn2.getDataAt(1) != RNull.instance) {
                dimNamesData[1] = dn2.getDataAt(1);
            }
            RList dimNames = RDataFactory.createList(dimNamesData);
            result.setDimNames(dimNames);
            return result;
        }
    }
}
