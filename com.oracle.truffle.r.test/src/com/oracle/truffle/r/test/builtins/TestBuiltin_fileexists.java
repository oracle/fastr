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
public class TestBuiltin_fileexists extends TestBase {

    @Test
    public void testfileexists1() {
        assertEval("argv <- list('/home/lzhao/hg/r-instrumented/library/methods/data/Rdata.rdb'); .Internal(file.exists(argv[[1]]))");
    }

    @Test
    public void testfileexists2() {
        assertEval("argv <- list(c('src/Makevars', 'src/Makevars.in')); .Internal(file.exists(argv[[1]]))");
    }

    @Test
    public void testfileexists3() {
        assertEval("argv <- list(character(0)); .Internal(file.exists(argv[[1]]))");
    }
}
