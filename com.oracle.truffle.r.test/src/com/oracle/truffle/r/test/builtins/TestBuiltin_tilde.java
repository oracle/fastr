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

public class TestBuiltin_tilde extends TestBase {

    @Test
    public void testTildeDirect() {
        assertEval("~ x + y");
        assertEval("x ~ y + z");
        assertEval("y ~ 0 + x");
    }

    @Test
    public void testTildeIndirect() {
        assertEval("do.call('~', list(quote(x + y)))");
        assertEval("do.call('~', list(quote(x), quote(y + z)))");
        assertEval("do.call('~', list(quote(y), quote(0 + x)))");
    }

}
