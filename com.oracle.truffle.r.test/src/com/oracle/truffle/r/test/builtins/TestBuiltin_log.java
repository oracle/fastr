/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_log extends TestBase {

    @Test
    public void testlog1() {
        assertEval("argv <- list(0.7800058115849);do.call('log', argv)");
    }

    @Test
    public void testLog() {
        assertEval("{ log(1) } ");
        assertEval("{ log(0) }");
        assertEval("{ log(c(0,1)) }");
        assertEval("{ round( log(10,), digits = 5 ) }");
        assertEval("{ round( log(10,2), digits = 5 ) }");
        assertEval("{ round( log(10,10), digits = 5 ) }");
    }
}
