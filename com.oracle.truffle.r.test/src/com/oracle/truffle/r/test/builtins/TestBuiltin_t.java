/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_t extends TestBase {

    @Test
    public void testt1() {
        assertEval("argv <- structure(list(x = c(-2.13777446721376, 1.17045456767922,     5.85180137819007)), .Names = 'x');do.call('t', argv)");
    }

}
