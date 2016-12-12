/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestConditionHandling extends TestBase {

    @Test
    public void testTryCatch() {
        assertEval("{ tryCatch(1, finally = print(\"Hello\")) }");
        assertEval("{ e <- simpleError(\"test error\"); tryCatch(stop(e), finally = print(\"Hello\")) }");
        assertEval("{ e <- simpleError(\"test error\"); f <- function() { tryCatch(1, finally = print(\"Hello\")); stop(e)}; f() }");
        assertEval(Output.IgnoreErrorContext, "{ tryCatch(stop(\"fred\"), finally = print(\"Hello\")) }");
        assertEval("{ e <- simpleError(\"test error\"); tryCatch(stop(e), error = function(e) e, finally = print(\"Hello\"))}");
        assertEval(Ignored.Unknown, "{ tryCatch(stop(\"fred\"), error = function(e) e, finally = print(\"Hello\"))}");
        assertEval("{ f <- function() { tryCatch(1, error = function(e) print(\"Hello\")); stop(\"fred\")}; f() }");
        assertEval("{ f <- function() { tryCatch(stop(\"fred\"), error = function(e) print(\"Hello\"))}; f() }");
    }
}
