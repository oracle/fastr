/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestExternal_BinDist extends TestBase {
    @Test
    public void testBinDist() {
        assertEval(".Call(stats:::C_BinDist, c(1,2,3), c(4,5,6), 0, 3, 5)");
        assertEval(".Call(stats:::C_BinDist, c('1','2','3'), c(4,5,6), 0, c(3, 4), c('5', '8'))");
        assertEval(".Call(stats:::C_BinDist, c(0,0,0), c(1,2,3), 1, 10, 10)");
        assertEval(".Call(stats:::C_BinDist, c(2.2,4,3), 5:7, -1, 5, 8)");
    }

    @Test
    public void testBinDistWrongArgs() {
        assertEval(".Call(stats:::C_BinDist, 0, 0, 'string', 3, 5)");
        assertEval(".Call(stats:::C_BinDist, c(1,2,3), c(4,5,6), 0, 3, c(NA, 3L))");
        assertEval(".Call(stats:::C_BinDist, c(1,2,3), c(4,5,6), 0, 3, -5L)");
    }
}
