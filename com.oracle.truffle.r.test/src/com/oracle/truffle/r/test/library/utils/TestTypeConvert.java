/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

public class TestTypeConvert extends TestBase {
    @Test
    public void basicTests() {
        assertEval("type.convert('42')");
        assertEval("type.convert(c('42', '42.42'))"); // convert to double vector
        assertEval("type.convert(c('NA', 'NA'))");
        assertEval("type.convert(c('NA', 'TRUE'))");
        assertEval("type.convert(c('NA', '42'))");
        assertEval("type.convert(c('NA', '44.5'))");
        assertEval("type.convert(c(NA, '44.5'))");  // string NA
        // looks like integer, but is double (because it would be INT_NA)
        assertEval("type.convert('-2147483648')");
    }

    private static final String[] LIT_VALUES = new String[]{"0xFFF", "0xFFFFFFFFFFF", "123", "2147483648"};

    @Test
    public void testConvertLiterals() {
        for (String suf : new String[]{"", "L"}) {
            for (String sign : new String[]{"", "-", "+"}) {
                String l = sign + "%0" + suf;
                assertEval(template("type.convert('" + l + "')", LIT_VALUES));
                assertEval(template("typeof(type.convert('" + l + "'))", LIT_VALUES));
            }
        }
    }

    @Test
    public void testFirstTypeMustBeOfModeTest() {
        // UnsupportedSpecializationException: Unexpected values provided for ...
        assertEval(Ignored.Unimplemented, "type.convert('NA', 1)");
    }
}
