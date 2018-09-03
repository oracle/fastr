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

public class TestBuiltin_log extends TestBase {

    @Test
    public void testlog1() {
        assertEval("argv <- list(0.7800058115849);do.call('log', argv)");
    }

    @Test
    public void testLog() {
        assertEval("{ log(1) } ");
        assertEval("{ log(0) }");
        assertEval("{ log(c(0,1)) }");
        assertEval("{ round( log(10,), digits = 5 ) }");
        assertEval("{ round( log(10,2), digits = 5 ) }");
        assertEval("{ round( log(10,10), digits = 5 ) }");
        assertEval("{ log(c(2,3), NA) } ");
        assertEval("{ log(c(2,3), 0/0) } ");

        assertEval("{ log(NaN) }");
        assertEval("{ log(NA) }");
        assertEval("{ log(1, NaN) }");
        assertEval("{ log(1, NA) }");
        assertEval("{ log(NaN, NaN) }");
        assertEval("{ log(NA, NA) }");
    }

    @Test
    public void testLogLogical() {
        assertEval("{ log(T) }");
        assertEval("{ log(c(T)) }");
        assertEval("{ log(c(T, F)) }");
        assertEval("{ log(c(T, F), T) }");
        assertEval("{ log(T, T) }");
        assertEval("{ log(T, F) }");
        assertEval("{ log(F, F) }");
        assertEval("{ log(F, T) }");
        assertEval("{ log(c(T, T), NA) }");
        assertEval("{ log(c(T, T), NaN) }");
    }

    @Test
    public void testLogInt() {
        assertEval("{ log(1L) }");
        assertEval("{ log(-1L) }");
        assertEval("{ log(0L) }");
        assertEval("{ log(NA_integer_) }");
        assertEval("{ log(0L, NA_integer_) }");
        assertEval("{ log(c(0L, NA_integer_)) }");
        assertEval("{ log(1L, 1L) }");
        assertEval("{ log(10L, 1L) }");
        assertEval("{ log(10L, -1L) }");
        assertEval("{ log(10L, 10L) }");
        assertEval("{ log(c(1L, 1L, 0L, 10L), 10L) }");
        assertEval("{ log(c(1L, 0L, 10L), 1L) }");
        assertEval("{ log(c(1L, 2L), NA) }");
        assertEval("{ log(c(1L, 2L), NaN) }");
        assertEval("{ log(c(1L, 2L, NA)) }");
    }

    @Test
    public void testLogDouble() {
        assertEval("{ log(1.1) }");
        assertEval("{ log(-1.1) }");
        assertEval("{ log(0.0) }");
        assertEval("{ log(NA_real_) }");
        assertEval("{ log(10, NA_real_) }");
        assertEval("{ log(c(10, NA_real_)) }");
        assertEval("{ log(1.0, 1.0) }");
        assertEval("{ log(10.0, 1.0) }");
        assertEval("{ log(10.0, -1.0) }");
        assertEval("{ log(10.0, 10.0) }");
        assertEval("{ log(c(1.0, 0.0, 10.0), 10.0) }");
        assertEval("{ log(c(1.0, 0.0, 10.0), 1.0) }");
        assertEval("{ log(c(1.0, 2.0), NA) }");
        assertEval("{ log(c(1.0, 2.0), NaN) }");
        assertEval("{ log(c(1.0, 2.0, NA)) }");
    }

