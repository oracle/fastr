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

public class TestBuiltin_lengths extends TestBase {
    @Test
    public void basics() {
        assertEval("lengths(c(1,2,3))");
        assertEval("lengths(1:11)");
        assertEval("lengths(list(1, c(1,2), 1:3))");
        assertEval("lengths(list(1, list(1, c(1,2)), 1:3))");
        assertEval("lengths(NULL)");
    }

    @Test
    public void withNames() {
        assertEval("lengths(list(a=1, b=c(1,2)))");
        assertEval("lengths(list(a=1, b=c(1,2)), use.names=FALSE)");
        assertEval("x<-c(1,2); names(x) <- c('a', 'b'); lengths(x)");
        assertEval("x<-c(1,2); names(x) <- c('a', 'b'); lengths(x, use.names=FALSE)");
        // dimnames are not used:
        assertEval("lengths(matrix(1:4, nrow=2, ncol=2, dimnames=list(c('a', 'b'), c('d', 'e'))))");
    }

    @Test
    public void wrongArgs() {
        assertEval(Output.IgnoreErrorContext, "lengths(quote(a))");
        assertEval(Output.IgnoreErrorContext, "lengths(42, use.names='as')");
    }
}
