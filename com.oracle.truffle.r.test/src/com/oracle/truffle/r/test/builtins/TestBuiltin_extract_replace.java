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

// Checkstyle: stop line length check

/**
 * Tests for assignments that go through {@code CachedReplaceVectorNode}.
 */
public class TestBuiltin_extract_replace extends TestBase {

    @Test
    public void extractAndReplaceByItself() {
        assertEval("tmp <- c(1,8,NA,3); pivot <- c(1,2,4,3); tmp[pivot] <- tmp; tmp");
    }

    @Test
    public void replaceInLanguagePreservesAttributes() {
        assertEval("f <- quote(a+b); attr(f, 'mya') <- 42; f[[2]] <- quote(q); f");
    }

    @Test
    public void replaceExpressionInLanguage() {
        assertEval("e1 <- expression(x^2); l1 <- quote(y^2); l1[1] <- e1; l1[[1]]==e1[[1]]");
        assertEval(Ignored.OutputFormatting, "e1 <- expression(x^2); l1 <- quote(y^2); l1[1] <- e1; l1");
    }
}
