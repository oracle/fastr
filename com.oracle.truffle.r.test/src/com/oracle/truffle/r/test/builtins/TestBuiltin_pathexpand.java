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
import static org.junit.Assert.assertEquals;

// Checkstyle: stop line length check
public class TestBuiltin_pathexpand extends TestBase {

    @Test
    public void testpathexpand1() {
        assertEval("argv <- list('/tmp/RtmptPgrXI/Pkgs/pkgA'); .Internal(path.expand(argv[[1]]))");
    }

    @Test
    public void testpathexpand2() {
        assertEval("argv <- list(c('/home/lzhao/hg/r-instrumented/tests/compiler.Rcheck', '/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0')); .Internal(path.expand(argv[[1]]))");
    }

    @Test
    public void testpathexpand3() {
        assertEval("argv <- list(character(0)); .Internal(path.expand(argv[[1]]))");
    }

    @Test
    public void testpathexpand5() {
        assertEval("argv <- structure(list(path = '/tmp/RtmpagC9oa/Pkgs/exNSS4'),     .Names = 'path');do.call('path.expand', argv)");
    }

    @Test
    public void testpathexpand6() {
        assertEval("path.expand('.'); wd <- getwd(); tryCatch({ setwd('/tmp'); path.expand('.') }, finally = { setwd(wd) })");
    }

    @Test
    public void testArgsValidation() {
        assertEval("path.expand(NULL)");
        assertEval("path.expand(42)");
    }

    @Test
    public void testTildeExpand() {
        String userHome = System.getProperty("user.home");
        assertEquals("[1] \"" + userHome + "\"\n", fastREval("path.expand('~')"));
        assertEquals("[1] \"" + userHome + "/.\"\n", fastREval("path.expand('~/.')"));
        assertEquals("[1] \"" + userHome + "/..\"\n", fastREval("path.expand('~/..')"));
        assertEquals("[1] \"" + userHome + "/Dis-doz-nottt_exyst\"\n", fastREval("path.expand('~/Dis-doz-nottt_exyst')"));
        assertEquals("[1] \"" + userHome + "/Dis-doz-nottt_exyst/..\"\n", fastREval("path.expand('~/Dis-doz-nottt_exyst/..')"));
    }
}
