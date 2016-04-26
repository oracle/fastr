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

public class TestBuiltin_matchfun extends TestBase {

    @Test
    public void testmatchfun() {
        assertEval("min <- 1; match.fun('min')");
        assertEval("min <- 1; match.fun('min', descend=T)");
        assertEval("min <- 1; match.fun('min', descend=F)");
        assertEval("min <- 1; match.fun(min)");
        assertEval("min <- 1; match.fun(min, descend=T)");
        assertEval("min <- 1; match.fun(min, descend=F)");
        assertEval("f <- function() { min <- 1; match.fun('min')}; f()");
        assertEval("f <- function() { min <- 1; match.fun('min', descend=T)}; f()");
        assertEval("f <- function() { min <- 1; match.fun('min', descend=F)}; f()");
        assertEval("f <- function() { min <- 1; match.fun(as.symbol('min'))}; f()");
        assertEval("f <- function() { min <- 1; match.fun(as.symbol('min'), descend=T)}; f()");
        assertEval("f <- function() { min <- 1; match.fun(as.symbol('min'), descend=F)}; f()");
        assertEval("x <- min; f <- function() { min <- 1; match.fun(x)}; f()");
        assertEval("x <- min; f <- function() { min <- 1; match.fun(x, descend=T)}; f()");
        assertEval("x <- min; f <- function() { min <- 1; match.fun(x, descend=F)}; f()");
        assertEval("f <- function(x) { min <- 1; match.fun(x)}; f(min)");
        assertEval("f <- function(x) { min <- 1; match.fun(x, descend=T)}; f(min)");
        assertEval("f <- function(x) { min <- 1; match.fun(x, descend=F)}; f(min)");
        assertEval("min <- 1; f <- function() { match.fun('min')}; f()");
        assertEval("min <- 1; f <- function() { match.fun('min', descend=T)}; f()");
        assertEval("min <- 1; f <- function() { match.fun('min', descend=F)}; f()");
        assertEval("min <- 1; f <- function() { match.fun(as.symbol('min'))}; f()");
        assertEval("min <- 1; f <- function() { match.fun(as.symbol('min'), descend=T)}; f()");
        assertEval("min <- 1; f <- function() { match.fun(as.symbol('min'), descend=F)}; f()");
        assertEval("x <- min; min <- 1; f <- function() { match.fun(x)}; f()");
        assertEval("x <- min; min <- 1; f <- function() { match.fun(x, descend=T)}; f()");
        assertEval("x <- min; min <- 1; f <- function() { match.fun(x, descend=F)}; f()");
        assertEval("min <- 1; f <- function(x) { match.fun(x)}; f(min)");
        assertEval("min <- 1; f <- function(x) { match.fun(x, descend=T)}; f(min)");
        assertEval("min <- 1; f <- function(x) { match.fun(x, descend=F)}; f(min)");

        assertEval("min <- 1; f <- function(min) { match.fun(min)}; f(min)");
        assertEval("min <- 1; f <- function(min) { match.fun(min, descend=T)}; f(min)");
        assertEval("min <- 1; f <- function(min) { match.fun(min, descend=F)}; f(min)");

        assertEval(Output.ContainsError, "min <- 1; f <- function(min) { match.fun(min)}; f(baz)");
        assertEval(Output.ContainsError, "min <- 1; f <- function(min) { match.fun(min, descend=T)}; f(baz)");
        assertEval(Output.ContainsError, "min <- 1; f <- function(min) { match.fun(min, descend=F)}; f(baz)");

        assertEval("min <- 1; f <- function(x) { match.fun(x)}; f(c('min'))");
        assertEval("min <- 1; f <- function(x) { match.fun(x)}; f(c('min', 'max'))");
        assertEval("min <- 1; f <- function() { match.fun(c('min'))}; f()");
        assertEval("min <- 1; f <- function() { match.fun(c('min', 'max'))}; f()");
        assertEval("min <- 1; f <- function() { match.fun(c(1L))}; f()");
        assertEval("min <- 1; f <- function() { match.fun(as.raw(100))}; f()");

        assertEval("x <- min; f <- function(x) { min <- 1; match.fun(x, descend=T)}; f2 <- function(y) f(y); f2(min)");
        assertEval("x <- min; f <- function(x) { min <- 1; match.fun(x, descend=T)}; f2 <- function(max) f(max); f2(min)");

        assertEval("x <- min; f <- function(x) { min <- function(x) x; match.fun(x, descend=T)}; f(min)");
        assertEval("min <- function(x) x; f <- function(x) { match.fun(x, descend=T)}; f(min)");
        assertEval("f <- function(x) { match.fun(x, descend=T)}; f2 <- function() { min <- max; f(min) }; f2()");
    }
}
