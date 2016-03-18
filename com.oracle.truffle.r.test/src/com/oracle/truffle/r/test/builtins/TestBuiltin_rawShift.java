/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_rawShift extends TestBase {

    @Test
    public void testrawShift1() {
        assertEval(Output.ContainsError, "argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = -10L), .Names = c('x', 'n'));do.call('rawShift', argv)");
        assertEval(Output.ContainsError, "argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = -9), .Names = c('x', 'n'));do.call('rawShift', argv)");
        for (int i = -8; i <= 8; i++) {
            assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = " + i + "L), .Names = c('x', 'n'));do.call('rawShift', argv)");
            assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = " + i + ".1), .Names = c('x', 'n'));do.call('rawShift', argv)");
        }
        assertEval(Output.ContainsError, "argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = 9), .Names = c('x', 'n'));do.call('rawShift', argv)");
    }
}
