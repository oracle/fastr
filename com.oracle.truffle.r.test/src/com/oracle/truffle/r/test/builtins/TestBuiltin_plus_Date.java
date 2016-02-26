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

public class TestBuiltin_plus_Date extends TestBase {

    @Test
    public void testplus_Date1() {
        assertEval("argv <- structure(list(e1 = structure(1, units = 'days', class = 'difftime'),     e2 = structure(14579, class = 'Date')), .Names = c('e1',     'e2'));" +
                        "do.call('+.Date', argv)");
    }
}
