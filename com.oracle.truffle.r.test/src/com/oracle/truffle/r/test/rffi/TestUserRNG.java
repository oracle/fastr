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
package com.oracle.truffle.r.test.rffi;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.Test;

import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.test.TestBase;

/**
 * Test for a user-defined random number generator. Implicitly tests {@code dyn.load} as well as the
 * {@link UserRngRFFI} interface. The actual library is stored in a tar file, the location of which
 * we can get from the {@code fastr.test.native} system property.
 */
public class TestUserRNG extends TestBase {
    @Test
    public void testUserRNG() {
        Path dir = createTestDir("userrng");
        String tarFile = System.getProperty("fastr.test.native");
        assert tarFile != null;
        String[] tarC = new String[]{"tar", "xf", tarFile};
        ProcessBuilder pb = new ProcessBuilder(tarC);
        pb.directory(dir.toFile());
        try {
            Process p = pb.start();
            int rc = p.waitFor();
            assert rc == 0;
            assertEval(TestBase.template("{ dyn.load(\"%0\"); RNGkind(\"user\"); print(RNGkind()); set.seed(4567); runif(10) }", new String[]{dir.toString() + "/liburand.so"}));
        } catch (IOException ex) {
            assert false;
        } catch (InterruptedException ex) {
        }
    }
}
