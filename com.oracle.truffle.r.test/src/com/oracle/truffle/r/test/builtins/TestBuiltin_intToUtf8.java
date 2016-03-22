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
public class TestBuiltin_intToUtf8 extends TestBase {

    @Test
    public void testintToUtf81() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf82() {
        assertEval(Ignored.Unknown, "argv <- list(list(), FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf83() {
        assertEval(Ignored.Unknown, "argv <- list(FALSE, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf85() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = NA_integer_, multiple = TRUE), .Names = c('x',     'multiple'));do.call('intToUtf8', argv)");
    }

    @Test
    public void testintToUtf86() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = NA_integer_), .Names = 'x');do.call('intToUtf8', argv)");
    }
}
