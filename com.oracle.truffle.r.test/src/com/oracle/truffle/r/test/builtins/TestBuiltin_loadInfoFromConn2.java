/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_loadInfoFromConn2 extends TestBase {

    private static final String[] VERSIONS = new String[]{"2", "3"};

    @Test
    public void testLoadInfoFromConn2() {
        assertEval(template("{ con <- rawConnection(raw(0), 'r+'); a <- 42; save('a', file=con, version=%0); seek(con, 0); .Internal(loadInfoFromConn2(con))$version; }", VERSIONS));
    }

    @Test
    public void testToolsGetSerializationVersion() {
        // Note: we use "as.integer" to remove the "names" attribute that contains name of the file
        assertEval(template("{ f <- tempfile(fileext = '.rds'); saveRDS(42, f, version=%0); as.integer(tools:::get_serialization_version(f)); }", VERSIONS));
    }
}
