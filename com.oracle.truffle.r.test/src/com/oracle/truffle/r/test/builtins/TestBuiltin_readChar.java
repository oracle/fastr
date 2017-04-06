/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_readChar extends TestBase {

    @Test
    public void testreadChar1() {
        // FIXME raw character vector as first argument seems to be unsupported:
        // Error in readChar(as.raw(c(65, 66, 67)), nchars = 3) : invalid connection
        assertEval(Ignored.ImplementationError, "readChar(as.raw(c(65,66,67)), nchars=3);");
        assertEval(Ignored.ImplementationError, "argv <- structure(list(con = as.raw(c(65, 66, 67, 68, 69, 70,     71, 72, 73, 74)), nchars = c(3, 3, 0, 3, 3, 3)), .Names = c('con',     'nchars'));" +
                        "do.call('readChar', argv)");
    }
}
