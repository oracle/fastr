/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_aregexec extends TestBase {

    @Test
    public void testaregexec1() {
        // FIXME
        // FastR output: com.oracle.truffle.r.runtime.RInternalError: not implemented: .Internal
        // aregexec
        assertEval(Ignored.Unimplemented,
                        "argv <- list('FALSE', 'FALSE', c(0.1, NA, NA, NA, NA), c(1L, 1L, 1L), FALSE, FALSE, FALSE); .Internal(aregexec(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testaregexec2() {
        assertEval(Ignored.Unimplemented,
                        "argv <- list('(lay)(sy)', c('1 lazy', '1', '1 LAZY'), c(2, NA, NA, NA, NA), c(1L, 1L, 1L), FALSE, FALSE, FALSE); .Internal(aregexec(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }
}
