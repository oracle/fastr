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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_iscomplex extends TestBase {

    @Test
    public void testiscomplex1() {
        assertEval("argv <- list(integer(0));is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex2() {
        assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'));is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex3() {
        assertEval("argv <- list(structure(1:24, .Dim = 2:4));is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex4() {
        assertEval("argv <- list(NA_complex_);is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex5() {
        assertEval("argv <- list(1.3+0i);is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex6() {
        assertEval("argv <- list(complex(0));is.complex(argv[[1]]);");
    }

    @Test
    public void testiscomplex7() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.complex(argv[[1]]);");
    }
}
