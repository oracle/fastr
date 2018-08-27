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
        assertEval("intToBits(5+7i)");
        assertEval("intToBits(NULL)");
        assertEval("intToBits(c(1,2,3))");
        assertEval("intToBits(c(5L,99L))");
        assertEval("intToBits(integer(0))");
        assertEval("intToBits(double(0))");
        assertEval("intToBits(6:9)");
        assertEval("intToBits('23rrff')");
        assertEval(Output.IgnoreErrorMessage, "intToBits(new.env())");
        assertEval("intToBits(environment)");
        assertEval("intToBits(stdout())");
        assertEval("intToBits(list(c(5,5,7,8),88,6L))");
        assertEval("intToBits(list(5,5,7,8))");
        assertEval("intToBits(2147483648)");
        assertEval("intToBits(c(2147483648, 2147483648))");
    }
}
