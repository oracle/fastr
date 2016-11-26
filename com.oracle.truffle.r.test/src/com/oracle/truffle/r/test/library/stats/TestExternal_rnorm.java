/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestExternal_rnorm extends TestBase {
    @Test
    public void testRnorm() {
        assertEval("set.seed(42); rnorm(10, 10, 10)");
        assertEval("set.seed(42); rnorm('10', 10, 5)");
        assertEval(Output.IgnoreWarningContext, "set.seed(42); rnorm('aa', 10, 0.5)");
        assertEval("set.seed(42); rnorm(10, 2:10, c(0.1, 0.5, 0.9))");
        assertEval("set.seed(42); rnorm(1:10, 2:10, c(0.1, 0.5, 0.9))");
        assertEval("set.seed(42); rnorm(c(1,2), 11:12, c(0.1, 0.5, 0.9))");
    }
}
