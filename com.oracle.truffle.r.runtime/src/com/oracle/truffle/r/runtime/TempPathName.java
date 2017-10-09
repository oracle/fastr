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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

/**
 *
 * As per the GnuR spec, the tempdir() directory is identified on startup. It <b>must</b>be
 * initialized before the first RFFI call as the value is available in the R FFI.
 *
 */
public class TempPathName implements RContext.ContextState {
    private static final String RANDOM_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int RANDOM_CHARACTERS_LENGTH = RANDOM_CHARACTERS.length();
    private static final int RANDOM_LENGTH = 12; // as per GnuR
    private static final Random rand = new Random();
    private static final String DEPARSE_DIR_NAME = "deparse";

    @CompilationFinal private static String deparseDir = null;

    private String tempDirPath;

    @Override
    public RContext.ContextState initialize(RContext context) {
        if (context.getKind() == RContext.ContextKind.SHARE_PARENT_RW) {
            // share tempdir with parent
            tempDirPath = context.getParent().stateTempPath.tempDirPath;
            return this;
        }
        String startingTempDir = Utils.getUserTempDir();
        Path startingTempDirPath = FileSystems.getDefault().getPath(startingTempDir, "Rtmp");
        // ensure absolute, to avoid problems with R code does a setwd
        if (!startingTempDirPath.isAbsolute()) {
            startingTempDirPath = startingTempDirPath.toAbsolutePath();
        }
        String t = (String) BaseRFFI.MkdtempRootNode.create().getCallTarget().call(startingTempDirPath.toString() + "XXXXXX");
        if (t != null) {
            tempDirPath = t;
        } else {
            Utils.rSuicide("cannot create 'R_TempDir'");
        }

        // initialize deparse directory
        if (deparseDir == null && FastROptions.EmitTmpSource.getBooleanValue()) {
            deparseDir = determineDeparseDir();
        }

        return this;
    }

    @TruffleBoundary
    private String determineDeparseDir() {
        try {
            // primary location: $R_HOME/deparse
            String rHomeParent = ensureAccessibleDeparseDir(REnvVars.rHome());
            if (rHomeParent != null) {
                return rHomeParent;
            }

            // secondary location: /tmp/deparse
            String tmpDir = ensureAccessibleDeparseDir(Utils.getUserTempDir());
            if (tmpDir != null) {
                return tmpDir;
            }

            // tertiary location: RtmpXXXXXX/deparse
            String rtmpDir = ensureAccessibleDeparseDir(tempDirPath);
            if (rtmpDir != null) {
                return rtmpDir;
            }
        } catch (IOException e) {
            // Be very defensive and do not even report this exception.
        }

        // Cannot find writable location
        return null;
    }

    private static String ensureAccessibleDeparseDir(String parentDeparseDir) throws IOException {
        Path rHomePath = Paths.get(parentDeparseDir);
        if (Files.isDirectory(rHomePath) && Files.isWritable(rHomePath)) {
            Path resolvedDeparseDir = Files.createDirectories(rHomePath.resolve(DEPARSE_DIR_NAME));
            if (Files.isDirectory(resolvedDeparseDir) && Files.isWritable(resolvedDeparseDir)) {
                return resolvedDeparseDir.toAbsolutePath().toString();
            }
        }
        return null;
    }

    @Override
    @TruffleBoundary
    public void beforeDispose(RContext context) {
        if (context.getKind() == RContext.ContextKind.SHARE_PARENT_RW) {
            return;
        }
        try {
            Files.walkFileTree(Paths.get(tempDirPath), new DeleteVisitor());
        } catch (Throwable e) {
            // unexpected and we are exiting anyway
        }
    }

    public static String tempDirPath() {
        return RContext.getInstance().stateTempPath.tempDirPath;
    }

    public static TempPathName newContextState() {
        return new TempPathName();
    }

    public static String deparsePath() {
        return deparseDir;
    }

    @TruffleBoundary
    public static String createNonExistingFilePath(String pattern, String tempDir, String fileExt) {
        while (true) {
            StringBuilder sb = new StringBuilder(tempDir);
            sb.append(File.separatorChar);
            sb.append(pattern);
            appendRandomString(sb);
            if (fileExt.length() > 0) {
                sb.append(fileExt);
            }
            String path = sb.toString();
            if (!new File(path).exists()) {
                return path;
            }
        }
    }

    private static void appendRandomString(StringBuilder sb) {
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(RANDOM_CHARACTERS.charAt(rand.nextInt(RANDOM_CHARACTERS_LENGTH)));
        }
    }

    private static final class DeleteVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return del(file);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return del(dir);
        }

        private static FileVisitResult del(Path p) throws IOException {
            Files.delete(p);
            return FileVisitResult.CONTINUE;
        }
    }
}
