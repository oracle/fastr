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
public class TestBuiltin_any extends TestBase {

    @Test
    public void testany1() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Tsp = c(1, 101, 1), class = 'ts'));any(argv[[1]]);");
    }

    @Test
    public void testany2() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE), .Dim = 3L, .Dimnames = list(c('A', 'B', 'C'))));any(argv[[1]]);");
    }

    @Test
    public void testany3() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Tsp = c(1949, 1960.91666666667, 12), class = 'ts'));any(argv[[1]]);");
    }

    @Test
    public void testany4() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE, TRUE), .Names = c('1', '2', '3', '4', '5'), .Dim = 5L, .Dimnames = list(c('1', '2', '3', '4', '5'))));any(argv[[1]]);");
    }

    @Test
    public void testany5() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c(14, 2, 0, 2, -7, 0), .Dim = c(3L, 2L)));any(argv[[1]]);");
    }

    @Test
    public void testany6() {
        assertEval("any( );");
    }

    @Test
    public void testany7() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = 3:4, .Dimnames = structure(list(x1 = c('a', 'b', 'c'), x2 = c('a', 'b', 'c', NA)), .Names = c('x1', 'x2'))));any(argv[[1]]);");
    }

    @Test
    public void testany8() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(4L, 3L), .Dimnames = list(c('<none>', 'Hair:Eye', 'Hair:Sex', 'Eye:Sex'), c('Df', 'Deviance', 'AIC'))));any(argv[[1]]);");
    }

    @Test
    public void testany9() {
        assertEval("argv <- list(c(1L, 1L, 1L, 1L, 1L));any(argv[[1]]);");
    }

    @Test
    public void testany10() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE), .Dim = 3L, .Dimnames = structure(list(c('1', '2', NA)), .Names = '')));any(argv[[1]]);");
    }

    @Test
    public void testany11() {
        assertEval("argv <- list(logical(0));any(argv[[1]]);");
    }

    @Test
    public void testany12() {
        assertEval("argv <- list(structure(FALSE, .Dim = c(1L, 1L)));any(argv[[1]]);");
    }

    @Test
    public void testany13() {
        assertEval("argv <- list(structure(logical(0), .Dim = c(0L, 0L)));any(argv[[1]]);");
    }

    @Test
    public void testany14() {
        assertEval("argv <- list(c(FALSE, TRUE, FALSE));any(argv[[1]]);");
    }

    @Test
    public void testany15() {
        assertEval("argv <- list(structure(c(NA, NA, NA, NA, NA, NA, NA, NA), .Names = c('base', 'utils', 'methods', 'grDevices', 'graphics', 'stats', 'lapack', 'R_X11')));any(argv[[1]]);");
    }

    @Test
    public void testany17() {
        assertEval(Ignored.Unknown, "argv <- list('NA');do.call('any', argv)");
    }

    @Test
    public void testAny() {
        assertEval("{ any(TRUE) }");
        assertEval("{ any(TRUE, TRUE, TRUE) }");
        assertEval("{ any(TRUE, FALSE) }");
        assertEval("{ any(TRUE, TRUE, NA) }");

        assertEval("{ any() }");
        assertEval("{ any(logical(0)) }");
        assertEval("{ any(FALSE) }");

        assertEval("{ any(NA, NA, NA) }");
        assertEval("{ any(NA) }");

        assertEval("{ any(TRUE, TRUE, NA,  na.rm=TRUE) }");
        assertEval("{ any(TRUE, FALSE, NA,  na.rm=TRUE) }");
        assertEval("{ any(FALSE, NA,  na.rm=TRUE) }");
        assertEval("{ any(FALSE, NA,  na.rm=FALSE) }");

        assertEval("{ any(NULL); }");

        // FIXME coercion warning missing
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ any(1) }");
        // FIXME coercion warning missing
        assertEval(Ignored.Unknown, Output.IgnoreWarningContext, "{ any(0) }");

        assertEval("{ d<-data.frame(c(1L,2L), c(10L, 20L)); any(d) }");
    }
}
