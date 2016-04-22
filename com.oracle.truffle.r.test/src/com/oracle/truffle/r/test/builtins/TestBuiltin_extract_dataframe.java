/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
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
        String[] args = new String[]{"" /* empty */, "'cyl'", "42", "c(1,2,10,5)", "matrix(c(1,2,10,5),nrow=2,ncol=2)", "1,2", "1,2,drop=0"};
        assertEval(template("matcars[%0]", args));
    }

    @Test
    public void extractDataFrameWithNAinNames() {
        String[] args = new String[]{"1,2", "1,2,drop=0"};
        assertEval(template("{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- c(NA,'col'); fr[%0] }", args));
    }

    @Test
    public void extractDataFrameWithNULLNames() {
        assertEval("{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- NULL; fr[1,2] }");
        // N.B.: this warning is surprisingly formatted with extra new line before the contents of
        // warning(...) invoked from R code
        assertEval(Output.ContainsWarning, "{ fr <- data.frame(1:3,4:6); attr(fr,'names') <- NULL; fr['col'] }");
    }
}
