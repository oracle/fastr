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
 * FNAME,FINGERPRINT,RPATH
 * </pre>
 *
 * where FNAME is the function name as it appears in the source code and RPATH is a relative
 * pathname to the generated source file. Internally we record the path in canonical form for fast
 * processing.
 */
public class PackageSource {
    public static final String PKGSOURCE_PROJECT = "Rpkgsource";
    public static final String INDEX = "INDEX";
    private static final String DOT_PREFIX = "_dot_";

    private static class FunctionInfo {
        private final String path;
        /**
         * name of function in source code.
         */
        private final String sourceName;

        FunctionInfo(String sourceName, String path) {
            this.sourceName = sourceName;
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
                String canonPath = dirPath.resolve(parts[2]).toString();
                indexMap.put(parts[1], new FunctionInfo(parts[0], canonPath));
                pathToNameMap.put(canonPath, parts[0]);
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

    public static void register(Path sourcePath, String fname) {
        try {
            byte[] sourceData = Files.readAllBytes(sourcePath);
            String fingerprint = getFingerPrint(sourceData);
            FunctionInfo prev = indexMap.get(fingerprint);
            if (!((prev == null) || (prev.sourceName == fname && prev.path == sourcePath.toString()))) {
                throw RInternalError.shouldNotReachHere("two package functions with same fingerprint");
            }
            indexMap.put(fingerprint, new FunctionInfo(fname, dirPath().relativize(sourcePath).toString()));
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

    public static void postLoad(String pkg, String fname, Object val) {
        if (val instanceof RFunction) {
            /*
             * RSerialize will have saved the deparsed output in DEPARSE, so we move it to the
             * correct location based on the "fname", and update the index.
             */
            Path source = FileSystems.getDefault().getPath(REnvVars.rHome(), "DEPARSE");
            try {
                Path target = targetPath(pkg, fname);
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                PackageSource.register(target, fname);
            } catch (IOException ex) {
                throw RError.error(RError.Message.GENERIC, ex.getMessage());
            }
        }
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
        if (fname.charAt(0) == '.') {
            return DOT_PREFIX + fname.substring(1);
        } else {
            return fname;
        }
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
