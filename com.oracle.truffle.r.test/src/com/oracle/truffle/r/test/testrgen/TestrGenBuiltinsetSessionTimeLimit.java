/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinsetSessionTimeLimit extends TestBase {

    @Test
    @Ignore
    public void testsetSessionTimeLimit1() {
        assertEval("argv <- list(NULL, NULL); .Internal(setSessionTimeLimit(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testsetSessionTimeLimit2() {
        assertEval("argv <- list(FALSE, Inf); .Internal(setSessionTimeLimit(argv[[1]], argv[[2]]))");
    }
}
