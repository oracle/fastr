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
package com.oracle.truffle.r.test.builtins;

import static com.oracle.truffle.r.test.builtins.TestBuiltin_sysparent.SYS_PARENT_SETUP;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_parentframe extends TestBase {

    @Test
    public void testS3ParentFrame() {
        assertEval("a <- 0; f <- function(x) { a <- 1; UseMethod('f') }; f.default <- function(x) { a <- 2; (function(x) get('a', envir = parent.frame(x)))(x) }; f(1); f(2); f(3)");
        assertEval("a <- 0; f <- function(x) { a <- 1; UseMethod('f') }; f.default <- function(x) { a <- 2; get('a', envir = parent.frame()) }; f(1)");
        assertEval("a <- 0; f <- function(x) { a <- 1; UseMethod('f') }; f.foo <- function(x) { a <- 2; NextMethod(); };\n" +
                        "f.default <- function(x) { a <- 3; (function(x) get('a', envir = parent.frame(x)))(x) }; v <- 1; class(v) <- 'foo'; f(v); v <- 2; class(v) <- 'foo'; f(v)");
    }

    @Test
    public void testParentFrame() {
        assertEval("parent.frame()");
        assertEval("{ f <- function() parent.frame() }");
        assertEval("{ f <- function() parent.frame() ; g <- function() { n <- 100; f() }; r <- g(); ls(r) }");
        assertEval("{ f <- function() parent.frame(2); g <- function() f(); g() }");
        assertEval("{ f <- function() parent.frame(3); g <- function() f(); g() }");
        assertEval("parent.frame(0)");
        assertEval("parent.frame(-1)");
    }

    @Test
    public void testParentFramePromises() {
        assertEval("{ f <- function(frame) frame; g <- function() f(parent.frame()); g() }");
        assertEval("{ f <- function(frame) frame; g <- function() f(parent.frame(3)); g() }");
    }

    @Test
    public void frameAccessCommonTest() {
        assertEval("{ foo <- function(x) sapply(1:7, function(fr) sort(tolower(ls(parent.frame(fr)))));" + SYS_PARENT_SETUP + "}");
    }
}
