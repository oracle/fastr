/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.utils;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestInteractiveDebug extends TestBase {
    @Test
    public void testSimple() {
        assertEval("f <- function(x) {\n  t <- x + 1\n  print(t)\n  t}\ndebug(f)\nf(5)\nx\nn\nn\nt\nn\nn");
    }

    @Test
    public void testInvalidName() {
        assertEval("f <- function(x) {\n  `123t` <- x + 1\n  print(`123t`)\n  `123t`}\ndebug(f)\nf(5)\nx\nn\nn\n`123t`\nn\nn");
    }

    @Test
    public void testNoBracket() {
        assertEval("f <- function(x) print(x)\ndebug(f)\nf(5)\nx\nn\n");
    }

    @Test
    public void testLoop() {
        assertEval("fun <- function(x) { for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\n");
        assertEval("fun <- function(x) { for(i in seq(x)) { cat(i) }; cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\n");
        assertEval("fun <- function(x) { for(i in seq(x)) { cat(i) }; cat(5) }; debug(fun); fun(3)\n\n\n\nf\n\n");
        assertEval("fun <- function(x) { for(j in seq(2)) for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\n\n\n\n\n\n\n\n\n");
        assertEval("fun <- function(x) { for(j in seq(2)) for(i in seq(x)) cat(i); cat(5) }; debug(fun); fun(3)\n\n\n\nf\nf\n\n");
    }
}
