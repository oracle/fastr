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

import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

/**
 * Checks that the invocations of `[` ({@link ExtractVectorNode}) from within the R code for
 * `[.data.frame` are handled correctly. Especially if there is a need for special handling of
 * 'data.frames' in {@link ExtractVectorNode}. These examples were selected using rcov to get a
 * decent coverage of `[.data.frame`. Note: there is no invocation of 'NextMethod' in
 * `[<-.data.frame`, `[[<-.data.frame`, and `[[.data.frame`.
 */
public class TestBuiltin_extract_dataframe extends TestBase {

    @Test
    public void extractNormalDataFrame() {
        String[] args = new String[]{"" /* empty */, "'cyl'", "4", "c(1,2,10,5)", "matrix(c(1,2,10,5),nrow=2,ncol=2)", "1,2", "1,2,drop=0", "-3", "1:4,'cyl'"};
        assertEval(template("mtcars[%0]", args));
    }

    @Test
    public void undefinedColumnGivesError() {
        assertEval(Output.IgnoreErrorContext, "mtcars[42]");
    }

    @Test
    public void extractDataFrameWithNAinNames() {
        String[] args = new String[]{"1,2", "1,2,drop=0"};
        assertEval(template("{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- c(NA,'col'); fr[%0] }", args));
    }

    @Test
    public void extractDataFrameWithNULLNames() {
        assertEval("{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- NULL; fr[1,2] }");
        assertEval("{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- NULL; fr['col'] }");
    }
}
