/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ComplexVectorMetrics;
import com.oracle.truffle.r.nodes.builtin.base.printer.ComplexVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorMetrics;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.PrintParameters;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

/**
 * Information is returned on how format() would be formatted.
 */
@RBuiltin(name = "format.info", kind = INTERNAL, parameterNames = {"x", "digits", "nsmall"}, behavior = PURE)
public abstract class FormatInfo extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(FormatInfo.class);
        casts.arg(0).mustBe(abstractVectorValue(), Message.ATOMIC_VECTOR_ARGUMENTS_ONLY);
        casts.arg(1).mapNull(constant(-1)).mustNotBeNA(Message.INVALID_ARGUMENT, "digits").asIntegerVector().findFirst().mustBe(gte(0).or(lte(22)));
        casts.arg(2).mustNotBeNA(Message.INVALID_ARGUMENT, "nsmall").asIntegerVector().findFirst().mustBe(gte(0).or(lte(20)));
    }

    @Specialization
    protected int doInt(int n, @SuppressWarnings("unused") int digits, @SuppressWarnings("unused") int nsmall) {
        return (n == RRuntime.INT_NA) ? 2 : intLength(n);
    }

    @Specialization
    protected int doString(String s, @SuppressWarnings("unused") int digits, @SuppressWarnings("unused") int nsmall) {
        return s.length();
    }

    @Specialization(guards = "vAccess.supports(v)", limit = "getVectorAccessCacheSize()")
    protected Object doVector(RAbstractVector v, int digits, int nsmall,
                    @Cached("v.access()") VectorAccess vAccess,
                    @Cached("create()") VectorFactory factory) {

        switch (vAccess.getType()) {
            case Integer: {
                SequentialIterator vIter = vAccess.access(v);
                boolean naFound = false;
                if (!vAccess.next(vIter)) {
                    return 1;
                }
                int vMin = vAccess.getInt(vIter);
                if (vAccess.na.check(vMin)) {
                    vMin = 0; // Anything <= "NA".length()
                    naFound = true;
                }
                int vMax = vMin;
                while (vAccess.next(vIter)) {
                    int n = vAccess.getInt(vIter);
                    if (!vAccess.na.check(n)) {
                        vMin = Math.min(vMin, n);
                        vMax = Math.max(vMax, n);
                    } else {
                        naFound = true;
                    }
                }
                int result;
                if (vMin > 0) {
                    result = nonNegativeIntLength(vMax);
                } else if (vMax < 0) {
                    result = intLength(vMin);
                } else {
                    result = intLength(vMax);
                    if (vMin != vMax) {
                        result = Math.max(result, intLength(vMin));
                    }
                }
                if (naFound) {
                    result = Math.max(result, 2);
                }
                return result;
            }

            case Double: {
                PrintParameters pp = new PrintParameters();
                pp.setDefaults();
                if (digits != -1) {
                    pp.setDigits(digits);
                }
                RandomIterator vIter = vAccess.randomAccess(v);
                DoubleVectorMetrics dvm = DoubleVectorPrinter.formatDoubleVector(vIter, vAccess, 0, vAccess.getLength(vIter), nsmall, pp.getDigits(), pp.getScipen(), pp.getNaWidth());
                return factory.createIntVector(new int[]{dvm.getAdjustedMaxWidth(), dvm.d, dvm.e}, true);
            }

            case Complex: {
                PrintParameters pp = new PrintParameters();
                pp.setDefaults();
                if (digits != -1) {
                    pp.setDigits(digits);
                }
                RandomIterator vIter = vAccess.randomAccess(v);
                ComplexVectorMetrics cvm = ComplexVectorPrinter.formatComplexVector(vIter, vAccess, 0, vAccess.getLength(vIter), nsmall, pp.getDigits(), pp.getScipen(), pp.getNaWidth());
                return factory.createIntVector(new int[]{cvm.wr, cvm.dr, cvm.er, cvm.wi, cvm.di, cvm.ei}, true);
            }

            case Logical: {
                SequentialIterator vIter = vAccess.access(v);
                int vMaxLen = 0;
                while (vAccess.next(vIter)) {
                    byte b = vAccess.getLogical(vIter);
                    if (!vAccess.na.check(b)) {
                        vMaxLen = Math.max(vMaxLen, (b == RRuntime.LOGICAL_TRUE) ? 4 : 5);
                    } else {
                        vMaxLen = Math.max(vMaxLen, 2);
                    }
                }
                return vMaxLen;
            }

            case Raw: {
                return 2;
            }

            case Character: {
                SequentialIterator vIter = vAccess.access(v);
                int vMaxLen = 0;
                while (vAccess.next(vIter)) {
                    String s = vAccess.getString(vIter);
                    if (!vAccess.na.check(s)) {
                        vMaxLen = Math.max(vMaxLen, s.length());
                    }
                }
                return vMaxLen;
            }

            default:
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("Unhandled type " + vAccess.getType());
        }
    }

    @Specialization(replaces = "doVector")
    protected Object doVectorGeneric(RAbstractVector v, int digits, int nsmall,
                    @Cached("create()") VectorFactory factory) {
        return doVector(v, digits, nsmall, v.slowPathAccess(), factory);
    }

    private static int intLength(int n) {
        return (n < 0) ? (1 + nonNegativeIntLength(n)) : nonNegativeIntLength(n);
    }

    private static int nonNegativeIntLength(int n) {
        // Avoid series of div operations which may be costly on some platforms
        return (n < 10000)
                        ? ((n < 100) ? ((n < 10) ? 1 : 2) : ((n < 1000) ? 3 : 4))
                        : ((n < 1000000) ? ((n < 100000) ? 5 : 6) : ((n < 100000000) ? ((n < 10000000) ? 7 : 8) : ((n < 1000000000) ? 9 : 10)));
        // int result = 1;
        // while (n > 9) {
        // result++;
        // n /= 10;
        // }
        // return result;
    }

}
