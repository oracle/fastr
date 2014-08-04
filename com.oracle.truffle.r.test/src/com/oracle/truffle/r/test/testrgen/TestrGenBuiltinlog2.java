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

public class TestrGenBuiltinlog2 extends TestBase {

    @Test
    public void testlog21() {
        assertEval("argv <- list(48L);log2(argv[[1]]);");
    }

    @Test
    public void testlog22() {
        assertEval("argv <- list(FALSE);log2(argv[[1]]);");
    }

    @Test
    public void testlog23() {
        assertEval("argv <- list(2.2250738585072e-308);log2(argv[[1]]);");
    }
}