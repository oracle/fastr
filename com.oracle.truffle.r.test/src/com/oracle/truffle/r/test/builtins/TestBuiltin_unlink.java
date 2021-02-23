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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_unlink extends TestBase {

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

    @Test
    public void testUnlinkExpandParameter() throws IOException {
        String fileName = "tmp_file77845610.txt";

        Path fileInHomeDir = Paths.get(System.getProperty("user.home"), fileName);
        Files.createFile(fileInHomeDir);
        if (!Files.exists(Paths.get("~"))) {
            Files.createDirectory(Paths.get("~"));
        }
        Path fileInTildeDir = Paths.get("~", fileName);
        Files.createFile(fileInTildeDir);

        Assume.assumeTrue(Files.exists(fileInHomeDir));
        Assume.assumeTrue(Files.exists(fileInTildeDir));

        String filePath = "~" + File.separator + fileName;
        assertEvalFastR(String.format("{ res <- unlink('%s', expand=TRUE); res }", filePath), "0");
        Assert.assertTrue(!Files.exists(fileInHomeDir) && Files.exists(fileInTildeDir));

        assertEvalFastR(String.format("{ res <- unlink('%s', expand=FALSE); res }", filePath), "0");
        Assert.assertTrue(!Files.exists(fileInHomeDir) && !Files.exists(fileInTildeDir));

        Files.delete(Paths.get("~"));
    }
}
