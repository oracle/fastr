/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
