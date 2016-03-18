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
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSimpleIfEvaluator extends TestBase {

    @Test
    public void testIfWithoutElse() {
        assertEval("if(TRUE) 1");
    }

    @Test
    public void testIfWithoutElseIgnore() {
        assertEval("if(FALSE) 1");
    }

    @Test
    public void testIf2() {
        assertEval("if(TRUE) 1 else 2");
        assertEval("if(FALSE) 1 else 2");
    }

    @Test
    public void testIfNot1() {
        assertEval("if(!FALSE) 1 else 2");
        assertEval("if(!TRUE) 1 else 2");
    }

    @Test
    public void testIfDanglingElse() {
        assertEval("if(TRUE) if (FALSE) 1 else 2");
    }

    @Test
    public void testIfDanglingElseIgnore() {
        assertEval("if(FALSE) if (FALSE) 1 else 2");
    }

    @Test
    public void testIf() {
        assertEval("{ x <- 2 ; if (1==x) TRUE else 2 }");
        assertEval(Output.ContainsError, "{ x <- 2 ; if (NA) x <- 3 ; x }");
        assertEval(Output.ContainsError, "{ f <- function(x) { if (x) 1 else 2 } ; f(NA)  }");
        assertEval(Output.ContainsError, "{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(NA) }");
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(TRUE) }");
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(FALSE) }");

        assertEval(Output.ContainsError, "{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(\"hello\") }");
        assertEval(Output.ContainsError, "{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(logical()) }");
        assertEval(Output.ContainsError, "{ f <- function(x) { if (x == 2) 1 else 2 } ; f(1) ; f(NA) }");

        assertEval("{ if (TRUE==FALSE) TRUE else FALSE }");
        assertEval(Output.ContainsError, "{ if (NA == TRUE) TRUE else FALSE }");
        assertEval(Output.ContainsError, "{ if (TRUE == NA) TRUE else FALSE }");
        assertEval("{ if (FALSE==TRUE) TRUE else FALSE }");
        assertEval("{ if (FALSE==1) TRUE else FALSE }");
        assertEval("{ f <- function(v) { if (FALSE==v) TRUE else FALSE } ; f(TRUE) ; f(1) }");
        assertEval(Output.ContainsError, Output.ContainsWarning, "{ x<-list(1,2); if (x) 7 else 42 }");
    }

    @Test
    public void testIfIgnore() {
        assertEval(Output.ContainsWarning, "{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(1:3) }");
    }

    @Test
    public void testCast() {
        assertEval(Output.ContainsWarning, "{ f <- function(a) { if (is.na(a)) { 1 } else { 2 } } ; f(5) ; f(1:3)}");
        assertEval(Output.ContainsWarning, "{ if (1:3) { TRUE } }");
        assertEval(Output.ContainsError, "{ if (integer()) { TRUE } }");
        assertEval(Output.ContainsError, "{ if (1[2:1]) { TRUE } }");
        assertEval(Output.ContainsWarning, "{ if (c(0,0,0)) { TRUE } else { 2 } }");
        assertEval(Output.ContainsWarning, "{ if (c(1L,0L,0L)) { TRUE } else { 2 } }");
        assertEval(Output.ContainsWarning, "{ if (c(0L,0L,0L)) { TRUE } else { 2 } }");
        assertEval(Output.ContainsError, "{ if (c(1L[2],0L,0L)) { TRUE } else { 2 } }");
        assertEval(Output.ContainsWarning, "{ f <- function(cond) { if (cond) { TRUE } else { 2 } } ; f(1:3) ; f(2) }");
        assertEval(Output.ContainsWarning, "{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(FALSE) }");
        assertEval(Output.ContainsError, "{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(logical()) }");
        assertEval(Output.ContainsWarning, "{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(1) }");
    }

    @Test
    public void testIfVisibility() {
        assertEval("{ if (FALSE) 23 }");
        assertEval("{ if (FALSE) 23 else NULL }");
        assertEval("{ if (TRUE) invisible(23) else 23 }");
        assertEval("{ if (TRUE) invisible(23) }");
        assertEval("{ if (FALSE) 23 else invisible(23) }");
    }
}
