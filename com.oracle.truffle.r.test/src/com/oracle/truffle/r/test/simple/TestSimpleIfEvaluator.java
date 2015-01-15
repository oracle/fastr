/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.simple;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleIfEvaluator extends TestBase {

    @Test
    public void testIfWithoutElse() {
        assertEval("if(TRUE) 1");
    }

    @Test
    public void testIfWithoutElseIgnore() {
        assertEvalNoOutput("if(FALSE) 1");
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
        assertEvalNoOutput("if(FALSE) if (FALSE) 1 else 2");
    }

    @Test
    public void testIf() {
        assertEval("{ x <- 2 ; if (1==x) TRUE else 2 }");
        assertEvalError("{ x <- 2 ; if (NA) x <- 3 ; x }");
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(NA)  }");
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(NA) }");
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(TRUE) }");
        assertEval("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(FALSE) }");

        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(\"hello\") }");
        assertEvalError("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(logical()) }");
        assertEvalError("{ f <- function(x) { if (x == 2) 1 else 2 } ; f(1) ; f(NA) }");

        assertEval("{ if (TRUE==FALSE) TRUE else FALSE }");
        assertEvalError("{ if (NA == TRUE) TRUE else FALSE }");
        assertEvalError("{ if (TRUE == NA) TRUE else FALSE }");
        assertEval("{ if (FALSE==TRUE) TRUE else FALSE }");
        assertEval("{ if (FALSE==1) TRUE else FALSE }");
        assertEval("{ f <- function(v) { if (FALSE==v) TRUE else FALSE } ; f(TRUE) ; f(1) }");
    }

    @Test
    public void testIfIgnore() {
        assertEvalWarning("{ f <- function(x) { if (x) 1 else 2 } ; f(1) ; f(1:3) }");
    }

    @Test
    public void testCast() {
        assertEvalWarning("{ f <- function(a) { if (is.na(a)) { 1 } else { 2 } } ; f(5) ; f(1:3)}");
        assertEvalWarning("{ if (1:3) { TRUE } }");
        assertEvalError("{ if (integer()) { TRUE } }");
        assertEvalError("{ if (1[2:1]) { TRUE } }");
        assertEvalWarning("{ if (c(0,0,0)) { TRUE } else { 2 } }");
        assertEvalWarning("{ if (c(1L,0L,0L)) { TRUE } else { 2 } }");
        assertEvalWarning("{ if (c(0L,0L,0L)) { TRUE } else { 2 } }");
        assertEvalError("{ if (c(1L[2],0L,0L)) { TRUE } else { 2 } }");
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 } } ; f(1:3) ; f(2) }");
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(FALSE) }");
        assertEvalError("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(logical()) }");
        assertEvalWarning("{ f <- function(cond) { if (cond) { TRUE } else { 2 }  } ; f(c(TRUE,FALSE)) ; f(1) }");
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
