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
 * Tests fitting functions: {@code lm}.
 */
public class TestFitting extends TestBase {
    private static final String[] CHECK_RESULT_NAMES = new String[]{"coefficients", "fitted.values", "xlevels", "residuals", "assign", "effects", "qr$qr", "rank", "model"};
    private static final String RANDOM_VECTOR = "c(26.55, 37.21, 57.28, 90.82, 20.16, 89.838, 94.46, 20.5, 17.6, 68.7, 38.41, 76.9, 49.7, 71, 99.19, 16)";
    private static final String RANDOM_FACTOR = "factor(c('m', 'm', 'f', 'm', 'm', 'm', 'f', 'f', 'f', 'm', 'f', 'f', 'm', 'f', 'f', 'm'))";
    private static final String[] VALUES = new String[]{
                    RANDOM_VECTOR,
                    "c(rep(1,8), rep(2,8))"
    };

    @Test
    public void testLm() {
        StringBuffer printCode = new StringBuffer();
        for (String name : CHECK_RESULT_NAMES) {
            printCode.append("print(res$").append(name).append(");");
        }

        assertEval(Output.IgnoreWhitespace, template("y <- %0; x <- %1; res <- lm(y~x); " + printCode, VALUES, VALUES));
        assertEval(Output.IgnoreWhitespace, template("y <- %0; x <- %1; z <- %2; res <- lm(y~x*z); " + printCode, VALUES, VALUES, new String[]{RANDOM_VECTOR}));
        assertEval(Output.IgnoreWhitespace, template("y <- %0; x <- %1; z <- %2; res <- lm(y~(x:z)^3); " + printCode, VALUES, VALUES, new String[]{RANDOM_VECTOR}));
        assertEval(Output.IgnoreWhitespace, String.format("y <- %s; x <- %s; res <- lm(y~x); " + printCode, RANDOM_VECTOR, RANDOM_FACTOR));
    }
}
