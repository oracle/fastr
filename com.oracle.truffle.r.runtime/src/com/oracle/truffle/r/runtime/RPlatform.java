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
package com.oracle.truffle.r.runtime;

public class RPlatform {
    public static final class OSInfo {
        public final String libExt;
        public final String osSubDir;
        public final String osName;

        private OSInfo() {
            osName = System.getProperty("os.name");
            switch (osName) {
                case "Mac OS X":
                    osSubDir = "darwin";
                    libExt = "dylib";
                    break;

                case "Linux":
                    osSubDir = "linux";
                    libExt = "so";
                    break;

                case "SunOS":
                    osSubDir = "solaris";
                    libExt = "so";
                    break;

                default:
                    osSubDir = null;
                    libExt = null;
                    Utils.fail("CallRFFI: unsupported OS: " + osName);
            }
        }
    }

    private static OSInfo osInfo;

    public static OSInfo getOSInfo() {
        if (osInfo == null) {
            osInfo = new OSInfo();
        }
        return osInfo;
    }
}
