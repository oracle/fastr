/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.test.TestBase;

import org.junit.Test;

public class TestBuiltin_dotdotdotlength extends TestBase {

    @Test
    public void testdotdotdotLength() {
        assertEval("tst <- function(...) ...length(); tst()");
        assertEval("tst <- function(...) ...length(); tst(1, 2, 3)");
        assertEval("tst <- function(a, b, c, ...) ...length(); tst(c(3,2,1), 'a', TRUE, 3.6, print('hi'))");
        assertEval("tst <- function(..., a) ...length(); tst(c(3,2,1), 'a', TRUE, 3.6)");
        assertEval("tst <- function(...) ...length(); a <- 1; tst(a, 1)");
    }

    @Test
    public void testdotdotdotLengthErr() {
        assertEval("tst <- function() ...length(); tst()");
        // TODO: wrong error reported, unused argument when it should be object not found.
        assertEval(Ignored.ImplementationError, "tst <- function() ...length(n); tst()");
        assertEval("tst <- function(a) ...length(); tst(1, 2, 3)");
    }
}
