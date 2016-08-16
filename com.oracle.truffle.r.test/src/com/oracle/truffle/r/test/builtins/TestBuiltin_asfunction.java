/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_asfunction extends TestBase {

    @Test
    public void testasfunction() {
        assertEval("as.function(c(alist(a=1+14, b=foo(x),c=), quote(a+foo(c)*b)))");
        assertEval("f <- function() a+foo(c)*b; as.function(c(alist(a=1+14, b=foo(x),c=), body(f)))");
        assertEval("foo <- function(x) x*2; as.function(c(alist(a=1+14, b=foo(x),c=), quote(a+foo(c)*b)))(c=3,b=1)");
        assertEval("foo <- function(x) x*2; f <- function() a+foo(c)*b; as.function(c(alist(a=1+14, b=foo(x),c=), body(f)))(c=3,b=1)");
        assertEval("{ as.function(alist(42))() }");
        assertEval("{ as.function(alist(42L))() }");
        assertEval("{ as.function(alist(TRUE))() }");
        assertEval("{ as.function(alist(\"foo\"))() }");
        assertEval("{ as.function(alist(7+42i))() }");
        assertEval("{ as.function(alist(as.raw(7)))() }");
        assertEval(Output.IgnoreErrorContext, "{ .Internal(as.function.default(alist(a+b), \"foo\")) }");
        assertEval(Output.IgnoreErrorContext, "{ .Internal(as.function.default(function() 42, parent.frame())) }");
    }
}
