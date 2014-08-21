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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

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

    @Specialization(guards = "isNullInt")
    protected int doInt(@SuppressWarnings("unused") RNull operand) {
        if (semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return semantics.getIntStart();
    }

    @Specialization(guards = "!isNullInt")
    protected double doDouble(@SuppressWarnings("unused") RNull operand) {
        if (semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return semantics.getDoubleStart();
    }

    @Specialization
    protected int doInt(int operand) {
        na.enable(operand);
        return na.check(operand) ? RRuntime.INT_NA : arithmetic.op(semantics.getIntStart(), operand);
    }

    @Specialization
    protected double doDouble(double operand) {
        na.enable(operand);
        return na.check(operand) ? RRuntime.DOUBLE_NA : arithmetic.op(semantics.getDoubleStart(), operand);
    }

    @Specialization
    protected int doIntVector(RIntVector operand) {
        int result = semantics.getIntStart();
        na.enable(operand);
        int i = 0;
        for (; i < operand.getLength(); i++) {
            int d = operand.getDataAt(i);
            na.enable(d);
            result = na.check(d) ? RRuntime.INT_NA : arithmetic.op(result, d);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected double doDoubleVector(RDoubleVector operand) {
        double result = semantics.getDoubleStart();
        na.enable(operand);
        int i = 0;
        for (; i < operand.getLength(); i++) {
            double d = operand.getDataAt(i);
            na.enable(d);
            result = na.check(d) ? RRuntime.DOUBLE_NA : arithmetic.op(result, d);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected int doLogicalVector(RLogicalVector operand) {
        int result = semantics.getIntStart();
        na.enable(operand);
        int i = 0;
        for (; i < operand.getLength(); i++) {
            byte d = operand.getDataAt(i);
            na.enable(d);
            result = na.check(d) ? RRuntime.INT_NA : arithmetic.op(result, d);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected int doIntSequence(RIntSequence operand) {
        int result = semantics.getIntStart();
        int current = operand.getStart();
        int i = 0;
        for (; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
            current += operand.getStride();
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected double doDoubleSequence(RDoubleSequence operand) {
        double result = semantics.getDoubleStart();
        double current = operand.getStart();
        int i = 0;
        for (; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
            current += operand.getStride();
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    @Specialization
    protected RComplex doComplexVector(RComplexVector operand) {
        RComplex result = RRuntime.double2complex(semantics.getDoubleStart());
        int i = 0;
        for (; i < operand.getLength(); ++i) {
            RComplex current = operand.getDataAt(i);
            na.enable(current);
            result = na.check(current) ? RRuntime.createComplexNA() : arithmetic.op(result.getRealPart(), result.getImaginaryPart(), current.getRealPart(), current.getImaginaryPart());
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
        }
        if (i == 0 && semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return result;
    }

    // the algorithm that works for other types (reducing a vector starting with the "start value")
    // does not work for String-s as, in particular, we cannot supply the (lexicographically)
    // "largest" String for the implementation of max function

    @Specialization(guards = "empty")
    protected String doStringVectorEmpty(@SuppressWarnings("unused") RStringVector operand) {
        if (semantics.getEmptyWarning() != null) {
            RError.warning(semantics.emptyWarning);
        }
        return semantics.getStringStart();
    }

    @Specialization(guards = "lengthOne")
    protected String doStringVectorOneElem(RStringVector operand) {
        return operand.getDataAt(0);
    }

    @Specialization(guards = "longerThanOne")
    protected String doStringVector(RStringVector operand) {
        String result = operand.getDataAt(0);
        na.enable(result);
        if (na.check(result)) {
            return result;
        }
        for (int i = 1; i < operand.getLength(); ++i) {
            String current = operand.getDataAt(i);
            na.enable(current);
            if (na.check(current)) {
                return current;
            }
            result = arithmetic.op(result, current);
            na.enable(result);
            if (na.check(result)) {
                return result;
            }
        }
        return result;
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

        public ReduceSemantics(int intStart, double doubleStart, boolean nullInt, RError.Message emptyWarning) {
            this.intStart = intStart;
            this.doubleStart = doubleStart;
            this.nullInt = nullInt;
            this.emptyWarning = emptyWarning;
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

    }

}
