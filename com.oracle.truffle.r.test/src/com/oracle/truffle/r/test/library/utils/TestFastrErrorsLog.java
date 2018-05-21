/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.REnvVars;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class TestFastrErrorsLog extends TestBase {

    @Test
    public void testThrowInternalError() throws Exception {
        boolean origValue = FastROptions.PrintErrorStacktracesToFile.getBooleanValue();
        FastROptions.setValue(FastROptions.PrintErrorStacktracesToFile.name(), true);
        try {
            final String FASTR_ERRORS_LOG = "fastr_errors"; // Copy of
                                                            // RInternalError.FASTR_ERRORS_LOG
            int pid = RContext.getInitialPid();
            String baseName = FASTR_ERRORS_LOG + "_pid" + Integer.toString(pid) + ".log";
            if (RContext.isEmbedded()) {
                String dir1 = System.getProperty("java.io.tmpdir");
                Path path1 = FileSystems.getDefault().getPath(dir1, baseName);
                try {
                    assertFalse(Files.exists(path1));
                    reportError();
                    assertTrue(Files.exists(path1));
                } finally {
                    try {
                        Files.delete(path1);
                    } catch (IOException ex) {
                    }
                }
            } else {
                String dir1 = System.getProperty("user.dir");
                Path path1 = FileSystems.getDefault().getPath(dir1, baseName);

                String dir2 = System.getProperty("user.home");
                Path path2 = FileSystems.getDefault().getPath(dir2, baseName);

                String dir3 = System.getProperty("java.io.tmpdir");
                Path path3 = FileSystems.getDefault().getPath(dir3, baseName);

                String dir4 = REnvVars.rHome();
                Path path4 = FileSystems.getDefault().getPath(dir4, baseName);

                try {
                    // Intentionally not testing that the particular log file is not present yet
                    // to not depend on in which directory the test gets started.
                    // assertFalse(Files.exists(path1));
                    reportError();
                    assertTrue(Files.exists(path1));
                    setReadonly(path1, true);

                    reportError();
                    assertTrue(Files.exists(path2));
                    setReadonly(path2, true);

                    reportError();
                    assertTrue(Files.exists(path3));
                    setReadonly(path3, true);

                    reportError();
                    assertTrue(Files.exists(path4));
                } finally {
                    try {
                        setReadonly(path1, false);
                        Files.delete(path1);
                    } catch (IOException ex) {
                    }
                    try {
                        setReadonly(path2, false);
                        Files.delete(path2);
                    } catch (IOException ex) {
                    }
                    try {
                        setReadonly(path3, false);
                        Files.delete(path3);
                    } catch (IOException ex) {
                    }
                    try {
                        Files.delete(path4);
                    } catch (IOException ex) {
                    }
                }
            }
        } finally {
            FastROptions.setValue(FastROptions.PrintErrorStacktracesToFile.name(), origValue);
        }
    }

    private static final void setReadonly(Path path, boolean readonly) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
        if (readonly) {
            perms.remove(PosixFilePermission.OWNER_WRITE);
        } else {
            perms.add(PosixFilePermission.OWNER_WRITE);
        }
        Files.setPosixFilePermissions(path, perms);
    }

    private static final void reportError() {
        PrintStream temp = new PrintStream(new ByteArrayOutputStream());
        PrintStream origErr = System.err;
        System.setErr(temp);
        RInternalError.reportError(new Throwable("Throwing expected error in test."));
        System.setErr(origErr);
    }

}
