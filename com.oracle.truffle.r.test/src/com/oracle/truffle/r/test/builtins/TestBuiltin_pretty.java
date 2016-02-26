/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_pretty extends TestBase {

    @Test
    public void testpretty1() {
        assertEval(Ignored.Unknown, "argv <- list(0L, 3L, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty2() {
        assertEval(Ignored.Unknown, "argv <- list(-0.03, 1.11, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(-6.64448090063514e-06, 6.64454021993011e-06, 1, 0, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(1.234e+100, 1.234e+100, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }
}
