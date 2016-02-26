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

public class TestBuiltin_extract_parentasis_extract_parentasis_assign_factor extends TestBase {

    @Test
    public void testextract_parentasis_extract_parentasis_assign_factor1() {
        assertEval("argv <- structure(list(x = structure(c(2L, 2L, 3L), .Label = c('One',     'Two', 'Three'), class = 'factor'), 2, value = 'One'), .Names = c('x',     '', 'value'));" +
                        "do.call('[[<-.factor', argv)");
    }
}
