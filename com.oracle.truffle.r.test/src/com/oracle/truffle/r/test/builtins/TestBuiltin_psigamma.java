/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
