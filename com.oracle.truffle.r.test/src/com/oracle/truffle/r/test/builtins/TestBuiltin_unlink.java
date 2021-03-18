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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_unlink extends TestBase {
    private static final String fileName = "tmp_file77845610.txt";
    private static final String filePath = "~" + File.separator + fileName;
    private static final Path fileInHomeDir = Paths.get(System.getProperty("user.home"), fileName);
    private static final Path tildeDir = Paths.get("~");
    private static final Path fileInTildeDir = Paths.get("~", fileName);

    @BeforeClass
    public static void createFiles() {
        try {
            if (!Files.exists(tildeDir)) {
                Files.createDirectory(tildeDir);
            }
            Files.createFile(fileInHomeDir);
            Files.createFile(fileInTildeDir);
        } catch (IOException e) {
            // no-op - if creation of files fails, we just ignore the tests that use them.
        }
    }

    @AfterClass
    public static void deleteFiles() {
        try {
            Files.deleteIfExists(fileInHomeDir);
            Files.deleteIfExists(fileInTildeDir);
            Files.deleteIfExists(tildeDir);
        } catch (IOException e) {
            // no-op - we just leave the files there if something wrong occurs.
        }
    }

    @Test
    public void testunlink1() {
        assertEval(Ignored.SideEffects, "argv <- list('/tmp/RtmptPgrXI/Pkgs', TRUE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlink2() {
        assertEval(Ignored.SideEffects, "argv <- list(character(0), FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlink3() {
        assertEval(Ignored.SideEffects, "argv <- list('/home/lzhao/tmp/Rtmphu0Cms/file74e1676db2e7', FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlink5() {
        assertEval(Ignored.SideEffects, "argv <- structure(list(x = '/tmp/RtmpHjOdmd/file7ac7792619bc'),     .Names = 'x');do.call('unlink', argv)");
    }

    @Test
    public void testunlink() {
        assertEval("{ unlink('abcnonexistentxyz*') }");

        assertEval("{ td <- tempfile(pattern='r-test-unlink'); dir.exists(td); dir.create(td); dir.exists(td); unlink(td, recursive=F); dir.exists(td) }");
        assertEval("{ td <- tempfile(pattern='r-test-unlink'); dir.exists(td); dir.create(td); dir.exists(td); unlink(td, recursive=T); dir.exists(td) }");
        assertEval("{ td <- tempfile(pattern='r-test-unlink'); td <- paste0(td, '/dir1/dir2'); dir.exists(td); dir.create(td, recursive=T); dir.exists(td); unlink(td, recursive=F); dir.exists(td) }");
        assertEval("{ td <- tempfile(pattern='r-test-unlink'); td <- paste0(td, '/dir1/dir2'); dir.exists(td); dir.create(td, recursive=T); dir.exists(td); unlink(td, recursive=T); dir.exists(td) }");
    }

    /**
     * Creates a random file in user home directory and in "tilde" directory, i.e., in ~ directory.
     * Expand parameter to unlink builtin should either expand ~ as user home, or interpret it
     * literally.
     */
    @Test
    public void testUnlinkExpandParameter() {
        Assume.assumeTrue(Files.exists(fileInHomeDir));
        Assume.assumeTrue(Files.exists(fileInTildeDir));

        assertEval(String.format("{ res <- unlink('%s', expand=TRUE); res }", filePath));
        Assert.assertTrue(!Files.exists(fileInHomeDir) && Files.exists(fileInTildeDir));

        assertEval(String.format("{ res <- unlink('%s', expand=FALSE); res }", filePath));
        Assert.assertTrue(!Files.exists(fileInHomeDir) && !Files.exists(fileInTildeDir));
    }
}
