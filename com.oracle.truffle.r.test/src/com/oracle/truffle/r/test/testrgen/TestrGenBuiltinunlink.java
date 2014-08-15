/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinunlink extends TestBase {

    @Test
    @Ignore
    public void testunlink1() {
        assertEval("argv <- list(\'/tmp/RtmptPgrXI/Pkgs\', TRUE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testunlink2() {
        assertEval("argv <- list(character(0), FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testunlink3() {
        assertEval("argv <- list(\'/home/lzhao/tmp/Rtmphu0Cms/file74e1676db2e7\', FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
    }
}

