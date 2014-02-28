/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.ffi.*;

/**
 * 
 * As per the GnuR spec, the tempdir() directory is identified on startup.
 * 
 */
public class TempDirPath {

    private static String tempDirPath;

    static {
        //
        final String[] envVars = new String[]{"TMPDIR", "TMP", "TEMP"};
        String startingTempDirPath = null;
        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value != null && BaseRFFIFactory.getRFFI().isWriteableDirectory(value)) {
                startingTempDirPath = value;
            }
        }
        if (startingTempDirPath == null) {
            startingTempDirPath = "/tmp/"; // TODO Windows
        }
        if (!startingTempDirPath.endsWith(File.separator)) {
            startingTempDirPath += startingTempDirPath;
        }
        String t = BaseRFFIFactory.getRFFI().mkdtemp(startingTempDirPath + "Rtmp" + "XXXXXX");
        if (t != null) {
            tempDirPath = t;
        } else {
            Utils.fail("cannot create 'R_TempDir'");
        }
    }

    public static String tempDirPath() {
        return tempDirPath;
    }
}
