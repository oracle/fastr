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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_append extends TestBase {

    @Test
    public void testappend1() {
        assertEval("argv <- structure(list(x = 1:5, values = 0:1, after = 3), .Names = c('x',     'values', 'after'));do.call('append', argv)");
    }
}
