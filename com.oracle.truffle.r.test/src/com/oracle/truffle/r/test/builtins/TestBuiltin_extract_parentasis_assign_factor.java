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

public class TestBuiltin_extract_parentasis_assign_factor extends TestBase {

    @Test
    public void testextract_parentasis_assign_factor1() {
        assertEval("argv <- structure(list(x = structure(c(4L, 1L, 4L, 4L, 6L, 4L,     5L, 5L, 4L, 6L, 6L, 2L, 3L, 6L, 4L, 2L, 1L, 6L, 1L, 3L, 3L,     5L, 2L, 2L, 2L, 5L, 3L, 3L, 1L, 2L, 5L, 6L, 6L, 6L, 6L, 2L,     6L, 1L, 5L, 1L, 2L, 4L, 4L, 6L, 5L, 5L, 2L, 6L, 4L, 6L, 5L,     1L, 2L, 5L, 1L, 1L, 4L, 3L, 3L, 4L, 4L, 2L, 5L, 3L, 4L, 5L,     4L, 6L, 4L, 5L, 2L, 6L, 2L, 4L, 2L, 2L, 4L, 4L, 1L, 6L, 2L,     1L, 5L, 3L, 5L, 1L, 2L, 2L, 4L, 2L, 4L, 2L, 5L, 6L, 5L, 6L,     3L, 1L, 2L, 4L, 6L, 6L, 3L, 3L, 2L, 6L, 2L, 5L, 3L, 4L, 3L,     4L, 6L, 3L, 4L, 2L, 3L, 1L, 6L, 2L, 4L, 4L, 1L, 3L, 4L, 3L,     4L, 1L, 4L, 1L, 3L, 5L, 5L, 5L, 4L, 4L, 6L, 2L, 6L, 3L, 2L,     1L, 1L, 6L, 2L, 2L, 5L, 1L, 5L, 3L, 2L, 2L, 5L, 1L, 6L, 3L,     6L, 4L, 2L, 2L, 5L, 6L, 6L, 1L, 1L, 6L, 6L, 5L, 2L, 5L, 6L,     5L, 4L, 6L, 2L, 5L, 4L, 3L, 5L, 1L, 3L, 4L, 4L, 3L, 1L, 1L,     5L, 4L, 1L, 3L, 5L, 4L, 5L, 4L, 6L, 6L, 2L, 4L, 3L, 3L),     .Label = c('a', 'b', 'c', 'd', 'e', 'f'), class = 'factor'),     c(189L, 84L, 154L, 9L, 130L, 44L, 137L, 12L, 50L, 1L, 42L,         174L, 194L, 131L, 157L, 101L, 37L, 128L, 117L, 181L,         51L, 109L, 110L, 67L, 69L, 124L, 192L, 65L, 171L, 168L),     value = NA), .Names = c('x', '', 'value'));" +
                        "do.call('[<-.factor', argv)");
    }
}
