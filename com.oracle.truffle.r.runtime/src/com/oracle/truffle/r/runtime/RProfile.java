/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

/**
 * Handles the setup of system, site and user profile code. N.B. this class only reads the files and
 * leaves the evaluation to the caller, using {@link #siteProfile()} and {@link #userProfile()}.
 */
public final class RProfile implements RContext.ContextState {

    private final REnvVars envVars;

    private RProfile(REnvVars envVars) {
        this.envVars = envVars;
    }

    @Override
    @TruffleBoundary
    public RContext.ContextState initialize(RContext context) {
        Source newSiteProfile = null;
        Source newUserProfile = null;

        if (context.getStartParams().getLoadSiteFile()) {
            String siteProfilePath = envVars.get("R_PROFILE");
            if (siteProfilePath == null) {
                siteProfilePath = REnvVars.getRHomeTruffleFile(context).resolve("etc").resolve("Rprofile.site").toString();
            }
            TruffleFile siteProfileFile = context.getSafeTruffleFile(siteProfilePath);
            if (siteProfileFile.exists()) {
                newSiteProfile = getProfile(context, siteProfilePath, false);
            }
        }

        if (context.getStartParams().loadInitFile()) {
            String userProfilePath = envVars.get("R_PROFILE_USER");
            if (userProfilePath == null) {
                String dotRenviron = ".Rprofile";
                userProfilePath = context.getSafeTruffleFile((String) BaseRFFI.GetwdRootNode.create().getCallTarget().call()).resolve(dotRenviron).getPath();
                if (!context.getSafeTruffleFile(userProfilePath).exists()) {
                    userProfilePath = context.getSafeTruffleFile(System.getProperty("user.home")).resolve(dotRenviron).getPath();
                }
            }
            if (userProfilePath != null) {
                TruffleFile userProfileFile = context.getSafeTruffleFile(userProfilePath);
                if (userProfileFile.exists()) {
                    newUserProfile = getProfile(context, userProfilePath, false);
                }
            }
        }
        siteProfile = newSiteProfile;
        userProfile = newUserProfile;
        return this;
    }

    private Source siteProfile;
    private Source userProfile;

    public static Source systemProfile(RContext context) {
        TruffleFile path = REnvVars.getRHomeTruffleFile(context).resolve("library").resolve("base").resolve("R").resolve("Rprofile");
        Source source = getProfile(context, path.getPath(), true);
        if (source == null) {
            RSuicide.rSuicide("can't find system profile");
        }
        return source;
    }

    public Source siteProfile() {
        return siteProfile;
    }

    public Source userProfile() {
        return userProfile;
    }

    private static Source getProfile(RContext context, String path, boolean internal) {
        try {
            return RSource.fromFileName(context, path, internal);
        } catch (IOException ex) {
            // GnuR does not report an error, just ignores
            return null;
        }
    }

    public static RProfile newContextState(REnvVars envVars) {
        return new RProfile(envVars);
    }
}
