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
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.r.runtime.data.*;

public abstract class UnaryArithmetic extends Operation {

    public static final UnaryArithmeticFactory NEGATE = new UnaryArithmeticFactory() {

        @Override
        public UnaryArithmetic create() {
            return new Negate();
        }
    };

    public static final UnaryArithmeticFactory ROUND = new UnaryArithmeticFactory() {

        @Override
        public UnaryArithmetic create() {
            return new Round();
        }
    };

    public static final UnaryArithmeticFactory FLOOR = new UnaryArithmeticFactory() {

        @Override
        public UnaryArithmetic create() {
            return new Floor();
        }
    };

    public static final UnaryArithmeticFactory CEILING = new UnaryArithmeticFactory() {

        @Override
        public UnaryArithmetic create() {
            return new Ceiling();
        }
    };

    public UnaryArithmetic() {
        super(false, false);
    }

    public abstract int op(byte op);

    public abstract int op(int op);

    public abstract double op(double op);

    public abstract RComplex op(double re, double im);

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

    public static class Round extends UnaryArithmetic {

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return Math.rint(op);
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public RComplex op(double re, double im) {
            throw new UnsupportedOperationException();
        }

    }

    public static class Floor extends Round {

        @Override
        public double op(double op) {
            return Math.floor(op);
        }
    }

    public static class Ceiling extends Round {

        @Override
        public double op(double op) {
            return Math.ceil(op);
        }
    }

}
