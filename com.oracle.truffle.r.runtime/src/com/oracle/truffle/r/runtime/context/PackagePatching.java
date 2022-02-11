/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.FileSystemUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RLogger;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Patches C/C++ source code at given path to be more compatible with FastR, i.e. replaces typical
 * hacks with usage of proper API that FastR implements.
 *
 * This patching is run from during the package installation, but only for packages installed from
 * tarballs.
 *
 * To enable the logger, set MX_R_CMD_ARGS env var to
 * '--log.R.com.oracle.truffle.r.runtime.context.PackagePatching.level=FINE'. Because the code here
 * will be run in a subprocess, if you install the package via e.g. {@code install.packages}.
 */
public class PackagePatching {

    private static final TruffleLogger LOGGER = RLogger.getLogger(PackagePatching.class.getName());

    private static final String NO_PATCHING_ENV_VAR = "FASTR_NO_PKG_PATCHING";
    private static final Patch[] patches = new Patch[]{
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp & (\\w+|\\(.*\\))", "LEVELS($1) & ($2)"),
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp \\| (\\w+|\\(.*\\))", "LEVELS($1) | ($2)"),
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp \\^ (\\w+|\\(.*\\))", "LEVELS($1) ^ ($2)"),
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp &= (\\w+|\\(.*\\))", "SETLEVELS($1, LEVELS($1) & ($2))"),
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp \\|= (\\w+|\\(.*\\))", "SETLEVELS($1, LEVELS($1) | ($2))"),
                    new Patch("reinterpret_cast<.*>\\((.*)\\)->gp \\^= (\\w+|\\(.*\\))", "SETLEVELS($1, LEVELS($1) ^ ($2))"),
                    // FastR does not support these global variables:
                    new Patch("R_Interactive\\s*=\\s*[01]", ""),
                    new Patch("R_isForkedChild\\s*=\\s*[01]", ""),
                    new Patch("Rf_KillAllDevices\\s*\\(\\s*\\)", ""),
                    // This patch was first introduced because compilation of testthat 3.0.1 failed
                    // on systems
                    // with newer glibc, as described in
                    // https://github.com/r-lib/testthat/issues/1373.
                    // In newer glibc, SIGSTKSZ is no longer a constexpr, so it cannot be used in
                    // C++ in static array declaration.
                    // So a workaround (as implemented in the aforementioned issue on GitHub), is to
                    // replace
                    // globally every occurence of SIGSTKSZ with a constant.
                    new Patch("(\\S*)\\[SIGSTKSZ\\]", "$1[32768]"),
    };

    @TruffleBoundary
    public static void patchPackage(RContext context, String path) {
        if (System.getenv(NO_PATCHING_ENV_VAR) != null) {
            return;
        }
        AtomicBoolean error = new AtomicBoolean(false);
        TruffleFile source = context.getSafeTruffleFile(path);
        log("Patching package %s", path);
        FilePatch fp = new FilePatch();
        List<String> messages = fp.messages;
        try {
            FileSystemUtils.walkFileTree(source, fp);
        } catch (IOException e) {
            error.set(true);
        }
        if (messages.size() > 0) {
            for (String msg : messages) {
                RError.warning(RError.NO_CALLER, Message.GENERIC, msg);
            }
        }
        if (error.get()) {
            RError.warning(RError.NO_CALLER, Message.GENERIC, "Error during C/C++ code patching for FastR. The package may still work.");
        }
        if (error.get() || messages.size() > 0) {
            RError.warning(RError.NO_CALLER, Message.GENERIC, "To turn off this patching run FastR with " + NO_PATCHING_ENV_VAR + " environment variable set.");
        }
    }

    private static final class FilePatch implements FileVisitor<TruffleFile> {
        private final AtomicBoolean error = new AtomicBoolean(false);
        private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

        private FilePatch() {
        }

        @Override
        public FileVisitResult visitFile(TruffleFile file, BasicFileAttributes attrs) throws IOException {
            if (!isSource(file) || !file.exists()) {
                return FileVisitResult.CONTINUE;
            }
            try {
                boolean matchFound = false;
                log("Checking file for patches %s", file);
                try (BufferedReader reader = file.newBufferedReader()) {
                    String line;
                    findMatch: while ((line = reader.readLine()) != null) {
                        for (Patch patche : patches) {
                            if (patche.find(line)) {
                                matchFound = true;
                                break findMatch;
                            }
                        }
                    }
                }
                if (!matchFound) {
                    log("No match found for patching in file %s", file);
                    return FileVisitResult.CONTINUE;
                }

                List<String> lines = Arrays.asList(new String(file.readAllBytes()).split("\n"));
                try (BufferedWriter writer = file.newBufferedWriter()) {
                    for (String l : lines) {
                        String line = l;    // stylecheck
                        for (Patch patch : patches) {
                            if (patch.find(line)) {
                                String newLine = patch.replace(line);
                                messages.add(String.format("FastR patched file '%s': line '%s' was replaced with '%s'.", file.getName(), line, newLine));
                                line = newLine;
                            }
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                error.set(true);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(TruffleFile f, IOException exc) throws IOException {
            throw exc;
        }

        @Override
        public FileVisitResult postVisitDirectory(TruffleFile f, IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(TruffleFile dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }

    private static void log(String fmt, Object... args) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, String.format(fmt, args));
        }
    }

    private static boolean isSource(TruffleFile path) {
        String sPath = path.toString().toLowerCase(Locale.ROOT);
        return sPath.endsWith(".c") || sPath.endsWith(".h") || sPath.endsWith(".cpp") || sPath.endsWith(".hpp") || sPath.endsWith(".cc");
    }

    private static final class Patch {
        private final Pattern pattern;
        private final String replace;

        Patch(String pattern, String replace) {
            this.pattern = Pattern.compile(pattern);
            this.replace = replace;
        }

        public boolean find(String line) {
            return pattern.matcher(line).find();
        }

        public String replace(String line) {
            return pattern.matcher(line).replaceAll(replace);
        }
    }
}
