/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-1997, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998, Ross Ihaka
 * Copyright (c) 1998-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;

public abstract class UnaryArithmetic extends Operation {

    public static final UnaryArithmeticFactory NEGATE = Negate::new;
    public static final UnaryArithmeticFactory PLUS = Plus::new;

    public UnaryArithmetic() {
        super(false, false);
    }

    public RType calculateResultType(RType argumentType) {
        return argumentType;
    }

    public abstract int op(byte op);

    public abstract int op(int op);

    public abstract double op(double op);

    public abstract RComplex op(double re, double im);

    @SuppressWarnings("unused")
    public double opd(double re, double im) {
        throw new UnsupportedOperationException();
    }

    public static class Negate extends UnaryArithmetic {

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

        @Override
        public RComplex op(double re, double im) {
            return RDataFactory.createComplex(op(re), op(im));
        }
    }

    public static class Plus extends UnaryArithmetic {

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
