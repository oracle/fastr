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
import java.nio.file.*;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * Repository for environment variables, including those set by FastR itself, e.g.
 * {@code R_LIBS_USER}.
 */
public class REnvVars {

    private static Map<String, String> envVars;

    private static Map<String, String> getEnvVars() {
        if (envVars == null) {
            envVars = new HashMap<>(System.getenv());
        }
        return envVars;
    }

    private static Map<String, String> checkEnvVars() {
        if (envVars == null) {
            Utils.fail("envVars not initialized");
        }
        return envVars;
    }

    public static void initialize() {
        getEnvVars();
        // set the standard vars defined by R
        String rHome = rHome();
        FileSystem fileSystem = FileSystems.getDefault();
        envVars.put("R_DOC_DIR", fileSystem.getPath(rHome, "doc").toString());
        envVars.put("R_INCLUDE_DIR", fileSystem.getPath(rHome, "include").toString());
        envVars.put("R_SHARE_DIR", fileSystem.getPath(rHome, "share").toString());

        if (!RCmdOptions.NO_ENVIRON.getValue()) {
            String siteFile = envVars.get("R_ENVIRON");
            if (siteFile == null) {
                siteFile = fileSystem.getPath(rHome, "etc", "Renviron.site").toString();
            }
            if (new File(siteFile).exists()) {
                safeReadEnvironFile(siteFile);
            }
            String userFile = envVars.get("R_ENVIRON_USER");
            if (userFile == null) {
                String dotRenviron = ".Renviron";
                userFile = fileSystem.getPath(RFFIFactory.getRFFI().getBaseRFFI().getwd(), dotRenviron).toString();
                if (!new File(userFile).exists()) {
                    userFile = fileSystem.getPath(System.getProperty("user.home"), dotRenviron).toString();
                }
            }
            if (userFile != null && new File(userFile).exists()) {
                safeReadEnvironFile(userFile);
            }
        }
    }

    public static String rHome() {
        // This can be called before initialize, "R RHOME"
        String rHome = getEnvVars().get("R_HOME");
        if (rHome == null) {
            // Should only happen in a unit test run
            rHome = System.getProperty("user.dir");
        }
        return rHome;
    }

    public static String put(String key, String value) {
        return checkEnvVars().put(key, value);
    }

    public static String get(String key) {
        return checkEnvVars().get(key);
    }

    public static Map<String, String> getMap() {
        return checkEnvVars();
    }

    public static void readEnvironFile(String path) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            checkEnvVars();
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                // name=value
                int ix = line.indexOf('=');
                if (ix < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw invalid(path, line);
                }
                String var = line.substring(0, ix);
                String value = expandParameters(line.substring(ix + 1)).trim();
                // GnuR does not seem to remove quotes, although the spec says it should
                envVars.put(var, value);
            }
        }
    }

    protected static String expandParameters(String value) {
        StringBuffer result = new StringBuffer();
        int x = 0;
        int paramStart = value.indexOf("${", x);
        checkEnvVars();
        while (paramStart >= 0) {
            result.append(value.substring(x, paramStart));
            int paramEnd = value.indexOf('}', paramStart);
            String param = value.substring(paramStart + 2, paramEnd);
            String paramDefault = "";
            String paramName = param;
            int dx = param.indexOf('-');
            if (dx > 0) {
                paramName = param.substring(0, dx);
                paramDefault = expandParameters(param.substring(dx + 1));
            }
            String paramValue = envVars.get(paramName);
            if (paramValue == null || paramValue.length() == 0) {
                paramValue = paramDefault;
            }
            result.append(paramValue);
            x = paramEnd + 1;
            paramStart = value.indexOf("${", x);
        }
        result.append(value.substring(x));
        return result.toString();
    }

    private static IOException invalid(String path, String line) {
        return new IOException("   File " + path + " contains invalid line(s)\n      " + line + "\n   They were ignored\n");
    }

    public static void safeReadEnvironFile(String path) {
        try {
            readEnvironFile(path);
        } catch (IOException ex) {
            // CheckStyle: stop system..print check
            System.out.println(ex.getMessage());
        }
    }

}
