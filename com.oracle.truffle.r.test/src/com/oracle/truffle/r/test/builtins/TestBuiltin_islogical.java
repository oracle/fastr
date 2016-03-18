/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_islogical extends TestBase {

    @Test
    public void testislogical1() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(22L, 13L), .Dimnames = list(c('r39', 'r17', 'r39', 'r14', 'r39', 'r8', 'r25', 'r9', 'r17', 'r27', 'r17', 'r14', 'r39', 'r27', 'r9', 'r25', 'r8', 'r17', 'r9', 'r8', 'r25', 'r5'), c('c4', 'c1', 'c13', 'c13', 'c1', 'c20', 'c20', 'c13', 'c20', 'c8', 'c8', 'c8', 'c13'))));is.logical(argv[[1]]);");
    }

    @Test
    public void testislogical2() {
        assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'));is.logical(argv[[1]]);");
    }

    @Test
    public void testislogical3() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.logical(argv[[1]]);");
    }

    @Test
    public void testislogical4() {
        assertEval("argv <- list(structure(c(1, 4.16333634234434e-17, 5.55111512312578e-17, -1.38777878078145e-17, 2.77555756156289e-17, 4.16333634234434e-17, 1, -1.11022302462516e-16, -2.77555756156289e-17, -5.55111512312578e-17, 5.55111512312578e-17, -1.11022302462516e-16, 1, 5.55111512312578e-17, 0, -1.38777878078145e-17, -2.77555756156289e-17, 5.55111512312578e-17, 1, -1.11022302462516e-16, 2.77555756156289e-17, -5.55111512312578e-17, 0, -1.11022302462516e-16, 1), .Dim = c(5L, 5L)));is.logical(argv[[1]]);");
    }

    @Test
    public void testislogical5() {
        assertEval("argv <- list(structure(c(1, 0, 0, 0, NA, 6, 0, 0, 0, 14, 3, 0, 15, 0, 0, 8), .Dim = c(4L, 4L)));is.logical(argv[[1]]);");
    }

    @Test
    public void testislogical7() {
        assertEval("argv <- list(c(FALSE, TRUE, FALSE));do.call('is.logical', argv)");
    }
}
