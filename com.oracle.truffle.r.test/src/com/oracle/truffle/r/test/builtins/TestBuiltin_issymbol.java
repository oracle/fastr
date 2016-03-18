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
public class TestBuiltin_issymbol extends TestBase {

    @Test
    public void testissymbol1() {
        assertEval("argv <- list(0.05);is.symbol(argv[[1]]);");
    }

    @Test
    public void testissymbol2() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1), .Dim = 1:3));is.symbol(argv[[1]]);");
    }

    @Test
    public void testissymbol3() {
        assertEval("argv <- list(numeric(0));is.symbol(argv[[1]]);");
    }

    @Test
    public void testissymbol4() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.symbol(argv[[1]]);");
    }
}
