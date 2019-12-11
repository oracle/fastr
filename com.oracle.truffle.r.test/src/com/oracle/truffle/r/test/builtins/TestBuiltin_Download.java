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
 * Copyright (c) 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_Download extends TestBase {

    private static String file = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data/tree1/bar.txt";

    @Test
    public void testDownload() {
        assertEval("dstF <- tempfile('fastr-download-test-'); srcF <- paste0('file:///', getwd(), '/', '" + file + "'); utils::download.file(srcF, dstF); readLines(file(dstF))");
    }
}
