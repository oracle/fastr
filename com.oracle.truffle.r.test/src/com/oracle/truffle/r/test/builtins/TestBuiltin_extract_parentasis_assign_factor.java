/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_extract_parentasis_assign_factor extends TestBase {

    @Test
    public void testextract_parentasis_assign_factor1() {
        assertEval("argv <- structure(list(x = structure(c(4L, 1L, 4L, 4L, 6L, 4L,     5L, 5L, 4L, 6L, 6L, 2L, 3L, 6L, 4L, 2L, 1L, 6L, 1L, 3L, 3L,     5L, 2L, 2L, 2L, 5L, 3L, 3L, 1L, 2L, 5L, 6L, 6L, 6L, 6L, 2L,     6L, 1L, 5L, 1L, 2L, 4L, 4L, 6L, 5L, 5L, 2L, 6L, 4L, 6L, 5L,     1L, 2L, 5L, 1L, 1L, 4L, 3L, 3L, 4L, 4L, 2L, 5L, 3L, 4L, 5L,     4L, 6L, 4L, 5L, 2L, 6L, 2L, 4L, 2L, 2L, 4L, 4L, 1L, 6L, 2L,     1L, 5L, 3L, 5L, 1L, 2L, 2L, 4L, 2L, 4L, 2L, 5L, 6L, 5L, 6L,     3L, 1L, 2L, 4L, 6L, 6L, 3L, 3L, 2L, 6L, 2L, 5L, 3L, 4L, 3L,     4L, 6L, 3L, 4L, 2L, 3L, 1L, 6L, 2L, 4L, 4L, 1L, 3L, 4L, 3L,     4L, 1L, 4L, 1L, 3L, 5L, 5L, 5L, 4L, 4L, 6L, 2L, 6L, 3L, 2L,     1L, 1L, 6L, 2L, 2L, 5L, 1L, 5L, 3L, 2L, 2L, 5L, 1L, 6L, 3L,     6L, 4L, 2L, 2L, 5L, 6L, 6L, 1L, 1L, 6L, 6L, 5L, 2L, 5L, 6L,     5L, 4L, 6L, 2L, 5L, 4L, 3L, 5L, 1L, 3L, 4L, 4L, 3L, 1L, 1L,     5L, 4L, 1L, 3L, 5L, 4L, 5L, 4L, 6L, 6L, 2L, 4L, 3L, 3L),     .Label = c('a', 'b', 'c', 'd', 'e', 'f'), class = 'factor'),     c(189L, 84L, 154L, 9L, 130L, 44L, 137L, 12L, 50L, 1L, 42L,         174L, 194L, 131L, 157L, 101L, 37L, 128L, 117L, 181L,         51L, 109L, 110L, 67L, 69L, 124L, 192L, 65L, 171L, 168L),     value = NA), .Names = c('x', '', 'value'));"
                        + "do.call('[<-.factor', argv)");
    }
}
