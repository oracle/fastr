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

public class TestBuiltin_levelsassign_ extends TestBase {

    @Test
    public void testlevelsassign_1() {
        assertEval("argv <- structure(list(structure(c(4L, 4L, 3L, 6L, 5L, 4L, 4L,     5L, 4L, 4L, 2L, 1L, 3L, 2L, 2L, 3L, 4L, 3L, 2L, 2L, 2L, 4L,     3L, 2L, 6L, 3L, 6L, 2L, 3L, 5L, 2L, 6L, 4L, 2L, 1L, 6L, 6L,     4L, 5L, 3L, 5L, 5L, 6L, 2L, 4L, 3L, 4L, 5L, 4L, 4L, 2L, 6L,     1L, 3L, 5L, 4L, 5L, 2L, 5L, 1L, 6L, 5L, 3L, 6L, 3L, 6L, 3L,     6L, 4L, 6L, 1L, 3L, 2L, 5L, 3L, 3L, 2L, 4L, 3L, 4L, 2L, 1L,     4L, 5L, 3L, 6L, 3L, 6L, 6L, 6L, 4L, 4L, 6L, 4L, 2L, 2L, 3L,     3L, 3L, 5L, 3L, 1L, 5L, 6L, 6L, 6L, 2L, 3L, 3L, 5L, 4L, 2L,     2L, 5L, 2L, 3L, 1L, 3L, 3L, 6L, 1L, 6L, 5L, 4L, 3L, 1L, 1L,     1L, 3L, 5L, 4L, 4L, 1L, 1L, 6L, 2L, 6L, 6L, 1L, 5L, 6L, 6L,     2L, 4L, 6L, 6L, 4L, 4L, 5L, 4L, 4L, 1L, 3L, 1L, 2L, 6L, 6L,     1L, 2L, 6L, 3L, 5L, 4L, 6L, 5L, 3L, 4L, 2L, 5L, 2L, 3L, 2L,     1L, 5L, 1L, 1L, 6L, 6L, 1L, 4L, 2L, 3L, 4L, 3L, 3L, 5L, 3L,     6L, 4L, 3L, 5L, 3L, 5L, 5L, 4L, 2L, 2L, 4L, 1L, 6L, 5L, 6L,     3L, 5L, 1L, 2L, 1L, 3L, 5L, 1L, 4L, 1L, 2L, 4L, 5L, 4L, 6L,     3L, 5L, 6L, 1L, 3L, 4L, 6L, 5L, 5L, 1L, 3L, 2L, 6L, 5L, 5L,     4L, 3L, 5L, 3L, 6L, 5L, 4L, 4L, 2L, 4L, 5L, 2L, 5L, 3L, 1L,     1L, 6L, 5L, 6L, 6L, 1L, 5L, 5L, 3L, 6L, 6L, 1L, 4L, 3L, 6L,     4L, 4L, 6L, 4L, 4L, 3L, 5L, 4L, 1L, 2L, 1L, 5L, 2L, 6L, 4L,     1L, 3L, 4L, 4L, 2L, 4L, 5L, 6L, 5L, 5L, 5L, 3L, 1L, 6L, 2L,     2L, 2L, 6L, 4L, 2L, 5L, 4L, 3L, 4L, 3L, 1L, 2L, 2L, 3L, 4L,     5L, 6L, 4L, 3L, 4L, 6L, 3L, 3L, 5L, 6L, 3L, 3L, 3L, 1L, 2L,     4L, 5L, 2L, 5L, 4L, 4L, 2L, 4L, 3L, 1L, 3L, 3L, 6L, 5L, 4L,     2L, 1L, 4L, 4L, 1L, 4L, 3L, 4L, 3L, 1L, 4L, 6L, 2L, 5L, 6L,     3L, 6L, 2L, 1L, 6L, 6L, 2L, 6L, 6L, 5L, 2L, 2L, 4L, 6L, 6L,     5L, 2L, 3L, 3L, 1L, 3L, 4L, 3L, 5L, 5L, 2L, 4L, 2L, 2L, 6L,     3L, 6L, 4L, 4L, 1L, 4L, 5L, 2L, 2L, 4L, 6L, 5L, 4L, 3L, 2L,     4L, 4L, 6L, 5L, 4L, 4L, 6L, 4L, 4L, 1L, 5L, 2L, 1L, 6L, 5L,     4L, 2L, 5L, 4L, 2L, 4L, 6L, 1L, 6L, 5L, 4L, 5L, 1L, 4L, 6L,     2L, 4L, 4L, 2L, 3L, 2L, 1L, 5L, 2L, 4L, 5L, 2L, 5L, 3L, 2L,     3L, 6L, 6L, 3L, 1L, 3L, 2L, 6L, 5L, 5L, 4L, 3L, 3L, 6L, 5L,     2L, 5L, 4L, 5L, 1L, 2L, 6L, 2L, 6L, 3L, 5L, 6L, 1L, 6L, 3L,     4L, 2L, 1L, 6L, 2L, 5L, 5L, 4L, 3L, 2L, 2L, 2L, 1L, 2L, 6L,     1L, 5L, 1L, 3L, 1L, 1L, 6L, 4L, 5L, 2L, 4L, 2L, 5L, 3L, 4L,     1L, 2L, 5L, 1L, 1L, 2L, 6L, 2L, 4L, 3L, 3L, 4L, 4L, 5L, 5L,     6L, 1L, 4L, 2L, 2L, 3L, 3L, 3L, 6L, 3L, 5L, 4L, 4L, 3L, 3L,     3L, 3L, 5L, 4L, 5L, 1L, 4L, 4L, 5L, 6L, 4L, 5L, 1L, 6L, 2L,     1L, 3L, 6L, 3L, 2L, 5L, 1L, 3L, 2L, 3L, 3L, 2L, 5L, 3L, 5L,     5L, 4L, 6L, 6L, 5L, 6L, 6L, 3L, 4L, 2L, 4L, 2L, 3L, 1L, 4L,     5L, 4L, 1L, 5L, 4L, 5L, 6L, 3L, 5L, 6L, 5L, 1L, 2L, 2L, 4L,     6L, 4L, 5L, 6L, 3L, 4L, 2L, 1L, 2L, 5L, 3L, 6L, 5L, 5L, 5L,     3L, 5L, 5L, 2L, 2L, 3L, 2L, 5L, 5L, 4L, 5L, 1L, 5L, 2L, 5L,     4L, 2L, 4L, 6L, 3L, 6L, 3L, 1L, 6L, 5L, 4L, 5L, 6L, 4L, 5L,     2L, 1L, 3L, 6L, 1L, 5L, 1L, 2L, 5L, 2L, 1L, 6L, 4L, 1L, 6L,     3L, 2L, 2L, 4L, 5L, 5L, 5L, 3L, 3L, 1L, 4L, 2L, 4L, 6L, 1L,     3L, 1L, 6L, 3L, 2L, 1L, 3L, 3L, 4L, 1L, 3L, 3L, 5L, 1L, 2L,     2L, 5L, 2L, 4L, 3L, 2L, 3L, 3L, 6L, 5L, 1L, 4L, 3L, 4L, 5L,     5L, 1L, 5L, 6L, 5L, 2L, 2L, 3L, 5L, 3L, 1L, 2L, 5L, 5L, 1L,     3L, 4L, 3L, 3L, 6L, 5L, 2L, 5L, 5L, 2L, 6L, 2L, 1L, 1L, 2L,     6L, 4L, 5L, 1L, 2L, 1L, 1L, 4L, 4L, 1L, 3L, 5L, 4L, 4L, 3L,     4L, 5L, 3L, 4L, 5L, 1L, 3L, 2L, 3L, 4L, 3L, 5L, 3L, 2L, 4L,     5L, 1L, 2L, 4L, 3L, 6L, 3L, 6L, 3L, 6L, 3L, 4L, 3L, 2L, 3L,     6L, 2L, 4L, 1L, 1L, 2L, 2L, 5L, 3L, 2L, 3L, 6L, 2L, 3L, 2L,     5L, 5L, 2L, 3L, 3L, 5L, 3L, 5L, 4L, 6L, 2L, 2L, 1L, 5L, 4L,     4L, 4L, 1L, 6L, 6L, 3L, 2L, 3L, 6L, 4L, 4L, 4L, 4L, 4L, 5L,     3L, 5L, 6L, 5L, 2L, 4L, 6L, 5L, 6L, 5L, 5L, 1L, 3L, 6L, 3L,     2L, 2L, 4L, 4L, 2L, 5L, 4L, 4L, 6L, 4L, 5L, 5L, 5L, 3L, 6L,     4L, 6L, 5L, 6L, 4L, 4L, 6L, 2L, 3L, 5L, 5L, 2L, 5L, 4L, 4L,     1L, 4L, 2L, 6L, 2L, 1L, 4L, 2L, 6L, 4L, 2L, 3L, 4L, 6L, 6L,     2L, 3L, 4L, 3L, 2L, 3L, 5L, 2L, 6L, 4L, 4L, 1L, 5L, 3L, 6L,     1L, 2L, 3L, 5L, 5L, 5L, 5L, 3L, 5L, 4L, 5L, 6L, 4L, 5L, 5L,     3L, 4L, 4L, 2L, 4L, 3L, 4L, 6L, 3L, 5L, 2L, 5L, 5L, 4L, 2L,     1L, 6L, 2L, 4L, 6L, 3L, 3L, 6L, 5L, 6L, 1L, 5L, 2L, 4L, 6L,     4L, 5L, 3L, 2L, 6L, 1L, 3L, 3L, 3L, 2L, 4L, 3L, 2L, 5L, 5L,     4L, 2L, 6L, 6L, 2L, 6L, 3L, 6L, 1L, 4L, 6L, 4L, 6L, 6L, 1L,     5L, 1L, 3L, 1L, 6L, 1L, 3L, 2L, 3L, 2L, 2L, 4L, 1L, 3L, 1L,     5L, 3L, 5L, 4L, 3L, 2L, 3L, 2L, 3L, 3L, 6L, 1L, 2L, 6L, 5L,     2L, 6L, 2L, 6L, 5L, 3L, 1L, 2L, 2L, 4L, 5L, 4L, 6L, 5L, 3L,     4L, 2L, 5L, 6L, 4L, 1L, 5L, 3L, 1L, 5L, 4L, 2L, 2L, 5L, 2L,     5L, 4L, 3L, 5L, 3L, 2L, 2L, 3L, 2L, 1L, 4L, 5L, 4L, 6L, 3L,     6L, 3L, 6L, 4L, 1L, 6L, 6L, 4L, 1L, 5L, 2L, 5L, 5L, 4L, 2L,     5L, 4L, 6L, 4L, 6L, 3L, 4L, 1L, 4L, 3L, 1L, 4L, 5L, 3L, 4L,     1L, 5L, 5L, 1L, 2L, 1L, 4L, 1L, 5L, 5L, 4L, 4L, 6L, 6L, 4L,     6L, 4L, 2L, 2L, 3L, 5L, 1L, 2L, 3L, 6L, 3L, 4L, 4L, 2L, 4L,     2L, 3L, 3L, 2L, 1L, 1L, 3L, 2L, 2L, 1L, 1L, 6L, 3L, 3L, 6L,     1L, 6L, 4L, 2L, 4L, 2L, 1L, 1L, 4L, 4L, 4L, 6L, 4L, 4L, 6L,     2L, 3L, 3L, 3L, 2L, 2L, 4L, 4L, 6L, 5L, 5L, 3L, 4L, 5L, 4L,     1L, 3L, 1L, 5L, 5L, 6L, 5L, 1L, 5L, 3L, 6L, 3L, 4L, 6L, 3L,     3L, 5L, 1L, 5L, 2L, 3L, 2L, 2L, 2L, 2L, 4L, 4L, 2L, 5L, 4L,     1L, 2L, 3L, 1L, 4L, 2L, 1L, 6L, 4L, 1L, 4L, 2L, 5L, 4L, 3L,     5L, 2L, 1L, 1L, 4L, 3L, 2L, 3L, 1L, 2L, 6L, 6L, 1L, 3L, 4L,     1L, 5L, 6L, 4L, 4L), .Label = c('(0,25]', '(25,35]', '(35,45]',     '(45,55]', '(55,65]', '(65,99]'), class = 'factor'), value = c('age1824',     'age2534', 'age3544', 'age4554', 'age5564', 'age6599')),     .Names = c('', 'value'));" +
                        "do.call('levels<-', argv)");
    }
}
