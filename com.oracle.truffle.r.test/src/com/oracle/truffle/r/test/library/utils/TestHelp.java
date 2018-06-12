/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.utils;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.TestBase;

public class TestHelp extends TestBase {
    @Test
    public void testInteropHelp() {
        assertHelpResult(fastREval("?java.type", ContextKind.SHARE_PARENT_RW, false), "==== R Help on ‘java.type’ ====", "Access to a java type given by",
                        "An polyglot value representing a java type");
        assertHelpResult(fastREval("help(java.type)", ContextKind.SHARE_PARENT_RW, false), "==== R Help on ‘java.type’ ====", "Access to a java type given by",
                        "An polyglot value representing a java type");
        assertHelpResult(fastREval("example(java.type)", ContextKind.SHARE_PARENT_RW, false), null, "java.type('java.util.ArrayList')", "$class");
    }

    @Test
    public void testGrDevicesHelp() {
        assertHelpResult(fastREval("?svg.off", ContextKind.SHARE_PARENT_RW, false), "==== R Help on ‘svg.off’ ====", "SVG");
        assertHelpResult(fastREval("help(svg.off)", ContextKind.SHARE_PARENT_RW, false), "==== R Help on ‘svg.off’ ====", "SVG");
    }

    private static void assertHelpResult(String result, String startsWith, String... contains) {
        if (startsWith != null) {
            assertThat(result, CoreMatchers.startsWith(startsWith));
        }
        for (String s : contains) {
            assertThat(result, CoreMatchers.containsString(s));
        }
    }
}
