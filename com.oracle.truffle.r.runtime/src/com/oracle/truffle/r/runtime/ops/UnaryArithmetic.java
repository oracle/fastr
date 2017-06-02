/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-1997, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998, Ross Ihaka
 * Copyright (c) 1998-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;

/**
 * Base class for the implementation of unary arithmetic operations. This covers functions like "+",
 * "-", "sqrt", and many more.
 */
public abstract class UnaryArithmetic extends Operation {

    public static final UnaryArithmeticFactory NEGATE = Negate::new;
    public static final UnaryArithmeticFactory PLUS = Plus::new;

    public UnaryArithmetic() {
        super(false, false);
    }

    /**
     * The lowest type with which this operation will be executed. E.g., if this is double, then
     * integer arguments will be coerced to double before performing the actual operation.
     */
    public RType getMinPrecedence() {
        return RType.Double;
    }

    /**
     * Specifies the error that will be raised for invalid argument types.
     */
    public Message getArgumentError() {
        return RError.Message.NON_NUMERIC_MATH;
    }

    /**
     * Determines, for a given argument type (after coercion according to
     * {@link #getMinPrecedence()}), the type of the return value. This is mainly intended to
     * support operations that return double values for complex arguments.
     */
    public RType calculateResultType(RType argumentType) {
        return argumentType;
    }

    public int op(@SuppressWarnings("unused") byte op) {
        throw new UnsupportedOperationException();
    }

    public int op(@SuppressWarnings("unused") int op) {
        throw new UnsupportedOperationException();
    }

    public double op(@SuppressWarnings("unused") double op) {
        throw new UnsupportedOperationException();
    }

    public RComplex op(double re, double im) {
        // default: perform operation on real and imaginary part
        return RDataFactory.createComplex(op(re), op(im));
    }

    public double opd(@SuppressWarnings("unused") double re, @SuppressWarnings("unused") double im) {
        throw new UnsupportedOperationException();
    }

    public static final class Negate extends UnaryArithmetic {

        @Override
        public RType getMinPrecedence() {
            return RType.Integer;
        }

        @Override
        public Message getArgumentError() {
            return RError.Message.INVALID_ARG_UNARY;
        }

        @Override
        public int op(int op) {
            return -op;
        }

        @Override
        public double op(double op) {
            return -op;
        }

        @Override
        public int op(byte op) {
            return -(int) op;
        }
    }

    public static final class Plus extends UnaryArithmetic {

        @Override
        public RType getMinPrecedence() {
            return RType.Integer;
        }

        @Override
        public Message getArgumentError() {
            return RError.Message.INVALID_ARG_UNARY;
        }

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return op;
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public RComplex op(double re, double im) {
            return RDataFactory.createComplex(re, im);
        }
    }
}
