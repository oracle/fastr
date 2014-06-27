/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.ffi.*;

/**
 * Handles the setup of system, site and user profile code. N.B. {@link #initialize()} only reads
 * the files and leaves the evaluation to the caller, using {@link #siteProfile()} and
 * {@link #userProfile()}.
 */
public class RProfile {
    /**
     * The system profile location is hard-wired relative to this class and loaded statically for
     * AOT VMs.
     */
    private static String systemProfile = Utils.getResourceAsString(RProfile.class, "R/Rprofile.R", true);

    public static void initialize() {
        String rHome = REnvVars.rHome();
        FileSystem fileSystem = FileSystems.getDefault();

        if (!RCmdOptions.NO_SITE_FILE.getValue()) {
            String siteProfilePath = REnvVars.get("R_PROFILE");
            if (siteProfilePath == null) {
                siteProfilePath = fileSystem.getPath(rHome, "etc", "Rprofile.site").toString();
            } else {
                siteProfilePath = Utils.tildeExpand(siteProfilePath);
            }
            File siteProfileFile = new File(siteProfilePath);
            if (siteProfileFile.exists()) {
                siteProfile = source(siteProfileFile);
            }
        }

        if (!RCmdOptions.NO_INIT_FILE.getValue()) {
            String userProfilePath = REnvVars.get("R_PROFILE_USER");
            if (userProfilePath == null) {
                String dotRenviron = ".Rprofile";
                userProfilePath = fileSystem.getPath(RFFIFactory.getRFFI().getBaseRFFI().getwd(), dotRenviron).toString();
                if (!new File(userProfilePath).exists()) {
                    userProfilePath = fileSystem.getPath(System.getProperty("user.home"), dotRenviron).toString();
                }
            } else {
                userProfilePath = Utils.tildeExpand(userProfilePath);
            }
            if (userProfilePath != null) {
                File userProfileFile = new File(userProfilePath);
                if (userProfileFile.exists()) {
                    userProfile = source(userProfileFile);
                }
            }

        }
    }

    private static String siteProfile;
    private static String userProfile;

    public static String systemProfile() {
        return systemProfile;
    }

    public static String siteProfile() {
        return siteProfile;
    }

    public static String userProfile() {
        return userProfile;
    }

    private static String source(File file) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[(int) file.length()];
            is.read(bytes);
            return new String(bytes);
        } catch (IOException ex) {
            Utils.fail("unexpected error reading profile file: " + file);
            return null;
        }

    }
}
