/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
public class TestBuiltin_list2env extends TestBase {

    @Test
    public void testEmptyTarget() {
        assertEval("{ e1 <- list2env(list(a = 1, b = 2), new.env()); c(e1$a, e1$b) }");
        // the duplicate member `a` in the source list should win
        assertEval("{ e1 <- list2env(list(a = 1, b = 2, a = 4), new.env()); c(e2$a, e2$b) }");
    }

    @Test
    public void testOverwriteTarget() {
        assertEval("{ e1 <- list2env(list(a = 1, b = 2), new.env()); e2 <- list2env(list(b = 20, c = 30), e1); c(e2$a, e2$b, e2$c) }");
        // the duplicate member `a` in the source list should win and replace the `a` in the target
        assertEval("{ e1 <- list2env(list(a = 1, b = 2, a = 4), new.env()); e2 <- list2env(list(b = 20, c = 30), e1); c(e2$a, e2$b, e2$c) }");
    }

}
