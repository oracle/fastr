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

public class TestBuiltin_regmatchesassign_ extends TestBase {

    @Test
    public void testregmatchesassign_1() {
        assertEval("argv <- structure(list(x = c('A', 'B', 'C'), m = structure(c(1L,     -1L, 1L), match.length = c(1L, -1L, 1L), useBytes = TRUE),     value = c('A', 'C')), .Names = c('x', 'm', 'value'));"
                        + "do.call('regmatches<-', argv)");
    }

}
