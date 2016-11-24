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
public class TestBuiltin_whichmax extends TestBase {

    @Test
    public void testwhichmax1() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = 4:5, .Dimnames = list(c('I(a)', 'b', 'c', 'a'), c('I(a)', 'b', 'c', 'b:c', 'a:x')))); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax2() {
        assertEval("argv <- list(structure(c(TRUE, FALSE), .Names = c('d', 'I(as.numeric(d)^2)'))); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax3() {
        assertEval("argv <- list(c(7985.84636551931, 7366.07281363396, 7342.71367123673, 7315.48787041648, 7290.90503004105)); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax4() {
        assertEval("argv <- list(structure(c(NA, 87, 82, 75, 63, 50, 43, 32, 35, 60, 54, 55, 36, 39, NA, NA, 69, 57, 57, 51, 45, 37, 46, 39, 36, 24, 32, 23, 25, 32, NA, 32, 59, 74, 75, 60, 71, 61, 71, 57, 71, 68, 79, 73, 76, 71, 67, 75, 79, 62, 63, 57, 60, 49, 48, 52, 57, 62, 61, 66, 71, 62, 61, 57, 72, 83, 71, 78, 79, 71, 62, 74, 76, 64, 62, 57, 80, 73, 69, 69, 71, 64, 69, 62, 63, 46, 56, 44, 44, 52, 38, 46, 36, 49, 35, 44, 59, 65, 65, 56, 66, 53, 61, 52, 51, 48, 54, 49, 49, 61, NA, NA, 68, 44, 40, 27, 28, 25, 24, 24), .Tsp = c(1945, 1974.75, 4), class = 'ts')); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax5() {
        assertEval("argv <- list(NULL); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax6() {
        assertEval("argv <- list(list()); .Internal(which.max(argv[[1]]))");
    }

    @Test
    public void testwhichmax8() {
        assertEval("argv <- structure(list(x = c(NA, NA)), .Names = 'x');do.call('which.max', argv)");
    }

    @Test
    public void testWhichMax() {
        assertEval("{ which.max(c(5,5,5,5,5)) }");
        assertEval("{ which.max(c(1,2,3,4,5)) }");
        assertEval("{ which.max(c(2,4))}");
        assertEval("{ which.max(c(2L,4L,3L))}");
        assertEval("{ which.max(c(1,2,3,4,5))}");
        assertEval("{ which.max(c(TRUE, TRUE))}");
        assertEval("{ which.max(c(TRUE, FALSE))}");
        assertEval("{ which.max(c(1:5))}");
        assertEval("{ which.max(c(5:1))}");
        assertEval("{ which.max(c(1:10000))}");
    }
}
