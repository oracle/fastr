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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.oracle.truffle.r.test.packages.analyzer.detectors.DiffDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.InstallationProblemDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.RErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.RInternalErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.SegfaultDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.SymbolLookupErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.UnsupportedSpecializationDetector;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParseException;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParser;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParser.LogFile;

public class FileTreeWalker {
    private static final Logger LOGGER = Logger.getLogger(FileTreeWalker.class.getName());

    private Collection<LogFileParseException> parseErrors;

    /** List of test run directories that were candidates for analysis. */
    private Collection<Path> consideredTestRuns;

    public Collection<RPackage> ftw(Path root, Date sinceDate, String glob) throws IOException {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, glob)) {
            reset();
            Collection<RPackage> pkgs = new LinkedList<>();
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    Collection<RPackage> pkgVersions = visitPackageRoot(p, sinceDate);
                    pkgs.addAll(pkgVersions);
                }
            }
            LOGGER.info("Total number of analysis candidates: " + consideredTestRuns.size());
            return pkgs;
        }
    }

    private void reset() {
        parseErrors = new ArrayList<>();
        consideredTestRuns = new ArrayList<>();
    }

    protected Collection<RPackage> visitPackageRoot(Path pkgRoot, Date sinceDate) throws IOException {
        String pkgName = pkgRoot.getFileName().toString();

        Collection<RPackage> pkgs = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgRoot)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    pkgs.add(visitPackageVersion(p, pkgName, sinceDate));
                }
            }
        }
        return pkgs;
    }

    protected RPackage visitPackageVersion(Path pkgVersionDir, String pkgName, Date sinceDate) {
        String pkgVersion = pkgVersionDir.getFileName().toString();
        RPackage pkg = new RPackage(pkgName, pkgVersion);
        LOGGER.info("Found package " + pkg);

        Collection<RPackageTestRun> runs = new LinkedList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgVersionDir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    RPackageTestRun testRun = visitTestRun(p, pkg, sinceDate);
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

    protected RPackageTestRun visitTestRun(Path testRunDir, RPackage pkg, Date sinceDate) {
        int testRun = Integer.parseInt(testRunDir.getFileName().toString());
        LOGGER.info("Visiting test run " + testRun + " of package " + pkg);
        try {
            RPackageTestRun pkgTestRun = new RPackageTestRun(pkg, testRun);
            Path logFile = testRunDir.resolve(pkg.getName() + ".log");
            FileTime lastModifiedTime = Files.getLastModifiedTime(logFile);
            if (isNewerThan(lastModifiedTime, sinceDate)) {
                Collection<Problem> problems = parseLogFile(logFile, pkgTestRun);
                consideredTestRuns.add(testRunDir);
                pkgTestRun.setProblems(problems);
                return pkgTestRun;
            } else {
                LOGGER.info(String.format("Skipping package test run %s because it is too old (%s must be newer than %s)", pkgTestRun, lastModifiedTime, sinceDate));
            }
        } catch (IOException e) {
            LOGGER.severe(String.format("Error while parsing test run %d of package \"%s-%s\": %s", testRun,
                            pkg.getName(), pkg.getVersion(), e.getMessage()));
        } catch (LogFileParseException e) {
            LOGGER.severe(String.format("Error while parsing test run %d of package \"%s-%s\": %s", testRun,
                            pkg.getName(), pkg.getVersion(), e.getMessage()));
            parseErrors.add(e);
        }
        return null;
    }

    private static boolean isNewerThan(FileTime lastModifiedTime, Date sinceDate) {
        Date lastModDate = new Date(lastModifiedTime.toMillis());
        return sinceDate.compareTo(lastModDate) <= 0;
    }

    private static Collection<Problem> parseLogFile(Path logFile, RPackageTestRun pkgTestRun) throws IOException {
        LOGGER.info("Parsing log file " + logFile);

        LogFileParser lfParser = new LogFileParser(logFile, pkgTestRun);
        lfParser.addDetector(InstallationProblemDetector.INSTANCE);
        lfParser.addDetector(SegfaultDetector.INSTANCE);
        lfParser.addDetector(RErrorDetector.INSTANCE);
        lfParser.addDetector(UnsupportedSpecializationDetector.INSTANCE);
        lfParser.addDetector(RInternalErrorDetector.INSTANCE);
        lfParser.addTestResultDetector(DiffDetector.INSTANCE);
        lfParser.addDetector(SymbolLookupErrorDetector.INSTANCE);

        LogFile parseLogFile = lfParser.parseLogFile();
        Collection<Problem> problems = parseLogFile.collectProblems();
        pkgTestRun.setSuccess(parseLogFile.isSuccess());

        // log problems
        LOGGER.fine("Overall test result: " + (pkgTestRun.isSuccess() ? "OK" : "FAILED"));
        for (Problem problem : problems) {
            LOGGER.fine(problem.toString());
        }

        return problems;
    }

    public Collection<LogFileParseException> getParseErrors() {
        return parseErrors;
    }

    public Collection<Path> getConsideredTestRuns() {
        return consideredTestRuns;
    }

}
