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
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
