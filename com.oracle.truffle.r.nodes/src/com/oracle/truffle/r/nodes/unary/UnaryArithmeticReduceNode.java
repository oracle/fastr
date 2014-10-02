/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChild(value = "naRm", type = RNode.class)
public abstract class UnaryArithmeticReduceNode extends UnaryNode {

    private final BinaryArithmeticFactory factory;

    @Child private BinaryArithmetic arithmetic;

    private final ReduceSemantics semantics;

    private final NACheck na = NACheck.create();

    public UnaryArithmeticReduceNode(ReduceSemantics semantics, BinaryArithmeticFactory factory) {
        this.factory = factory;
        this.semantics = semantics;
        this.arithmetic = factory.create();
    }

    public UnaryArithmeticReduceNode(UnaryArithmeticReduceNode op) {
        // we recreate the arithmetic each time this specialization specializes
        // it also makes sense for polymorphic variations of this node
        this(op.semantics, op.factory);
    }

    protected boolean isNullInt() {
        return semantics.isNullInt();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullInt")
    protected int doInt(RNull operand, byte naRm) {
        if (semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return semantics.getIntStart();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isNullInt")
    protected double doDouble(RNull operand, byte naRm) {
        if (semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return semantics.getDoubleStart();
    }

    @Specialization
    protected int doInt(int operand, byte naRm) {
        na.enable(operand);
        if (naRm == RRuntime.LOGICAL_TRUE) {
            if (na.check(operand)) {
                if (semantics.getEmptyWarning() != null) {
                    RError.warning(semantics.emptyWarning);
                }
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
        if (naRm == RRuntime.LOGICAL_TRUE) {
            if (na.check(operand)) {
                if (semantics.getEmptyWarning() != null) {
                    RError.warning(semantics.emptyWarning);
                }
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
        if (naRm == RRuntime.LOGICAL_TRUE) {
            if (na.check(operand)) {
                if (semantics.getEmptyWarning() != null) {
                    RError.warning(semantics.emptyWarning);
                }
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
            if (naRm == RRuntime.LOGICAL_TRUE) {
                if (na.check(operand)) {
                    if (semantics.getEmptyWarning() != null) {
                        RError.warning(semantics.emptyWarning);
                    }
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
            if (naRm == RRuntime.LOGICAL_TRUE) {
                if (na.check(operand)) {
                    if (semantics.getEmptyWarning() != null) {
                        RError.warning(semantics.emptyWarning);
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
        int result = semantics.getIntStart();
        na.enable(operand);
        int opCount = 0;
        for (int i = 0; i < operand.getLength(); i++) {
            int d = operand.getDataAt(i);
            na.enable(d);
            if (na.check(d)) {
                if (naRm == RRuntime.LOGICAL_TRUE) {
                    continue;
                } else {
                    return RRuntime.INT_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected double doDoubleVector(RDoubleVector operand, byte naRm) {
        double result = semantics.getDoubleStart();
        na.enable(operand);
        int opCount = 0;
        for (int i = 0; i < operand.getLength(); i++) {
            double d = operand.getDataAt(i);
            na.enable(d);
            if (na.check(d)) {
                if (naRm == RRuntime.LOGICAL_TRUE) {
                    continue;
                } else {
                    return RRuntime.DOUBLE_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected int doLogicalVector(RLogicalVector operand, byte naRm) {
        int result = semantics.getIntStart();
        na.enable(operand);
        int opCount = 0;
        for (int i = 0; i < operand.getLength(); i++) {
            byte d = operand.getDataAt(i);
            na.enable(d);
            if (na.check(d)) {
                if (naRm == RRuntime.LOGICAL_TRUE) {
                    continue;
                } else {
                    return RRuntime.INT_NA;
                }
            } else {
                result = arithmetic.op(result, d);
            }
            opCount++;
        }
        if (opCount == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected int doIntSequence(RIntSequence operand, @SuppressWarnings("unused") byte naRm) {
        int result = semantics.getIntStart();
        int current = operand.getStart();
        int opCount = 0;
        for (int i = 0; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        if (opCount == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected double doDoubleSequence(RDoubleSequence operand, @SuppressWarnings("unused") byte naRm) {
        double result = semantics.getDoubleStart();
        double current = operand.getStart();
        int opCount = 0;
        for (int i = 0; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        if (opCount == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected RComplex doComplexVector(RComplexVector operand, byte naRm) {
        if (semantics.supportComplex) {
            RComplex result = RRuntime.double2complex(semantics.getDoubleStart());
            int opCount = 0;
            for (int i = 0; i < operand.getLength(); ++i) {
                RComplex current = operand.getDataAt(i);
                na.enable(current);
                if (na.check(current)) {
                    if (naRm == RRuntime.LOGICAL_TRUE) {
                        continue;
                    } else {
                        return RRuntime.createComplexNA();
                    }
                } else {
                    result = arithmetic.op(result.getRealPart(), result.getImaginaryPart(), current.getRealPart(), current.getImaginaryPart());
                }
                opCount++;
            }
            if (opCount == 0 && semantics.getEmptyWarning() != null) {
                RError.warning(semantics.emptyWarning);
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
    @Specialization(guards = "empty")
    protected String doStringVectorEmpty(RStringVector operand, byte naRm) {
        if (semantics.supportString) {
            if (semantics.getEmptyWarning() != null) {
                RError.warning(semantics.emptyWarning);
            }
            return semantics.getStringStart();
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @Specialization(guards = "lengthOne")
    protected String doStringVectorOneElem(RStringVector operand, byte naRm) {
        if (semantics.supportString) {
            String result = operand.getDataAt(0);
            if (naRm == RRuntime.LOGICAL_TRUE) {
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

    @SlowPath
    private String doStringVectorMultiElem(RStringVector operand, byte naRm, int offset) {
        String result = operand.getDataAt(offset);
        na.enable(result);
        if (naRm == RRuntime.LOGICAL_TRUE) {
            if (na.check(result)) {
                // the following is meant to eliminate leading NA-s
                if (offset == operand.getLength() - 1) {
                    // last element - all other are NAs
                    return doStringVectorEmpty(operand, naRm);
                } else {
                    return doStringVectorMultiElem(operand, naRm, offset + 1);
                }
            }
        } else {
            if (na.check(result)) {
                return result;
            }
        }
        // when we reach here, it means that we have already seen one non-NA element
        assert !RRuntime.isNA(result);
        for (int i = offset + 1; i < operand.getLength(); ++i) {
            String current = operand.getDataAt(i);
            na.enable(current);
            if (na.check(current)) {
                if (naRm == RRuntime.LOGICAL_TRUE) {
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

    @Specialization(guards = "longerThanOne")
    protected String doStringVector(RStringVector operand, byte naRm) {
        if (semantics.supportString) {
            return doStringVectorMultiElem(operand, naRm, 0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RRaw doString(RRawVector operand, byte naRm) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_ARGUMENT, "raw");
    }

    protected boolean empty(RStringVector vector) {
        return vector.getLength() == 0;
    }

    protected boolean lengthOne(RStringVector vector) {
        return vector.getLength() == 1;
    }

    protected boolean longerThanOne(RStringVector vector) {
        return vector.getLength() > 1;
    }

    public static final class ReduceSemantics {

        private final int intStart;
        private final double doubleStart;
        private final String stringStart = RRuntime.STRING_NA; // does not seem to change
        private final boolean nullInt;
        private final RError.Message emptyWarning;
        private final boolean supportComplex;
        private final boolean supportString;

        public ReduceSemantics(int intStart, double doubleStart, boolean nullInt, RError.Message emptyWarning, boolean supportComplex, boolean supportString) {
            this.intStart = intStart;
            this.doubleStart = doubleStart;
            this.nullInt = nullInt;
            this.emptyWarning = emptyWarning;
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

        public boolean supportsComplex() {
            return supportComplex;
        }

        public boolean supportsString() {
            return supportString;
        }

    }

}
