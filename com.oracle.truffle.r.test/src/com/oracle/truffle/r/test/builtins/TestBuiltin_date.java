/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_date extends TestBase {

    @Test
    public void testDate() {
        // Date at real time differs by milliseconds.
        // Here the output would always differ since the GnuR test outputs are pre-generated
        assertEval(Ignored.OutputFormatting, "{date()}");
    }
}
