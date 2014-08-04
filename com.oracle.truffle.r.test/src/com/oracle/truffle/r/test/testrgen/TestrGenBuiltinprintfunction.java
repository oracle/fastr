/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinprintfunction extends TestBase {

    @Test
    public void testprintfunction1() {
        assertEval("argv <- list(.Primitive(\'+\'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction2() {
        assertEval("argv <- list(.Primitive(\'if\'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction3() {
        assertEval("argv <- list(.Primitive(\'c\'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction4() {
        assertEval("argv <- list(.Primitive(\'.Internal\'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testprintfunction5() {
        assertEval("argv <- list(.Primitive(\'log\'), TRUE); .Internal(print.function(argv[[1]], argv[[2]]))");
    }
}