/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@ImportStatic(RType.class)
@RBuiltin(name = "mean", kind = INTERNAL, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE_SUMMARY)
public abstract class Mean extends RBuiltinNode.Arg1 {

    static {
        Casts.noCasts(Mean.class);
    }

    @Specialization(guards = {"access.supports(x)", "access.getType() != Complex"})
    protected double meanDoubleCached(RAbstractVector x,
                    @Cached("x.access()") VectorAccess access,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        try (SequentialIterator iter = access.access(x)) {
            if (emptyProfile.profile(!access.next(iter))) {
                return Double.NaN;
            }
            double sum = 0;
            do {
                double value = access.getDouble(iter);
                if (access.na.checkNAorNaN(value)) {
                    return value;
                }
                sum += value;
            } while (access.next(iter));
            return sum / access.getLength(iter);
        }
    }

    @Specialization(replaces = "meanDoubleCached", guards = "x.getRType() != Complex")
    protected double meanDoubleGeneric(RAbstractVector x,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        return meanDoubleCached(x, x.slowPathAccess(), emptyProfile);
    }

    @Specialization(guards = {"access.supports(x)", "access.getType() == Complex"})
    protected RComplex meanComplexCached(RAbstractVector x,
                    @Cached("x.access()") VectorAccess access,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        try (SequentialIterator iter = access.access(x)) {
            if (emptyProfile.profile(!access.next(iter))) {
                return RComplex.valueOf(Double.NaN, Double.NaN);
            }
            double sumR = 0;
            double sumI = 0;
            do {
                double valueR = access.getComplexR(iter);
                double valueI = access.getComplexI(iter);
                if (access.na.check(valueR, valueI)) {
                    return RComplex.valueOf(valueR, valueI);
                }
                sumR += valueR;
                sumI += valueI;
            } while (access.next(iter));
            int length = access.getLength(iter);
            return RComplex.valueOf(sumR / length, sumI / length);
        }
    }

    @Specialization(replaces = "meanComplexCached", guards = "x.getRType() == Complex")
    protected RComplex meanComplexGeneric(RAbstractVector x,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        return meanComplexCached(x, x.slowPathAccess(), emptyProfile);
    }
}
