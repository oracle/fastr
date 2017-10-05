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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

/**
 * Repository for environment variables, including those set by FastR itself, e.g.
 * {@code R_LIBS_USER}.
 *
 * N.B. We assert that the {@code R_HOME} environment variable is set by the launch script(s) with
 * one exception, when run under the GraalVM shell, in which case it is set explicitly in
 * {@link #rHome()}.
 */
public final class REnvVars implements RContext.ContextState {

    private final Map<String, String> envVars = new HashMap<>(System.getenv());

    private REnvVars() {
    }

    @Override
    public RContext.ContextState initialize(RContext context) {
        // explicit environment settings in nested contexts must be installed first
        checkExplicitEnvSettings(context);
        RCmdOptions cmdOptions = context.getCmdOptions();
        // If running Rscript, R_DEFAULT_PACKAGES may need to be set
        String defaultPackages = cmdOptions.getString(RCmdOption.DEFAULT_PACKAGES);
        if (defaultPackages != null) {
            envVars.put("R_DEFAULT_PACKAGES", defaultPackages);
        }
        // set the standard vars defined by R
        checkRHome();
        // Always read the system file
        FileSystem fileSystem = FileSystems.getDefault();
        safeReadEnvironFile(fileSystem.getPath(rHome, "etc", "Renviron").toString());
        envVars.put("R_DOC_DIR", fileSystem.getPath(rHome, "doc").toString());
        envVars.put("R_INCLUDE_DIR", fileSystem.getPath(rHome, "include").toString());
        envVars.put("R_SHARE_DIR", fileSystem.getPath(rHome, "share").toString());
        String rLibsUserProperty = envVars.get("R_LIBS_USER");
        if (rLibsUserProperty == null) {
            if (isMacOS()) {
                rLibsUserProperty = "~/Library/R/%v/library";
            } else {
                rLibsUserProperty = "~/R/%p-library/%v";
            }
            envVars.put("R_LIBS_USER", rLibsUserProperty);
            // This gets expanded by R code in the system profile
        }

        if (!context.getStartParams().noRenviron()) {
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
                userFile = fileSystem.getPath((String) BaseRFFI.GetwdRootNode.create().getCallTarget().call(), dotRenviron).toString();
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
        return this;
    }

    public static REnvVars newContextState() {
        return new REnvVars();
    }

    private void checkExplicitEnvSettings(RContext context) {
        String[] envs = context.getEnvSettings();
        if (envs == null || envs.length == 0) {
            return;
        }
        for (String envdef : envs) {
            String[] parts = envdef.split("=");
            if (parts.length == 2) {
                envVars.put(parts[0], parts[1]);
            } else {
                // for now just ignore
            }
        }
    }

    private String getEitherCase(String var) {
        String val = envVars.get(var);
        return val != null ? val : envVars.get(var.toUpperCase());
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os.contains("Mac OS");
    }

    private static final String R_HOME = "R_HOME";

    /**
     * Cached value of {@code R_HOME}.
     */
    private static String rHome;

    /**
     * Returns a file that serves to distinguish a FastR {@code R_HOME}.
     */
    private static String markerFile() {
        return "Makeconf";
    }

    /**
     * Returns the value of the {@code R_HOME} environment variable (setting it in the unusual case
     * where it it is not set by the initiating shell scripts. This may be called very early in the
     * startup possibly before the initial context is initialized and, therefore, before
     * {@link #envVars} is available.
     */
    public static String rHome() {
        if (rHome == null) {
            rHome = System.getenv(R_HOME);
            Path rHomePath;
            if (rHome == null) {
                rHomePath = getRHomePath();
            } else {
                rHomePath = Paths.get(rHome);
            }
            if (!validateRHome(rHomePath, markerFile())) {
                Utils.rSuicide("R_HOME is not set correctly");
            }
            rHome = rHomePath.toString();
        }
        return rHome;
    }

    private static final CodeSource codeSource = REnvVars.class.getProtectionDomain().getCodeSource();

    /**
     * In the case where {@code R_HOME} is not set, which should only occur when FastR is invoked
     * from a {@link PolyglotEngine} created by another language, we try to locate the
     * {@code R_HOME} dynamically by using the location of this class. The logic varies depending on
     * whether this class was stored in a {@code .jar} file or in a {@code .class} file in a
     * directory.
     *
     * @return either a valid {@code R_HOME} or {@code null}
     */
    private static Path getRHomePath() {
        Path path = Paths.get(codeSource.getLocation().getPath()).getParent();
        String markerFile = markerFile();
        while (path != null) {
            if (validateRHome(path, markerFile)) {
                return path;
            }
            path = path.getParent();
        }
        return path;
    }

    /**
     * Sanity check on the expected structure of an {@code R_HOME}.
     */
    @TruffleBoundary
    private static boolean validateRHome(Path path, String markerFile) {
        if (path == null) {
            return false;
        }
        Path etc = path.resolve("etc");
        Path absMarkerFile = etc.resolve(markerFile);
        return Files.exists(etc) && Files.isDirectory(etc) && Files.exists(absMarkerFile) && isFastR(absMarkerFile);
    }

    private static boolean isFastR(Path makeconf) {
        try {
            List<String> lines = Files.readAllLines(makeconf);
            for (String line : lines) {
                if (line.startsWith("CFLAGS")) {
                    return line.contains("-DFASTR");
                }
            }
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere();
        }
        return false;
    }

    private void checkRHome() {
        String envRHome = envVars.get(R_HOME);
        if (envRHome == null) {
            envVars.put(R_HOME, rHome());
        } else {
            if (rHome == null) {
                rHome = envRHome;
            }
        }
    }

    public String put(String key, String value) {
        // TODO need to set value for sub-processes
        return envVars.put(key, value);
    }

    public String get(String key) {
        return envVars.get(key);
    }

    public boolean unset(String key) {
        // TODO remove at the system level
        envVars.remove(key);
        return true;
    }

    public Map<String, String> getMap() {
        return envVars;
    }

    public void readEnvironFile(String path) throws IOException {
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
                envVars.put(var, value);
            }
        }
    }

    private String expandParameters(String value) {
        StringBuilder result = new StringBuilder();
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
            String paramValue = envVars.get(paramName);
            if (paramValue == null || paramValue.length() == 0) {
                paramValue = stripQuotes(paramDefault);
            }
            result.append(paramValue);
            x = paramEnd + 1;
            paramStart = value.indexOf("${", x);
        }
        result.append(value.substring(x));
        return result.toString();
    }

    private static String stripQuotes(String s) {
        if (s.length() == 0) {
            return s;
        }
        if (s.charAt(0) == '\'') {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    @TruffleBoundary
    private static IOException invalid(String path, String line) throws IOException {
        throw new IOException("   File " + path + " contains invalid line(s)\n      " + line + "\n   They were ignored\n");
    }

    private void safeReadEnvironFile(String path) {
        try {
            readEnvironFile(path);
        } catch (IOException ex) {
            // CheckStyle: stop system..print check
            System.out.println(ex.getMessage());
        }
    }
}
