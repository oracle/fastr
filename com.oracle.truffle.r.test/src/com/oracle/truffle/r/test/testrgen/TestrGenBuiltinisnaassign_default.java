/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestrGenBuiltinisnaassign_default extends TestBase {

    @Test
    public void testisnaassign_default1() {
        assertEval("argv <- structure(list(x = 9L, value = TRUE), .Names = c('x',     'value'));do.call('is.na<-.default', argv)");
    }

    @Test
    public void testisnaassign_default2() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = structure(c('A', '3', 'C'), class = 'AsIs'),     value = 2), .Names = c('x', 'value'));do.call('is.na<-.default', argv)");
    }

}
