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
public class TestBuiltin_chartr extends TestBase {

    @Test
    public void testchartr1() {
        assertEval("argv <- list('.', '.', c('0.02', '0.06', '0.11', '0.22', '0.56', '1.1')); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testchartr2() {
        assertEval("argv <- list('iXs', 'why', 'MiXeD cAsE 123'); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testchartr3() {
        assertEval("argv <- list('a-cX', 'D-Fw', 'MiXeD cAsE 123'); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testchartr4() {
        assertEval("argv <- list('.', '.', character(0)); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testchartr6() {
        assertEval("argv <- structure(list(old = 'NA', new = 'na', x = c('NA', NA,     'BANANA')), .Names = c('old', 'new', 'x'));do.call('chartr', argv)");
    }

    @Test
    public void tests() {
        assertEval("chartr(c('abq'), 'cd', c('agbc', 'efb'))");
        assertEval("chartr(c('a','b'), c('c', 'e'), c('abc', 'efb'))");
        assertEval("chartr(c('a','b'), c(3, 2), c('abc', 'efb'))");
        assertEval("chartr(c(3, 2), c('q', 'c'), c('abc', 'efb'))");
        assertEval("chartr('0-5', '0-3', 'ah3g4t')");
        assertEval("chartr('0-5', '0-', 'ah3g4t')");
        assertEval("chartr('0-5', '045', 'ah3g4t')");
    }
}
