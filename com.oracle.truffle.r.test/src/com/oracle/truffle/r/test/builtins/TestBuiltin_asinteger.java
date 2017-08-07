/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_asinteger extends TestBase {

    @Test
    public void testasinteger1() {
        assertEval("argv <- list(structure(c(4L, 5L, 3L, 2L, 2L, 1L, 6L), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables', 'R Core'), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger2() {
        // FIXME according to docs a leading whitespace should be accepted
        assertEval(Ignored.ImplementationError,
                        "argv <- list(c('   33', '   34', '   35', '   36', '   37', '   38', '   18', '   19', '   20', '   21', '   22', '   23', '   36', '   37', '   38', '   39'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger3() {
        // FIXME combination of Inf and a number causes AssertionError
        assertEval(Ignored.ImplementationError,
                        "argv <- list(c(-Inf, -8.5, -2.83333333333333, -1.41666666666667, -0.85, -0.566666666666666, -0.404761904761905, -0.303571428571428, -0.236111111111111, -0.188888888888889));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger4() {
        assertEval("argv <- list(c(0, 1, NA, NA, 1, 1, -1, 1, 3, -2, -2, 7, -1, -1, -1, -1, -1, -1, -1, -1, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger6() {
        assertEval("argv <- list(2e+05);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger7() {
        assertEval("argv <- list(NULL);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger8() {
        assertEval("argv <- list(list(7L, 20, 0L, 1));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger9() {
        assertEval("argv <- list('-1');as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger10() {
        assertEval("argv <- list(c('1', NA, '0'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger11() {
        // FIXME combination of a large number and a number causes AssertionError
        assertEval(Ignored.ImplementationError, "argv <- list(c('3', '14159265358979'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger12() {
        assertEval("argv <- list(TRUE);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger13() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0), .Dim = c(13L, 1L), .Dimnames = list(c('59', '115', '156', '268', '329', '431', '448', '477', '638', '803', '855', '1040', '1106'), NULL)));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger14() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger15() {
        assertEval("argv <- list(character(0));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger16() {
        assertEval("argv <- list(4999.0000000001);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger17() {
        assertEval("argv <- list(structure(c(100, -1e-13, Inf, -Inf, NaN, 3.14159265358979, NA), .Names = c(' 100', '-1e-13', ' Inf', '-Inf', ' NaN', '3.14', '  NA')));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger18() {
        assertEval("argv <- list(structure(c(1L, 2L, 3L, 2L), .Label = c('1', '2', NA), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger19() {
        assertEval("argv <- list(structure(c(NA, 1L, NA, 2L, 1L, NA, NA, 1L, 4L, 1L, NA, 4L, 1L, 3L, NA, 4L, 2L, 2L, NA, 4L, 4L, 2L, 4L, 4L, 2L, 1L, 4L, 4L, 3L, 1L, 1L, 4L, 1L, 4L, NA, 1L, 4L, 4L, 2L, 2L, 4L, 4L, 3L, 4L, 2L, 2L, 3L, 3L, 4L, 1L, 1L, 1L, 4L, 1L, 4L, 4L, 4L, 4L, NA, 4L, 4L, 4L, NA, 1L, 2L, 3L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 4L, 1L, 4L, NA, 4L, 2L, 1L, 4L, 1L, 1L, 1L, 4L, 4L, 2L, 4L, 1L, 1L, 1L, 4L, 1L, 1L, 1L, 4L, 3L, 1L, 4L, 3L, 2L, 4L, 3L, 1L, 4L, 2L, 4L, NA, 4L, 4L, 4L, 2L, 1L, 4L, 4L, NA, 2L, 4L, 4L, 1L, 1L, 1L, 1L, 4L, 1L, 2L, 3L, 2L, 1L, 4L, 4L, 4L, 1L, NA, 4L, 2L, 2L, 2L, 4L, 4L, 3L, 3L, 4L, 2L, 4L, 3L, 1L, 1L, 4L, 2L, 4L, 3L, 1L, 4L, 3L, 4L, 4L, 1L, 1L, 4L, 4L, 3L, 1L, 1L, 2L, 1L, 3L, 4L, 2L, 2L, 2L, 4L, 4L, 3L, 2L, 1L, 1L, 4L, 1L, 1L, 2L, NA, 2L, 3L, 3L, 2L, 1L, 1L, 1L, 1L, 4L, 4L, 4L, 4L, 4L, 4L, 2L, 2L, 1L, 4L, 1L, 4L, 3L, 4L, 2L, 3L, 1L, 3L, 1L, 4L, 1L, 4L, 1L, 4L, 3L, 3L, 4L, 4L, 1L, NA, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 3L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 2L, NA, 4L, 4L, 4L, 4L, 1L, 2L, 1L, 1L, 2L, 1L, 4L, 2L, 3L, 1L, 4L, 4L, 4L, 1L, 2L, 1L, 4L, 2L, 1L, 3L, 1L, 2L, 2L, 1L, 2L, 1L, NA, 3L, 2L, 2L, 4L, 1L, 4L, 4L, 2L, 4L, 4L, 4L, 2L, 1L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 1L, 3L, 4L, 3L, 4L, 1L, NA, 4L, NA, 1L, 1L, 1L, 4L, 4L, 4L, 4L, 2L, 4L, 3L, 2L, NA, 1L, 4L, 4L, 3L, 4L, 4L, 4L, 2L, 4L, 2L, 1L, 4L, 4L, NA, 4L, 4L, 3L, 3L, 4L, 2L, 2L, 4L, 1L, 4L, 4L, 4L, 3L, 4L, 4L, 4L, 3L, 2L, 1L, 3L, 1L, 4L, 1L, 4L, 2L, NA, 1L, 4L, 4L, 3L, 1L, 4L, 1L, 4L, 1L, 4L, 4L, 1L, 2L, 2L, 1L, 4L, 1L, 1L, 4L, NA, 4L, NA, 4L, 4L, 4L, 1L, 4L, 2L, 1L, 2L, 2L, 2L, 2L, 1L, 1L, 2L, 1L, 4L, 2L, 3L, 3L, 1L, 3L, 1L, 4L, 1L, 3L, 2L, 2L, 4L, 1L, NA, 3L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 4L, 3L, 4L, 4L, 3L, 2L, 1L, 4L, 4L, 2L, 4L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 4L, 4L, 1L, 1L, 4L, 1L, 4L, 4L, 4L, 1L, 1L, NA, 3L, 2L, 4L, 4L, 4L, 4L, 2L, 3L, 3L, 2L, NA, 4L, 2L, 4L, 4L, 1L, 1L, 4L, 4L, 1L, 1L, 4L, 1L, 2L, 2L, 2L, 2L, 1L, 4L, 4L, 1L, 2L, 2L, 2L, 3L, 4L, 4L, 3L, 4L, 1L, 1L, 4L, 4L, NA, 4L, 1L, 4L, 4L, 4L, 1L, 4L, 4L, 1L, 2L, 4L, 4L, 4L, 4L, 1L, 2L, 4L, 4L, 2L, 1L, 4L, 2L, 4L, 2L, 2L, 4L, 1L, 3L, 3L, 2L, 4L, 1L, 4L, 4L, 4L, 1L, NA, 4L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 2L, NA, 4L, 2L, 4L, 3L, 1L, 4L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 2L, 1L, 4L, 1L, 3L, 3L, 1L, 4L, 4L, 2L, 4L, 4L, 4L, 4L, 3L, 2L, 3L, 3L, 2L, NA, 3L, 4L, 4L, 3L, 3L, 4L, 4L, 4L, 1L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 2L, 4L, 2L, 3L, 4L, 1L, 3L, 1L, NA, 4L, 1L, 2L, 2L, 1L, 4L, 3L, 3L, 4L, 1L, 1L, 3L), .Label = c('(1) Approve STRONGLY', '(2) Approve SOMEWHAT', '(3) Disapprove SOMEWHAT', '(4) Disapprove STRONGLY'), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger20() {
        assertEval("argv <- list(39);as.integer(argv[[1]]);");
    }

    @Test
    public void testAsInteger() {
        assertEval("{ as.integer() }");
        assertEval("{ as.integer(\"1\") }");
        assertEval("{ as.integer(c(\"1\",\"2\")) }");
        assertEval("{ as.integer(c(1,2,3)) }");
        assertEval("{ as.integer(c(1.0,2.5,3.9)) }");
        assertEval("{ as.integer(0/0) }");
        assertEval("{ as.integer(-0/0) }");
        assertEval("{ as.integer(as.raw(c(1,2,3,4))) }");
        assertEval(Output.IgnoreWarningContext, "{ as.integer(10+2i) }");
        assertEval(Output.IgnoreWarningContext, "{ as.integer(c(3+3i, 4+4i)) }");
        assertEval("{ as.integer(10000000000000) }");
        assertEval("{ as.integer(list(c(1),2,3)) }");
        assertEval("{ as.integer(list(integer(),2,3)) }");
        assertEval("{ as.integer(list(list(1),2,3)) }");
        assertEval("{ as.integer(list(1,2,3,list())) }");
        assertEval("{ as.integer(10000000000) }");
        assertEval("{ as.integer(-10000000000) }");
        assertEval(Output.IgnoreWarningContext, "{ as.integer(c(\"1\",\"hello\")) }");
        assertEval(Output.IgnoreWarningContext, "{ as.integer(\"TRUE\") }");
        assertEval("{ as.integer(as.raw(1)) }");
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
        assertEval("{ as.integer(1.1:5.1) }");
        assertEval("{ as.integer(NULL) }");
        assertEval("{ as.integer(\"\") }");
        assertEval("{ as.integer(as.character(NA)) }");
        assertEval("{ as.integer(\"1\", as.character(NA)) }");
        assertEval("{ as.integer.cls <- function(x) 42; as.integer(structure(c(1,2), class='cls')); }");
    }
}
