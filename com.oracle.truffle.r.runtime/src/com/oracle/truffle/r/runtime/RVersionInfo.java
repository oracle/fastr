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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;

public enum RVersionInfo {
    // @formatter:off
    Platform(),
    Arch(java.lang.System.getProperty("os.arch")),
    OS(),
    System(),
    Major(RVersionNumber.MAJOR),
    Minor(RVersionNumber.MINOR_PATCH), // GnuR compatibility
    Year(RVersionNumber.RELEASE_YEAR),
    Month(RVersionNumber.RELEASE_MONTH),
    Day(RVersionNumber.RELEASE_DAY),
    SvnRev("svn rev", null),
    Language("R"),
    Engine("FastR"),
    VersionString("version.string", RVersionNumber.VERSION_STRING);
    // @formatter:on

    public static final int SERIALIZE_VERSION = (2 << 16) + (3 << 8) + 0;

    @CompilationFinal(dimensions = 1) private static final RVersionInfo[] VALUES = RVersionInfo.values();
    @CompilationFinal(dimensions = 1) private static String[] ListValues;
    @CompilationFinal(dimensions = 1) private static String[] ListNames;

    private final String listName;
    private String value;

    RVersionInfo() {
        this(null);
    }

    RVersionInfo(String value) {
        this(null, value);
    }

    RVersionInfo(String name, String value) {
        this.listName = name == null ? name().toLowerCase() : name;
        this.value = value;
    }

    public String listName() {
        return listName;
    }

    public String value() {
        return value;
    }

    private static String toFirstLower(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static void initialize() {
        if (ListValues == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ListValues = new String[VALUES.length];
            ListNames = new String[VALUES.length];
            UtsName utsname = (UtsName) BaseRFFI.UnameRootNode.create().getCallTarget().call();
            String osName = toFirstLower(utsname.sysname());
            String vendor = osName.equals("darwin") ? "apple" : "unknown";
            OS.value = osName + utsname.release();
            for (int i = 0; i < VALUES.length; i++) {
                RVersionInfo data = VALUES[i];
                ListNames[i] = data.listName;
                if (data.value == null) {
                    switch (data) {
                        case Platform:
                            /*
                             * FIXME In order to match the info in the default packages copied from
                             * GnuR, this value on Linux has to be x86_64-unknown-linux-gnu
                             */
                            if (osName.equals("linux")) {
                                if (Arch.value.equals("sparcv9")) {
                                    data.value = "sparc64-unknown-linux-gnu";
                                } else {
                                    data.value = "x86_64-unknown-linux-gnu";
                                }
                            } else if (osName.toLowerCase().equals("sunos")) {
                                data.value = "sparc-sun-solaris2.11";
                            } else {
                                data.value = Arch.value + "-" + vendor + "-" + OS.value;
                            }
                            break;
                        case System:
                            data.value = Arch.value + ", " + OS.value;
                            break;
                        default:
                            data.value = "";
                    }
                }
                ListValues[i] = data.value;
            }
        }
    }

    public static String[] listNames() {
        initialize();
        return ListNames;
    }

    public static String[] listValues() {
        initialize();
        return ListValues;
    }
}
