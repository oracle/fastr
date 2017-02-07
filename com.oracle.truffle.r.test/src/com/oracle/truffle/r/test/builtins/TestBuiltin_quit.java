/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.builtins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_quit extends TestBase {

    private static final Path PATH_RDATA = Paths.get(".RData");
    private static final Path PATH_RHISTORY = Paths.get(".Rhistory");
    private static final Path PATH_RDATA_BAK = PATH_RDATA.resolveSibling(PATH_RDATA.getFileName() + ".bak");
    private static final Path PATH_RHISTORY_BAK = PATH_RHISTORY.resolveSibling(PATH_RHISTORY.getFileName() + ".bak");

    /**
     * Test case {@link #testQuitEmptyEnv()} possibly destroys previously stored sessions. If so,
     * first backup {@code .RData} and {@code .Rhistory}.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void backupHistory() throws IOException {
        if (Files.exists(PATH_RDATA)) {
            move(PATH_RDATA, PATH_RDATA_BAK);
        }
        if (Files.exists(PATH_RHISTORY)) {
            move(PATH_RHISTORY, PATH_RHISTORY_BAK);
        }
    }

    private static void move(Path pathRData, Path dest) throws IOException {

        Files.move(pathRData, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testQuitErrorSave() {
        assertEval("{ quit(\"xx\") }");
    }

    @Test
    public void testQuitEmptyEnv() {
        assertEval("{ quit(\"yes\") }");
    }

    /**
     * Removes temporarily created files {@code .RData} and {@code .Rhistory} and restore backups if
     * available.
     * 
     * @throws IOException
     */
    @AfterClass
    public static void restoreHistory() throws IOException {

        // remove any created ".RData" and ".Rhistory" file
        if (Files.exists(PATH_RDATA)) {
            Files.delete(PATH_RDATA);
        }
        if (Files.exists(PATH_RHISTORY)) {
            Files.delete(PATH_RHISTORY);
        }

        // restore previously rescued files
        if (Files.exists(PATH_RDATA_BAK)) {
            move(PATH_RDATA_BAK, PATH_RDATA);
        }
        if (Files.exists(PATH_RHISTORY_BAK)) {
            move(PATH_RHISTORY_BAK, PATH_RHISTORY);
        }
    }
}
