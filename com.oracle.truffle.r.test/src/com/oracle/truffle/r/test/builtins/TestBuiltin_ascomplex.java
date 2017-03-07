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
public class TestBuiltin_ascomplex extends TestBase {

    @Test
    public void testascomplex1() {
        assertEval("argv <- list(logical(0), logical(0));as.complex(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testascomplex2() {
        assertEval("argv <- list(FALSE, FALSE);as.complex(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testascomplex3() {
        assertEval(Ignored.Unknown, "argv <- list(' ');as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex4() {
        assertEval("argv <- list(structure(c(-0.626453810742332, 0.183643324222082, -0.835628612410047, 1.59528080213779, 0.329507771815361, -0.820468384118015, 0.487429052428485, 0.738324705129217, 0.575781351653492, -0.305388387156356, 1.51178116845085, 0.389843236411431, -0.621240580541804, -2.2146998871775, 1.12493091814311, -0.0449336090152309, -0.0161902630989461, 0.943836210685299, 0.821221195098089, 0.593901321217509, 0.918977371608218, 0.782136300731067, 0.0745649833651906, -1.98935169586337, 0.61982574789471), .Dim = c(5L, 5L), .Dimnames = list(c('1', '2', '3', '4', '5'), c('a', 'b', 'c', 'd', 'e'))));as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex5() {
        assertEval("argv <- list('1.3');as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex6() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'));as.complex(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testascomplex7() {
        assertEval("argv <- list(NA_complex_);as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex8() {
        assertEval("argv <- list(integer(0));as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex9() {
        assertEval("argv <- list(1L);as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex10() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a'));as.complex(argv[[1]]);");
    }

    @Test
    public void testascomplex11() {
        assertEval("argv <- list(NULL, NULL);as.complex(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testAsComplex() {
        assertEval("{ as.complex() }");
        assertEval("{ as.complex(0) }");
        assertEval("{ as.complex(TRUE) }");
        assertEval("{ as.complex(\"1+5i\") }");
        assertEval("{ as.complex(\"-1+5i\") }");
        assertEval("{ as.complex(\"-1-5i\") }");
        assertEval("{ as.complex(0/0) }");
        assertEval("{ as.complex(c(0/0, 0/0)) }");
        assertEval(Output.IgnoreWarningContext, "{ as.complex(c(\"1\",\"hello\")) }");
        assertEval(Output.IgnoreWarningContext, "{ as.complex(\"TRUE\") }");
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
        assertEval("{ as.complex(\"Inf\") }");
        assertEval("{ as.complex(\"NaN\") }");
        assertEval("{ as.complex(\"0x42\") }");
        assertEval("{ as.complex(NULL) }");

        assertEval("{ as.complex(\"1e10+5i\") }");
        assertEval("{ as.complex(\"-.1e10+5i\") }");
        assertEval(Ignored.Unknown, "{ as.complex(\"1e-2+3i\") }");
        assertEval(Ignored.Unknown, "{ as.complex(\"+.1e+2-3i\") }");

        assertEval("{ as.complex(list(42)) }");
        assertEval(Output.IgnoreErrorContext, "{ as.complex(list(NULL)) }");
        assertEval(Output.IgnoreWarningContext, "{ as.complex(list(\"foo\")) }");
    }
}
