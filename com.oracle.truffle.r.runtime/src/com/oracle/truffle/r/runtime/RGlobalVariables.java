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
package com.oracle.truffle.r.runtime;

import java.io.*;

import com.oracle.truffle.r.runtime.data.*;

public class RGlobalVariables {

    private static final String[] PLATFORM_NAMES = new String[]{"OS.type", "file.sep", "dynlib.ext", "GUI", "endian", "pkgType", "path.sep", "r_arch"};

    static void initialize() {
        // .Platform TODO be more accurate
        String[] platformData = new String[]{"unix", File.separator, ".so", "unknown", "little", "source", File.pathSeparator, ""};
        RList platformValue = RDataFactory.createList(platformData, RDataFactory.createStringVector(PLATFORM_NAMES, RDataFactory.COMPLETE_VECTOR));
        // TODO where do we store this value?
    }
}
