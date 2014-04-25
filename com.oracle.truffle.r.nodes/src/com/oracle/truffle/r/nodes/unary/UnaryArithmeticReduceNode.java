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

public abstract class UnaryArithmeticReduceNode extends UnaryNode {

    private final BinaryArithmeticFactory factory;

    @Child private BinaryArithmetic arithmetic;

    private final ReduceSemantics semantics;

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

    @Specialization(order = 1, guards = "isNullInt")
    public int doInt(@SuppressWarnings("unused") RNull operand) {
        if (semantics.getEmptyWarning() != null) {
            warning(semantics.emptyWarning);
        }
        return semantics.getIntStart();
    }

    @Specialization(order = 2, guards = "!isNullInt")
    public double doDouble(@SuppressWarnings("unused") RNull operand) {
        if (semantics.getEmptyWarning() != null) {
            warning(semantics.emptyWarning);
        }
        return semantics.getDoubleStart();
    }

    @Specialization(order = 3)
    public int doInt(int operand) {
        return arithmetic.op(semantics.getIntStart(), operand);
    }

    @Specialization(order = 4)
    public double doDouble(double operand) {
        return arithmetic.op(semantics.getDoubleStart(), operand);
    }

    @Specialization(order = 5)
    public int doIntVector(RIntVector operand) {
        int result = semantics.getIntStart();
        for (int i = 0; i < operand.getLength(); i++) {
            result = arithmetic.op(result, operand.getDataAt(i));
        }
        return result;
    }

    @Specialization(order = 6)
    public double doDoubleVector(RDoubleVector operand) {
        double result = semantics.getDoubleStart();
        for (int i = 0; i < operand.getLength(); i++) {
            result = arithmetic.op(result, operand.getDataAt(i));
        }
        return result;
    }

    @Specialization(order = 7)
    public double doLogicalVector(RLogicalVector operand) {
        double result = semantics.getIntStart();
        for (int i = 0; i < operand.getLength(); i++) {
            result = arithmetic.op(result, operand.getDataAt(i));
        }
        return result;
    }

    @Specialization(order = 10)
    public int doIntSequence(RIntSequence operand) {
        int result = semantics.getIntStart();
        int current = operand.getStart();
        for (int i = 0; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        return result;
    }

    @Specialization(order = 11)
    public double doDoubleSequence(RDoubleSequence operand) {
        double result = semantics.getDoubleStart();
        double current = operand.getStart();
        for (int i = 0; i < operand.getLength(); ++i) {
            result = arithmetic.op(result, current);
            current += operand.getStride();
        }
        return result;
    }

    @Specialization(order = 12)
    public RComplex doComplexVector(RComplexVector operand) {
        RComplex result = RRuntime.double2complex(semantics.getDoubleStart());
        for (int i = 0; i < operand.getLength(); ++i) {
            RComplex current = operand.getDataAt(i);
            result = arithmetic.op(result.getRealPart(), result.getImaginaryPart(), current.getRealPart(), current.getImaginaryPart());
        }
        return result;
    }

    public static final class ReduceSemantics {

        private final int intStart;
        private final double doubleStart;
        private final boolean nullInt;
        private final String emptyWarning;

        public ReduceSemantics(int intStart, double doubleStart, boolean nullInt, String emptyWarning) {
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

        public boolean isNullInt() {
            return nullInt;
        }

        public String getEmptyWarning() {
            return emptyWarning;
        }

    }

}
