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

public class TestBuiltin_starts_endsWith extends TestBase {
    @Test
    public void testStartsWith() {
        assertEval("{ startsWith(\"abc\", \"a\") }");
        assertEval("{ startsWith(\"abc\", \"b\") }");
        assertEval("{ startsWith(c(\"abc\", \"xyz\", \"ade\"), \"a\") }");
        assertEval("{ startsWith(c(\"abc\", \"xyz\", \"ade\"), \"b\") }");
        assertEval("{ startsWith(\"abc\", 1) }");
        assertEval("{ startsWith(2, \"1\") }");
        assertEval("{ startsWith(2, 1) }");
    }

    @Test
    public void testEndsWith() {
        assertEval("{ endsWith(\"abc\", \"c\") }");
        assertEval("{ endsWith(\"abc\", \"b\") }");
        assertEval("{ endsWith(c(\"abc\", \"xyz\", \"ade\"), \"c\") }");
        assertEval("{ endsWith(c(\"abc\", \"xyz\", \"ade\"), \"b\") }");
        assertEval("{ endsWith(\"abc\", 1) }");
        assertEval("{ endsWith(2, \"1\") }");
        assertEval("{ endsWith(2, 1) }");
    }

}
