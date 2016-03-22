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

public class TestBuiltin_aslist extends TestBase {

    @Test
    public void testaslist1() {
        assertEval("argv <- structure(list(x = structure(c(9.83610941897737, 1.76740501065812,     3.23822416444495, -2.66666666666667, -10, 28), .Names = c('X',     'Y', 'Z', 'a', 'b', 'c'))), .Names = 'x');"
                        + "do.call('as.list', argv)");
    }
}
