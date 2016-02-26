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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_rownames extends TestBase {

    @Test
    public void testrownames1() {
        assertEval("argv <- structure(list(x = structure(list(x = 1:3, y = c(6.28318530717959,     3.14159265358979, 0)), .Names = c('x', 'y'), row.names = c(NA,     -3L), class = 'data.frame')), .Names = 'x');"
                        + "do.call('row.names', argv)");
    }

    @Test
    public void testrownames2() {
        assertEval("argv <- structure(list(x = structure(logical(0), .Dim = c(4L,     0L)), do.NULL = FALSE), .Names = c('x', 'do.NULL'));do.call('rownames', argv)");
    }

    @Test
    public void testrownames3() {
        assertEval("argv <- structure(list(x = structure(list(x = 3:4), .Names = 'x',     row.names = c(NA, -2L), class = 'data.frame')), .Names = 'x');" + "do.call('rownames', argv)");
    }
}
