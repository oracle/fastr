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

public class TestBuiltin_asarraydefault extends TestBase {

    @Test
    public void testasarraydefault1() {
        assertEval("argv <- structure(list(x = structure(c(1, 2), .Dim = 2L, .Dimnames = list(c('a',     'b')))), .Names = 'x');do.call('as.array.default', argv)");
    }
}