    @Test
    public void testLogComplex() {
        assertEval("{ log(0+0i) }");

        // NaN warnings
        assertEval(Output.IgnoreWarningContext, "{ log(0+0i, 0) }");
        assertEval(Output.IgnoreWarningContext, "{ log(0L, 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(0.0, 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(F, 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(0+0i, 0+0i) }");

        // division by complex(r=0, i=0) returns NaN+NaNi, instead of -Inf+NaNi
        assertEval(Ignored.ImplementationError, "{ log(0+0i, 1) }");

        assertEval("{ log(1+1i) }");
        assertEval("{ log(complex(real=NA, imaginary=1i)) }");
        assertEval("{ log(complex(real=1, imaginary=NA)) }");
        assertEval("{ log(complex(real=NA, imaginary=NA)) }");
        assertEval("{ log(NA_complex_) }");
        assertEval(Ignored.NewRVersionMigration, "{ log(1+1i, NA_complex_) }");
        assertEval("{ log(c(1+1i, NA_complex_)) }");
        assertEval("{ log(1+1i, 0) }");

        // division by complex(r=0, i=0) returns NaN+NaNi, instead of -Inf+NaNi
        assertEval(Ignored.ImplementationError, "{ log(1+1i, 1) }");

        assertEval("{ log(10+1i, 10) }");
        assertEval("{ log(10-1i, 10) }");
        assertEval("{ log(-10-1i, 10) }");
        assertEval("{ log(10+1i, -10) }");
        assertEval("{ log(10+10i, 10) }");
        assertEval("{ log(c(1+1i)) }");
        assertEval("{ log(c(1+1i), 0) }");

        assertEval(Ignored.NewRVersionMigration, "{ log(c(1+1i, 2+2i), NA) }");
        assertEval("{ log(c(1+1i, 2+2i), NaN) }");
        assertEval("{ log(c(1+1i, 2+2i), complex(real=1, imaginary=NaN)) }");
        assertEval("{ log(c(1+1i, 2+2i), complex(real=NaN, imaginary=1)) }");
        assertEval("{ log(c(1+1i, 2+2i), complex(real=NaN, imaginary=NaN)) }");
        assertEval(Ignored.NewRVersionMigration, "{ log(c(1+1i, 2+2i), complex(real=1, imaginary=NA)) }");
        assertEval(Ignored.NewRVersionMigration, "{ log(c(1+1i, 2+2i), complex(real=NA, imaginary=1)) }");
        assertEval("{ log(c(1+1i, 2+2i, complex(real=NA, imaginary=NA))) }");

        assertEval("{ log(c(10+1i, 10), 10) }");
        assertEval("{ log(c(10+10i, 10), 10) }");
        assertEval("{ log(c(1+1i, 2+1i)) }");

        assertEval("{ log(c(10, 10+10i), 10) }");
        assertEval("{ log(c(10.0, 10+10i), 10) }");
        assertEval("{ log(c(T, 10+10i), 10) }");

        assertEval("{ log(1, 1+1i) }");
        assertEval("{ log(10, 10+10i) }");
        assertEval("{ log(1.0, 1+1i) }");
        assertEval("{ log(10.0, 1+1i) }");
        assertEval("{ log(T, 1+1i) }");

        assertEval("{ log(1+1i, 1+1i) }");
        assertEval("{ log(1+1i, 1-1i) }");
        assertEval("{ log(1+1i, -1-1i) }");
        assertEval("{ log(10+10i, 10+10i) }");
        assertEval("{ log(1+1i, 10+10i) }");

        assertEval("{ log(c(10, 10), 10+10i) }");
        assertEval("{ log(c(10.0, 10.0), 10+10i) }");
        assertEval("{ log(c(T, F), 1+1i) }");
        assertEval("{ log(c(10+10i, 10+10i), 10+10i) }");
        // division by complex(r=0, i=0) returns NaN+NaNi, instead of -Inf+NaNi
        assertEval(Ignored.ImplementationError, "{ log(complex(real=sqrt(.5), imaginary=sqrt(.5)), 1) }");
    }

    @Test
    public void testProducedManyNaNsButOneWarnig() {
        assertEval("{ log(c(F, F), F) }");
        assertEval("{ log(c(1L, 1L), 1L) }");
        assertEval("{ log(c(1.0, 1.0), 1.0) }");
        assertEval(Output.IgnoreWarningContext, "{ log(c(0+0i, 0+0i), 0) }");

        assertEval(Output.IgnoreWarningContext, "{ log(c(F, F), 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(c(0L, 0L), 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(c(0.0, 0.0), 0+0i) }");
        assertEval(Output.IgnoreWarningContext, "{ log(c(0+0i, 0+0i), 0+0i) }");
    }

    @Test
    public void testLogAttrs() {
        assertEval("{ x <- array(1:3, 1); dimnames(x) <- list('a'); r <- log(x); names(r)[[1]] <- 'new'; list(x=x, r=r); }");
        assertEval("{ x <- array(1:3, 3, list(x=c('x1','x2','x3'))); r <- log(x); r; }");
        assertEval("{ y <- array(1:6, c(2,3), list(y=c('y1','y2'), x=c('x1','x2','x3'))); r <- log(y); r; }");
    }

    @Test
    public void testSideEffect() {
        assertEval("{ a <- c(1, 2, 4); foo <- function() { a[[1]] <<- 42; 33; }; log(a, foo()) }");
    }

}
