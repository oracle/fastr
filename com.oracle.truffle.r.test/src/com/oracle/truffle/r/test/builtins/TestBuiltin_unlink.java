/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

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
}
