/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_rawToBits extends TestBase {

    @Test
    public void testRawToBits() {
        assertEval("rawToBits(raw(0))");
        assertEval("rawToBits(raw(10))");
        assertEval("rawToBits(as.raw(0))");
        assertEval("rawToBits(as.raw(1))");
        assertEval("rawToBits(as.raw(255))");
        assertEval("rawToBits(c(as.raw(1), as.raw(255)))");
        assertEval("rawToBits(raw(0), raw(1))");
        assertEval("rawToBits(as.raw(0:255))");
        assertEval("rawToBits(as.raw(c(0,1,255)))");

        assertEval("rawToBits(as.raw(1)[1])");

        assertEval("rawToBits(0:255)");
        assertEval("rawToBits(NA)");
        assertEval("rawToBits(NULL)");
        assertEval("rawToBits(list(list()))");
        assertEval("rawToBits(list(NULL))");
        assertEval("rawToBits(c(NULL))");

        assertEval("rawToBits(integer(0))");
        assertEval("rawToBits(double(0))");

        assertEval("rawToBits(01)");
        assertEval("rawToBits('a')");

        assertEval("rawToBits(new.env())");
        assertEval("rawToBits(environment)");
        assertEval("rawToBits(stdout())");
    }
}
