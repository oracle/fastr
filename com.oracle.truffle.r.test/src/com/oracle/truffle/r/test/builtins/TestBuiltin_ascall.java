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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_ascall extends TestBase {

    @Test
    public void testascall1() {
        assertEval("argv <- list(list(quote(quote), c(0.568, 1.432, -1.08, 1.08)));as.call(argv[[1]]);");
    }

    @Test
    public void testascall2() {
        assertEval("argv <- list(list(quote(quote), FALSE));as.call(argv[[1]]);");
    }

    @Test
    public void testascall3() {
        assertEval("argv <- list(list(quote(quote), list(NULL, c('time', 'status'))));as.call(argv[[1]]);");
    }

    @Test
    public void testascall4() {
        assertEval("argv <- list(structure(expression(data.frame, check.names = TRUE, stringsAsFactors = TRUE), .Names = c('', 'check.names', 'stringsAsFactors')));as.call(argv[[1]]);");
    }

    @Test
    public void testascall5() {
        assertEval("argv <- list(list(quote(quote), 80L));as.call(argv[[1]]);");
    }

    @Test
    public void testascall6() {
        assertEval("argv <- list(list(quote(quote), NA));as.call(argv[[1]]);");
    }

    @Test
    public void testAsCall() {
        assertEval("{ l <- list(f) ; as.call(l) }");
        assertEval("{ l <- list(f, 2, 3) ; as.call(l) }");
        assertEval("{ g <- function() 23 ; l <- list(f, g()) ; as.call(l) }");
        assertEval("{ f <- round ; g <- as.call(list(f, quote(A))) }");
        assertEval("{ f <- function() 23 ; l <- list(f) ; cl <- as.call(l) ; eval(cl) }");
        assertEval("{ f <- function(a,b) a+b ; l <- list(f,2,3) ; cl <- as.call(l) ; eval(cl) }");
        assertEval("{ f <- function(x) x+19 ; g <- function() 23 ; l <- list(f, g()) ; cl <- as.call(l) ; eval(cl) }");

        assertEval("{ f <- function(x) x ; l <- list(f, 42) ; cl <- as.call(l); typeof(cl[[1]]) }");
        assertEval("{ f <- function(x) x ; l <- list(f, 42) ; cl <- as.call(l); typeof(cl[[2]]) }");
    }
}
