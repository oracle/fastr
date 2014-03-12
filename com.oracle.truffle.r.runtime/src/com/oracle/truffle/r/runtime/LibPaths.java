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
import java.nio.file.*;

/**
 * Handles the initialization of the {@code R_LIBS_USER} environment variable and processes
 * {@code R_LIBS}.
 */
public class LibPaths {

    private static String dotLibrary;
    private static String[] dotLibrarySite;
    private static String[] dotLibPaths;
    private static String[] dotLibrarySitePlusLibrary;

    private static boolean SystemInstall;

    public static void initialize() {
        SystemInstall = false;
        dotLibrary = FileSystems.getDefault().getPath(REnvVars.rHome(), "library").toString();

        String rLibsSiteProperty = System.getenv("R_LIBS_SITE");
        if (rLibsSiteProperty == null) {
            dotLibrarySite = new String[]{FileSystems.getDefault().getPath(REnvVars.rHome(), "site-library").toString()};
        } else {
            dotLibrarySite = expandAndSplit(rLibsSiteProperty);
        }

        dotLibrarySitePlusLibrary = new String[dotLibrarySite.length + 1];
        System.arraycopy(dotLibrarySite, 0, dotLibrarySitePlusLibrary, 0, dotLibrarySite.length);
        dotLibrarySitePlusLibrary[dotLibrarySite.length] = dotLibrary;

        String rLibsUserProperty = System.getenv("R_LIBS_USER");
        if (rLibsUserProperty == null) {
            String os = System.getProperty("os.name");
            // GnuR for Mac OS X follows Mac conventions for the path,
            // no value in that for FastR at this time (not an installed app).
            if (os.contains("Mac OS") && SystemInstall) {
                rLibsUserProperty = REnvVars.rHome() + "/Library/R/" + RVersionNumber.MAJOR_MINOR + "/library";
            } else {
                rLibsUserProperty = REnvVars.rHome() + "/R/" + "%p-library/" + RVersionNumber.MAJOR_MINOR;
            }
            REnvVars.put("R_LIBS_USER", rLibsUserProperty);
        }
        String[] rLibsUser = expandAndSplit(rLibsUserProperty);

        String rLibsProperty = System.getenv("R_LIBS");
        // According to the spec, R_LIBS is not subject to specifier expansion
        String[] rLibs = new String[0];
        if (rLibsProperty != null) {
            rLibs = expandAndSplit(rLibsProperty, false);
        }

        rLibs = validate(rLibs);
        rLibsUser = validate(rLibsUser);
        dotLibPaths = new String[rLibs.length + rLibsUser.length];
        System.arraycopy(rLibs, 0, dotLibPaths, 0, rLibs.length);
        System.arraycopy(rLibsUser, 0, dotLibPaths, rLibs.length, rLibsUser.length);

    }

    private static String[] validate(String[] paths) {
        boolean[] valid = new boolean[paths.length];
        int validCount = 0;
        for (int i = 0; i < paths.length; i++) {
            valid[i] = new File(paths[i]).exists();
            if (valid[i]) {
                validCount++;
            }
        }
        if (validCount == paths.length) {
            return paths;
        }
        String[] newPaths = new String[validCount];
        int x = 0;
        for (int i = 0; i < paths.length; i++) {
            if (valid[i]) {
                newPaths[x++] = paths[i];
            }
        }
        return newPaths;
    }

    private static String[] expandAndSplit(String property) {
        return expandAndSplit(property, true);
    }

    private static String[] expandAndSplit(String property, boolean expand) {
        String[] paths = property.split(":");
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (expand) {
                int ixp = path.indexOf('%');
                if (ixp >= 0) {
                    StringBuffer result = new StringBuffer();
                    int ixs = 0;
                    while (ixp >= 0) {
                        result.append(path.substring(ixs, ixp));
                        ixp++;
                        if (ixp >= path.length()) {
                            Utils.fail("conversion character expected after '%'; in " + path);
                        }
                        String replacement = null;
                        switch (path.charAt(ixp)) {
                            case 'V':
                                replacement = RVersionNumber.FULL;
                                break;
                            case 'v':
                                replacement = RVersionNumber.MAJOR_MINOR;
                                break;
                            case 'p':
                                replacement = RVersionInfo.Platform.value();
                                break;
                            case 'o':
                                replacement = RVersionInfo.OS.value();
                                break;
                            case 'a':
                                replacement = RVersionInfo.Arch.value();
                                break;
                            default:
                                Utils.fail("unknown conversion character " + path.charAt(ixp) + " in " + path);
                        }
                        result.append(replacement);
                        ixs = ixp + 1;
                        ixp = path.indexOf('%', ixs);
                    }
                    result.append(path.substring(ixs));
                    paths[i] = result.toString();
                }
            }
        }
        return paths;
    }

    public static String dotLibrary() {
        return dotLibrary;
    }

    public static String[] dotLibrarySite() {
        return dotLibrarySite;
    }

    public static String[] dotLibrarySitePlusLibrary() {
        return dotLibrarySitePlusLibrary;
    }

    public static String[] dotLibPaths() {
        return dotLibPaths;
    }
}
