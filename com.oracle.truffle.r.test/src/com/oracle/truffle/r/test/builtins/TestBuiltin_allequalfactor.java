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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_allequalfactor extends TestBase {

    @Test
    public void testallequalfactor1() {
        assertEval("argv <- structure(list(target = structure(c(4L, 5L, 1L, 5L, 3L,     4L, 5L, 3L, 2L, 4L), .Label = c('a', 'c', 'i', 's', 't'),     class = 'factor', contrasts = structure(c(1, 0, 0, 0, -1,         0, 1, 0, 0, -1, -0.247125681008604, -0.247125681008604,         -0.149872105789645, 0.891249148815458, -0.247125681008604,         0.268816352031209, 0.268816352031209, -0.881781351530059,         0.0753322954364324, 0.268816352031209), .Dim = c(5L,         4L), .Dimnames = list(c('a', 'c', 'i', 's', 't'), NULL))),     current = structure(c(4L, 5L, 1L, 5L, 3L, 4L, 5L, 3L, 2L,         4L), .Label = c('a', 'c', 'i', 's', 't'), class = 'factor',         contrasts = structure(c(1, 0, 0, 0, -1, 0, 1, 0, 0, -1,             -0.247125681008604, -0.247125681008604, -0.149872105789645,             0.891249148815458, -0.247125681008604, 0.268816352031209,             0.268816352031209, -0.881781351530059, 0.0753322954364324,             0.268816352031209), .Dim = c(5L, 4L), .Dimnames = list(c('a',             'c', 'i', 's', 't'), NULL)))), .Names = c('target',     'current'));"
                        + "do.call('all.equal.factor', argv)");
    }

}
