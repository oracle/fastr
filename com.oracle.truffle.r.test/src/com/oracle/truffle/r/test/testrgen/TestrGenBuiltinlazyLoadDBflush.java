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

// Checkstyle: stop line length check
public class TestrGenBuiltinlazyLoadDBflush extends TestBase {

    @Test
    @Ignore
    public void testlazyLoadDBflush1() {
        assertEval("argv <- list(\'/home/roman/r-instrumented/library/tools/R/tools.rdb\'); .Internal(lazyLoadDBflush(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testlazyLoadDBflush2() {
        assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/library/stats4/R/stats4.rdb\'); .Internal(lazyLoadDBflush(argv[[1]]))");
    }
}
