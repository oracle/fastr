/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.fastr;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestInterop extends TestBase {

    @Test
    public void testInteropEval() {
        assertEvalFastR(".fastr.interop.eval('application/x-r', '14 + 2')", "16");
        assertEvalFastR(".fastr.interop.eval('application/x-r', '1')", "1");
        assertEvalFastR(".fastr.interop.eval('application/x-r', '1L')", "1L");
        assertEvalFastR(".fastr.interop.eval('application/x-r', 'TRUE')", "TRUE");
        assertEvalFastR(".fastr.interop.eval('application/x-r', 'as.character(123)')", "as.character(123)");
    }

    @Test
    public void testInteropExport() {
        assertEvalFastR(".fastr.interop.export('foo', 14 + 2)", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', 'foo')", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', 1:100)", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', new.env())", "invisible()");
    }
}
