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
 * Handles the loading of site and user profile code. TODO implement the actual sourceing of the
 * file content.
 */
public class RProfile {
    public static void initialize() {
        String rHome = REnvVars.rHome();
        FileSystem fileSystem = FileSystems.getDefault();

        if (!ROptions.NO_SITE_FILE.getValue()) {
            String siteProfile = REnvVars.get("R_PROFILE");
            if (siteProfile == null) {
                siteProfile = fileSystem.getPath(rHome, "etc", "Rprofile.site").toString();
            } else {
                siteProfile = Utils.tildeExpand(siteProfile);
            }
            if (new File(siteProfile).exists()) {
                // TODO source the content
            }
        }

        if (!ROptions.NO_INIT_FILE.getValue()) {
            String userProfile = REnvVars.get("R_PROFILE_USER");
            if (userProfile == null) {
                String dotRenviron = ".Rprofile";
                userProfile = fileSystem.getPath(BaseRFFIFactory.getRFFI().getwd(), dotRenviron).toString();
                if (!new File(userProfile).exists()) {
                    userProfile = fileSystem.getPath(System.getProperty("user.home"), dotRenviron).toString();
                }
            } else {
                userProfile = Utils.tildeExpand(userProfile);
            }
            if (userProfile != null && new File(userProfile).exists()) {
                // TODO source the content
            }

        }
    }
}
