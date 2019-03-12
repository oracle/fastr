/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

import java.nio.file.FileSystems;

import com.oracle.truffle.r.launcher.RVersionNumber;
import com.oracle.truffle.r.runtime.REnvVars;

public class Utils {
    private static final String GNUR_R_HOME = "GNUR_HOME_BINARY";
    /**
     * Cached value of {@code GNUR_HOME}.
     */
    private static String gnurHome;

    /**
     * @return the original GNUR home directory. This directory is needed for building FastR and
     *         testing.
     */
    public static String gnurHome() {
        if (gnurHome == null) {
            gnurHome = System.getenv(GNUR_R_HOME);
            if (gnurHome == null) {
                gnurHome = FileSystems.getDefault().getPath(REnvVars.rHome(), "libdownloads", RVersionNumber.R_HYPHEN_FULL).toString();
            }
        }
        return gnurHome;
    }
}
