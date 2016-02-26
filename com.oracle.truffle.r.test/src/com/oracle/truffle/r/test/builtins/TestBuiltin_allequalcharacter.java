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

public class TestBuiltin_allequalcharacter extends TestBase {

    @Test
    public void testallequalcharacter1() {
        assertEval("argv <- structure(list(target = structure(c('A', 'E', 'I', 'M',     'Q', 'U', 'B', 'F', 'J', 'N', 'R', 'V', 'C', 'G', 'K', 'O',     'S', 'W', 'D', 'H', 'L', 'P', 'T', 'X'), .Dim = c(6L, 4L)),     current = structure(c('A', 'E', 'I', 'M', 'Q', 'U', 'B',         'F', 'J', 'N', 'R', 'V', 'C', 'G', 'K', 'O', 'S', 'W',         'D', 'H', 'L', 'P', 'T', 'X'), .Dim = c(6L, 4L))), .Names = c('target',     'current'));" +
                        "do.call('all.equal.character', argv)");
    }

}
