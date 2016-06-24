/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.test;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.test.generate.FastRSession;

public class TestBase {

    private static PolyglotEngine testVM;
    static RContext testVMContext;

    @BeforeClass
    public static void setupClass() throws IOException {
        testVM = FastRSession.create().createTestContext(null);
        testVMContext = testVM.eval(FastRSession.GET_CONTEXT).as(RContext.class);
    }

    // clear out warnings (which are stored in shared base env)
    @SuppressWarnings("deprecation") private static final Source CLEAR_WARNINGS = Source.fromText("assign('last.warning', NULL, envir = baseenv())", "<clear_warnings>").withMimeType(
                    TruffleRLanguage.MIME);

    @AfterClass
    public static void finishClass() throws IOException {
        testVM.eval(CLEAR_WARNINGS);
        testVM.dispose();
    }
}
