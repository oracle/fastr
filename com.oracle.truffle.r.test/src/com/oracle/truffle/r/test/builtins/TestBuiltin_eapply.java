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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_eapply extends TestBase {

    @Test
    public void testLapply() {
        assertEval(Output.IgnoreErrorContext, "{ eapply() }");
        assertEval("{ eapply(new.env(), 1) }");
        assertEval(Output.IgnoreErrorContext, "{ eapply(new.env()) }");
        assertEval("{ eapply(FUN=function(v) {v}) }");
        assertEval("{ eapply(1, function(v) {v}) }");
        assertEval("{ eapply(new.env(), function(v) {v}, all.names='abc') }");
        assertEval("{ eapply(new.env(), function(v) {v}, all.names=NA) }");
        assertEval("{ eapply(new.env(), function(v) {v}, all.names=T, USE.NAMES='abc') }");
        assertEval("{ eapply(new.env(), function(v) {v}, all.names=T, USE.NAMES=NA) }");

        String env = "e <- new.env(); e$a <- 1; e$b <- 2; e$z <- 100; e$.a <- 'dot.a';";
        assertEval("{ " + env + " l <- eapply(e, function(v) {v}, all.names=T); l[order(names(l))] }");
        assertEval("{ " + env + " l <- eapply(e, function(v) {v}, all.names=F); l[order(names(l))] }");
        assertEval("{ " + env + " l <- eapply(e, function(v) {v}, all.names=T, USE.NAMES=T); l[order(names(l))] }");
        assertEval("{ " + env + " l <- eapply(e, function(v) {v}, all.names=T, USE.NAMES=F); l[order(names(l))] }");

        assertEval("{ eapply(list2env(list(a=1)), function(v) {sys.call()}) }");

    }

}
