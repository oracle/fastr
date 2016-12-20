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
public class TestBuiltin_crc64 extends TestBase {

    @Test
    public void testCrc64() {

        assertEval("crc64()");
        assertEval("crc64('a')");
        assertEval(".Internal(crc64())");
        assertEval(".Internal(crc64('a'))");
        assertEval(".Internal(crc64(paste(c(letters, LETTERS, 0:9), collapse=\"\")))");
        assertEval(".Internal(crc64(c('a')))");

        // Expression: .Internal(crc64('a', 'b'))
        // Expected output: Error: 2 arguments passed to .Internal(crc64) which requires 1
        // FastR output: Error in crc64("a", "b") : unused argument ('b')
        // should be handled in .Internal-s impl ?
        assertEval(Ignored.ImplementationError, ".Internal(crc64('a', 'b'))");

        assertEval(".Internal(crc64(c(1, 2)))");

        assertEval(".Internal(crc64(c('a', 'b')))");

        assertEval(".Internal(crc64(NA))");
        assertEval(".Internal(crc64(NULL))");
        assertEval(".Internal(crc64(list(list())))");
        assertEval(".Internal(crc64(list(NULL)))");
        assertEval(".Internal(crc64(c(NULL)))");

        assertEval(".Internal(crc64(integer(0)))");
        assertEval(".Internal(crc64(double(0)))");

        assertEval(".Internal(crc64(01))");

        assertEval(".Internal(crc64(new.env()))");
        assertEval(".Internal(crc64(environment))");
        assertEval(".Internal(crc64(stdout()))");
    }

}
