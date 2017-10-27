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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_packBits extends TestBase {

    @Test
    public void testPackBits() {
        assertEval("packBits(c(1L,3L,5L,7L,9L,11L,13L,15L))");
        assertEval("packBits(c(TRUE,FALSE,TRUE,FALSE,TRUE,FALSE,TRUE,FALSE))");
        assertEval("packBits(as.raw(c(1L,3L,5L,7L,9L,11L,13L,15L)))");
        assertEval("packBits(as.raw(c(1L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L,0L)), type=\"integer\")");

        assertEval("packBits(c(1L,3L,5L,7L,9L,11L,13L,NA))");
        assertEval("packBits(c(TRUE,FALSE,TRUE,FALSE,TRUE,FALSE,TRUE,NA))");

        assertEval("packBits(c(TRUE))");

        // double quotes
        assertEval(Output.IgnoreErrorMessage, "packBits(c(TRUE,NA),type=\"wrongType\")");
        assertEval(Output.IgnoreErrorMessage, "packBits(c(TRUE,TRUE,FALSE,TRUE,FALSE,TRUE,FALSE,TRUE),type=\"wrongType\")");
    }
}
