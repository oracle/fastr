/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen.MultiElemStringHandlerNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@TypeSystemReference(RTypes.class)
public abstract class UnaryArithmeticReduceNode extends Node {

    public abstract Object executeReduce(Object value, Object naRm);

    @Child private MultiElemStringHandler stringHandler;

    private final BinaryArithmeticFactory factory;

    @Child private BinaryArithmetic arithmetic;

    protected final ReduceSemantics semantics;

    private final NACheck na = NACheck.create();

    private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile warningProfile = BranchProfile.create();

    protected UnaryArithmeticReduceNode(ReduceSemantics semantics, BinaryArithmeticFactory factory) {
        this.factory = factory;
        this.semantics = semantics;
        this.arithmetic = factory.create();
    }

    protected UnaryArithmeticReduceNode(UnaryArithmeticReduceNode op) {
        // we recreate the arithmetic each time this specialization specializes
        // it also makes sense for polymorphic variations of this node
        this(op.semantics, op.factory);
    }

    private String handleString(RStringVector operand, byte naRm, int offset) {
        if (stringHandler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na));
        }
        return stringHandler.executeString(operand, naRm, offset);
    }

    private void emptyWarning() {
        if (semantics.getEmptyWarning() != null) {
            warningProfile.enter();
            RError.warning(semantics.emptyWarning);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "semantics.isNullInt()")
    protected int doInt(RNull operand, byte naRm) {
        emptyWarning();
        return semantics.getIntStart();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!semantics.isNullInt()")
    protected double doDouble(RNull operand, byte naRm) {
        emptyWarning();
        return semantics.getDoubleStart();
    }

    @Specialization
    protected int doInt(int operand, byte naRm) {
        na.enable(operand);
        if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
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
    protected double doDouble(double operand, byte naRm) {
        na.enable(operand);
        if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
            if (na.check(operand)) {
                emptyWarning();
                return semantics.getIntStart();
            } else {
                return operand;
            }
        } else {
            return na.check(operand) ? RRuntime.DOUBLE_NA : operand;
        }
    }

    @Specialization
    protected int doLogical(byte operand, byte naRm) {
        na.enable(operand);
        if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
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
    protected RComplex doComplex(RComplex operand, byte naRm) {
        if (semantics.supportComplex) {
            na.enable(operand);
            if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
                if (na.check(operand)) {
                    emptyWarning();
                    return RRuntime.double2complex(semantics.getDoubleStart());
                } else {
                    return operand;
                }
            } else {
                return na.check(operand) ? RRuntime.createComplexNA() : operand;
            }
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "complex");
        }
    }

    @Specialization
    protected String doString(String operand, byte naRm) {
        if (semantics.supportString) {
            na.enable(operand);
            if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
                if (na.check(operand)) {
                    if (semantics.getEmptyWarning() != null) {
                        RError.warning(semantics.emptyWarningCharacter);
                    }
                    return semantics.getStringStart();
                } else {
                    return operand;
                }
            } else {
                return na.check(operand) ? RRuntime.STRING_NA : operand;
            }
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RRaw doString(RRaw operand, byte naRm) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "raw");
    }

    @Specialization
    protected int doIntVector(RIntVector operand, byte naRm) {
        boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
        int result = semantics.getIntStart();
        na.enable(operand);
        int opCount = 0;
        int[] data = operand.getDataWithoutCopying();
        for (int i = 0; i < operand.getLength(); i++) {
            int d = data[i];
            if (na.check(d)) {
                if (profiledNaRm) {
                    continue;
                } else {
                    return RRuntime.INT_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0) {
            emptyWarning();
        }
        return result;
    }

    @Specialization
    protected double doDoubleVector(RDoubleVector operand, byte naRm) {
        boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
        double result = semantics.getDoubleStart();
        na.enable(operand);
        int opCount = 0;
        double[] data = operand.getDataWithoutCopying();
        for (int i = 0; i < operand.getLength(); i++) {
            double d = data[i];
            if (na.check(d)) {
                if (profiledNaRm) {
                    continue;
                } else {
                    return RRuntime.DOUBLE_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0) {
            emptyWarning();
        }
        return result;
    }

    @Specialization
    protected int doLogicalVector(RLogicalVector operand, byte naRm) {
        boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
        int result = semantics.getIntStart();
        na.enable(operand);
        int opCount = 0;
        byte[] data = operand.getDataWithoutCopying();
        for (int i = 0; i < operand.getLength(); i++) {
            byte d = data[i];
            if (na.check(d)) {
                if (profiledNaRm) {
                    continue;
                } else {
                    return RRuntime.INT_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0) {
            emptyWarning();
        }
        return result;
    }

    @Specialization
    protected int doIntSequence(RIntSequence operand, @SuppressWarnings("unused") byte naRm) {
        int result = semantics.getIntStart();
        int current = operand.getStart();
        for (int i = 0; i < operand.getLength(); i++) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        if (operand.getLength() == 0) {
            emptyWarning();
        }
        return result;
    }

    @Specialization
    protected double doDoubleSequence(RDoubleSequence operand, @SuppressWarnings("unused") byte naRm) {
        double result = semantics.getDoubleStart();
        double current = operand.getStart();
        for (int i = 0; i < operand.getLength(); i++) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        if (operand.getLength() == 0) {
            emptyWarning();
        }
        return result;
    }

    @Specialization
    protected RComplex doComplexVector(RComplexVector operand, byte naRm) {
        if (semantics.supportComplex) {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            RComplex result = RRuntime.double2complex(semantics.getDoubleStart());
            int opCount = 0;
            na.enable(operand);
            for (int i = 0; i < operand.getLength(); i++) {
                RComplex current = operand.getDataAt(i);
                if (na.check(current)) {
                    if (profiledNaRm) {
                        continue;
                    } else {
                        return RRuntime.createComplexNA();
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
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "complex");

        }
    }

    // the algorithm that works for other types (reducing a vector starting with the "start value")
    // does not work for String-s as, in particular, we cannot supply the (lexicographically)
    // "largest" String for the implementation of max function

    @SuppressWarnings("unused")
    private static String doStringVectorEmptyInternal(RStringVector operand, byte naRm, ReduceSemantics semantics, SourceSection sourceSection) {
        if (semantics.supportString) {
            if (semantics.getEmptyWarning() != null) {
                RError.warning(semantics.emptyWarningCharacter);
            }
            return semantics.getStringStart();
        } else {
            throw RError.error(sourceSection, RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @Specialization(guards = "operand.getLength() == 0")
    protected String doStringVectorEmpty(RStringVector operand, byte naRm) {
        return doStringVectorEmptyInternal(operand, naRm, semantics, getEncapsulatingSourceSection());
    }

    @Specialization(guards = "operand.getLength() == 1")
    protected String doStringVectorOneElem(RStringVector operand, byte naRm) {
        if (semantics.supportString) {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            String result = operand.getDataAt(0);
            if (profiledNaRm) {
                na.enable(result);
                if (na.check(result)) {
                    return doStringVectorEmpty(operand, naRm);
                }
            }
            return result;
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @Specialization(guards = "operand.getLength() > 1")
    protected String doStringVector(RStringVector operand, byte naRm) {
        if (semantics.supportString) {
            return handleString(operand, naRm, 0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RRaw doString(RRawVector operand, byte naRm) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "raw");
    }

    public static final class ReduceSemantics {

        private final int intStart;
        private final double doubleStart;
        private final String stringStart = RRuntime.STRING_NA; // does not seem to change
        private final boolean nullInt;
        private final RError.Message emptyWarning;
        private final RError.Message emptyWarningCharacter;
        private final boolean supportComplex;
        private final boolean supportString;

        public ReduceSemantics(int intStart, double doubleStart, boolean nullInt, RError.Message emptyWarning, RError.Message emptyWarningCharacter, boolean supportComplex, boolean supportString) {
            this.intStart = intStart;
            this.doubleStart = doubleStart;
            this.nullInt = nullInt;
            this.emptyWarning = emptyWarning;
            this.emptyWarningCharacter = emptyWarningCharacter;
            this.supportComplex = supportComplex;
            this.supportString = supportString;
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
    }

    @TypeSystemReference(RTypes.class)
    protected abstract static class MultiElemStringHandler extends Node {

        public abstract String executeString(RStringVector operand, byte naRm, int offset);

        @Child private MultiElemStringHandler recursiveStringHandler;
        private final ReduceSemantics semantics;
        private final BinaryArithmeticFactory factory;
        @Child private BinaryArithmetic arithmetic;
        private final NACheck na;
        private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();

        public MultiElemStringHandler(ReduceSemantics semantics, BinaryArithmeticFactory factory, NACheck na) {
            this.semantics = semantics;
            this.factory = factory;
            this.arithmetic = factory.create();
            this.na = na;
        }

        public MultiElemStringHandler(MultiElemStringHandler other) {
            this(other.semantics, other.factory, other.na);
        }

        private String handleString(RStringVector operand, byte naRm, int offset) {
            if (recursiveStringHandler == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveStringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na));
            }
            return recursiveStringHandler.executeString(operand, naRm, offset);
        }

        @Specialization
        protected String doStringVectorMultiElem(RStringVector operand, byte naRm, int offset) {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            na.enable(operand);
            String result = operand.getDataAt(offset);
            if (profiledNaRm) {
                if (na.check(result)) {
                    // the following is meant to eliminate leading NA-s
                    if (offset == operand.getLength() - 1) {
                        // last element - all other are NAs
                        return doStringVectorEmptyInternal(operand, naRm, semantics, getEncapsulatingSourceSection());
                    } else {
                        return handleString(operand, naRm, offset + 1);
                    }
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
    }
}
