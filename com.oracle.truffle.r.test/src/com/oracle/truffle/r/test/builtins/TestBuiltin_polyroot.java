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
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
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
        assertEval("argv <- list(1:2); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot2() {
        assertEval("argv <- list(FALSE); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot3() {
        assertEval("argv <- list(structure(c(1, 0.035205614861993, 0.237828814667385), .Names = c('', '', ''))); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot4() {
        assertEval("argv <- list(c(1, -1.16348488318732, 0.667550726251972, -0.342308178637008)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot5() {
        assertEval("argv <- list(c(1, 0.0853462951557329, -0.433003162033324, 0.141816558560935, -0.268523717394886, -0.0970671649038473)); .Internal(polyroot(argv[[1]]))");
    }

    @Test
    public void testpolyroot6() {
        assertEval("argv <- list(c(1, 8, 28, 56, 70, 56, 28, 8, 1)); .Internal(polyroot(argv[[1]]))");
    }
}
