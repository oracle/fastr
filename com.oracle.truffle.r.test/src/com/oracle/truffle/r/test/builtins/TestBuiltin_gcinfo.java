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
public class TestBuiltin_gcinfo extends TestBase {

    @Test
    public void testgcinfo1() {
        // FIXME com.oracle.truffle.r.runtime.RInternalError: not implemented: .Internal gcinfo
        assertEval(Ignored.Unimplemented, "argv <- list(list()); .Internal(gcinfo(argv[[1]]))");
    }

    @Test
    public void testgcinfo2() {
        // FIXME com.oracle.truffle.r.runtime.RInternalError: not implemented: .Internal gcinfo
        assertEval(Ignored.Unimplemented, "argv <- list(FALSE); .Internal(gcinfo(argv[[1]]))");
    }
}
