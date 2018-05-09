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
public class TestBuiltin_isnan extends TestBase {

    @Test
    public void testisnan1() {
        assertEval("argv <- list(NA);is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan2() {
        assertEval("argv <- list(structure(1:7, .Names = c('a1', 'a2', 'a3', 'a4', 'a5', 'a6', 'a7')));is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan3() {
        assertEval("argv <- list(1:3);is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan4() {
        assertEval("argv <- list(structure(NaN, .Dim = c(1L, 1L)));is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan5() {
        assertEval("argv <- list(3.14159265358979);is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan6() {
        assertEval("argv <- list(NULL);is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan7() {
        assertEval("argv <- list(structure(1:3, .Dim = c(3L, 1L)));is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan9() {
        assertEval("argv <- list(c(-Inf, 2.17292368994844e-311, 4.34584737989688e-311, 8.69169475979376e-311, 1.73833895195875e-310, 3.4766779039175e-310, 6.953355807835e-310, 1.390671161567e-309, 2.781342323134e-309, 5.562684646268e-309, 1.1125369292536e-308, 2.2250738585072e-308, 4.4501477170144e-308, 8.90029543402881e-308, 1.78005908680576e-307, 2.2250738585072e-303, 2.2250738585072e-298, 1.79769313486232e+298, 1.79769313486232e+303, 2.24711641857789e+307, 4.49423283715579e+307, 8.98846567431158e+307, 1.79769313486232e+308, Inf, Inf, NaN, NA));is.nan(argv[[1]]);");
    }

    @Test
    public void testisnan10() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1), .Dim = 1:3));is.nan(argv[[1]]);");
    }

    @Test
    public void testisnanGenericDispatch() {
        assertEval("{ is.nan.cls <- function(x) 42; is.nan(structure(c(1,2), class='cls')); }");
    }
}
