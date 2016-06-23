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
public class TestBuiltin_intToBits extends TestBase {

    @Test
    public void testintToBits1() {
        assertEval("argv <- list(list()); .Internal(intToBits(argv[[1]]))");
    }

    @Test
    public void testintToBits2() {
        assertEval("argv <- list(NULL); .Internal(intToBits(argv[[1]]))");
    }

    @Test
    public void testintToBits() {
        assertEval("intToBits()");
        assertEval("intToBits(-1)");
        assertEval("intToBits(-99L)");
        assertEval("intToBits(6543L)");
        assertEval("intToBits(2345234.77)");
        assertEval("intToBits(1.22)");
        assertEval("intToBits(-1.56)");
        assertEval("intToBits(-0.3)");
        assertEval("intToBits('123')");
        assertEval(Output.IgnoreWarningContext, "intToBits(5+7i)");
        assertEval("intToBits(NULL)");
        assertEval("intToBits(c(1,2,3))");
        assertEval("intToBits(c(5L,99L))");
        assertEval("intToBits(integer(0))");
        assertEval("intToBits(double(0))");
        assertEval("intToBits(6:9)");
        assertEval(Output.IgnoreWarningContext, "intToBits('23rrff')");
        assertEval("intToBits(new.env())");
        assertEval("intToBits(environment)");
        assertEval(Ignored.ImplementationError, "intToBits(stdout())");
        assertEval(Output.IgnoreErrorContext, "intToBits(list(c(5,5,7,8),88,6L))");
        assertEval("intToBits(list(5,5,7,8))");
    }
}
