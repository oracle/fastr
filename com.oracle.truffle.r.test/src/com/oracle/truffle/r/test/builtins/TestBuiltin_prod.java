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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_prod extends TestBase {

    @Test
    public void testprod1() {
        assertEval("argv <- list(9L);prod(argv[[1]]);");
    }

    @Test
    public void testprod2() {
        assertEval(Ignored.Unknown, "argv <- list(c(1000L, 1000L));prod(argv[[1]]);");
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
        assertEval(Ignored.Unknown, "argv <- list(integer(0));prod(argv[[1]]);");
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
        assertEval(Ignored.Unknown, "argv <- list(NA_integer_);prod(argv[[1]]);");
    }

    @Test
    public void testprod10() {
        assertEval(Ignored.Unknown, "prod( );");
    }

    @Test
    public void testprod11() {
        assertEval(Ignored.Unknown, "argv <- list(numeric(0));prod(argv[[1]]);");
    }

    @Test
    public void testProd() {
        assertEval("{prod(c(2,4))}");
        assertEval("{prod(c(2,4,3))}");
        assertEval("{prod(c(1,2,3,4,5))}");
        assertEval("{prod(c(1+2i))}");
        assertEval("{prod(c(1+2i, 2+3i))}");
        assertEval("{prod(c(1+2i,1+3i,1+45i))}");
        assertEval("{prod(c(TRUE, TRUE))}");
        assertEval("{prod(c(TRUE, FALSE))}");
    }

    @Test
    public void testProdNa() {
        assertEval(Ignored.Unknown, "{prod(c(2,4,NA))}");
        assertEval(Ignored.Unknown, "{prod(c(2,4,3,NA),TRUE)}");
        assertEval(Ignored.Unknown, "{prod(c(1,2,3,4,5,NA),FALSE)}");
    }
}
