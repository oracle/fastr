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
package com.oracle.truffle.r.test.library.parallel;

import com.oracle.truffle.r.test.TestBase;
import org.junit.Test;

public class TestBuiltin_mclapply extends TestBase {

    @Test
    public void testMCLapply() {
        // race-conditions, easilly reproducible with LLVM
        assertEval(Ignored.ImplementationError, "l <- list(list(x=1, y=2)); parallel:::mclapply(l, function(ll) { ll$y })");
        assertEval(Ignored.ImplementationError, "l <- list(list(x=1, y=2), list(x=11, y=22)); parallel:::mclapply(l, function(ll) { ll$y })");

        String[] cores = {"1", "2", "3", "4", "5", "6"};
        assertEval(Ignored.ImplementationError, template("l <- list(list(x=1, y=2), list(x=11, y=22), list(x=111, y=222), list(x=1111, y=2222), list(x=11111, y=22222), list(x=111111, y=222222)); " +
                        "parallel:::mclapply(l, function(ll) { ll$y }, mc.cores=%0)", cores));

        assertEval(Ignored.ImplementationError, "f <- function() { res <- parallel:::mclapply(1:2, function(i) i); print(res)}; f()");
        assertEval(Ignored.ImplementationError, "f <- function() { res <- parallel:::mclapply(1:3, function(i) i); print(res)}; f()");
        assertEval(Ignored.ImplementationError, "f <- function() { res <- parallel:::mclapply(1:10, function(i) i); print(res)}; f()");

        // we are checking for functions env being properly unserialized in the second run
        // so yes, the test executes function f() twice
        assertEval(Ignored.ImplementationError, "f <- function() { res <- parallel:::mclapply(1:3, function(i) i)}; f() ; f()");
    }

    @Test
    public void testMCLapplyNested() {
        // race-conditions, easilly reproducible with LLVM
        assertEval(Ignored.ImplementationError, "parallel:::mclapply(1:3, function(i) { Sys.sleep(.2); parallel:::mclapply(1:3, function(ii) {ii}) })");
        assertEval(Ignored.ImplementationError,
                        "parallel:::mclapply(1:3, function(i) { Sys.sleep(.1); parallel:::mclapply(1:3, function(i) { Sys.sleep(.1); parallel:::mclapply(1:3, function(i) {i}) }) })");
    }
}
