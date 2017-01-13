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
 * Common tests for functions implemented using {@code StatsFunctions} infrastructure.
 */
public class TestStatFunctions extends TestBase {
    private static final String[] FUNCTION3_1_NAMES = {"dlnorm", "dlogis"};
    private static final String[] FUNCTION3_1_PARAMS = {
                    "10, 10, 10, log=TRUE",
                    "3, 3, 3, log=FALSE",
                    "c(-1, 0, 1), c(-1, 0, 0.2, 2:5), rep(c(-1, 0, 0.1, 0.9, 3), 12), log=TRUE",
                    "c(-1, 0, 1), c(-1, 0, 0.2, 2:5), rep(c(-1, 0, 0.1, 0.9, 3), 12), log=FALSE",
                    "0, c(NA, 0, NaN, 1/0, -1/0), c(NaN, NaN, NA, 0, 1/0, -1/0), log=FALSE",
                    "0, c(0.0653, 0.000123, 32e-80, 8833, 79e70), c(0.0653, 0.000123, 32e-80, 8833, 79e70, 0, -1), log=FALSE",
                    "c(NA, NaN, 1/0, -1/0), 2, 2, log=FALSE"
    };

    @Test
    public void testFunctions31() {
        assertEval(Output.IgnoreWhitespace, template("set.seed(1); %0(%1)", FUNCTION3_1_NAMES, FUNCTION3_1_PARAMS));
    }

    private static final String[] FUNCTION2_1_NAMES = {"dchisq", "dgeom", "dt"};
    private static final String[] FUNCTION2_1_PARAMS = {
                    "10, 10, log=TRUE",
                    "3, 3, log=FALSE",
                    "c(-1, 0, 0.2, 2), rep(c(-1, 0, 0.1, 0.9, 3), 4), log=TRUE",
                    "c(-1, 0, 0.2, 2), rep(c(-1, 0, 0.1, 0.9, 3), 4), log=FALSE",
                    "c(NA, 0, NaN, 1/0, -1/0), rep(c(1, 0, 0.1), 5), log=FALSE",
                    "0, c(0.0653, 0.000123, 32e-80, 8833, 79e70, 0, -1), log=FALSE"
    };

    @Test
    public void testFunctions21() {
        assertEval(Output.IgnoreWhitespace, template("set.seed(1); %0(%1)", FUNCTION2_1_NAMES, FUNCTION2_1_PARAMS));
    }

    private static final String[] FUNCTION2_2_NAMES = {"pchisq", "qgeom", "pgeom", "qt", "pt", "qchisq"};
    private static final String[] FUNCTION2_2_PARAMS = {
                    "0, 10",
                    "c(-1, 0, 0.2, 2), rep(c(-1, 0, 0.1, 0.9, 3), 4)",
                    "0, c(0.0653, 0.000123, 32e-80, 8833, 79e70, 0, -1)"
    };

    @Test
    public void testFunctions22() {
        // first: the "normal params" with all the combinations of log.p and lower.tail
        assertEval(Output.IgnoreWhitespace, template("set.seed(1); %0(%1, %2, %3)",
                        FUNCTION2_2_NAMES, FUNCTION2_2_PARAMS, new String[]{"lower.tail=TRUE", "lower.tail=FALSE"}, new String[]{"log.p=TRUE", "log.p=FALSE"}));
        // the error cases (where log.p nor lower.tail should make no difference)
        // first parameter wrong
        assertEval(Output.IgnoreWhitespace, template("set.seed(1); %0(%1)", FUNCTION2_2_NAMES, new String[]{"c(NA, 0, NaN, 1/0, -1/0), rep(c(1, 0, 0.1), 5)"}));
        // second parameter wrong
        assertEval(Output.IgnoreWhitespace, template("set.seed(1); %0(%1)", FUNCTION2_2_NAMES, new String[]{"rep(c(1, 0, 0.1), 5), c(NA, 0, NaN, 1/0, -1/0)"}));
    }

    private static final String[] FUNCTION3_2_NAMES = {"qlnorm", "plnorm", "qlogis", "pf", "plogis", "qf"};
    private static final String[] FUNCTION3_2_PARAMS = {
                    "0, 10, 10",
                    "c(-1, 0, 0.2, 2), c(-1, 0, 0.1, 0.9, 3), rep(c(-1, 0, 1, 0.1, -0.1, 0.0001), 20)",
                    "0, c(0.0653, 0.000123, 32e-80, 8833, 79e70, 0, -1), rep(c(0.0653, 0.000123, 32e-80, 8833, 79e70), 7)"
    };

    @Test
    public void testFunctions32() {
        // first: the "normal params" with all the combinations of log.p and lower.tail
        assertEval(Output.MayIgnoreWarningContext, template("set.seed(1); %0(%1, %2, %3)",
                        FUNCTION3_2_NAMES, FUNCTION3_2_PARAMS, new String[]{"lower.tail=TRUE", "lower.tail=FALSE"}, new String[]{"log.p=TRUE", "log.p=FALSE"}));
        // the error cases (where log.p nor lower.tail should make no difference)
        // first parameter wrong
        assertEval(Output.MayIgnoreWarningContext,
                        template("set.seed(1); %0(%1)", FUNCTION3_2_NAMES, new String[]{"c(NA, 0, NaN, 1/0, -1/0), rep(c(1, 0, 0.1), 5), rep(c(1, 0, 0.1), 5)"}));
        // second parameter wrong
        assertEval(Output.MayIgnoreWarningContext,
                        template("set.seed(1); %0(%1)", FUNCTION3_2_NAMES, new String[]{"rep(c(1, 0, 0.1), 5), c(NA, 0, NaN, 1/0, -1/0), rep(c(1, 0, 0.1), 5)"}));
        // third parameter wrong
        assertEval(Output.MayIgnoreWarningContext,
                        template("set.seed(1); %0(%1)", FUNCTION3_2_NAMES, new String[]{"rep(c(1, 0, 0.1), 5), rep(c(1, 0, 0.1), 5), c(NA, 0, NaN, 1/0, -1/0)"}));
    }
}
