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

public class TestBuiltin_asdataframe extends TestBase {

    @Test
    public void testasdataframe1() {
        assertEval("argv <- structure(list(x = structure(c(3.5, 2, 1.7, 0.40625,     0.5, 0.882, 4, 2, 2, 4, 2, 3, 0.625, 0.5, 0.444444444444444,     0, 0, 0.333333333333333, 0.833333333333333, 1, 0.333333333333333,     0.5, 0.666666666666667, 0.666666666666667, 0.166666666666667,     0, 0.5), .Dim = c(3L, 9L), .Dimnames = list(c('q1.csv', 'q2.csv',     'q3.csv'), c('effsize', 'constraint', 'outdegree', 'indegree',     'efficiency', 'hierarchy', 'centralization', 'gden', 'ego.gden')))),     .Names = 'x');"
                        + "do.call('as.data.frame', argv)");
    }

}
