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

// Checkstyle: stop line length check
public class TestrGenBuiltinasraw extends TestBase {

    @Test
    public void testasraw1() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw2() {
        assertEval("argv <- list(integer(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw3() {
        assertEval("argv <- list(logical(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw4() {
        assertEval("argv <- list(character(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw5() {
        assertEval("argv <- list(NULL);as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw6() {
        assertEval("argv <- list(list());as.raw(argv[[1]]);");
    }
}
