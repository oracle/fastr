/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_crc64 extends TestBase {

    @Test
    public void testCrc64_Internal() {

        assertEval("crc64()");
        assertEval("crc64('a')");
        assertEval(".Internal(crc64())");
        assertEval(".Internal(crc64('a'))");
        assertEval(".Internal(crc64(paste(c(letters, LETTERS, 0:9), collapse=\"\")))");
        assertEval(".Internal(crc64(c('a')))");

        assertEval(".Internal(crc64('a', 'b'))");

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

    @Test
    public void testCrc64_Call() {

        assertEval(".Call(utils:::C_crc64, 'abc')");

        assertEval(".Call(utils:::C_crc64, paste(c(letters, LETTERS, 0:9), collapse=\"\"))");

        assertEval(".Call(utils:::C_crc64, c('a'))");

        assertEval(".Call(utils:::C_crc64, 'a', 'b')");
        assertEval(".Call(utils:::C_crc64)");

        assertEval(".Call(utils:::C_crc64, c(1, 2))");

        assertEval(".Call(utils:::C_crc64, c('a', 'b'))");

        assertEval(".Call(utils:::C_crc64, NA)");
        assertEval(".Call(utils:::C_crc64, NULL)");
        assertEval(".Call(utils:::C_crc64, list(list()))");
        assertEval(".Call(utils:::C_crc64, list(NULL))");
        assertEval(".Call(utils:::C_crc64, c(NULL))");

        assertEval(".Call(utils:::C_crc64, integer(0))");
        assertEval(".Call(utils:::C_crc64, double(0))");

        assertEval(".Call(utils:::C_crc64, 01)");

        assertEval(".Call(utils:::C_crc64, new.env())");
        assertEval(".Call(utils:::C_crc64, environment)");
        assertEval(".Call(utils:::C_crc64, stdout())");

    }
}
