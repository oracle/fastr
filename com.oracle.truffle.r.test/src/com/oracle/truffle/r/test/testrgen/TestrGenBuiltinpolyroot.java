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
public class TestrGenBuiltinpolyroot extends TestBase {

    @Test
    @Ignore
    public void testpolyroot1() {
        assertEval("argv <- list(1:2); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testpolyroot2() {
        assertEval("argv <- list(FALSE); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testpolyroot3() {
        assertEval("argv <- list(structure(c(1, 0.035205614861993, 0.237828814667385), .Names = c(\'\', \'\', \'\'))); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testpolyroot4() {
        assertEval("argv <- list(c(1, -1.16348488318732, 0.667550726251972, -0.342308178637008)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testpolyroot5() {
        assertEval("argv <- list(c(1, 0.0853462951557329, -0.433003162033324, 0.141816558560935, -0.268523717394886, -0.0970671649038473)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testpolyroot6() {
        assertEval("argv <- list(c(1, 8, 28, 56, 70, 56, 28, 8, 1)); .Internal(polyroot(argv[[1]]))");
    }
}

