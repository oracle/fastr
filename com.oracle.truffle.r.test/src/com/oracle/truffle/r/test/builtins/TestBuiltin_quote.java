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

public class TestBuiltin_quote extends TestBase {

    @Test
    public void testQuote() {
        assertEval("{ quote(1:3) }");
        assertEval("{ quote(list(1, 2)) }");
        assertEval("{ typeof(quote(1)) }");
        assertEval("{ typeof(quote(x + y)) }");
        assertEval("{ class(quote(x + y)) }");
        assertEval("{ mode(quote(x + y)) }");
        assertEval("{ is.call(quote(x + y)) }");
        assertEval("{ quote(x <- x + 1) }");
        assertEval("{ typeof(quote(x)) }");

        assertEval(Output.ContainsError, "{ l <- quote(a[3] <- 4) ; f <- function() { eval(l) } ; f() }");
        assertEval(Output.ContainsError, "{ l <- quote(a[3] <- 4) ; eval(l) ; f() }");

        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; x <- 20 ; f() }");
    }
}
