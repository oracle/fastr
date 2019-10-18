/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
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

    private static String dirPath = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data/tree1";

    @Test
    public void testFileExist() {
    }

    @Test
    public void testFileDoesNotExist() {
        // make sure dirPath exists
        assertEvalFastR(" file.exists('" + dirPath + "')", "TRUE");

        assertEval(" file.exists('" + dirPath + "/filedoesnotexist')");
        // TODO: ignored tests GR-18968
        assertEval(Ignored.ImplementationError, " file.exists('" + dirPath + "/filedoesnotexist/..')");
        assertEval(" file.exists('" + dirPath + "/filedoesnotexist/../aa')");
        assertEval(Ignored.ImplementationError, " file.exists('" + dirPath + "/filedoesnotexist/../aa/..')");

        assertEval(Ignored.ImplementationError, " file.exists('" + dirPath + "/filedoesnotexist/../dummy.txt')");
    }
}
