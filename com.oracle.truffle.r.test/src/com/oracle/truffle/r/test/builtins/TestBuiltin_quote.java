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
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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

        assertEval(Output.IgnoreErrorContext, "{ l <- quote(a[3] <- 4) ; f <- function() { eval(l) } ; f() }");
        assertEval(Output.IgnoreErrorContext, "{ l <- quote(a[3] <- 4) ; eval(l) ; f() }");

        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; x <- 20 ; f() }");

        assertEval("quote(?sum)");
        assertEval("quote(??show)");
        assertEval("quote(?`[[`)");
        assertEval("quote(?'+')");

        // in GNUR, these behave inconsistently:
        assertEval(Ignored.ImplementationError, "quote()");
        assertEval("quote(expr=)");
        assertEval("quote(expr=...)");
        assertEval("quote(...)");

        assertEval(Ignored.ImplementationError, "typeof(quote(a[,2])[[3]])");
        assertEval(Ignored.ImplementationError, "{ res <- quote(a[,2])[[3]]; typeof(res) }");
    }

    @Test
    public void testQuoteWithFields() {
        assertEval("quote(foo@bar)[[3]]");
        assertEval("quote(foo$bar)[[3]]");
        assertEval("quote(foo$'bar')[[3]]");
        assertEval("quote(foo@'bar')[[3]]");
        assertEval("quote(foo@bar <- 3)[[2]][[3]]");
    }
}
