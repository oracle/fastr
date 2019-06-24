/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * {@code stopifnot} is implemented in R code, but FastR has a fast-path replacement, which is
 * tested here.
 */
public class TestBuiltin_stopifnot extends TestBase {
    @Test
    public void testStopifnotFastPathWithWarning() {
        assertEval("{ foo <- function(a) { if (a != 42L) warning('my warning'); a }; stopifnot(foo(33) < 42) }");
    }

    @Test
    public void testStopifnotFastPathBailoutWithSideEffect() {
        assertEval("{ global <- 42; foo <- function() { if (global == 42) { global <<- 44; F } else T }; tryCatch(stopifnot(foo()), error=function(x) cat(x$message,'\\n')); global }");
    }

    @Test
    public void testStopifnotFastPathConditionThrowsError() {
        assertEval("{ foo <- function() { stop('my error') }; stopifnot(foo()) }");
    }

    @Test
    public void testStopifnotBasicUsage() {
        assertEval("stopifnot(4 < 5, 7 < 10, T)");
        assertEval("stopifnot(4 < 5, 7 > 10, T)");
        assertEval("stopifnot(exprs = { 4 < 5; 7 < 10; T })");
        assertEval("stopifnot(exprs = { 4 < 5; 7 > 10; T })");
        assertEval("stopifnot(1 == 1, all.equal(pi, 3.14159265), 1 < 2)");
    }
}
