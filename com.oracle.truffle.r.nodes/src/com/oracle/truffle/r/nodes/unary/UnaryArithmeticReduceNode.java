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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.BinaryArithmeticFactory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * This node is used at several places, but only 'range' actually uses the 'finite' parameter,
 * others should typically use {@code false} as its value. The 'finite' parameter is not handled
 * consistently in GnuR: the documentation reads ‘finite = TRUE’ _includes_ ‘na.rm = TRUE’, but this
 * only applies to some types (e.g. double or integer), for other types 'finite' seems to be ignored
 * (e.g. logical). The only situation where semantics of finite is different to na.rm is double
 * values: na.rm removes NA and NaN, but not -/+Inf.
 *
 * FastR handles finite consistently (setting na.rm = TRUE if finite = TRUE) in the range builtin.
 */
@ImportStatic({RRuntime.class})
@TypeSystemReference(RTypes.class)
public abstract class UnaryArithmeticReduceNode extends RBaseNode {

    public abstract Object executeReduce(Object value, boolean naRm, boolean finite);

    @Child private BinaryArithmetic arithmetic;

    private final BinaryArithmeticFactory factory;
    protected final ReduceSemantics semantics;
    protected final boolean supportString;
    protected final boolean supportComplex;

    private final NACheck na = NACheck.create();
    private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile emptyProfile = BranchProfile.create();
    private final BranchProfile naResultProfile = BranchProfile.create();

    protected UnaryArithmeticReduceNode(ReduceSemantics semantics, BinaryArithmeticFactory factory) {
        this.factory = factory;
        this.semantics = semantics;
        this.arithmetic = factory.createOperation();
        this.supportString = semantics.supportString;
        this.supportComplex = semantics.supportComplex;
    }

    private void emptyWarning() {
        emptyProfile.enter();
        if (semantics.getEmptyWarning() != null) {
            warning(semantics.emptyWarning);
        }
    }

