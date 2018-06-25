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
public class TestBuiltin_allnames extends TestBase {

    @Test
    public void testallnames1() {
        assertEval("argv <- list(quote(y ~ ((g1) * exp((log(g2/g1)) * (1 - exp(-k * (x - Ta)))/(1 - exp(-k * (Tb - Ta)))))), FALSE, -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testallnames2() {
        assertEval("argv <- list(logical(0), logical(0), -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testallnames3() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)), TRUE, -1L, FALSE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testallnames4() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), -1L, FALSE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testallnames5() {
        assertEval("argv <- list(0.1, FALSE, -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testAllNames() {
        assertEval("all.names(quote(y ~ ((g1) * exp((log(g2/g1)) * (1 - exp(-k * (x - Ta)))/(1 - exp(-k * (Tb - Ta)))))), FALSE, -1L, TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*foo(bar/2)), functions=TRUE, max.names=10, unique=TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*foo(bar/2)), functions=TRUE, max.names=10, unique=FALSE)");
        assertEval("all.names(quote(a+y+z+y*y*foo(bar/2)), functions=TRUE, max.names=2, unique=TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*foo(bar/2)), functions=FALSE, max.names=10, unique=TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*function(x=bar/2) baz(1)), functions=TRUE, max.names=10, unique=TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*function(x=bar/2) baz(1)), functions=TRUE, max.names=10, unique=FALSE)");
        assertEval("all.names(quote(a+y+z+y*y*function(x=bar/2) baz(1)), functions=TRUE, max.names=2, unique=TRUE)");
        assertEval("all.names(quote(a+y+z+y*y*function(x=bar/2) baz(1)), functions=FALSE, max.names=10, unique=TRUE)");

        assertEval("{ all.names(expression(sin(x+y+x)), functions=F) }");
        assertEval("{ all.names(expression(sin(x+y+x)), functions=T) }");
        assertEval("{ all.names(expression(sin(x+y+x)), functions=NULL) }");
        assertEval("{ all.names(expression(sin(x+y+x)), functions=NA) }");
        assertEval("{ all.names(expression(sin(x+y+x)), max.names=NULL) }");
        assertEval("{ all.names(expression(sin(x+y+x)), max.names=NA) }");
        assertEval("{ all.names(expression(sin(x+y+x)), unique=F) }");
        assertEval("{ all.names(expression(sin(x+y+x)), unique=T) }");
        assertEval("{ all.names(expression(sin(x+y+x)), unique=NULL) }");
        assertEval("{ all.names(expression(sin(x+y+x)), unique=NA) }");

        assertEval("{ all.names(quote(switch(x, 'median' =, 'hello' = print('hello case')))) }");

        assertEval("{ all.names(as.symbol('a'), max.names=1)}");
        assertEval("{ all.names(as.symbol('a'), max.names=0)}");
        assertEval("{ all.names(as.symbol('a'), max.names=-1)}");
        assertEval("{ all.names(as.symbol('a'), max.names=-2)}");
    }
}
