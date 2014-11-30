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
public class TestrGenBuiltinargs extends TestBase {

    @Test
    @Ignore
    public void testargs1() {
        assertEval("argv <- list(NULL); .Internal(args(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testargs2() {
        assertEval("argv <- list(character(0)); .Internal(args(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testargs3() {
        assertEval("argv <- list(.Primitive(\':\')); .Internal(args(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testargs4() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\')); .Internal(args(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testargs5() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(args(argv[[1]]))");
    }
}

