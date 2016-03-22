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

public class TestBuiltin_shQuote extends TestBase {

    @Test
    public void testshQuote1() {
        assertEval(Ignored.Unknown, "argv <- structure(list(string = c('ABC', '\\'123\\'', 'a'b'), type = 'cmd'),     .Names = c('string', 'type'));do.call('shQuote', argv)");
    }
}
