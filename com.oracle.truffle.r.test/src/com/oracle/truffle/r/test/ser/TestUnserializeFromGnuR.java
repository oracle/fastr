/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.ser;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

public class TestUnserializeFromGnuR extends TestBase {

    private static Map<String, Path> paths = new HashMap<>();

    /**
     * Somewhat roundabout way to get an absolute file path that we can pass to the {@code readRDS}
     * function.
     */
    @Before
    public void init() {
        try {
            InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), "data");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.endsWith(".rds") || line.endsWith(".bin")) {
                        String key = line.trim();
                        final String rResource = "data/" + key;
                        URL url = ResourceHandlerFactory.getHandler().getResource(getClass(), rResource);
                        paths.put(key, TestBase.relativize(Paths.get(url.getPath())));
                    }
                }
            }
        } catch (IOException ex) {
            Assert.fail("error loading serialization data for " + getClass().getSimpleName() + " : " + ex);
        }

    }

    @Test
    public void testVectors() {
        runUnserializeFromConn("vector1.rds");
        runUnserializeFromConn("list2.rds");
    }

    @Test
    public void readChar() {
        readCharTests("testchar.bin", new String[]{"c(3, 10, 3)", "c(1, 8, 3)", "c(4, 9, 3)"});
    }

    private static void runUnserializeFromConn(String fileName) {
        assertTemplateEval(TestBase.template("{ print(.Internal(unserializeFromConn(gzfile(\"%0\"), NULL))) }", new String[]{paths.get(fileName).toString()}));
    }

    private static void readCharTests(String fileName, String[] nchars) {
        assertTemplateEval(TestBase.template("{ zz <- file(\"%0\", \"rb\"); nc<-%1; readChar(zz, nc) }", new String[]{paths.get(fileName).toString()}, nchars));
    }

}
