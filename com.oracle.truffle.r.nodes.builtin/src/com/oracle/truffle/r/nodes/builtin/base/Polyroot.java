/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 2000-2016, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.CPolyrootNode;

@RBuiltin(name = "polyroot", kind = INTERNAL, parameterNames = {"z"}, behavior = PURE_ARITHMETIC)
public abstract class Polyroot extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(Polyroot.class);
        casts.arg(0).mustBe(complexValue().or(numericValue()), RError.Message.UNIMPLEMENTED_TYPE_IN_FUNCTION, typeName(), "polyroot").asComplexVector();
    }

    @Specialization(guards = "vAccess.supports(v)", limit = "getVectorAccessCacheSize()")
    protected RComplexVector polyroot(RComplexVector v,
                    @Cached("v.access()") VectorAccess vAccess,
                    @Cached("create()") CPolyrootNode cpolyrootNode) {
        int degree = 0;
        int n;
        SequentialIterator vIter = vAccess.access(v);
        while (vAccess.next(vIter)) {
            if (vAccess.getComplexR(vIter) != 0 || vAccess.getComplexI(vIter) != 0) {
                degree = vIter.getIndex();
            }
        }
        n = degree + 1;
        RComplexVector result;
        if (degree >= 1) {
            double[] rr = new double[n];
            double[] ri = new double[n];
            double[] zr = new double[n];
            double[] zi = new double[n];
            vIter = vAccess.access(v);
            vAccess.next(vIter);
            do {
                if (!RRuntime.isFinite(vAccess.getComplexR(vIter)) || !RRuntime.isFinite(vAccess.getComplexI(vIter))) {
                    throw error(RError.Message.INVALID_POLYNOMIAL_COEFFICIENT);
                }
                zr[degree - vIter.getIndex()] = vAccess.getComplexR(vIter);
                zi[degree - vIter.getIndex()] = vAccess.getComplexI(vIter);
                vAccess.next(vIter);
            } while (vIter.getIndex() < n);
            int fail = cpolyrootNode.cpolyroot(zr, zi, degree, rr, ri);
            if (fail != 0) {
                throw error(RError.Message.ROOT_FINDING_FAILED);
            }
            double[] resultData = new double[degree << 1];
            for (int i = 0, j = 0; i < degree; i++) {
                resultData[j++] = rr[i];
                resultData[j++] = ri[i];
            }
            result = RDataFactory.createComplexVector(resultData, true);
        } else {
            result = RDataFactory.createEmptyComplexVector();
        }
        return result;
    }

    @Specialization(replaces = "polyroot")
    protected RComplexVector polyrootGeneric(RComplexVector v,
                    @Cached("create()") CPolyrootNode cpolyrootNode) {
        return polyroot(v, v.slowPathAccess(), cpolyrootNode);
    }

}
