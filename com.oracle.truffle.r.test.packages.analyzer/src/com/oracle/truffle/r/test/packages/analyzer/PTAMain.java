/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.packages.analyzer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import com.oracle.truffle.r.test.packages.analyzer.detectors.DiffDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.InstallationProblemDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.RErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.RInternalErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.SegfaultDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.UnsupportedSpecializationDetector;
import com.oracle.truffle.r.test.packages.analyzer.dump.HtmlDumper;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParseException;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParser;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParser.LogFile;

/**
 * Main class of the package analysis tool.<br>
 * <p>
 * Expected directory structure: <code>
 * root<br>
 * -+-- packageName<br>
 * ----+- version<br>
 * ------+- testRunNumber<br>
 * --------+- packageName.log<br>
 * --------+- testfiles<br>
 * ----------+- diff<br>
 * ----------+- fastr<br>
 * ----------+- gnu<br>
 * </code>
 * </p>
 */
public class PTAMain {
    private static final Logger LOGGER = Logger.getLogger(PTAMain.class.getName());

    public static void main(String[] args) throws IOException {
        OptionsParser parser = new OptionsParser();
        String[] remainingArgs = parser.parseOptions(args);
        if (remainingArgs.length != 1) {
            System.err.println("Unknown arguments: " + Arrays.toString(remainingArgs));
            printHelp();
            System.exit(1);
        }

        Path outDir = Paths.get(parser.get("--outDir", "html"));
        ftw(Paths.get(remainingArgs[0]), outDir, parser.get("--glob", "*"));
    }

    private static final String LF = System.lineSeparator();

    private static void ftw(Path root, Path outDir, String glob) throws IOException {
        // TODO FS checking

        HtmlDumper htmlDumper = new HtmlDumper(outDir);

        // fail early
        try {
            if (!htmlDumper.createAndCheckOutDir()) {
                LOGGER.severe("Cannot write to output directory: " + outDir);
                System.exit(1);
            }
        } catch (IOException e) {
            LOGGER.severe(String.format("Cannot create output directory: %s ", e.getMessage()));
            System.exit(1);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, glob)) {
            Collection<RPackage> pkgs = new LinkedList<>();
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    Collection<RPackage> pkgVersions = visitPackageRoot(p);
                    pkgs.addAll(pkgVersions);
                }
            }
            Collection<Problem> allProblems = collectAllProblems(pkgs);
            htmlDumper.dump(allProblems);
        }
    }

    private static Collection<Problem> collectAllProblems(Collection<RPackage> pkgs) {
        Collection<Problem> problems = new LinkedList<>();
        for (RPackage pkg : pkgs) {
            for (RPackageTestRun run : pkg.getTestRuns()) {
                problems.addAll(run.getProblems());
            }
        }
        return problems;
    }

    private static Collection<RPackage> visitPackageRoot(Path pkgRoot) throws IOException {
        String pkgName = pkgRoot.getFileName().toString();

        Collection<RPackage> pkgs = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgRoot)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    pkgs.add(visitPackageVersion(p, pkgName));
                }
            }
        }
        return pkgs;
    }

    private static RPackage visitPackageVersion(Path pkgVersionDir, String pkgName) {
        String pkgVersion = pkgVersionDir.getFileName().toString();
        RPackage pkg = new RPackage(pkgName, pkgVersion);
        LOGGER.info("Found package " + pkg);

        Collection<RPackageTestRun> runs = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgVersionDir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    RPackageTestRun testRun = visitTestRun(p, pkg);
                    if (testRun != null) {
                        runs.add(testRun);
                    }
                }
            }
            pkg.setTestRuns(runs);
        } catch (IOException e) {
            LOGGER.severe("Error while reading package root of \"" + pkgName + "\"");
        }

        return pkg;
    }

    private static RPackageTestRun visitTestRun(Path testRunDir, RPackage pkg) {
        int testRun = Integer.parseInt(testRunDir.getFileName().toString());
        LOGGER.info("Visiting test run " + testRun + " of package " + pkg);
        try {
            RPackageTestRun pkgTestRun = new RPackageTestRun(pkg, testRun);
            Path logFile = testRunDir.resolve(pkg.getName() + ".log");
            Collection<Problem> problems = parseLogFile(logFile, pkgTestRun);
            pkgTestRun.setProblems(problems);
            return pkgTestRun;
        } catch (IOException | LogFileParseException e) {
            LOGGER.severe(String.format("Error while parsing test run %d of package \"%s-%s\": %s", testRun,
                            pkg.getName(), pkg.getVersion(), e.getMessage()));
        }
        return null;
    }

    private static Collection<Problem> parseLogFile(Path logFile, RPackageTestRun pkgTestRun) throws IOException {
        LOGGER.info("Parsing log file " + logFile);

        LogFileParser lfParser = new LogFileParser(logFile, pkgTestRun);
        lfParser.addDetector(LogFileParser.Token.BEGIN_SUGGESTS_INSTALL, InstallationProblemDetector.INSTANCE);
        lfParser.addDetector(SegfaultDetector.INSTANCE);
        lfParser.addDetector(RErrorDetector.INSTANCE);
        lfParser.addDetector(UnsupportedSpecializationDetector.INSTANCE);
        lfParser.addDetector(RInternalErrorDetector.INSTANCE);
        lfParser.addTestResultDetector(DiffDetector.INSTANCE);

        LogFile parseLogFile = lfParser.parseLogFile();
        Collection<Problem> problems = parseLogFile.collectProblems();
        for (Problem problem : problems) {
            LOGGER.info(problem.toString());
        }

        return problems;
    }

    private static void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("USAGE: ").append(PTAMain.class.getSimpleName()).append(" [OPTIONS] ROOT").append(LF);
        sb.append(LF);
        sb.append("OPTIONS:").append(LF);
        sb.append(
                        "    --since MM-dd-YYYY\tOnly consider package tests since the provided date (default: no restriction).").append(LF);
        sb.append("    --glob GLOB\t\tGlob-style directory filter for packages to consider (default: \"*\").").append(LF);
        sb.append("    --outDir PATH\tPath to directory for HTML output (default: \"html\").").append(LF);
        System.out.println(sb.toString());
    }

    private static class OptionsParser {

        private Map<String, String> options = new HashMap<>();

        public String[] parseOptions(String[] args) {
            int i = 0;
            while (i < args.length) {
                String key = args[i];
                if (key.startsWith("--since") || key.startsWith("--glob") || key.startsWith("--outDir")) {
                    String value = getOptionArg(args, i);
                    ++i;
                    options.put(key, value);
                } else {
                    break;
                }
                ++i;
            }
            return Arrays.copyOfRange(args, i, args.length);
        }

        private static String getOptionArg(String[] args, int keyIndex) {
            if (keyIndex + 1 < args.length) {
                return args[keyIndex + 1];
            }
            throw new RuntimeException("Missing value for option: " + args[keyIndex]);
        }

        public boolean has(String key) {
            return options.containsKey(key);
        }

        public String get(String key, String defaultValue) {
            if (has(key)) {
                return options.get(key);
            }
            return defaultValue;
        }
    }
}
