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

package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_unserialize extends TestBase {
    private static final String[] BASIC_TYPE_VALUES = new String[]{
                    "c(1,2,3,4)", "3L", "42", "\"Hello world\"", "3+2i", "TRUE", "head(mtcars)",
                    "data.frame(col1=c(9,8,7), col2=1:3)", "expression(x+1)", "list(1,2)", "NULL"
    };

    @Test
    public void tests() {
        assertEval(template("unserialize(serialize(%0, NULL))", BASIC_TYPE_VALUES));
    }

    @Test
    public void testserializeAndUnserializeClosure() {
        // N.B.: FastR does not preserve code formatting like GNU R does
        assertEval(Ignored.OutputFormatting, "unserialize(serialize(function (x) { x }, NULL))");
        assertEval("f <- function() x; e <- new.env(); e$x <- 123; environment(f) <- e; expr <- substitute({ FUN() }, list(FUN=f)); eval(expr); expr <- unserialize(serialize(expr, NULL)); eval(expr)");
    }
}
