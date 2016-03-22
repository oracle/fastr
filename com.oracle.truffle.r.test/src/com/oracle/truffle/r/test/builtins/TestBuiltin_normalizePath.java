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

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_normalizePath extends TestBase {

    @Test
    public void testnormalizePath1() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(c('/home/lzhao/hg/r-instrumented/library', '/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0', '/home/lzhao/hg/r-instrumented/library'), '/', NA); .Internal(normalizePath(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
