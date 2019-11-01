/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.runtime.context.FastROptions;
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
        String home = rHome();
        envVars.put(R_HOME, home);
        // Always read the system file
        TruffleFile rHomeDir = FileSystemUtils.getSafeTruffleFile(context.getEnv(), home);
        safeReadEnvironFile(rHomeDir.resolve("etc").resolve("Renviron"));

        String internalArgs = System.getenv("FASTR_INTERNAL_ARGS");
        // Allow host access in FastR subprocesses if allowed here
        if (context.getEnv().isHostLookupAllowed()) {
            if (internalArgs == null) {
                internalArgs = "--jvm";
            } else if (!internalArgs.contains("--jvm")) {
                internalArgs += " --jvm";
            }
        }
        // Forward some FastR options to FastR subprocesses
        String forwardedOptions = FastROptions.getForwardedOptions(context);
        if (forwardedOptions != null) {
            if (internalArgs == null) {
                internalArgs = forwardedOptions;
            } else {
                internalArgs += ' ' + forwardedOptions;
            }
        }
        if (internalArgs != null) {
            envVars.put("FASTR_INTERNAL_ARGS", internalArgs);
        }

        envVars.put("R_DOC_DIR", rHomeDir.resolve("doc").toString());
        envVars.put("R_INCLUDE_DIR", rHomeDir.resolve("include").toString());
        envVars.put("R_SHARE_DIR", rHomeDir.resolve("share").toString());
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
                siteFile = rHomeDir.resolve("etc").resolve("Renviron.site").toString();
            }
            Env env = context.getEnv();
            TruffleFile siteTruffleFile = FileSystemUtils.getSafeTruffleFile(env, siteFile);
            if (siteTruffleFile.exists()) {
                safeReadEnvironFile(siteTruffleFile);
            }
            String userFilePath = envVars.get("R_ENVIRON_USER");
            if (userFilePath == null) {
                String dotRenviron = ".Renviron";
                userFilePath = FileSystemUtils.getSafeTruffleFile(env, (String) BaseRFFI.GetwdRootNode.create().getCallTarget().call()).resolve(dotRenviron).toString();
                if (FileSystemUtils.getSafeTruffleFile(env, userFilePath).exists()) {
                    userFilePath = FileSystemUtils.getSafeTruffleFile(env, System.getProperty("user.home")).resolve(dotRenviron).toString();
                }
            }
            TruffleFile userFile = userFilePath != null ? FileSystemUtils.getSafeTruffleFile(env, userFilePath) : null;
            if (userFile != null && userFile.exists()) {
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
                    // so always cleanup all protocol prefixes
                    proxy = proxy.replace("http://", "").replace("https://", "").replace("ftp://", "");
                    System.setProperty(protocol + ".proxyHost", proxy);

                    if (port != null) {
                        System.setProperty(protocol + ".proxyPort", port);
                    } else {
                        System.getProperties().remove(protocol + ".proxyPort");
                    }

                    // the setting for https is taken from http according to JDK docs
                    if ("http".equals(protocol) || "ftp".equals(protocol)) {
                        String noProxy = getEitherCase(protocol + "_no_proxy");
                        if (noProxy == null) {
                            noProxy = getEitherCase("no_proxy");
                        }

                        if (noProxy != null) {
                            System.setProperty(protocol + ".nonProxyHosts", convertNoProxy(noProxy));
                        } else {
                            System.getProperties().remove(protocol + ".nonProxyHosts");
                        }
                    }
                }
            }
        }
        return this;
    }

    // converts the no_proxy env variable syntax to Java syntax for nonProxyHosts
    private static String convertNoProxy(String value) {
        String[] items = value.split(",");
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (item.startsWith(".")) {
                // .some.url => *.some.url
                result.append('*').append(item);
            } else {
                result.append(item);
            }
            if (i != items.length - 1) {
                result.append('|');
            }
        }
        return result.toString();
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

    /**
     * Cached value of {@code R_HOME}.
     */
    private static AtomicReference<String> rHome = new AtomicReference<>();

    /**
     * Cached TruffleFile value of {@code R_HOME}.
     */
    private static AtomicReference<TruffleFile> rHomeTruffleFile = new AtomicReference<>();

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
        if (rHome.get() == null) {
            RContext ctx = RContext.getInstance();
            String home = System.getenv(R_HOME);
            if (home == null) {
                TruffleFile rHomePath = getRHomePath(ctx);
                if (rHomePath == null) {
                    // Create RError directly to avoid R error reporting, which may not be
                    // initialized yet
                    throw new RError("Cannot determine the R home. " +
                                    "Please export environment variable R_HOME with path to the FastR home directory. " +
                                    "FastR home is usually located in {GraalVM}/jre/languages/R in JDK8 based builds and " +
                                    "in {GraalVM}/languages/R in JDK11 based builds.", RError.NO_CALLER);
                }
                home = rHomePath.toString();
            } else if (!validateRHome(ctx.getEnv().getInternalTruffleFile(home), markerFile())) {
                throw new RError("The FastR home directory given in an environment variable R_HOME appears to be not valid FastR home directory. " +
                                "FastR home is usually located in {GraalVM}/jre/languages/R in JDK8 based builds and " +
                                "in {GraalVM}/languages/R in JDK11 based builds.", RError.NO_CALLER);
            }
            rHome.set(home);
        }
        return rHome.get();
    }

    @TruffleBoundary
    public static TruffleFile getRHomeTruffleFile(Env env) {
        if (rHomeTruffleFile.get() == null) {
            rHomeTruffleFile.set(env.getInternalTruffleFile(rHome()));
        }
        return rHomeTruffleFile.get();
    }

    /**
     * In the case where {@code R_HOME} is not set, which should only occur when FastR is invoked
     * from a {@link org.graalvm.polyglot.Engine} created by another language, we try to locate the
     * {@code R_HOME} dynamically by using the home location reported by Truffle.
     *
     * @return either a valid {@code R_HOME} or {@code null}
     */
    private static TruffleFile getRHomePath(RContext ctx) {
        String truffleRHome = ctx.getLanguage().getRHome();
        if (truffleRHome == null) {
            return null;
        }
        TruffleFile rHomePath = ctx.getEnv().getInternalTruffleFile(truffleRHome);
        String markerFile = markerFile();
        while (rHomePath != null) {
            if (validateRHome(rHomePath, markerFile)) {
                return rHomePath;
            }
            rHomePath = rHomePath.getParent();
        }
        return rHomePath;
    }

    /**
     * Sanity check on the expected structure of an {@code R_HOME}.
     */
    @TruffleBoundary
    private static boolean validateRHome(TruffleFile rHomePath, String markerFile) {
        if (rHomePath == null) {
            return false;
        }
        TruffleFile etc = rHomePath.resolve("etc");
        TruffleFile absMarkerFile = etc.resolve(markerFile);
        return etc.exists() && etc.isDirectory() && absMarkerFile.exists() && isFastR(absMarkerFile);
    }

    private static boolean isFastR(TruffleFile makeconf) {
        try {
            List<String> lines = FileSystemUtils.readAllLines(makeconf);
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

    public String put(String key, String value) {
        // TODO we set the value for sub-processes via ProcessBuilder, but native code will not see
        // this environment variable. We need to set it at the system level
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
        readEnvironFile(FileSystemUtils.getSafeTruffleFile(RContext.getInstance().getEnv(), path));
    }

    public void readEnvironFile(TruffleFile file) throws IOException {
        try (BufferedReader r = file.newBufferedReader()) {
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                // name=value
                int ix = line.indexOf('=');
                if (ix < 0) {
                    throw invalid(file.getPath(), line);
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
            TruffleFile defCranMirror = FileSystemUtils.getSafeTruffleFile(RContext.getInstance().getEnv(), REnvVars.rHome()).resolve("etc").resolve("DEFAULT_CRAN_MIRROR");
            if (!defCranMirror.exists()) {
                throw RSuicide.rSuicide("Missing etc/DEFAULT_CRAN_MIRROR file");
            }
            List<String> cranMirrors;
            try {
                cranMirrors = FileSystemUtils.readAllLines(defCranMirror);
            } catch (IOException e) {
                throw RSuicide.rSuicide("Invalid etc/DEFAULT_CRAN_MIRROR file");
            }
            assert !cranMirrors.isEmpty();
            cranMirror = cranMirrors.get(0);
            // If FASTR_MRAN_MIRROR is exported, we replace the MRAN base URL with our
            // overlay, but keep the path including the date
            String mranMirrorOverlay = System.getenv("FASTR_MRAN_MIRROR");
            if (mranMirrorOverlay != null && !mranMirrorOverlay.trim().isEmpty()) {
                String date = cranMirror.substring(cranMirror.length() - "2000-00-00".length());
                if (!mranMirrorOverlay.endsWith("/")) {
                    mranMirrorOverlay += '/';
                }
                cranMirror = mranMirrorOverlay + "snapshot/" + date;
            }
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

    private void safeReadEnvironFile(TruffleFile file) {
        try {
            readEnvironFile(file);
        } catch (IOException ex) {
            RLogger.getLogger(REnvVars.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
