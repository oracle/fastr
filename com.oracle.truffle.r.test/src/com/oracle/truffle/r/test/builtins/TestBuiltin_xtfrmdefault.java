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

public class TestBuiltin_xtfrmdefault extends TestBase {

    @Test
    public void testxtfrmdefault1() {
        assertEval("argv <- structure(list(x = structure(1:3, id = 'An Example',     class = structure('numWithId', package = '.GlobalEnv'))),     .Names = 'x');do.call('xtfrm.default', argv)");
    }
}
