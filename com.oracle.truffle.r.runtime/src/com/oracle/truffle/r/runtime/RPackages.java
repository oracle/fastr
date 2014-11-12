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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;

/**
 * Support for recording the set of default R packages.
 */
public class RPackages {
    // TODO: this should actually be private
    public static class RPackage {
        public final String name;
        public final String path;

        RPackage(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    @CompilationFinal public static final String[] DEFAULT_PACKAGES = new String[]{"methods", "fastr", "stats", "utils"};

    private static ArrayList<RPackage> packages = new ArrayList<>();

    /**
     * Set up the initial set of packages, checking {@code R_DEFAULT_PACKAGES}. GnuR does the latter
     * using "Rprofile", which we may do also eventually.
     *
     * @return the initial set of packages
     */
    public static ArrayList<RPackage> initialize() {
        String[] defaultPackages;
        String defaultPackagesEnv = REnvVars.get("R_DEFAULT_PACKAGES");
        if (defaultPackagesEnv == null || defaultPackagesEnv.equals("")) {
            defaultPackages = DEFAULT_PACKAGES;
        } else if (defaultPackagesEnv.equals("NULL")) {
            defaultPackages = new String[0];
        } else {
            defaultPackages = defaultPackagesEnv.split(",");
        }
        for (String pkg : defaultPackages) {
            packages.add(new RPackage(pkg, REnvVars.rHome()));
        }
        return packages;
    }

}
