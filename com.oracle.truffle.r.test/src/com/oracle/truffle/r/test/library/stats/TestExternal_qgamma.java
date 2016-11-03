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

public class TestExternal_qgamma extends TestBase {
    @Test
    public void testQgamma() {
        assertEval("qgamma(1, 1)");
        assertEval("qgamma(0.1, 1)");
        assertEval("qgamma(0, 1)");
        assertEval("qgamma(0.1, 10, rate=2, lower.tail=FALSE)");
        assertEval("qgamma(log(1.2e-8), 10, log.p=TRUE)");
        assertEval("qgamma(c(0.1, 0.5, 0.99), c(10, 20), 7)");
    }

    @Test
    public void testQgammaWrongArgs() {
        assertEval("qgamma(10, 1)");
    }
}
