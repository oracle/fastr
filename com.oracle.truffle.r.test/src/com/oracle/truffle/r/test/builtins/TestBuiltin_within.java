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

public class TestBuiltin_within extends TestBase {

    @Test
    public void testwithin1() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(data = structure(list(a = 1:5, b = 2:6,     c = 3:7), .Names = c('a', 'b', 'c'), row.names = c(NA, -5L),     class = 'data.frame')), .Names = 'data');"
                                        + "do.call('within', argv)");
    }

}
