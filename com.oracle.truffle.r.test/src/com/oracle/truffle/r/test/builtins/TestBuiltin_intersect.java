/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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

public class TestBuiltin_intersect extends TestBase {

    private static final String[] DATA = {
                    "1.1", "1L", "'a'", "1+1i", "T", "NA",
                    "c(1, 2, 3)", "c(1L, 2L, 3L)",
                    "c(2, 4, 3)", "c(2L, 4L, 3L)",
                    "c(1:3)", "c(1.1:3.1)",
                    "c(T, F, T)", "c(T, F)",
                    "c('a', 'b', 'c')", "c('a', 'c', 'b')",
                    "c(1+1i, 2+1i, 3+1i)", "c(1+1i, 4+1i, 3+1i)",
                    /* "as.raw(c(1, 2, 3))", "as.raw(c(2, 4, 3))" */};

    private static final String[] IGNORED_DATA = {
                    "as.raw(c(1, 2, 3))", "as.raw(c(2, 4, 3))"};

    @Test
    public void test() {
        assertEval(template("{ intersect(%0, %1) }", DATA, DATA));
        assertEval(Ignored.ImplementationError, template("{ intersect(%0, %1) }", DATA, IGNORED_DATA));
    }
}
