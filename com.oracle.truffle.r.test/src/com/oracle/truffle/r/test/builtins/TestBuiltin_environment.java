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
public class TestBuiltin_environment extends TestBase {

    @Test
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
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0'))); .Internal(environment(argv[[1]]))");
    }

    @Test
    public void testEnvironment() {
        assertEval("environment()");
        assertEval("environment(`+`)");
        assertEval("environment(print)");
        assertEval("environment(FALSE)");
        assertEval("environment(a~b)");
        assertEval("environment(function(x) 1)");
        assertEval("environment(NULL)");
        assertEval("{ f <- y~z; class(f) <- c('myclass', class(f)); environment(f) }");
        assertEval("{ x <- as.pairlist(c(1,2,3)); e <- as.environment(list(x=x)); print(x); print(e$x) }");
    }
}
