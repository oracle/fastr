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

public class TestBuiltin_asmatrix extends TestBase {

    @Test
    public void testasmatrix1() {
        assertEval("argv <- structure(list(x = structure(c(9L, 27L, 27L, 27L, 27L,     3L, 3L, 3L, 3L, 9L, 9L, 9L, 9L, 9L, 9L), .Names = c('Blocks',     'A', 'B', 'C', 'D', 'Blocks:A', 'Blocks:B', 'Blocks:C', 'Blocks:D',     'A:B', 'A:C', 'A:D', 'B:C', 'B:D', 'C:D'))), .Names = 'x');"
                        + "do.call('as.matrix', argv)");
    }

}
