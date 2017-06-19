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
public class TestBuiltin_putconst extends TestBase {

    @Test
    public void testputconst1() {
        // FIXME RInternalError: not implemented: .Internal putconst
        assertEval(Ignored.Unimplemented, "argv <- list(list(NULL), 0, NULL); .Internal(putconst(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testputconst2() {
        // FIXME RInternalError: not implemented: .Internal putconst
        assertEval(Ignored.Unimplemented, "argv <- list(list(list(), NULL), 1, list()); .Internal(putconst(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
