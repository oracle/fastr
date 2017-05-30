/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_psigamma extends TestBase {

    @Test
    public void testpsigamma1() {
        assertEval(Ignored.Unimplemented,
                        "argv <- list(c(-100, -3, -2, -1, 0, 1, 2, -99.9, -7.7, -3, -2.9, -2.8, -2.7, -2.6, -2.5, -2.4, -2.3, -2.2, -2.1, -2, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1, -1, -0.9, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.0999999999999996, 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3, 5.1, 77), 1); .Internal(psigamma(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpsigamma2() {
        assertEval(Ignored.Unimplemented, "argv <- list(c(1e+30, 1e+45, 1e+60, 1e+75, 1e+90), 2); .Internal(psigamma(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpsigamma3() {
        assertEval(Ignored.Unimplemented, "argv <- list(c(1e+20, 1e+30, 1e+40, 1e+50, 1e+60), 5); .Internal(psigamma(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpsigamma4() {
        assertEval(Ignored.Unimplemented,
                        "argv <- list(c(-100, -3, -2, -1, 0, 1, 2, -99.9, -7.7, -3, -2.9, -2.8, -2.7, -2.6, -2.5, -2.4, -2.3, -2.2, -2.1, -2, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1, -1, -0.9, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.0999999999999996, 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3, 5.1, 77), 0); .Internal(psigamma(argv[[1]], argv[[2]]))");
    }
}
