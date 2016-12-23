/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.test.library.stats.TestStatFunctions.PROBABILITIES;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Additional tests on the top of {@link TestStatFunctions}.
 */
public class TestExternal_qbeta extends TestBase {
    private static final String[] BOOL_VALUES = new String[]{"T", "F"};

    @Test
    public void testQBeta() {
        // check if in qbeta_raw with comment "p==0, q==0, p = Inf, q = Inf <==> treat as one- or
        // two-point mass"
        assertEval(template("qbeta(0.7, 0, 1/0, lower.tail=%0, log.p=F)", BOOL_VALUES));
        assertEval(template("qbeta(log(0.7), 0, 1/0, lower.tail=%0, log.p=T)", BOOL_VALUES));
        assertEval(template("qbeta(0.7, 1/0, 0, lower.tail=%0, log.p=F)", BOOL_VALUES));
        assertEval(template("qbeta(log(0.7), 1/0, 0, lower.tail=%0, log.p=T)", BOOL_VALUES));
        assertEval(template("qbeta(%0, 0, 0, lower.tail=%1, log.p=F)", new String[]{"0.1", "0.5", "0.7"}, BOOL_VALUES));
        assertEval(template("qbeta(log(%0), 0, 0, lower.tail=%1, log.p=T)", new String[]{"0.1", "0.5", "0.7"}, BOOL_VALUES));
        // checks swap_tail = (p_ > 0.5) where for lower.tail = log.p = TRUE is p_ = exp(p)
        // exp(0.1) = 1.10....
        assertEval(template("qbeta(log(%0), 0.1, 3, lower.tail=T, log.p=T)", PROBABILITIES));
        // exp(-1) = 0.36...
        assertEval(Output.MayIgnoreWarningContext, template("qbeta(log(%0), -1, 3, lower.tail=T, log.p=T)", PROBABILITIES));
    }
}
