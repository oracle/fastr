/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.common;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RPlatform;
import com.oracle.truffle.r.runtime.RPlatform.OSInfo;

public class LibPaths {

    /**
     * Returns the absolute path to the directory containing the builtin libraries.
     */
    @TruffleBoundary
    public static String getBuiltinLibPath() {
        return FileSystems.getDefault().getPath(REnvVars.rHome(), "lib").toString();
    }

    /**
     * Returns the absolute path to the builtin library {@code libName} for use with
     * {@link System#load}.
     */
    @TruffleBoundary
    public static String getBuiltinLibPath(String libName) {
        String rHome = REnvVars.rHome();
        OSInfo osInfo = RPlatform.getOSInfo();
        Path path = FileSystems.getDefault().getPath(rHome, "lib", "lib" + libName + "." + osInfo.libExt);
        return path.toString();
    }

    /**
     * Returns the absolute path to the shared library associated with package {@code name}. (Does
     * not check for existence).
     */
    @TruffleBoundary
    public static String getPackageLibPath(String name) {
        String rHome = REnvVars.rHome();
        String packageDir = "library";
        Path path = FileSystems.getDefault().getPath(rHome, packageDir, name, "libs", name + ".so");
        return path.toString();
    }
}
