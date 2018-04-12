/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_matcharg extends TestBase {

    @Test
    public void testMatchArg() {
        assertEval("match.arg(NULL, c('a'))");
        assertEval(Ignored.ImplementationError, "match.arg(NULL, c(1))");
        assertEval("match.arg(NULL, c())");
        assertEval("match.arg(NULL, character())");
        assertEval(Ignored.ImplementationError, "match.arg(NULL, list())");
        assertEval("match.arg(NULL, NULL)");

        // plain vector
        assertEval("match.arg(c('a'), c('a'), T)");

        assertEval("match.arg(c('a', 'b'), c('a'), F)");

        assertEval("match.arg(c('a', 'b'), c('a'), T)");
        assertEval("match.arg(c('a', 'b'), c('b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('b', 'c'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('c', 'b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('a', 'b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('a', 'c'), T)");

        assertEval(Ignored.ImplementationError, "match.arg(c('1'), c(1), T)");
        assertEval(Ignored.ImplementationError, "match.arg(c('1', '1'), c(1), T)");

        // named vector
        assertEval(Ignored.ImplementationError, "{ v<-c('a', 'b'); names(v) <- c('an', 'ab'); match.arg(c('a', 'b'), v, T)}");

        // list/named list
        assertEval(Ignored.ImplementationError, "match.arg(c('a'), list('a', 'b'), T)");
        assertEval(Ignored.ImplementationError, "match.arg(c('a'), list(a='a', b='b'), T)");
        assertEval(Ignored.ImplementationError, "match.arg(c('1'), list(1), T)");

        assertEval(Output.IgnoreErrorMessage, "match.arg(list('a', 'b'), 'c', T)");
        assertEval(Output.IgnoreErrorMessage, "match.arg(c('a'), c('b'), T)");
    }
}
