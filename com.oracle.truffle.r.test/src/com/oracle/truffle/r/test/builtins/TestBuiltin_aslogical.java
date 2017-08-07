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
public class TestBuiltin_aslogical extends TestBase {

    @Test
    public void testaslogical1() {
        assertEval("argv <- list(structure(c(0L, 0L, 0L, 1L), .Names = c('Y', 'B', 'V', 'N')));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical2() {
        assertEval("argv <- list(structure(c(-4, 1), .Names = c('', '')));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical3() {
        assertEval("argv <- list(structure(c(1L, 0L, 0L, 0L, 0L), .Names = c('bibtype', NA, NA, NA, NA)));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical4() {
        assertEval("argv <- list(c(1L, NA, 0L));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical7() {
        assertEval("argv <- list('FALSE');as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical8() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(0L, 0L), .Dimnames = list(NULL, NULL)));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical9() {
        assertEval("argv <- list(c(3.74165738677394, 0, 8.55235974119758, 1.96396101212393));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical10() {
        assertEval("argv <- list(NULL);as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical11() {
        assertEval("argv <- list(structure('TRUE', .Names = '.registration'));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical12() {
        assertEval("argv <- list(structure(c(0, 1, 2, 2), .Dim = c(4L, 1L), .Dimnames = list(c('Y', 'B', 'V', 'N'), NULL)));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical13() {
        assertEval("argv <- list(c(TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical14() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a'));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical15() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'));as.logical(argv[[1]]);");
    }

    @Test
    public void testaslogical17() {
        assertEval("argv <- list(c(1, 2, 3, 4, 5, NA, NA, 2, 3, 4, 5, 6));as.logical(argv[[1]]);");
    }

    @Test
    public void testAsLogical() {
        assertEval("{ as.logical(1) }");
        assertEval("{ as.logical(\"false\") }");
        assertEval("{ as.logical(\"dummy\") }"); // no warning produced (as it should be)
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
        assertEval("{ as.logical(c(\"1\",\"hello\")) }");
        assertEval("{ as.logical(\"TRUE\") }");
        assertEval("{ as.logical(10+2i) }");
        assertEval("{ as.logical(c(3+3i, 4+4i)) }");
        assertEval("{ as.logical(NULL) }");
        assertEval("{ as.logical.cls <- function(x) 42; as.logical(structure(c(1,2), class='cls')); }");
    }
}
