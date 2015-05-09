/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.instrument;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Support for locating the automatically generated source file for a package function when
 * deserializing a lazily loaded function. FastR generates its AST from this deparsed source and so
 * all instrumentation, including debugging, is tied to that source. This class manages the
 * directory tree where these files are stored. An index that is based on a SHA1 digest of the
 * source content is maintained allowing lookup when the function is loaded in subsequent
 * executions.
 *
 * The {@code INDEX} file is a series of lines of the form:
 *
 * <pre>
 * FNAME,FINGERPRINT,PKG,RPATH
 * </pre>
 *
 * where FNAME is the function name as it appears in the source code and RPATH is a relative
 * pathname to the generated source file from PKG. Internally we record the path in canonical form
 * for fast processing.
 */
public class RPackageSource {
    public static final String PKGSOURCE_PROJECT = "Rpkgsource";
    public static final String INDEX = "INDEX";
    private static final String DOT_PREFIX = "_dot_";
    private static final String SLASH_SWAP = "_slash_";
    private static final int FNAME = 0;
    private static final int FINGERPRINT = 1;
    private static final int PKG = 2;
    private static final int RPATH = 3;

    private static class FunctionInfo {
        /**
         * name of function in source code.
         */
        private final String sourceName;
        private final String path;
        private final String pkg;

        FunctionInfo(String sourceName, String pkg, String path) {
            this.sourceName = sourceName;
            this.pkg = pkg;
            this.path = path;
        }
    }

    /**
     * A map from source fingerprints to pathnames that are relative to the
     * {@value #PKGSOURCE_PROJECT} directory.
     */
    private static SortedMap<String, FunctionInfo> indexMap = new TreeMap<>();

    /**
     * A reverse map from pathnames to function names.
     */
    private static Map<String, String> pathToNameMap = new HashMap<>();

    public static void initialize() {
        Path indexPath = indexPath();
        try {
            List<String> lines = Files.readAllLines(indexPath);
            Path dirPath = dirPath();
            for (String line : lines) {
                String[] parts = line.split(",");
                String canonPath = dirPath.resolve(parts[PKG]).resolve(parts[RPATH]).toString();
                indexMap.put(parts[FINGERPRINT], new FunctionInfo(parts[FINGERPRINT], parts[PKG], canonPath));
                pathToNameMap.put(canonPath, parts[FNAME]);
            }
            RSerialize.setLocateSource(true);
        } catch (IOException ex) {
            // no index
        }
    }

    /**
     * Lookup the given source fingerprint in the index and return a canonical path for the
     * associated file or {@code null} if not found.
     */
    public static String lookup(String source) {
        String fingerprint = getFingerPrint(source.getBytes());
        FunctionInfo info = indexMap.get(fingerprint);
        if (info == null) {
            return null;
        } else {
            return info.path;
        }
    }

    public static void register(String fname, String pkg, Path sourcePath) {
        try {
            byte[] sourceData = Files.readAllBytes(sourcePath);
            String fingerprint = getFingerPrint(sourceData);
            FunctionInfo prev = indexMap.get(fingerprint);
            if (!((prev == null) || (prev.sourceName == fname && prev.path == sourcePath.toString()))) {
                /*
                 * This could arise in several ways. Most likely the same function is assigned to
                 * multiple names in the same package, or the same (likely trivial) body is assigned
                 * to several unrelated functions. These are annoying but essentially benign.
                 */
                RError.warning(RError.Message.GENERIC, "two package functions with same fingerprint, prev: '" + qualName(prev.pkg, prev.sourceName) + "', this '" + qualName(pkg, fname) + "'");
                return;
            }
            indexMap.put(fingerprint, new FunctionInfo(fname, pkg, dirPath().resolve(pkg).relativize(sourcePath).toString()));
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere(ex.getMessage());
        }
    }

    private static String getFingerPrint(byte[] sourceData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            return Utils.toHexString(digest.digest(sourceData));
        } catch (NoSuchAlgorithmException ex) {
            throw RInternalError.shouldNotReachHere(ex.getMessage());
        }

    }

    @SuppressWarnings("unused")
    public static void preLoad(String pkg, String fname) {
        RSerialize.setSaveDeparse(true);
        // make sure no previous file exists
        Path source = deparse(false);
        Path errorSource = deparse(true);
        try {
            Files.deleteIfExists(source);
            Files.deleteIfExists(errorSource);
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere("failed to delete existing DEPARSE");
        }
    }

    public static void postLoad(String pkg, String fname, Object val) {
        RSerialize.setSaveDeparse(false);
        if (val instanceof RFunction) {
            /*
             * RSerialize will have saved the deparsed output in DEPARSE, so we move it to the
             * correct location based on the "fname", and update the index. N.B. If the function had
             * been (lazily) loaded before this process began, the file will not exist. This is
             * quite likely for the default packages used on startup, which really need special
             * treatment. N.B. Also, if the function did not deparse correctly, it will be in a file
             * named DEPARSE_ERROR, which we ignore.
             */
            Path source = deparse(false);
            if (Files.exists(source)) {
                try {
                    Path target = targetPath(pkg, fname);
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                    RPackageSource.register(fname, pkg, target);
                } catch (IOException ex) {
                    throw RError.error(RError.Message.GENERIC, ex.getMessage());
                }
            } else {
                /*
                 * Either (a) no unserialize happened because it had already happened or (b) there
                 * was an error, the latter indicated by the existence of DEPARSE_ERROR
                 */
                String qualName = qualName(pkg, fname);
                if (Files.exists(deparse(true))) {
                    RError.warning(RError.Message.GENERIC, "the function '" + qualName + "' did not deparse successfully");
                } else {
                    RError.warning(RError.Message.GENERIC, "the function '" + qualName + "' has already been unserialized");
                }
            }
        }
    }

    private static String qualName(String pkg, String fname) {
        return pkg + "::" + fname;
    }

    public static String decodeName(String path) {
        String name = pathToNameMap.get(path);
        if (name == null) {
            throw RInternalError.shouldNotReachHere();
        }
        return name;
    }

    /**
     * Some function names do not map into useful or even legal filenames. This method takes care of
     * that.
     */
    private static String mungeName(String fname) {
        String result = fname;
        if (fname.charAt(0) == '.') {
            result = DOT_PREFIX + fname.substring(1);
        }
        result = result.replace("/", SLASH_SWAP);
        return result;
    }

    private static Path deparse(boolean isError) {
        return FileSystems.getDefault().getPath(REnvVars.rHome(), "DEPARSE" + (isError ? "_ERROR" : ""));
    }

    private static Path targetPath(String pkg, String fnameArg) throws IOException {
        Path targetDir = dirPath().resolve(pkg);
        Files.createDirectories(targetDir);
        String fname = mungeName(fnameArg);
        Path target = targetDir.resolve(fname + ".R");
        return target;
    }

    public static void saveMap() {
        try (BufferedWriter wr = new BufferedWriter(new FileWriter(indexPath().toFile()))) {
            for (Map.Entry<String, FunctionInfo> entry : indexMap.entrySet()) {
                wr.append(entry.getValue().sourceName);
                wr.append(',');
                wr.append(entry.getKey());
                wr.append(',');
                wr.append(entry.getValue().pkg);
                wr.append(',');
                wr.append(entry.getValue().path);
                wr.append('\n');
            }
        } catch (IOException ex) {
            Utils.fail("error writing package source index");
        }
    }

    private static Path dirPath() {
        return FileSystems.getDefault().getPath(REnvVars.rHome(), PKGSOURCE_PROJECT);
    }

    private static Path indexPath() {
        return dirPath().resolve(INDEX);
    }

}
