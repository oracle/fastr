/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinseterrmessage extends TestBase {

    @Test
    @Ignore
    public void testseterrmessage1() {
        assertEval("argv <- list(\'Error in cor(rnorm(10), NULL) : \\n  supply both 'x' and 'y' or a matrix-like 'x'\\n\'); .Internal(seterrmessage(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testseterrmessage2() {
        assertEval("argv <- list(\'Error in as.POSIXlt.character(x, tz, ...) : \\n  character string is not in a standard unambiguous format\\n\'); .Internal(seterrmessage(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testseterrmessage3() {
        assertEval("argv <- list(\'Error in validObject(.Object) : \\n  invalid class “trackCurve” object: Unequal x,y lengths: 20, 10\\n\'); .Internal(seterrmessage(argv[[1]]))");
    }
}
