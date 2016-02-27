/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_expm1 extends TestBase {

    @Test
    public void testexpm11() {
        assertEval("argv <- list(-0.518798300715899);expm1(argv[[1]]);");
    }

    @Test
    public void testexpm12() {
        assertEval("argv <- list(-1.5314339531682e-113);expm1(argv[[1]]);");
    }

    @Test
    public void testexpm13() {
        assertEval("argv <- list(structure(c(-0.0996985539253204, -0.208486018303182, -0.412624920187971, -0.781459230080118, -1.41933833538431, -2.49413413365086, -4.24041092023363, -7.0213317713299), .Names = c('1', '2', '3', '4', '5', '6', '7', '8')));expm1(argv[[1]]);");
    }

    @Test
    public void testexpm14() {
        assertEval(Ignored.Unknown, "argv <- list(logical(0));expm1(argv[[1]]);");
    }

    @Test
    public void testexpm15() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));expm1(argv[[1]]);");
    }

    @Test
    public void testTrigExp() {
        assertEval("{ expm1(2) }");
        assertEval("{ expm1(c(1,2,3)) }");
        assertEval("{ expm1() }");
    }
}
