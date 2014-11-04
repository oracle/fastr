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
public class TestrGenBuiltinenvironment extends TestBase {

    @Test
    @Ignore
    public void testenvironment1() {
        assertEval("argv <- list(quote(cbind(X, M) ~ M.user + Temp + M.user:Temp + Soft)); .Internal(environment(argv[[1]]))");
    }

    @Test
    public void testenvironment2() {
        assertEval("argv <- list(FALSE); .Internal(environment(argv[[1]]))");
    }

    @Test
    public void testenvironment3() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(environment(argv[[1]]))");
    }

    @Test
    public void testenvironment4() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\'))); .Internal(environment(argv[[1]]))");
    }
}
