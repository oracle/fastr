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
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The (global) variables defined in the {@code base} package, e.g. {@code .Platform}. As per
 * {@link BaseOptions} the definition and initialization is two-step process handled through the
 * {@link RPackageVariables} class.
 *
 * TODO Some of these are defined in the R (base) source code in GnuR, so long term FastR should do
 * the same.
 *
 */
public class BaseVariables implements RPackageVariables.Handler {
    // @formatter:off
    private static final String[] VARS = new String[]{
        ".Platform", ".Library", ".LibrarySite"
    };
    // @formatter:on

    // @formatter:off
    private static final String[] PLATFORM_NAMES = new String[] {
        "OS.type", "file.sep", "dynlib.ext", "GUI", "endian", "pkgType", "path.sep", "r_arch"
    };
    // @formatter:on

    public BaseVariables() {
        RPackageVariables.registerHandler("base", this);
    }

    public void initialize(REnvironment env) {
        for (String var : VARS) {
            @SuppressWarnings("unused")
            Object value = null;
            switch (var) {
                case ".Platform":
                    // .Platform TODO be more accurate
                    String[] platformData = new String[]{"unix", File.separator, ".so", "unknown", "little", "source", File.pathSeparator, ""};
                    value = RDataFactory.createList(platformData, RDataFactory.createStringVector(PLATFORM_NAMES, RDataFactory.COMPLETE_VECTOR));
                    break;

                case ".Library":
                    value = RDataFactory.createStringVector(com.oracle.truffle.r.runtime.LibPaths.dotLibrary());
                    break;

                case ".LibrarySite":
                    value = RDataFactory.createStringVector(com.oracle.truffle.r.runtime.LibPaths.dotLibrarySite(), RDataFactory.COMPLETE_VECTOR);
                    break;

            }
        }
        // TODO store the value through env (TBD)
    }

}
