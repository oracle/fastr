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

public class TestBuiltin_memDecompress extends TestBase {

    @Test
    public void testmemDecompress1() {
        // FIXME
        // com.oracle.truffle.r.runtime.RInternalError: not implemented: .Internal memDecompress
        assertEval(Ignored.Unimplemented,
                        "argv <- structure(list(from = as.raw(c(253, 55, 122, 88, 90,     0, 0, 1, 105, 34, 222, 54, 2, 0, 33, 1, 28, 0, 0, 0, 16,     207, 88, 204, 224, 7, 207, 0, 28, 93, 0, 24, 140, 130, 182,     196, 17, 52, 92, 78, 225, 221, 115, 179, 63, 98, 20, 119,     183, 90, 101, 43, 5, 112, 179, 75, 69, 222, 0, 0, 155, 136,     185, 16, 0, 1, 52, 208, 15, 0, 0, 0, 105, 254, 40, 141, 62,     48, 13, 139, 2, 0, 0, 0, 0, 1, 89, 90)), type = 'xz', asChar = TRUE),     .Names = c('from', 'type', 'asChar'));" +
                                        "do.call('memDecompress', argv)");
    }
}
