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

public class TestBuiltin_anyDuplicatedmatrix extends TestBase {

    @Test
    public void testanyDuplicatedmatrix1() {
        // FIXME RInternalError: java.lang.ArrayIndexOutOfBoundsException: 0
        assertEval(Ignored.ImplementationError,
                        "argv <- structure(list(x = structure(c(3, 2, 7, 2, 6, 2, 7, 2),     .Dim = c(4L, 2L), .Dimnames = list(c('A', 'B', 'C', 'D'),         c('M', 'F'))), MARGIN = 0), .Names = c('x', 'MARGIN'));" +
                                        "do.call('anyDuplicated.matrix', argv)");
    }
}
