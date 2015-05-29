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

public class TestBuiltin_labels extends TestBase {

    @Test
    public void testlabels1() {
        assertEval("argv <- structure(list(object = structure(c(-469.098459411633,     469.356672501203, -0.429918004252249, 0.00364370239091614,     -0.256875513692359, -0.0204799335117722, 2.00613934942808),     .Names = c('(Intercept)', 'gravity', 'ph', 'osmo', 'conduct',         'urea', 'log(calc)'))), .Names = 'object');"
                        + "do.call('labels', argv)");
    }
}
