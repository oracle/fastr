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
public class TestBuiltin_asraw extends TestBase {

    @Test
    public void testasraw1() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw2() {
        assertEval("argv <- list(integer(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw3() {
        assertEval("argv <- list(logical(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw4() {
        assertEval("argv <- list(character(0));as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw5() {
        assertEval("argv <- list(NULL);as.raw(argv[[1]]);");
    }

    @Test
    public void testasraw6() {
        assertEval("argv <- list(list());as.raw(argv[[1]]);");
    }

    @Test
    public void testAsRaw() {
        assertEval("{ as.raw() }");
        assertEval("{ as.raw(NULL) }");
        assertEval("{ as.raw(1) }");
        assertEval("{ as.raw(1L) }");
        assertEval("{ as.raw(TRUE) }");
        assertEval("{ as.raw(c(TRUE, FALSE)) }");
        assertEval("{ as.raw(NA_integer_) }");
        assertEval("{ as.raw(1.1) }");
        assertEval("{ as.raw(c(1, 2, 3)) }");
        assertEval("{ as.raw(c(1L, 2L, 3L)) }");
        assertEval("{ as.raw(c(1L, 2L, NA_integer_)) }");
        assertEval("{ as.raw(list(1,2,3)) }");
        assertEval("{ as.raw(list(\"1\", 2L, 3.4)) }");
        assertEval("{ as.raw(as.raw(list(1, 2, c(1, 2, 3))) }");
        assertEval("{ as.raw(as.raw(list(1, 2, new.env())) }");
        assertEval("{ as.raw(as.raw(list(1, 10000000000000)) }");
        assertEval("{ as.raw(as.raw(list(1, 1+1i)) }");
        assertEval("{ as.raw(as.raw(list(1, 10000000000000, 1+1i)) }");
        assertEval("{ as.raw(as.raw(list(1, NA)) }");
        assertEval("{ as.raw(as.raw(list(1, NA, list(1))) }");
        assertEval("{ as.raw(as.raw(list(1, NA, list(1, 2, 3))) }");
        assertEval("{ as.raw.cls <- function(x) 42; as.raw(structure(c(1,2), class='cls')); }");

        assertEval("{ as.raw(1+1i) }");
        assertEval("{ as.raw(-1) }");
        assertEval("{ as.raw(-1L) }");
        assertEval("{ as.raw(NA) }");
        assertEval("{ as.raw(1000L) }");
        assertEval("{ as.raw(1000) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(10000000000000000) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw('test') }");
        assertEval("{ as.raw('1') }");
        assertEval("{ as.raw('1000') }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw('10000000000000000') }");
        assertEval("{ as.raw('1.1') }");
        assertEval("{ as.raw(c('1.1', '1')) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c('10000000000000000', '1.1')) }");
        assertEval("{ as.raw('NaN') }");
        assertEval("{ as.raw(c('1.1', 'NaN')) }");
        assertEval("{ as.raw(c('1', '2')) }");
        assertEval("{ as.raw(c('1', '1000')) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c('10000000000000000', '1000')) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c('10000000000000000', '1')) }");
        assertEval(Output.IgnoreWarningContext, "{ as.raw(c(1+3i, -2-1i, NA)) }");
        // TODO: returns more warning messages than expected, trying to fix it in
        // Slow/FastPathFromComplexAccess broke
        // the test as.raw(c(10000000000000000+1i, 1000+1i, NA_complex_, 1+1i))
        // which then retuned less than expected warnings
        assertEval(Output.IgnoreWarningMessage, "{ as.raw(10000000000000000+1i) }");
        assertEval(Output.IgnoreWarningMessage, "{ as.raw(c(10000000000000000+1i, 10000000000000000+1i)) }");
        assertEval("{ as.raw(NA_complex_) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(1000+1i) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c(10000000000000000+1i, 1+1i)) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c(NA_complex_, 1+1i)) }");
        // gnur has a fixed warnign order,
        // no matter how they are encountered while traversing the vector
        assertEval(Output.IgnoreWarningMessage, "{ as.raw(c(1000L, 1+1i)) }");
        assertEval(Output.IgnoreWarningMessage, "{ as.raw(c(1000, 1+1i)) }");
        assertEval(Output.IgnoreWhitespace, "{ as.raw(c(10000000000000000+1i, 1000+1i, NA_complex_, 1+1i)) }");
        assertEval("{ as.raw(c(1, -2, 3)) }");
        assertEval("{ as.raw(c(1,1000,NA)) }");
        assertEval("{ as.raw(c(1L, -2L, 3L)) }");
        assertEval("{ as.raw(c(1L, -2L, NA)) }");
        assertEval("{ y <- as.raw(c(5L, 6L)); attr(y, 'someAttr') <- 'someValue'; x <- as.raw(y); x[[1]] <- as.raw(42); y }");

        assertEval("{ f <- function() as.raw('aaa'); f() }");
        assertEval("{ f1 <- function() {f<- function() as.raw('aaa'); f()}; f1() }");
    }

    @Test
    public void noCopyCheck() {
        assertEvalFastR("{ x <- as.raw(c(1, 3)); .fastr.identity(x) == .fastr.identity(as.raw(x)); }", "[1] TRUE");
    }
}
