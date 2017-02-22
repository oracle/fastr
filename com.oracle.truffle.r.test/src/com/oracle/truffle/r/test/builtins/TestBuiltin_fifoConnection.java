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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_fifoConnection extends TestBase {

    private static List<Path> TEMP_FIFOS = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        Path path = Paths.get(System.getProperty("java.io.tmpdir"));
        TEMP_FIFOS.add(path.resolve("pipe3408688236"));
        TEMP_FIFOS.add(path.resolve("pipe4039819292"));
    }

    @Test
    public void testFifoOpenInexisting() {
        assertEval("capabilities(\"fifo\")");

        Assert.assertFalse(Files.exists(TEMP_FIFOS.get(0)));
        assertEval(Output.IgnoreErrorContext, Output.IgnoreWarningContext, "{ zz <- fifo(\"" + TEMP_FIFOS.get(0) + "\", \"r\"); close(zz); }");
    }

    @Test(timeout = 100)
    public void testFifoOpenNonBlocking() {
        Assert.assertFalse(Files.exists(TEMP_FIFOS.get(0)));
        assertEval(Output.IgnoreErrorContext, Output.IgnoreWarningContext, "{ zz <- fifo(\"" + TEMP_FIFOS.get(0) + "\", \"r\"); close(zz); }");
    }

    @AfterClass
    public static void cleanup() {
        for (Path p : TEMP_FIFOS) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
