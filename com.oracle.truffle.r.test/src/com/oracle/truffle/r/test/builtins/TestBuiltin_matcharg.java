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
        assertEval("match.arg(NULL, c('a', 'b', 'c'))");
        assertEval("match.arg(NULL, c(1, 2, 3))");
        assertEval("match.arg(NULL, c())");
        assertEval("match.arg(NULL, character())");
        assertEval("match.arg(NULL, list())");
        assertEval("match.arg(NULL, NULL)");

        // plain vector
        assertEval("match.arg(c('a'), c('a'), T)");
        assertEval("match.arg(c('a', 'b'), c('a', 'b'), T)");
        assertEval("match.arg(c('a', 'b'), list('a', 'b'), T)");
        assertEval("match.arg(c('1', '2'), list(1, 2), T)");

        assertEval("match.arg(c('a', 'b'), c('a'), F)");
        assertEval("match.arg(c('a', 'b'), c('a', 'b'), F)");
        assertEval("match.arg(c('a', 'b'), list('a', 'b'), F)");
        assertEval("match.arg(c('1', '2'), list(1, 2), F)");

        assertEval("match.arg(c('a', 'b'), c('a'), T)");
        assertEval("match.arg(c('a', 'b'), c('b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('b', 'c'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('c', 'b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('a', 'b'), T)");
        assertEval("match.arg(c('a', 'b', 'c'), c('a', 'c'), T)");

        assertEval("match.arg(c('1'), c(1), T)");
        assertEval("match.arg(c('1', '2'), c(1), T)");

        assertEval("match.arg(c('1'), c(1), F)");
        assertEval("match.arg(c('1', '2'), c(1, 2), F)");
        assertEval("match.arg(c('1', '2'), c('1', '2'), F)");

        // named vector
        assertEval("{ v<-c('a', 'b'); names(v) <- c('an', 'bn'); match.arg(c('a', 'b'), v, F) }");

        assertEval("{ v<-c('a', 'b'); names(v) <- c('an', 'bn'); match.arg(c('b'), v, F) }");
        assertEval("{ v<-c(1, 2); names(v) <- c('an', 'bn'); match.arg(c('1'), v, F) }");
        assertEval("{ v<-c(1L, 2L); names(v) <- c('an', 'bn'); match.arg(c('1'), v, F) }");
        assertEval("{ v<-as.raw(c(1, 2)); names(v) <- c('an', 'bn'); match.arg(c('01'), v, F) }");
        assertEval("{ v<-c(T, F); names(v) <- c('an', 'bn'); match.arg(c('F'), v, F) }");
        assertEval("{ v<-c(1+1i, 2+1i); names(v) <- c('an', 'bn'); match.arg(c('1+1i'), v, F) }");

        assertEval("{ v<-c('a', 'b'); names(v) <- c('an', 'bn'); match.arg(c('a', 'b'), v, T) }");
        assertEval("{ v<-c('a', 'b'); names(v) <- c('an', 'bn'); match.arg(c('b'), v, T) }");
        assertEval("{ v<-c(1, 2); names(v) <- c('an', 'bn'); match.arg(c('1'), v, T) }");
        assertEval("{ v<-c(1L, 2L); names(v) <- c('an', 'bn'); match.arg(c('1'), v, T) }");
        assertEval("{ v<-as.raw(c(1, 2)); names(v) <- c('an', 'bn'); match.arg(c('01'), v, T) }");
        assertEval("{ v<-c(T, F); names(v) <- c('an', 'bn'); match.arg(c('F'), v, T) }");
        assertEval("{ v<-c(1+1i, 2+1i); names(v) <- c('an', 'bn'); match.arg(c('1+1i'), v, T) }");

        // list/named list
        assertEval("match.arg(c('1'), list(c(1, 11), 3, 2))");

        assertEval("match.arg(c('a'), list('a', 'b'), F)");
        assertEval("match.arg(c('a'), list('a', 'b'), T)");

        assertEval("match.arg(c('a', 'c'), list('a', 'b', 'c'), F)");
        assertEval("match.arg(c('a', 'c'), list('a', 'b', 'c'), T)");

        // gnur doesn't match, fastr does
        assertEval(Ignored.ImplementationError, "match.arg(c('01'), list(as.raw(1)), T)");

        assertEval("match.arg(c('a', '1', '2L', '01', 'TRUE', '1+1i'), list('a', 1, 2L, TRUE, 1+1i, 'xxx'), T)");

        assertEval("match.arg(c('a'), list(an='a', bn='b'), T)");
        assertEval("match.arg(c('a'), list(an='a', bn='b'), F)");

        assertEval("match.arg(c('a', 'c'), list(an='a', bn='b', cn='c'), T)");
        assertEval("match.arg(c('a', 'c'), list(an='a', bn='b', 'c'), T)");

        assertEval("match.arg(c('1'), list(1), T)");

        assertEval("match.arg(c('1', 'a'), list(1, 3, 2, 'a'), T)");

        assertEval(Output.IgnoreErrorContext, "match.arg(c('2'), environment(), T)");
        assertEval("match.arg(c('2'), list(2, environment()), T)");

        assertEval("match.arg(list('a', 'b'), 'c', T)");
        assertEval("match.arg(c('a'), c('b'), T)");

        // s3
        assertEval("{ as.character.foo <- function(x) 'bar'; x <- 42; class(x) <- 'foo'; match.arg('ba', x) }");

    }
}
