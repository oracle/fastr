/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltincache extends TestBase {

    @Test
    @Ignore
    public void testcache1() {
        assertEval("argv <- list(\'ddenseMatrix\', c(\'ddenseMatrix\', \'dMatrix\', \'denseMatrix\', \'Matrix\', \'mMatrix\'));.cache_class(argv[[1]],argv[[2]]);");
    }

    @Test
    @Ignore
    public void testcache2() {
        assertEval("argv <- list(\'numeric\', c(\'numeric\', \'vector\', \'atomicVector\'));.cache_class(argv[[1]],argv[[2]]);");
    }
}

