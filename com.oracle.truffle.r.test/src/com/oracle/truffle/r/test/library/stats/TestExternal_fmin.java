/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestExternal_fmin extends TestBase {
    @Test
    public void testFmin() {
        // Note: GNU-R hangs on this, but only in rtestgen, what is going on?
        // assertEval(".External2(stats:::C_do_fmin, function (x) (x - 1/3)^2, 0, 1, 0.001");
        assertEval(".External2(stats:::C_do_fmin, function(x) x^2, 0, 10, 0.001)");
    }

    @Test
    public void testArgsEvaluation() {
        assertEval(".External2(stats:::C_do_fmin, 42, 0, 10, 0.001)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, NaN, 10, 0.001)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, Inf, 10, 0.001)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, 0, NaN, 0.001)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, 0, Inf, 0.001)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, 0, 1, NaN)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, 0, 1, Inf)");
        assertEval(".External2(stats:::C_do_fmin, function(x) x, 10, 1, 0.01)");
    }
}
