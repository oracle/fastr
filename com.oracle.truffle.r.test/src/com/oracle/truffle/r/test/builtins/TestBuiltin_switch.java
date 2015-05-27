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

public class TestBuiltin_switch extends TestBase {

    @Test
    public void testswitch1() {
        assertEval("argv <- structure(list('forward', forward = 'posS', reverse = 'negS'),     .Names = c('', 'forward', 'reverse'));do.call('switch', argv)");
    }

    @Test
    public void testswitch2() {
        assertEval("argv <- list(3L);do.call('switch', argv)");
    }

    @Test
    public void testswitch4() {
        assertEval("argv <- list(2L, TRUE, FALSE, FALSE);do.call('switch', argv)");
    }

}
