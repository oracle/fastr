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
public class TestBuiltin_prod extends TestBase {

    @Test
    public void testprod1() {
        assertEval("argv <- list(9L);prod(argv[[1]]);");
    }

    @Test
    public void testprod2() {
        assertEval("argv <- list(c(1000L, 1000L));prod(argv[[1]]);");
    }

    @Test
    public void testprod3() {
        assertEval("argv <- list(c(4, 2.5, 1.3, -1.20673076923077));prod(argv[[1]]);");
    }

    @Test
    public void testprod4() {
        assertEval("argv <- list(structure(c(4L, 4L, 2L), .Names = c('Hair', 'Eye', 'Sex')));prod(argv[[1]]);");
    }

    @Test
    public void testprod5() {
        assertEval("argv <- list(integer(0));prod(argv[[1]]);");
    }

    @Test
    public void testprod6() {
        assertEval("argv <- list(structure(c(2, 0, 1, 2), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))));prod(argv[[1]]);");
    }

    @Test
    public void testprod7() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))));prod(argv[[1]]);");
    }

    @Test
    public void testprod8() {
        assertEval("argv <- list(c(0.138260298853371, 0.000636169906925458));prod(argv[[1]]);");
    }

    @Test
    public void testprod9() {
        assertEval("argv <- list(NA_integer_);prod(argv[[1]]);");
    }

    @Test
    public void testprod10() {
        assertEval("prod( );");
    }

    @Test
    public void testprod11() {
        assertEval("argv <- list(numeric(0));prod(argv[[1]]);");
    }

    private static final String[] VALUES = {"FALSE", "TRUE", "1,FALSE", "2,TRUE", "c(2,4)", "c(2,4,3)", "c(2,4,NA)", "c(NA,2L,4L)", "1", "4L", "NA_integer_", "NA,c(1,2,3)", "c(1,2,NA),NA", "1,2,3,4",
                    "1L,NA,5+3i", "4+0i,6,NA", "numeric(),numeric()", "numeric()", "complex(),numeric()"};
    private static final String[] OPTIONS = {"", ",na.rm=TRUE", ",na.rm=FALSE"};

    @Test
    public void testProd() {
        assertEval(template("prod(%0%1)", VALUES, OPTIONS));
        assertEval("{ foo <- function(...) prod(...); foo(); }");
    }
}
