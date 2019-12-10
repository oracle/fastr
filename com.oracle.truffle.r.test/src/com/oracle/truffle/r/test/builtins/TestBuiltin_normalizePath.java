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
import java.io.File;
import static org.junit.Assert.assertTrue;

// Checkstyle: stop line length check
public class TestBuiltin_normalizePath extends TestBase {

    @Test
    public void testnormalizePath1() {
        assertEval(Output.IgnoreWarningContext,
                        "argv <- list(c('/home/lzhao/hg/r-instrumented/library', '/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0', '/home/lzhao/hg/r-instrumented/library'), '/', NA); .Internal(normalizePath(argv[[1]], argv[[2]], argv[[3]]))");
    }

    private static String dirPath = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data/tree1";

    private static String[] mustWork = new String[]{"T", "F", "NA"};

    @Test
    public void testFileDoesNotExist() {
        // make sure dirPath exists
        assertTrue(new File(dirPath).exists());

        assertEval(template("normalizePath('" + dirPath + "/filedoesnotexist', mustWork=%0)", mustWork));
        // TODO: ignored tests GR-18968
        assertEval(Ignored.ImplementationError, template("normalizePath('" + dirPath + "/filedoesnotexist/..', mustWork=%0)", mustWork));
        assertEval(template("normalizePath('" + dirPath + "/filedoesnotexist/../aa', mustWork=%0)", mustWork));
        assertEval(Ignored.ImplementationError, template("normalizePath('" + dirPath + "/filedoesnotexist/../aa/..', mustWork=%0)", mustWork));

        assertEval(Ignored.ImplementationError, template("normalizePath('" + dirPath + "/filedoesnotexist/../dummy.txt', mustWork=%0)", mustWork));
    }
}
