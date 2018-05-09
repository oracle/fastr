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
public class TestBuiltin_seterrmessage extends TestBase {

    @Test
    public void testseterrmessage1() {
        assertEval("argv <- list('Error in cor(rnorm(10), NULL) : \\n  supply both \\'x\\' and \\'y\\' or a matrix-like \\'x\\'\\n'); .Internal(seterrmessage(argv[[1]]))");
    }

    @Test
    public void testseterrmessage2() {
        assertEval("argv <- list('Error in as.POSIXlt.character(x, tz, ...) : \\n  character string is not in a standard unambiguous format\\n'); .Internal(seterrmessage(argv[[1]]))");
    }

    @Test
    public void testseterrmessage3() {
        assertEval("argv <- list('Error in validObject(.Object) : \\n  invalid class “trackCurve” object: Unequal x,y lengths: 20, 10\\n'); .Internal(seterrmessage(argv[[1]]))");
    }
}
