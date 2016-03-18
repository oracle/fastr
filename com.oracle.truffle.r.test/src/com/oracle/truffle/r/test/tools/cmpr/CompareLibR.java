/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tools.cmpr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.Utils;

/**
 * Compare the FastR versions of .R files in the standard packages against GnuR. Removes all
 * formatting to perform the check, replacing all whitespace (including newlines) with exactly one
 * space.
 * <p>
 * Usage:
 *
 * <pre>
 * --gnurhome path --package pkg | --files path1 path2
 * </pre>
 *
 * {@code gnurhome} is the path to the GnuR distribution. {@cpde package} gibes the package to
 * compare,e.g. {@code base}. The second form just compares the two files.
 */
public class CompareLibR {

    private static class FileContent {
        Path path;
        String content;
        String flattened;

        FileContent(Path path, String content) {
            this.path = path;
            this.content = content;
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        // Checkstyle: stop system print check
        String gnurHome = null;
        String pkg = null;
        String path1 = null;
        String path2 = null;
        boolean printPaths = false;
        String diffApp = "diff";
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            switch (arg) {
                case "--gnurhome":
                    i++;
                    gnurHome = args[i];
                    break;
                case "--package":
                    i++;
                    pkg = args[i];
                    break;
                case "--files":
                    if (args.length == 3) {
                        i++;
                        path1 = args[i];
                        i++;
                        path2 = args[i];
                    } else {
                        usage();
                    }
                    break;
                case "--paths":
                    printPaths = true;
                    break;

                case "--diffapp":
                    i++;
                    diffApp = args[i];
                    break;
                default:
                    usage();
            }
            i++;
        }

        if (gnurHome == null && path1 == null) {
            usage();
        }

        if (path1 != null) {
            compareFiles(path1, path2);
        } else {

            Map<String, FileContent> fastRFiles = getFastR(pkg);
            Map<String, FileContent> gnuRFiles = getGnuR(gnurHome, pkg, fastRFiles);
            deformat(gnuRFiles);
            deformat(fastRFiles);
            for (Map.Entry<String, FileContent> entry : fastRFiles.entrySet()) {
                FileContent fastR = entry.getValue();
                String fileName = entry.getKey();
                FileContent gnuR = gnuRFiles.get(fileName);
                if (gnuR == null) {
                    System.out.println("FastR has file: " + fileName + " not found in GnuR");
                } else {
                    if (!fastR.flattened.equals(gnuR.flattened)) {
                        if (printPaths) {
                            System.out.printf("%s %s %s%n", diffApp, gnuR.toString(), replaceBin(fastR.toString()));
                        } else {
                            System.out.println(fileName + " differs");
                        }
                    } else {
                        System.out.println(fileName + " is identical (modulo formatting)");
                    }
                }
            }
        }
    }

    private static String deformat(String s) {
        return s.replaceAll("\\s+", " ");
    }

    private static void deformat(Map<String, FileContent> map) {
        for (Map.Entry<String, FileContent> entry : map.entrySet()) {
            FileContent fc = entry.getValue();
            fc.flattened = deformat(fc.content);
        }
    }

    private static Map<String, FileContent> getGnuR(String gnurHome, String lib, Map<String, FileContent> filter) throws IOException {
        FileSystem fs = FileSystems.getDefault();
        Path baseR = fs.getPath(lib, "R");
        Path library = fs.getPath(gnurHome, "src", "library");
        baseR = library.resolve(baseR);
        Map<String, FileContent> result = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseR)) {
            for (Path entry : stream) {
                String entryName = entry.getFileName().toString();
                if (entryName.endsWith(".R") && (filter.get(entryName) != null)) {
                    File file = entry.toFile();
                    result.put(entryName, new FileContent(entry, readFileContent(file)));
                }
            }
        }
        return result;
    }

    private static String toFirstUpper(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private static Map<String, FileContent> getFastR(String lib) throws Exception {
        Class<?> klass = Class.forName("com.oracle.truffle.r.nodes.builtin." + lib + "." + toFirstUpper(lib) + "Package");
        InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(klass, "R");
        Map<String, FileContent> result = new HashMap<>();
        if (is == null) {
            return result;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.endsWith(".r") || line.endsWith(".R")) {
                    String fileName = line.trim();
                    final String rResource = "R/" + fileName;
                    URL url = klass.getResource(rResource);
                    String content = Utils.getResourceAsString(klass, rResource, true);
                    result.put(fileName, new FileContent(FileSystems.getDefault().getPath(url.getPath()), content));
                }
            }
        }
        return result;
    }

    private static String replaceBin(String s) {
        return s.replace("/bin/", "/src/");
    }

    private static String readFileContent(File file) throws IOException {
        byte[] buf = new byte[(int) file.length()];
        try (BufferedInputStream bs = new BufferedInputStream(new FileInputStream(file))) {
            bs.read(buf);
        }
        return new String(buf);
    }

    private static void writeFile(File file, String s) throws IOException {
        try (BufferedOutputStream bs = new BufferedOutputStream(new FileOutputStream(file))) {
            bs.write(s.getBytes());
        }
    }

    private static void compareFiles(String path1, String path2) throws IOException {
        String c1 = deformat(readFileContent(new File(path1)));
        String c2 = deformat(readFileContent(new File(path2)));
        if (c1.equals(c2)) {
            System.out.println("files are identical (modulo formatting)");
        } else {
            System.out.println("files differ");
            writeFile(new File(path1 + ".deformat"), c1);
            writeFile(new File(path2 + ".deformat"), c2);
        }
    }

    private static void usage() {
        // Checkstyle: stop system print check
        System.err.println("usage: --gnurhome path --package pkg | --files path1 path2");
        System.exit(1);
    }
}
