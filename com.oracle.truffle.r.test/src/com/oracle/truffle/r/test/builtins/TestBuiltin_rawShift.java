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

public class TestBuiltin_rawShift extends TestBase {

    @Test
    public void testrawShift1() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = as.raw(c(109, 121, 32, 116, 101, 120,     116)), n = 0L), .Names = c('x', 'n'));do.call('rawShift', argv)");
    }

    @Test
    public void testrawShift2() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = as.raw(c(109, 121, 32, 116, 101, 120,     116)), n = 3L), .Names = c('x', 'n'));do.call('rawShift', argv)");
    }

}
