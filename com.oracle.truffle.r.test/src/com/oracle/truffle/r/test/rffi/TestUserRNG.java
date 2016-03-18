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
package com.oracle.truffle.r.test.rffi;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.test.TestBase;

/**
 * Test for a user-defined random number generator. Implicitly tests {@code dyn.load} as well as the
 * {@link UserRngRFFI} interface. We take care to use relative paths so the expected output file is
 * portable.
 */
public class TestUserRNG extends TestBase {
    @Test
    public void testUserRNG() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path libPath = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test.native/urand/lib/liburand.so");
        Path relLibPath = cwd.relativize(libPath);
        assertEval(TestBase.template("{ dyn.load(\"%0\"); RNGkind(\"user\"); print(RNGkind()); set.seed(4567); runif(10) }", new String[]{relLibPath.toString()}));
    }
}
