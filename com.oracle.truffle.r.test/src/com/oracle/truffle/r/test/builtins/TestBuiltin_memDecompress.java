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

public class TestBuiltin_memDecompress extends TestBase {

    @Test
    public void testmemDecompress1() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(from = as.raw(c(253, 55, 122, 88, 90,     0, 0, 1, 105, 34, 222, 54, 2, 0, 33, 1, 28, 0, 0, 0, 16,     207, 88, 204, 224, 7, 207, 0, 28, 93, 0, 24, 140, 130, 182,     196, 17, 52, 92, 78, 225, 221, 115, 179, 63, 98, 20, 119,     183, 90, 101, 43, 5, 112, 179, 75, 69, 222, 0, 0, 155, 136,     185, 16, 0, 1, 52, 208, 15, 0, 0, 0, 105, 254, 40, 141, 62,     48, 13, 139, 2, 0, 0, 0, 0, 1, 89, 90)), type = 'xz', asChar = TRUE),     .Names = c('from', 'type', 'asChar'));"
                                        + "do.call('memDecompress', argv)");
    }
}
