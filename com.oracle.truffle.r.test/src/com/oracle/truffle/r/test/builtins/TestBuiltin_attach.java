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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_attach extends TestBase {
    @Test
    public void basicTests() {
        assertEval("d <- data.frame(colNameX=c(1,2,3)); attach(d); colNameX");
        assertEval("d <- list(colNameX=c(1,2,3)); attach(d); colNameX");
        assertEval("e <- attach(NULL); attr(e, 'name')");
        assertEval("d <- list(col=c(1,2,3)); e <- attach(d, name='hello'); attr(e, 'name')");
    }

    @Test
    public void testArguments() {
        assertEval("attach('string')");
        assertEval(Output.IgnoreErrorMessage, "attach(list(x=42), pos='string')");
        assertEval("attach(list(), name=42)");
        assertEval("detach('string')");
    }

    @Test
    public void detach() {
        assertEval("d <- list(colNameX=c(1,2,3)); attach(d); detach(d); colNameX");
    }

    @Test
    public void sharingTests() {
        assertEval("d <- data.frame(colNameX=c(1,2,3)); attach(d); d$colNameX[1] <- 42; colNameX");
    }
}
