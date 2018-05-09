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

public class TestBuiltin_allequalfactor extends TestBase {

    @Test
    public void testallequalfactor1() {
        assertEval("argv <- structure(list(target = structure(c(4L, 5L, 1L, 5L, 3L,     4L, 5L, 3L, 2L, 4L), .Label = c('a', 'c', 'i', 's', 't'),     class = 'factor', contrasts = structure(c(1, 0, 0, 0, -1,         0, 1, 0, 0, -1, -0.247125681008604, -0.247125681008604,         -0.149872105789645, 0.891249148815458, -0.247125681008604,         0.268816352031209, 0.268816352031209, -0.881781351530059,         0.0753322954364324, 0.268816352031209), .Dim = c(5L,         4L), .Dimnames = list(c('a', 'c', 'i', 's', 't'), NULL))),     current = structure(c(4L, 5L, 1L, 5L, 3L, 4L, 5L, 3L, 2L,         4L), .Label = c('a', 'c', 'i', 's', 't'), class = 'factor',         contrasts = structure(c(1, 0, 0, 0, -1, 0, 1, 0, 0, -1,             -0.247125681008604, -0.247125681008604, -0.149872105789645,             0.891249148815458, -0.247125681008604, 0.268816352031209,             0.268816352031209, -0.881781351530059, 0.0753322954364324,             0.268816352031209), .Dim = c(5L, 4L), .Dimnames = list(c('a',             'c', 'i', 's', 't'), NULL)))), .Names = c('target',     'current'));" +
                        "do.call('all.equal.factor', argv)");
    }
}
