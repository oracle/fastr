/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.utils;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestUtils extends TestBase {
    @Test
    public void testHeadNTail() {
        assertEval("{head(letters)}");
        assertEval("{head(letters, n = 10L)}");
        assertEval("{head(letters, n = -6L)}");
        assertEval("{tail(letters)}");
        assertEval("{tail(letters, n = 10L)}");
        assertEval("{tail(letters, n = -6L)}");
        assertEval("{x<-matrix(c(1,2,3,4),2,2); tail(x,1);}");
        assertEval("{x<-matrix(c(1,2,3,4),2,2); head(x,1);}");
    }

    @Test
    public void testMethods() {
        // The vector of methods is not sorted alphabetically
        assertEval(Ignored.ImplementationError, "methods(plot)");
    }
}
