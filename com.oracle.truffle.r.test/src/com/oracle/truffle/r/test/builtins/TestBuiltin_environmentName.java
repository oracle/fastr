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
public class TestBuiltin_environmentName extends TestBase {

    @Test
    public void testenvironmentName1() {
        assertEval("argv <- list(FALSE); .Internal(environmentName(argv[[1]]))");
    }

    @Test
    public void testenvironmentName2() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0'))); .Internal(environmentName(argv[[1]]))");
    }

    @Test
    public void testenvironmentName3() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(environmentName(argv[[1]]))");
    }

    @Test
    public void test() {
        assertEval("environmentName(globalenv())");
        assertEval("environmentName(baseenv())");
        assertEval("environmentName(emptyenv())");
        assertEval("environmentName(parent.env(globalenv()))");
        assertEval("environmentName(environment(sum))");
        assertEval("environmentName(environment(lm))");
    }
}