    private void naResultWarning() {
        naResultProfile.enter();
        if (semantics.getNAResultWarning() != null) {
            warning(semantics.getNAResultWarning());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "semantics.isNullInt()")
    protected int doInt(RNull operand, boolean naRm, boolean finite) {
        emptyWarning();
        return semantics.getIntStart();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!semantics.isNullInt()")
    protected double doDouble(RNull operand, boolean naRm, boolean finite) {
        emptyWarning();
        return semantics.getDoubleStart();
    }

    @Specialization
    protected int doInt(int operand, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        na.enable(operand);
        if (naRmProfile.profile(naRm)) {
            if (na.check(operand)) {
                emptyWarning();
                return semantics.getIntStart();
            } else {
                return operand;
            }
        } else {
            return na.check(operand) ? RRuntime.INT_NA : operand;
        }
    }

    @Specialization
    protected double doDouble(double operand, boolean naRm, boolean finite,
                    @Cached("createBinaryProfile()") ConditionProfile finiteProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isInfiniteProfile) {
        if (finiteProfile.profile(finite) && isInfiniteProfile.profile(!RRuntime.isFinite(operand))) {
            emptyWarning();
            return semantics.getIntStart();
        }
        na.enable(operand);
        if (naRmProfile.profile(naRm)) {
            if (na.checkNAorNaN(operand)) {
                // the only value we have should be removed...
                emptyWarning();
                return semantics.getIntStart();
            } else {
                // known not to be NA or NaN
                return operand;
            }
        }
        // since !naRm and !finite, NaN or +/-Inf can be valid results
        return na.check(operand) ? RRuntime.DOUBLE_NA : operand;
    }

    @Specialization
    protected int doLogical(byte operand, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        na.enable(operand);
        if (naRmProfile.profile(naRm)) {
            if (na.check(operand)) {
                emptyWarning();
                return semantics.getIntStart();
            } else {
                return operand;
            }
        } else {
            return na.check(operand) ? RRuntime.INT_NA : operand;
        }
    }

    @Specialization(guards = "supportComplex")
    protected RComplex doComplex(RComplex operand, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        na.enable(operand);
        if (naRmProfile.profile(naRm)) {
            if (na.check(operand)) {
                emptyWarning();
                return RRuntime.double2complex(semantics.getDoubleStart());
            } else {
                return operand;
            }
        } else {
            return na.check(operand) ? RComplex.createNA() : operand;
        }
    }

    @Specialization(guards = "supportString")
    protected String doString(String operand, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        na.enable(operand);
        if (naRmProfile.profile(naRm)) {
            if (na.check(operand)) {
                if (semantics.getEmptyWarning() != null) {
                    RError.warning(this, semantics.emptyWarningCharacter);
                }
                return semantics.getStringStart();
            } else {
                return operand;
            }
        } else {
            return na.check(operand) ? RRuntime.STRING_NA : operand;
        }
    }

    private Object doInt(RAbstractVector vector, boolean naRm, VectorAccess access) {
        boolean profiledNaRm = naRmProfile.profile(naRm);
        int result = semantics.getIntStart();
        boolean empty = true;
        try (VectorAccess.SequentialIterator iter = access.access(vector)) {
            while (access.next(iter)) {
                int d;
                switch (access.getType()) {
                    case Integer:
                        d = access.getInt(iter);
                        if (access.na.check(d)) {
                            if (profiledNaRm) {
                                continue;
                            } else {
                                return RRuntime.INT_NA;
                            }
                        }
                        break;
                    case Logical:
                        byte logical = access.getLogical(iter);
                        if (access.na.check(logical)) {
                            if (profiledNaRm) {
                                continue;
                            } else {
                                return RRuntime.INT_NA;
                            }
                        }
                        d = logical; // 0 or 1
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
                result = arithmetic.op(result, d);
                if (RRuntime.isNA(result)) {
                    naResultWarning();
                    return RRuntime.INT_NA;
                }
                empty = false;
            }
        }
        if (empty) {
            emptyWarning();
            if (semantics.isUseDoubleStartForEmptyVector()) {
                return semantics.getDoubleStart();
            }
        }
        return result;
    }

    @Specialization(guards = "access.supports(vector)")
    protected Object doIntCached(RAbstractIntVector vector, boolean naRm, @SuppressWarnings("unused") boolean finite,
                    @Cached("vector.access()") VectorAccess access) {
        return doInt(vector, naRm, access);
    }

    @Specialization(replaces = "doIntCached")
    protected Object doIntGeneric(RAbstractIntVector vector, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        return doInt(vector, naRm, vector.slowPathAccess());
    }

    @Specialization(guards = "access.supports(vector)")
    protected Object doLogicalCached(RAbstractLogicalVector vector, boolean naRm, @SuppressWarnings("unused") boolean finite,
                    @Cached("vector.access()") VectorAccess access) {
        return doInt(vector, naRm, access);
    }

    @Specialization(replaces = "doIntCached")
    protected Object doLogicalGeneric(RAbstractLogicalVector vector, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        return doInt(vector, naRm, vector.slowPathAccess());
    }

    private double doDouble(RAbstractDoubleVector vector, boolean naRm, boolean finite, ConditionProfile finiteProfile, ConditionProfile isInfiniteProfile, VectorAccess access) {
        boolean profiledNaRm = naRmProfile.profile(naRm);
        boolean profiledFinite = finiteProfile.profile(finite);
        double result = semantics.getDoubleStart();
        boolean empty = true;
        try (VectorAccess.SequentialIterator iter = access.access(vector)) {
            while (access.next(iter)) {
                double d = access.getDouble(iter);
                if (access.na.checkNAorNaN(d)) {
                    if (profiledNaRm) {
                        continue;   // ignore NA/NaN
                    } else if (access.na.check(d)) {
                        // NA produces NA directly, but NaN should be handled by arithmetics.op to
                        // produce NaN. We cannot directly return NaN because if we encounter NA
                        // later
                        // on, we should return NA not NaN
                        return RRuntime.DOUBLE_NA;
                    }
                } else if (profiledFinite && isInfiniteProfile.profile(!RRuntime.isFinite(d))) {
                    // ignore -/+Inf if 'infinite == TRUE'
                    continue;
                }
                result = arithmetic.op(result, d);
                empty = false;
            }
        }
        if (empty) {
            emptyWarning();
        }
        return result;
    }

    @Specialization(guards = "access.supports(vector)")
    protected double doDoubleCached(RAbstractDoubleVector vector, boolean naRm, boolean finite,
                    @Cached("createBinaryProfile()") ConditionProfile finiteProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isInfiniteProfile,
                    @Cached("vector.access()") VectorAccess access) {
        return doDouble(vector, naRm, finite, finiteProfile, isInfiniteProfile, access);
    }

    @Specialization(replaces = "doDoubleCached")
    protected double doDoubleGeneric(RAbstractDoubleVector vector, boolean naRm, boolean finite,
                    @Cached("createBinaryProfile()") ConditionProfile finiteProfile,
                    @Cached("createBinaryProfile()") ConditionProfile isInfiniteProfile) {
        return doDouble(vector, naRm, finite, finiteProfile, isInfiniteProfile, vector.slowPathAccess());
    }

    @Specialization(guards = "supportComplex")
    protected RComplex doComplexVector(RComplexVector operand, boolean naRm, @SuppressWarnings("unused") boolean finite) {
        RBaseNode.reportWork(this, operand.getLength());
        boolean profiledNaRm = naRmProfile.profile(naRm);
        RComplex result = RRuntime.double2complex(semantics.getDoubleStart());
        int opCount = 0;
        na.enable(operand);
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex current = operand.getDataAt(i);
            if (na.check(current)) {
                if (profiledNaRm) {
                    continue;
                } else {
                    return RComplex.createNA();
                }
            } else {
                result = arithmetic.op(result.getRealPart(), result.getImaginaryPart(), current.getRealPart(), current.getImaginaryPart());
            }
            opCount++;
        }
        if (opCount == 0) {
            emptyWarning();
        }
        return result;
    }

    // the algorithm that works for other types (reducing a vector starting with the "start value")
    // does not work for String-s as, in particular, we cannot supply the (lexicographically)
    // "largest" String for the implementation of max function

    @Specialization(guards = {"supportString", "operand.getLength() == 0"})
    protected String doStringVectorEmpty(@SuppressWarnings("unused") RStringVector operand, @SuppressWarnings("unused") boolean naRm, @SuppressWarnings("unused") boolean finite) {
        if (semantics.getEmptyWarning() != null) {
            warning(semantics.emptyWarningCharacter);
        }
        return semantics.getStringStart();
    }

    @Specialization(guards = {"supportString", "operand.getLength() == 1"})
    protected String doStringVectorOneElem(RStringVector operand, boolean naRm, boolean finite) {
        boolean profiledNaRm = naRmProfile.profile(naRm);
        String result = operand.getDataAt(0);
        if (profiledNaRm) {
            na.enable(result);
            if (na.check(result)) {
                return doStringVectorEmpty(operand, naRm, finite);
            }
        }
        return result;
    }

    @Specialization(guards = {"supportString", "operand.getLength() > 1"})
    protected String doStringVector(RStringVector operand, boolean naRm, boolean finite) {
        boolean profiledNaRm = naRmProfile.profile(naRm);
        na.enable(operand);
        int offset = 0;
        String result = operand.getDataAt(offset);
        if (profiledNaRm) {
            while (na.check(result)) {
                // the following is meant to eliminate leading NA-s
                if (offset == operand.getLength() - 1) {
                    // last element - all other are NAs
                    return doStringVectorEmpty(operand, naRm, finite);
                }
                result = operand.getDataAt(++offset);
            }
        } else {
            if (na.check(result)) {
                return result;
            }
        }
        // when we reach here, it means that we have already seen one non-NA element
        assert !RRuntime.isNA(result);
        for (int i = offset + 1; i < operand.getLength(); i++) {
            String current = operand.getDataAt(i);
            if (na.check(current)) {
                if (profiledNaRm) {
                    // skip NA-s
                    continue;
                } else {
                    return RRuntime.STRING_NA;
                }
            } else {
                result = arithmetic.op(result, current);
            }
        }
        return result;
    }

    @Specialization(guards = {"isForeignObject(obj)"})
    protected Object doForeignVector(TruffleObject obj, boolean naRm, boolean finite,
                    @Cached("create()") ForeignArray2R foreignArray2R,
                    @Cached("createRecursive()") UnaryArithmeticReduceNode recursive) {
        if (foreignArray2R.isForeignVector(obj)) {
            Object vec = foreignArray2R.convert(obj);
            return recursive.executeReduce(vec, naRm, finite);
        }
        return doFallback(obj, naRm, finite);
    }

    @Fallback
    protected Object doFallback(Object obj, @SuppressWarnings("unused") boolean naRm, @SuppressWarnings("unused") boolean infinite) {
        throw error(RError.Message.INVALID_TYPE_ARGUMENT, Predef.typeName().apply(obj));
    }

    protected UnaryArithmeticReduceNode createRecursive() {
        return UnaryArithmeticReduceNodeGen.create(semantics, factory);
    }

    public static final class ReduceSemantics {

        private final int intStart;
        private final double doubleStart;
        private final String stringStart = RRuntime.STRING_NA; // does not seem to change
        private final boolean nullInt;
        private final RError.Message emptyWarning;
        private final RError.Message emptyWarningCharacter;
        private final RError.Message naResultWarning;
        private final boolean supportComplex;
        private final boolean supportString;
        private final boolean useDoubleStartForEmptyVector;

        public ReduceSemantics(int intStart, double doubleStart, boolean nullInt, RError.Message emptyWarning, RError.Message emptyWarningCharacter, RError.Message naResultWarning,
                        boolean supportComplex, boolean supportString, boolean useDoubleStartForEmptyVector) {
            this.intStart = intStart;
            this.doubleStart = doubleStart;
            this.nullInt = nullInt;
            this.emptyWarning = emptyWarning;
            this.emptyWarningCharacter = emptyWarningCharacter;
            this.naResultWarning = naResultWarning;
            this.supportComplex = supportComplex;
            this.supportString = supportString;
            this.useDoubleStartForEmptyVector = useDoubleStartForEmptyVector;
        }

        public int getIntStart() {
            return intStart;
        }

        public double getDoubleStart() {
            return doubleStart;
        }

        public String getStringStart() {
            return stringStart;
        }

        public boolean isNullInt() {
            return nullInt;
        }

        public RError.Message getEmptyWarning() {
            return emptyWarning;
        }

        public RError.Message getEmptyWarningCharacter() {
            return emptyWarningCharacter;
        }

        public RError.Message getNAResultWarning() {
            return naResultWarning;
        }

        public boolean isUseDoubleStartForEmptyVector() {
            return useDoubleStartForEmptyVector;
        }
    }
}
