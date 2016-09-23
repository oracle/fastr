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
public class TestBuiltin_lengthassign extends TestBase {

    @Test
    public void testlengthassign1() {
        assertEval("argv <- list(c('A', 'B'), value = 5);`length<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlengthassign2() {
        assertEval("argv <- list(list(list(2, 2, 6), list(2, 2, 0)), value = 0);`length<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlengthassign3() {
        assertEval("argv <- list(list(list(2, 2, 6), list(1, 3, 9), list(1, 3, -1)), value = 1);`length<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlengthassign4() {
        assertEval("argv <- list(c(28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1), value = 0L);`length<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlengthassign5() {
        assertEval("argv <- list(c(0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L), value = 0L);`length<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlengthassign6() {
        assertEval("argv <- list(list(), value = 0L);`length<-`(argv[[1]],argv[[2]]);");

        assertEval(Ignored.Unknown, "argv <- structure(list(1:3, value = TRUE), .Names = c('', 'value'));do.call('length<-', argv)");
    }

    @Test
    public void testLengthUpdate() {
        assertEval("{ x<-c(a=1, b=2); length(x)<-1; x }");
        assertEval("{ x<-c(a=1, b=2); length(x)<-4; x }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); length(x)<-1; x }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); length(x)<-4; x }");
        assertEval("{ x<-data.frame(a=1,b=2); length(x)<-1; attributes(x) }");
        assertEval("{ x<-data.frame(a=1,b=2); length(x)<-4; attributes(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); length(x)<-1; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); length(x)<-4; x }");
        assertEval("{ x <- 1:4 ; length(x) <- 2 ; x }");
        assertEval("{ x <- 1:2 ; length(x) <- 4 ; x }");
        assertEval("{ x <- 1 ; f <- function() { length(x) <<- 2 } ; f() ; x }");
        assertEval("{ x <- 1:2 ; z <- (length(x) <- 4) ; z }");
        assertEval("{ x<-c(a=7, b=42); length(x)<-4; x }");
        assertEval("{ x<-c(a=7, b=42); length(x)<-1; x }");
        assertEval("{ x<-NULL; length(x)<-2; x }");
    }

    @Test
    public void testArgsCasts() {
        assertEval("{ x<-quote(a); length(x)<-2 }");
        assertEval("{ x<-c(42, 1); length(x)<-'3'; x }");
        assertEval("{ x<-c(42, 1); length(x)<-3.1; x }");
        assertEval("{ x<-c(42, 1); length(x)<-c(1,2) }");
    }
}
