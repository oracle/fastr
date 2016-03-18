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

public class TestBuiltin_formatdifftime extends TestBase {

    @Test
    public void testformatdifftime1() {
        assertEval("argv <- structure(list(x = structure(c(0, 30, 60), units = 'mins',     class = 'difftime')), .Names = 'x');do.call('format.difftime', argv)");
    }
}
