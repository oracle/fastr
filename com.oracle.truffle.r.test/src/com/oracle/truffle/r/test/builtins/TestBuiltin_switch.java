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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_switch extends TestBase {

    @Test
    public void testswitch1() {
        assertEval("argv <- structure(list('forward', forward = 'posS', reverse = 'negS'),     .Names = c('', 'forward', 'reverse'));do.call('switch', argv)");
    }

    @Test
    public void testswitch2() {
        assertEval(Output.IgnoreWarningContext, "argv <- list(3L);do.call('switch', argv)");
    }

    @Test
    public void testswitch4() {
        assertEval("argv <- list(2L, TRUE, FALSE, FALSE);do.call('switch', argv)");
    }

    @Test
    public void testSwitch() {
        assertEval("{ test1 <- function(type) { switch(type, mean = 1, median = 2, trimmed = 3) };test1(\"median\")}");
        assertEval("{switch(3,1,2,3)}");
        assertEval("{switch(4,1,2,3)}");
        assertEval("{switch(4,1,2,z)}");
        assertEval("{ test1 <- function(type) { switch(type, mean = mean(c(1,2,3,4)), median = 2, trimmed = 3) };test1(\"mean\")}");
        assertEval("{ u <- \"uiui\" ; switch(u, \"iuiu\" = \"ieps\", \"uiui\" = \"miep\") }");
        assertEval("{ answer<-\"no\";switch(as.character(answer), yes=, YES=1, no=, NO=2,3) }");
        assertEval("{ x <- \"<\"; v <- switch(x, \"<=\" =, \"<\" =, \">\" = TRUE, FALSE); v }");
        assertEval("{ x <- \"<\"; switch(x, \"<=\" =, \"<\" =, \">\" = TRUE, FALSE) }");
        assertEval("{ x <- \"<\"; switch(x, \"<=\" =, \"<\" =, \">\" =, FALSE) }");
        assertEval("{ a <- NULL ; switch(mode(a), NULL=\"naught\") }");
        assertEval("{ a <- NULL ; switch(mode(a), NULL=) }");
        assertEval(Output.IgnoreErrorContext, "{ x <- \"!\"; v <- switch(x, v77, \"<=\" =, \"<\" =, \">\" = 99, v55)}");
        assertEval("{ x <- \"!\"; v <- switch(x, \"\"=v77, \"<=\" =, \"<\" =, \">\" = 99, v55)}");
        // FIXME: swicth does not check for REmpty
        assertEval(Ignored.Unimplemented, "switch('q', a=42,)");
        assertEval("f <- function(x){switch(\"aa\", aa = x, ab = 42)};f()");
        assertEval("f <- function(x){switch(\"aa\", aa = , ab = 42)};f()");
        assertEval("f <- function(x){switch(\"aa\", aa = , ab = x)};f()");
        assertEval("f <- function(x){switch(\"aa\", aa = , ab = 42)};f(quote(f(1,2,))[[4]])");
        assertEval("f <- function(x){switch(\"aa\", aa = , ab = x)};f(quote(f(1,2,))[[4]])");
        assertEval("f <- function(x){switch(\"aa\", aa = x, ab = 42)};f(quote(f(1,2,))[[4]])");
    }

    @Test
    public void testSwitchInvalidExpr() {
        assertEval("{ x <- switch(NA, 1, 2, 3); x }");
        assertEval("{ switch(quote(a), 1, 2, 3) }");
        assertEval("{ x <- switch(expression(quote(1)), 1, 2, 3); x }");
        assertEval("{ x <- switch(expression(quote(1), quote(2)), 1, 2, 3); x }");
        assertEval("{ x <- switch(list(2), 1, 2, 3); x }");
        assertEval("{ x <- switch(list(1,2,3), 1, 2, 3); x }");
    }

    /**
     * In GNU-R, switch is a primitive with specific argument matching - only the first argument is
     * treated as EXPR, even if there is other argument tagged as EXPR.
     */
    @Test
    public void testSwitchArgumentMatching() {
        assertEval("switch(EXPR=1, c(0,0))");
        assertEval("switch(EXP=1, c(0,0))");
        assertEval("switch(EX=1, c(0,0))");
        assertEval("switch(E=1, c(0,0))");
        assertEval("switch(1, c(0,0))");
        assertEval("switch(1, EXPR=c(0,0))");
        assertEval("switch('EXPR'=1, c(0,0))");
        assertEval("switch('EXP'=1, c(0,0))");
        assertEval("switch('EX'=1, c(0,0))");
        assertEval("switch('E'=1, c(0,0))");
        assertEval("switch(1, 'E'=c(0,0))");
        assertEval("switch(1, 'EX'=c(0,0))");
        assertEval("switch(1, 'EXPR'=c(0,0))");
        assertEval("switch('E'=1, 'E'=c(0,0))");
        assertEval("switch('E'=1, E=c(0,0))");
        assertEval("switch('EX'=1, 'EX'=c(0,0))");
        assertEval("switch('EXPR'=1, 'EXPR'=c(0,0))");
        assertEval("switch(EXPR=1, EXPR=c(0,0))");
        assertEval("switch(EXPR=c(0,0), EXPR=1)");
    }
}
