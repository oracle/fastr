/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Tests the rxxx functions against common set of arguments. Each such function may have additional
 * tests for its specific corner cases if those are not covered here.
 */
public class TestRandGenerationFunctions extends TestBase {
    private static final String[] FUNCTION2_NAMES = {"rnorm", "runif", "rgamma", "rbeta", "rcauchy", "rf", "rlogis", "rweibull", "rchisq", "rwilcox"};
    private static final String[] FUNCTION2_PARAMS = {
                    "10, 10, 10",
                    "20, c(-1, 0, 0.2, 2:5), c(-1, 0, 0.1, 0.9, 3)",
                    "30, c(NA, 0, NaN, 1/0, -1/0), c(NaN, NaN, NA, 0, 1/0, -1/0)"
    };

    @Test
    public void testFunctions2() {
        assertEval(Output.IgnoreWarningContext, template("set.seed(1); %0(%1)", FUNCTION2_NAMES, FUNCTION2_PARAMS));
    }

    @Test
    public void testFunctions2Infrastructure() {
        // calculating the size of the result:
        assertEval("length(runif(c(1,2,3)))");
        assertEval("length(runif(c('a', 'b', 'b', 'd')))");
        assertEval("length(runif('3'))");
        // wrong size argument
        assertEval(Output.IgnoreWarningContext, "runif('hello')");
        // empty parameters
        assertEval("runif(2, numeric(), 2)");
        assertEval("runif(2, 2, numeric())");
        // wrong parameters
        assertEval("runif(-1, 1, 2)");
    }
}
