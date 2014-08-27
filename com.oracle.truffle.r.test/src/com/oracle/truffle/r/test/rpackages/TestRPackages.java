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
package com.oracle.truffle.r.test.rpackages;

import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

/**
 * Tests related to the loading, etc. of R packages.
 */
public class TestRPackages extends TestBase {

    @Test
    public void testLoadVanilla() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "rpackages");
        Path rpackagesLibs = rpackages.resolve("testrlibs_user");
        Path relLibsPath = cwd.relativize(rpackagesLibs);
        if (!relLibsPath.toFile().exists()) {
            relLibsPath.toFile().mkdir();
        }
        Path rpackagesDists = rpackages.resolve("distributions");
        Path vanilla = rpackagesDists.resolve("vanilla_1.0.tar.gz");
        // install the package (using GnuR for now)
        ProcessBuilder pb = new ProcessBuilder("R", "CMD", "INSTALL", vanilla.toString());
        Map<String, String> env = pb.environment();
        env.put("R_LIBS_USER", rpackagesLibs.toString());
        try {
            Process install = pb.start();
            int rc = install.waitFor();
            assertTrue(rc == 0);
            assertTemplateEval(TestBase.template("{ library(\"vanilla\", lib.loc = \"%0\"); vanilla() }", new String[]{relLibsPath.toString()}));
        } catch (Exception ex) {
            assertFalse();
        }
    }
}
