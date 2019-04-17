/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_readBin extends TestBase {

    @Test
    public void testReadBinRawVector() {
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'character', 1)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'character', 3)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'character', 5)");

        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'raw', 1)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'raw', 3)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'raw', 5)");

        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'double', 5)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'numeric', 5)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'logical', 5)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'int', 5)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'integer', 5)");
        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'complex', 5)");

        assertEval("readBin(as.raw(as.raw(c(1, 2, 3))), 'tralala', 5)");
    }
}
