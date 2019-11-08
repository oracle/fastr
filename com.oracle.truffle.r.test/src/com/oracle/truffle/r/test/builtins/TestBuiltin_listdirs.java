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
public class TestBuiltin_listdirs extends TestBase {

    private static String dirPath0 = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple";
    private static String dirPath1 = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data";

    @Test
    public void testlistdirs1() {
        assertEval("list.dirs('" + dirPath1 + "', full.names=T, recursive=T)");
        assertEval("list.dirs('" + dirPath1 + "', full.names=T, recursive=F)");
        assertEval("list.dirs('" + dirPath1 + "', full.names=F, recursive=T)");
        assertEval("list.dirs('" + dirPath1 + "', full.names=F, recursive=F)");

        assertEval("wd <- getwd(); setwd('" + dirPath0 + "'); list.dirs('data', full.names=T, recursive=T); setwd(wd)");
        assertEval("wd <- getwd(); setwd('" + dirPath0 + "'); list.dirs('data', full.names=T, recursive=F); setwd(wd)");
        assertEval("wd <- getwd(); setwd('" + dirPath0 + "'); list.dirs('data', full.names=F, recursive=T); setwd(wd)");
        assertEval("wd <- getwd(); setwd('" + dirPath0 + "'); list.dirs('data', full.names=F, recursive=F); setwd(wd)");

        assertEval("list.dirs('does-not-exist', full.names=F, recursive=F)");
    }
}
