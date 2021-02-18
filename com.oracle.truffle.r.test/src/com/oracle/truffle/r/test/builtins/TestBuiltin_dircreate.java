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
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_dircreate extends TestBase {

    private static String dirPath = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple";
    private static String dirPath1 = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/createDirTest1";
    private static String dirPath2 = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/createDirTest1/createDirTest2";

    @Test
    public void testlistdirs1() {
        assertTrue(new File(dirPath).exists());
        // this dir exists
        assertEval("dir.create('" + dirPath + "', showWarnings=T, recursive=T)");
        assertEval("dir.create('" + dirPath + "', showWarnings=F, recursive=F)");

        // now create some that do not exist
        assertEval(Ignored.NewRVersionMigration, "unlink('" + dirPath1 + "', recursive=T); dir.exists('" + dirPath1 + "'); dir.create('" + dirPath1 + "'); dir.exists('" + dirPath1 + "');");

        assertEval(Ignored.NewRVersionMigration,
                        "unlink('" + dirPath1 + "', recursive=T); dir.exists('" + dirPath1 + "'); dir.create('" + dirPath2 + "', showWarnings=T, recursive=F); dir.exists('" + dirPath2 + "')");
        assertEval(Ignored.NewRVersionMigration,
                        "unlink('" + dirPath1 + "', recursive=T); dir.exists('" + dirPath1 + "'); dir.create('" + dirPath2 + "', showWarnings=F, recursive=F); dir.exists('" + dirPath2 + "')");
        assertEval(Ignored.NewRVersionMigration,
                        "unlink('" + dirPath1 + "', recursive=T); dir.exists('" + dirPath1 + "'); dir.create('" + dirPath2 + "', recursive=T); dir.exists('" + dirPath2 + "')");
    }
}
