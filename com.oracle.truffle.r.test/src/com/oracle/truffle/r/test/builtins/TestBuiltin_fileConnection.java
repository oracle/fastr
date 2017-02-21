/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Class tests basic file connection functionality.
 */
// Checkstyle: stop line length check
public class TestBuiltin_fileConnection extends TestBase {

    private static Path TEMP_FILE_GZIP;

    @BeforeClass
    public static void setup() throws IOException {
        Path path = Paths.get(System.getProperty("java.io.tmpdir"));

        // create a gzipped file
        TEMP_FILE_GZIP = path.resolve("gzipped_____5137528280012599068___.gz");
        OutputStream gzos = new GZIPOutputStream(Files.newOutputStream(TEMP_FILE_GZIP, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
        gzos.write("Hello, World!".getBytes());
        gzos.close();
    }

    @Test
    public void testFileSummary() {

        Assert.assertTrue("Could not create required temp file for test.", Files.exists(TEMP_FILE_GZIP));
        assertEval("{ zz <- file(\"" + TEMP_FILE_GZIP + "\", \"r\"); res <- summary(zz); close(zz); res }");
    }

    @AfterClass
    public static void cleanup() throws IOException {
        Files.delete(TEMP_FILE_GZIP);
    }

}
