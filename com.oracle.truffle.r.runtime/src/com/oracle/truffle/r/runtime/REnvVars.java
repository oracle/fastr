/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.TimeZone;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.launcher.RVersionNumber;
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

    private final HashMap<String, String> envVars;

    private REnvVars(Map<String, String> initialEnvVars) {
        envVars = new HashMap<>(initialEnvVars);
    }

    @Override
    public RContext.ContextState initialize(RContext context) {
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

        String internalArgs = System.getenv("FASTR_INTERNAL_ARGS");
        if (context.getEnv().isHostLookupAllowed()) {
            if (internalArgs == null) {
                internalArgs = "--jvm";
            } else if (!internalArgs.contains("--jvm")) {
                internalArgs += " --jvm";
            }
        }
        if (internalArgs != null) {
            envVars.put("FASTR_INTERNAL_ARGS", internalArgs);
        }

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

        // Check for proxies
        for (String protocol : new String[]{"http", "https", "ftp"}) {
            String javaProxyHost = System.getProperty(protocol + ".proxyHost");
            if (javaProxyHost != null) {
                // if java proxy props are set, then let have them precedense over env settings
                // storing in envVars ensures they get propagated to child processes
                String javaProxyPort = System.getProperty(protocol + ".proxyPort");
                envVars.put(protocol + "_proxy", javaProxyHost + (javaProxyPort != null ? ":" + javaProxyPort : ""));

                if ("http".equals(protocol) || "ftp".equals(protocol)) {
                    // necessary only for http and ftp - according to
                    // https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
                    // The HTTPS handler will use the same nonProxyHosts property
                    // as the HTTP protocol.
                    String noProxy = System.getProperty(protocol + ".no_proxy");
                    if (noProxy != null) {
                        envVars.put(protocol + "_no_proxy", noProxy);
                    } else {
                        envVars.remove(protocol + "_no_proxy");
                    }
                }
            } else {
                // if java properties aren't set, try to use the env values
                String proxy = getEitherCase(protocol + "_proxy");
                if (proxy != null) {
                    String port = null;
                    int portIndex = proxy.lastIndexOf(':');
                    if (portIndex > 0) {
                        port = proxy.substring(portIndex + 1);
                        proxy = proxy.substring(0, portIndex);
                    }
                    // things like https_proxy='http://proxy-server:1234' are a valid case
                    // so always cleanup all protocal prefixes
                    proxy = proxy.replace("http://", "").replace("https://", "").replace("ftp://", "");
                    System.setProperty(protocol + ".proxyHost", proxy);

                    if (port != null) {
                        System.setProperty(protocol + ".proxyPort", port);
                    } else {
                        System.getProperties().remove(protocol + ".proxyPort");
                    }

                    if ("http".equals(protocol) || "ftp".equals(protocol)) {
                        String noProxy = getEitherCase(protocol + "_no_proxy");
                        if (noProxy == null) {
                            noProxy = getEitherCase("no_proxy");
                        }

                        if (noProxy != null) {
                            System.setProperty(protocol + ".no_proxy", noProxy);
                        } else {
                            System.getProperties().remove(protocol + ".no_proxy");
                        }
                    }
                }
            }
        }
        return this;
    }

    public static REnvVars newContextState(Map<String, String> initialEnvVars) {
        return new REnvVars(initialEnvVars);
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
    private static final String GNUR_R_HOME = "GNUR_HOME_BINARY";

    /**
     * Cached value of {@code R_HOME}.
     */
    private static String rHome;

    /**
     * Cached value of {@code GNUR_HOME}.
     */
    private static String gnurHome;

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
    @TruffleBoundary
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
                RSuicide.rSuicide("R_HOME is not set correctly");
            }
            rHome = rHomePath.toString();
        }
        return rHome;
    }

    /**
     * @return the original GNUR home directory. This directory is needed for building FastR and
     *         testing.
     */
    public static String gnurHome() {
        if (gnurHome == null) {
            gnurHome = System.getenv(GNUR_R_HOME);
            if (gnurHome == null) {
                gnurHome = FileSystems.getDefault().getPath(REnvVars.rHome(), "libdownloads", RVersionNumber.R_HYPHEN_FULL).toString();
            }
        }
        return gnurHome;
    }

    private static final CodeSource codeSource = REnvVars.class.getProtectionDomain().getCodeSource();

    /**
     * In the case where {@code R_HOME} is not set, which should only occur when FastR is invoked
     * from a {@link org.graalvm.polyglot.Engine} created by another language, we try to locate the
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

    public TimeZone getSystemTimeZone() {
        String tzName = envVars.get("TZ");
        if (tzName != null) {
            return TimeZone.getTimeZone(tzName);
        }
        return TimeZone.getDefault();
    }

    public static String getCRANMirror() {
        String cranMirror = System.getenv("CRAN_MIRROR");
        if (cranMirror == null) {
            Path defCranMirrorPath = Paths.get(REnvVars.rHome()).resolve("etc").resolve("DEFAULT_CRAN_MIRROR");
            if (!Files.exists(defCranMirrorPath)) {
                throw RSuicide.rSuicide("Missing etc/DEFAULT_CRAN_MIRROR file");
            }
            List<String> cranMirrors;
            try {
                cranMirrors = Files.readAllLines(defCranMirrorPath);
            } catch (IOException e) {
                throw RSuicide.rSuicide("Invalid etc/DEFAULT_CRAN_MIRROR file");
            }
            assert !cranMirrors.isEmpty();
            cranMirror = cranMirrors.get(0);
        }
        return cranMirror;
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
