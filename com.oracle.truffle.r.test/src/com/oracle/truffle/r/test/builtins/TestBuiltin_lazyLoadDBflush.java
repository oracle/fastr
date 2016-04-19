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
public class TestBuiltin_lazyLoadDBflush extends TestBase {

    @Test
    public void testlazyLoadDBflush1() {
        // disabled because it accesses files
        // assertEval(Ignored.Unknown,
        // "argv <- list('/home/roman/r-instrumented/library/tools/R/tools.rdb');
        // .Internal(lazyLoadDBflush(argv[[1]]))");
    }

    @Test
    public void testlazyLoadDBflush2() {
        // disabled because it accesses files
        // assertEval(Ignored.Unknown,
        // "argv <- list('/home/lzhao/hg/r-instrumented/library/stats4/R/stats4.rdb');
        // .Internal(lazyLoadDBflush(argv[[1]]))");
    }
}
