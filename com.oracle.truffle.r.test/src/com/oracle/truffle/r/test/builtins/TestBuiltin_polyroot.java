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
public class TestBuiltin_polyroot extends TestBase {

    @Test
    public void testpolyroot1() {
        assertEval(Ignored.Unknown, "argv <- list(1:2); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot2() {
        assertEval(Ignored.Unknown, "argv <- list(FALSE); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot3() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(1, 0.035205614861993, 0.237828814667385), .Names = c('', '', ''))); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot4() {
        assertEval(Ignored.Unknown, "argv <- list(c(1, -1.16348488318732, 0.667550726251972, -0.342308178637008)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot5() {
        assertEval(Ignored.Unknown, "argv <- list(c(1, 0.0853462951557329, -0.433003162033324, 0.141816558560935, -0.268523717394886, -0.0970671649038473)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot6() {
        assertEval(Ignored.Unknown, "argv <- list(c(1, 8, 28, 56, 70, 56, 28, 8, 1)); .Internal(polyroot(argv[[1]]))");
    }
}
