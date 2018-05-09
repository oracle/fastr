/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
