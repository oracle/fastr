/*
 * Copyright (c) 1995-1997, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998, Ross Ihaka
 * Copyright (c) 1998-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

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
        throw CompilerDirectives.shouldNotReachHere();
    }

    public int op(@SuppressWarnings("unused") int op) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    public double op(@SuppressWarnings("unused") double op) {
        throw CompilerDirectives.shouldNotReachHere();
    }

    public RComplex op(double re, double im) {
        // default: perform operation on real and imaginary part
        return RComplex.valueOf(op(re), op(im));
    }

    /**
     * Allows to override the default checking of NA, where NA in either im or re results into NA.
     */
    public double opdChecked(NACheck naCheck, double re, double im) {
        if (naCheck.check(re) || naCheck.check(im)) {
            return RRuntime.DOUBLE_NA;
        }
        return opd(re, im);
    }

    public double opd(@SuppressWarnings("unused") double re, @SuppressWarnings("unused") double im) {
        throw CompilerDirectives.shouldNotReachHere();
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
            return RComplex.valueOf(re, im);
        }
    }
}
