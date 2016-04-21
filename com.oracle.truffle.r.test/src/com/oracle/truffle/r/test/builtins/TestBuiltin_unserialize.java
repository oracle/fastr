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
    @Test
    public void testserializeAndUnserializeDataFrame() {
        test("data.frame(col1=c(9,8,7), col2=1:3)");
    }

    @Test
    public void testserializeAndUnserializeVector() {
        test("c(1, 2, 3, 4)");
    }

    @Test
    public void testserializeAndUnserializeScalars() {
        test("3L");
        test("42");
        test("\"Hello world\"");
        test("3+2i");
        test("TRUE");
    }

    @Test
    public void testserializeAndUnserializeMtcars() {
        test("head(mtcars)");
    }

    @Test
    public void testserializeAndUnserializeClosure() {
        // N.B.: FastR does not preserve code formatting like GNU R does
        assertEval(Ignored.OutputFormatting, "unserialize(serialize(function (x) { x }, NULL))");
    }

    /**
     * Runs serialize and unserialize with given expression.
     */
    private void test(String expr) {
        assertEval("unserialize(serialize(" + expr + ", NULL))");
    }
}
