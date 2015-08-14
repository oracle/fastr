/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileSystem;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * Repository for environment variables, including those set by FastR itself, e.g.
 * {@code R_LIBS_USER}.
 *
 * Environment variables are context specific and, certainly there is one case in package loading
 * where R uses an environment variable as a global variable to detect recursion.
 *
 * On startup, before we have any contexts created, we have to support access to the environment
 * variables inherited from the OS environment, e.g., for {@code R_PROFILE}. Additional variable are
 * set during the package loading and these are captured in {@link #systemInitEnvVars}. All
 * subsequent contexts inherit that set and may modify it further.
 */
public class REnvVars implements RContext.StateFactory {

    private static Map<String, String> initialEnvVars;
    private static HashMap<String, String> systemInitEnvVars;
    @CompilationFinal private static boolean initialized;

    private static class ContextStateImpl implements RContext.ContextState {
        private Map<String, String> envVars = new HashMap<>();

    }

    public RContext.ContextState newContext(RContext context, Object... objects) {
        ContextStateImpl result = new ContextStateImpl();
        result.envVars.putAll(systemInitEnvVars == null ? initialEnvVars : systemInitEnvVars);
        if (!initialized) {
            initialized = true;
        }
        return result;
    }

    @Override
    public void systemInitialized(RContext context, RContext.ContextState state) {
        ContextStateImpl optionsState = (ContextStateImpl) state;
        systemInitEnvVars = new HashMap<>(optionsState.envVars.size());
        systemInitEnvVars.putAll(optionsState.envVars);
    }

    private static ContextStateImpl getState() {
        return (ContextStateImpl) RContext.getContextState(RContext.ClassStateKind.REnvVars);
    }

    private static Map<String, String> getInitialEnvVars() {
        if (initialEnvVars == null) {
            initialEnvVars = new HashMap<>(System.getenv());
        }
        return initialEnvVars;
    }

    private static Map<String, String> getEnvVars() {
        if (initialized) {
            return getState().envVars;
        } else {
            return initialEnvVars;
        }
    }

    public static void initialize() {
        getInitialEnvVars();
        // set the standard vars defined by R
        String rHome = rHome();
        initialEnvVars.put("R_HOME", rHome);
        // Always read the system file
        FileSystem fileSystem = FileSystems.getDefault();
        safeReadEnvironFile(fileSystem.getPath(rHome, "etc", "Renviron").toString());
        getEnvVars().put("R_DOC_DIR", fileSystem.getPath(rHome, "doc").toString());
        getEnvVars().put("R_INCLUDE_DIR", fileSystem.getPath(rHome, "include").toString());
        getEnvVars().put("R_SHARE_DIR", fileSystem.getPath(rHome, "share").toString());
        String rLibsUserProperty = System.getenv("R_LIBS_USER");
        if (rLibsUserProperty == null) {
            String os = System.getProperty("os.name");
            if (os.contains("Mac OS")) {
                rLibsUserProperty = "~/Library/R/%v/library";
            } else {
                rLibsUserProperty = "~/R/%p-library/%v";
            }
            getEnvVars().put("R_LIBS_USER", rLibsUserProperty);
            // This gets expanded by R code in the system profile
        }

        if (!RCmdOptions.NO_ENVIRON.getValue()) {
            String siteFile = getEnvVars().get("R_ENVIRON");
            if (siteFile == null) {
                siteFile = fileSystem.getPath(rHome, "etc", "Renviron.site").toString();
            }
            if (new File(siteFile).exists()) {
                safeReadEnvironFile(siteFile);
            }
            String userFile = getEnvVars().get("R_ENVIRON_USER");
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
        // Check for http proxies
        String httpProxy = getEitherCase("http_proxy");
        if (httpProxy != null) {
            String port = null;
            int portIndex = httpProxy.lastIndexOf(':');
            if (portIndex > 0) {
                port = httpProxy.substring(portIndex + 1);
                httpProxy = httpProxy.substring(0, portIndex);
            }
            httpProxy = httpProxy.replace("http://", "");
            System.setProperty("http.proxyHost", httpProxy);
            if (port != null) {
                System.setProperty("http.proxyPort", port);
            }
        }
    }

    private static String getEitherCase(String var) {
        String val = getEnvVars().get(var);
        if (val != null) {
            return val;
        } else {
            return getEnvVars().get(var.toUpperCase());
        }
    }

    private static String rHomePath;

    public static String rHome() {
        // This can be called before initialize, "R RHOME"
        if (rHomePath == null) {
            String path = System.getProperty("rhome.path");
            if (path != null) {
                rHomePath = path;
            } else {
                File file = new File(System.getProperty("user.dir"));
                do {
                    File binR = new File(new File(file, "bin"), "R");
                    if (binR.exists()) {
                        break;
                    } else {
                        file = file.getParentFile();
                    }
                } while (file != null);
                if (file != null) {
                    rHomePath = file.getAbsolutePath();
                } else {
                    Utils.fail("cannot find a valid R_HOME");
                }
            }
            // Check any external setting is consistent
            String envRHomePath = getInitialEnvVars().get("R_HOME");
            if (envRHomePath != null) {
                new File(envRHomePath).getAbsolutePath();
                if (!envRHomePath.equals(rHomePath)) {
                    Utils.fail("R_HOME set to unexpected value in the environment");
                }
            }
        }
        return rHomePath;
    }

    public static String put(String key, String value) {
        // TODO need to set value for sub-processes
        return getEnvVars().put(key, value);
    }

    public static String get(String key) {
        return getEnvVars().get(key);
    }

    public static boolean unset(String key) {
        // TODO remove at the system level
        getEnvVars().remove(key);
        return true;
    }

    public static Map<String, String> getMap() {
        return getEnvVars();
    }

    public static void readEnvironFile(String path) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                // name=value
                int ix = line.indexOf('=');
                if (ix < 0) {
                    throw invalid(path, line);
                }
                String var = line.substring(0, ix);
                String value = expandParameters(line.substring(ix + 1)).trim();
                // GnuR does not seem to remove quotes, although the spec says it should
                getEnvVars().put(var, value);
            }
        }
    }

    protected static String expandParameters(String value) {
        StringBuffer result = new StringBuffer();
        int x = 0;
        int paramStart = value.indexOf("${", x);
        while (paramStart >= 0) {
            result.append(value.substring(x, paramStart));
            int paramEnd = value.lastIndexOf('}');
            String param = value.substring(paramStart + 2, paramEnd);
            String paramDefault = "";
            String paramName = param;
            int dx = param.indexOf('-');
            if (dx > 0) {
                paramName = param.substring(0, dx);
                paramDefault = expandParameters(param.substring(dx + 1));
            }
            String paramValue = getEnvVars().get(paramName);
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

    @TruffleBoundary
    private static IOException invalid(String path, String line) throws IOException {
        throw new IOException("   File " + path + " contains invalid line(s)\n      " + line + "\n   They were ignored\n");
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
