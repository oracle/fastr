/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_tilde extends TestBase {

    @Test
    public void testTildeDirect() {
        assertEval("y <- x ~ q; str(y); list(y[[1]], y[[2]], y[[3]])");
        assertEval("y <- x ~~ q; str(y); list(y[[1]], y[[2]], y[[3]], y[[3]][[1]], y[[3]][[2]])");
        assertEval("y <- x ~~~ q; str(y); list(y[[1]], y[[2]], y[[3]], y[[3]][[1]], y[[3]][[2]], y[[3]][[2]][[1]], y[[3]][[2]][[2]])");
        assertEval("y <- x ~~~~ q; str(y); list(y[[1]], y[[2]], y[[3]], y[[3]][[1]], y[[3]][[2]], y[[3]][[2]][[1]], y[[3]][[2]][[2]], y[[3]][[2]][[2]][[1]], y[[3]][[2]][[2]][[2]])");
        assertEval("~ x + y");
        assertEval("x ~ y + z");
        assertEval("y ~ 0 + x");
    }

    @Test
    public void testTildeIndirect() {
        assertEval("do.call('~', list(quote(x + y)))");
        assertEval("do.call('~', list(quote(x), quote(y + z)))");
        assertEval("do.call('~', list(quote(y), quote(0 + x)))");
    }

}
