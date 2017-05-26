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
package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSplineFunctions extends TestBase {

    @Test
    public void basicSplineCoef() {
        // method "periodic"
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1), c(1))");
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1:6), c(1:5, 1))");
        // method "natural"
        assertEval(".Call(stats:::C_SplineCoef, 2, c(1), c(1))");
        assertEval(".Call(stats:::C_SplineCoef, 2, c(1:5), c(1:5))");
        // method "fmm"
        assertEval(".Call(stats:::C_SplineCoef, 3, c(1:5), c(1:5))");
        // method "hyman"
        assertEval(".Call(stats:::C_SplineCoef, 4, c(1:5), c(1:5))");

        assertEval(".Call(stats:::C_SplineCoef, 0, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, -1, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 111, c(1:5), c(1:5))");

        assertEval(".Call(stats:::C_SplineCoef, NULL, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, NA, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, c(), c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, list(), c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, c(list()), c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 'abc', c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, c(1), c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, c(1, 2, 3), c(1), c(1))");
        assertEval(".Call(stats:::C_SplineCoef, c('a'), c(1), c(1))");
        assertEval(Ignored.WrongCaller, ".Call(stats:::C_SplineCoef, list(1), c(1), c(1))");
        assertEval(Ignored.WrongCaller, ".Call(stats:::C_SplineCoef, environment(), c(1:5), c(1:5))");
        assertEval(Ignored.WrongCaller, ".Call(stats:::C_SplineCoef, function() {}, c(1:5), c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 1, list(1), c(1))");
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1), list(1))");

        assertEval(".Call(stats:::C_SplineCoef, 1, NULL, c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 1, NA, c(1:5))");
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1:5), NULL)");
        assertEval(".Call(stats:::C_SplineCoef, 1, c(1:5), NA)");
        assertEval(".Call(stats:::C_SplineCoef, 1, NULL, NULL)");
        assertEval(".Call(stats:::C_SplineCoef, 1, NA, NA)");
    }
}
