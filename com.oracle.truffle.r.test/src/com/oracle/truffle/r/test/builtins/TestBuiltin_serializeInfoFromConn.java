/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_serializeInfoFromConn extends TestBase {

    private static final String[] VERSIONS = new String[]{"2", "3"};

    @Test
    public void testLoadInfoFromConn2() {
        assertEval(template("vi <- .Internal(serializeInfoFromConn(rawConnection(serialize(42, connection=NULL, version=%0)))); " +
                        "vi$version; vi$writer_version; vi$min_reader_version; vi$format; vi$native_encoding; ", VERSIONS));
    }

    @Test
    public void testToolsGetSerializationVersion() {
        // Note: we use "as.integer" to remove the "names" attribute that contains name of the file
        assertEval(template(
                        "{ f <- tempfile(fileext = '.rds'); a <- 42; con <- file(f,'wb'); serialize('a', connection=con, version=%0); close(con); as.integer(tools:::get_serialization_version(f)); }",
                        VERSIONS));
    }
}
