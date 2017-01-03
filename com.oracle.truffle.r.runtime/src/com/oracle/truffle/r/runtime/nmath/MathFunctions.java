/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nmath;

/**
 * Defines common interface for math functions operating on scalar values, which is used to
 * implement common code for the vectorized versions.
 */
public class MathFunctions {
    public interface Function4_2 {
        double evaluate(double a, double b, double c, double d, boolean x, boolean y);
    }

    public interface Function4_1 extends Function4_2 {
        @Override
        default double evaluate(double a, double b, double c, double d, boolean x, boolean y) {
            return evaluate(a, b, c, d, x);
        }

        double evaluate(double a, double b, double c, double d, boolean x);
    }

    public interface Function3_2 extends Function4_2 {
        @Override
        default double evaluate(double a, double b, double c, double d, boolean x, boolean y) {
            return evaluate(a, b, c, x, y);
        }

        double evaluate(double a, double b, double c, boolean x, boolean y);
    }

    public interface Function3_1 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, c, x);
        }

        double evaluate(double a, double b, double c, boolean x);
    }

    public interface Function2_1 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, x);
        }

        double evaluate(double a, double b, boolean x);
    }

    public interface Function2_2 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, x, y);
        }

        double evaluate(double a, double b, boolean x, boolean y);
    }
}
