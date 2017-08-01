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
package com.oracle.truffle.r.test.packages.analyzer.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.detectors.Detector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.DummyDetector;
import com.oracle.truffle.r.test.packages.analyzer.detectors.LineDetector;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;
import com.oracle.truffle.r.test.packages.analyzer.parser.DiffParser.DiffChunk;

public class LogFileParser {

    private static final Logger LOGGER = Logger.getLogger(LogFileParser.class.getName());

    private RPackageTestRun pkg;
    private BufferedReader reader;
    private Line curLine;
    private Line la;
    private final Map<String, Collection<LineDetector>> detectorsTable = new HashMap<>();
    private final Collection<LineDetector> anyDetectors = new ArrayList<>();
    private final Collection<Detector<List<DiffChunk>>> testResultDetectors = new ArrayList<>();
    private LogFile logFile;

    private int lineNr = 0;

    public LogFileParser(Path logFile, RPackageTestRun pkgTestRun) {
        this.pkg = pkgTestRun;
        this.logFile = new LogFile(logFile);
    }

    public void addDetector(Token hook, LineDetector detector) {
        Objects.requireNonNull(detector);
        Collection<LineDetector> registered = this.detectorsTable.get(hook);
        if (registered == null) {
            registered = new LinkedList<>();
            this.detectorsTable.put(hook.name(), registered);
        }
        registered.add(detector);
    }

    public void addDetector(LineDetector detector) {
        anyDetectors.add(Objects.requireNonNull(detector));
    }

    public void addTestResultDetector(Detector<List<DiffChunk>> detector) {
        testResultDetectors.add(Objects.requireNonNull(detector));
    }

    public LogFile parseLogFile() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(logFile.path)) {
            this.reader = r;
            consumeLine();
            Section installTest = parseInstallTest();
            logFile.addSection(installTest);
            if (!installTest.isSuccess()) {
                return logFile;
            }
            logFile.addSection(parseInstallTest());
            logFile.addSection(parseCheckResults());
            logFile.setSuccess(parseOverallStatus());
            expectEOF();
        } finally {
            this.reader = null;
        }
        return logFile;
    }

    private boolean parseOverallStatus() throws IOException {
        expect(Token.TEST_STATUS);
        consumeLine();
        return parseStatus(trim(curLine.text).substring((getPkgName() + ": ").length())).toBoolean();
    }

    private static boolean isEOF(Line l) {
        return l.text == null;
    }

    void expectEOF() throws IOException {
        consumeLine();
        if (!isEOF(curLine)) {
            throw new LogFileParseException("Expected end of file but was " + curLine.text, pkg, getCurrentLocation());
        }
    }

    private Section parseCheckResults() throws IOException {
        expect(Token.BEGIN_CHECKING);

        Section checkResults = new Section(logFile, Token.BEGIN_CHECKING.linePrefix, curLine.lineNr);
        checkResults.problems = new LinkedList<>();

        // TODO depending on the result, parse other files
        for (;;) {
            if (la.text.contains(Token.FAIL_OUTPUT_GNUR.linePrefix)) {
                consumeLine();
                // TODO
            } else if (la.text.contains(Token.FAIL_OUTPUT_FASTR.linePrefix)) {
                consumeLine();
                // TODO
            } else if (la.text.contains(Token.MISSING_OUTPUT_FILE.linePrefix)) {
                consumeLine();
                // TODO
            } else if (la.text.contains(Token.CONTENT_MALFORMED.linePrefix)) {
                consumeLine();
                int idx = curLine.text.indexOf(Token.CONTENT_MALFORMED.linePrefix);
                String fileNameStr = curLine.text.substring(idx + Token.CONTENT_MALFORMED.linePrefix.length()).trim();
                checkResults.problems.add(new ContentMalformedProblem(pkg, DummyDetector.INSTANCE, getCurrentLocation(), fileNameStr));
            } else if (la.text.contains(Token.OUTPUT_MISMATCH_FASTR.linePrefix)) {
                consumeLine();
                // extract file name of output file
                // format: <pkg name>: FastR output mismatch: <out file name>
                int idx = curLine.text.indexOf(Token.OUTPUT_MISMATCH_FASTR.linePrefix);
                String fileNameStr = curLine.text.substring(idx + Token.OUTPUT_MISMATCH_FASTR.linePrefix.length()).trim();
                Path outputFile = logFile.path.resolveSibling(Paths.get("testfiles", "fastr", fileNameStr));

                // report the problem
                checkResults.problems.add(new OutputMismatchProblem(pkg, DummyDetector.INSTANCE, getCurrentLocation(), fileNameStr));

                if (Files.isReadable(outputFile)) {
                    checkResults.problems.addAll(applyDetectors(Token.OUTPUT_MISMATCH_FASTR, outputFile, 0, Files.readAllLines(outputFile)));
                } else {
                    // try to find the file anywhere in the test run directory (there were some
                    // cases)
                    Optional<Path> findFirst = null;
                    try {
                        findFirst = Files.find(logFile.path.getParent(), 3, (path, attr) -> path.getFileName().equals(outputFile.getFileName())).findFirst();
                        if (findFirst.isPresent()) {
                            checkResults.problems.addAll(applyDetectors(Token.OUTPUT_MISMATCH_FASTR, findFirst.get(), 0, Files.readAllLines(findFirst.get())));
                        }
                    } catch (NoSuchFileException e) {
                    }
                    if (findFirst == null || !findFirst.isPresent()) {
                        LOGGER.warning("Cannot read output file " + outputFile);

                        // consume any lines to be able to continue
                        collectBody(Token.END_CHECKING);
                    }
                }

                checkResults.setSuccess(false);
            } else {
                break;
            }
        }
        expect(Token.END_CHECKING);

        return checkResults;
    }

    Location getCurrentLocation() {
        return new Location(logFile.path, curLine.lineNr);
    }

    private Section parseInstallTest() throws IOException {
        expect(Token.BEGIN_INSTALL_TEST);
        Section installTest = new Section(logFile, Token.BEGIN_INSTALL_TEST.linePrefix, curLine.lineNr);
        boolean success = true;

        String mode = trim(curLine.text).substring(Token.BEGIN_INSTALL_TEST.linePrefix.length());
        if (!("FastR".equals(mode) || "GnuR".equals(mode))) {
            throw parseError("Invalid mode: " + mode);
        }
        installTest.setMode(mode);

        Section installationTask = parseInstallationTask();
        installTest.addSection(installationTask);
        if ("FastR".equals(mode)) {
            installTest.setSuccess(parseInstallStatus().toBoolean() && success);

        }
        installTest.addSection(parseInstallSuggests());
        Section testing = parseTesting();
        installTest.addSection(testing);
        installTest.setSuccess(testing.isSuccess() && success);
        expect(Token.END_INSTALL_TEST);

        return installTest;
    }

    private Section parseTesting() throws IOException {
        expect(Token.BEGIN_PACKAGE_TESTS);

        Section packageTests = new Section(logFile, Token.BEGIN_PACKAGE_TESTS.linePrefix, curLine.lineNr);

        if (("install failed, not testing: " + getPkgName()).equals(trim(la.text))) {
            consumeLine();
            packageTests.setSuccess(false);
        } else {
            while (laMatches(Token.BEGIN_TESTING)) {
                packageTests.addSection(parsePackageTest());
            }
            packageTests.setSuccess(true);
        }
        expect(Token.END_PACKAGE_TESTS);
        return packageTests;
    }

    private Section parsePackageTest() throws IOException {
        expect(Token.BEGIN_TESTING);
        Section testing = new Section(logFile, Token.BEGIN_TESTING.linePrefix, curLine.lineNr);
        testing.problems = new LinkedList<>();

        // EXAMPLES
        if (laMatches(Token.TESTING_EXAMPLES)) {
            consumeLine();
            testing.problems.addAll(applyDetectors(Token.TESTING_EXAMPLES, logFile.path, collectBody(Token.RUNNING_SPECIFIC_TESTS, Token.RUNNING_VIGNETTES, Token.END_TESTING)));
        }

        // SPECIFIC TESTS
        if (laMatches(Token.RUNNING_SPECIFIC_TESTS)) {
            consumeLine();

            // e.g.: Running ‘runRUnitTests.R’
            while (laMatches("Running ")) {
                consumeLine();
                // e.g.: comparing ‘test.read.xls.Rout’ to ‘test.read.xls.Rout.save’
                if (laMatches("comparing ")) {
                    consumeLine();

                    // e.g.: files differ in number of lines:
                    if (laMatches("files differ in number of lines:")) {
                        consumeLine();
                        List<DiffChunk> diffResult = new DiffParser(this).parseDiff();
                        testing.problems.addAll(applyTestResultDetectors(diffResult));
                        diffResult.stream().forEach(chunk -> {
                            if (!chunk.getLeft().isEmpty()) {
                                testing.problems.addAll(applyDetectors(Token.RUNNING_SPECIFIC_TESTS, chunk.getLeftFile(), chunk.getLeftStartLine(), chunk.getLeft()));
                            }
                            if (!chunk.getRight().isEmpty()) {
                                testing.problems.addAll(applyDetectors(Token.RUNNING_SPECIFIC_TESTS, chunk.getRightFile(), chunk.getRightStartLine(), chunk.getRight()));
                            }
                        });
                        consumeLine();
                        parseStatus(trim(curLine.text));
                    } else {
                        int dotsIdx = curLine.text.lastIndexOf("...");
                        parseStatus(trim(curLine.text.substring(dotsIdx + "...".length())));
                    }
                }
            }
        }

        // VIGNETTES
        if (laMatches(Token.RUNNING_VIGNETTES)) {
            // e.g. Running vignettes for package ‘gdata’
            consumeLine();

            // e.g.: Running ‘mapLevels.Rnw’
            if (laMatches("Running ")) {
                consumeLine();
            }
            // TODO anything more to parse ?
            testing.problems.addAll(applyDetectors(Token.RUNNING_VIGNETTES, logFile.path, collectBody(Token.END_TESTING)));
        }

        // consume any remaining lines to the end of this section
        testing.problems.addAll(applyDetectors(Token.BEGIN_TESTING, logFile.path, collectBody(Token.END_TESTING)));
        expect(Token.END_TESTING);
        return testing;
    }

    private Section parseInstallationTask() throws IOException {
        expect(Token.BEGIN_INSTALLATION);
        Section installation = new Section(logFile, Token.BEGIN_INSTALLATION.linePrefix, curLine.lineNr);
        Section processing = parseProcessingTask();
        installation.addSection(processing);
        expect(Token.END_INSTALLATION);
        return installation;
    }

    private Section parseProcessingTask() throws IOException {
        expect(Token.BEGIN_PROCESSING);
        Section installation = new Section(logFile, Token.BEGIN_PROCESSING.linePrefix, curLine.lineNr);
        installation.problems = applyDetectors(Token.BEGIN_PROCESSING, logFile.path, collectBody(Token.END_PROCESSING));
        expect(Token.END_PROCESSING);
        return installation;
    }

    private TestResult parseInstallStatus() throws IOException {
        expect(Token.BEGIN_INSTALL_STATUS);
        // pkgName: FAILED / OK
        expect(getPkgName());
        TestResult res = parseStatus(trim(curLine.text).substring((getPkgName() + ": ").length()));
        expect(Token.END_INSTALL_STATUS);
        return res;
    }

    private String getPkgName() {
        return pkg.getPackage().getName();
    }

    private TestResult parseStatus(String substring) {
        if (Token.OK.linePrefix.equals(substring.trim())) {
            return TestResult.OK;
        } else if (Token.FAILED.linePrefix.equals(substring.trim())) {
            return TestResult.FAILED;
        } else if (Token.INDETERMINATE.linePrefix.equals(substring.trim())) {
            return TestResult.INDETERMINATE;
        }
        throw parseError("Unexpected status: " + substring);
    }

    private Section parseInstallSuggests() throws IOException {
        expect(Token.BEGIN_SUGGESTS_INSTALL);
        Section section = new Section(logFile, Token.BEGIN_SUGGESTS_INSTALL.linePrefix, curLine.lineNr);

        // collect body of this section

        Collection<Problem> problems = applyDetectors(Token.BEGIN_SUGGESTS_INSTALL, logFile.path, collectBody(Token.END_SUGGESTS_INSTALL));
        section.problems = problems;

        expect(Token.END_SUGGESTS_INSTALL);

        return section;
    }

    private Collection<Problem> applyDetectors(Token start, Path file, List<Line> body) {
        if (!body.isEmpty()) {
            Line firstLine = body.get(0);
            List<String> strBody = body.stream().map(l -> l.text).collect(Collectors.toList());
            return applyDetectors(start, file, firstLine.lineNr, strBody);
        }
        return new LinkedList<>();
    }

    private Collection<Problem> applyDetectors(Token start, Path file, int startLineNr, List<String> body) {
        assert Files.isRegularFile(file);
        Location startLocation = null;
        if (!body.isEmpty()) {
            assert startLineNr >= 0;
            startLocation = new Location(file, startLineNr);
        }
// List<Problem> problems = new LinkedList<>();
        Map<LineDetector, Collection<Problem>> problems = new HashMap<>();
        Collection<LineDetector> collection = detectorsTable.get(start.name());
        if (collection != null) {
            for (LineDetector detector : collection) {
                Collection<Problem> detectedProblems = detector.detect(pkg, startLocation, body);
                if (detectedProblems != null) {
                    problems.put(detector, detectedProblems);
                }
            }
        }
        for (LineDetector detector : anyDetectors) {
            Collection<Problem> detectedProblems = detector.detect(pkg, startLocation, body);
            if (detectedProblems != null) {
                problems.put(detector, detectedProblems);
            }
        }

        return problems.values().stream().flatMap(p -> p.stream()).collect(Collectors.toList());
    }

    private Collection<Problem> applyTestResultDetectors(List<DiffChunk> diffChunk) {
        return testResultDetectors.stream().map(detector -> detector.detect(pkg, null, diffChunk)).flatMap(l -> l.stream()).collect(Collectors.toList());
    }

    /**
     * Collects all lines until the provided token (excluding the ending line).
     */
    private List<Line> collectBody(Token endSuggestsInstall) throws IOException {
        List<Line> lines = new ArrayList<>();
        while (!laMatches(endSuggestsInstall) && !isEOF(la)) {
            consumeLine();
            assert curLine.text != null;
            lines.add(curLine);
        }
        return lines;
    }

    private List<Line> collectBody(Token... endSuggestsInstall) throws IOException {
        List<Line> lines = new ArrayList<>();
        while (!startsWithOneOf(endSuggestsInstall) && !isEOF(la)) {
            consumeLine();
            lines.add(curLine);
        }
        return lines;
    }

    private boolean startsWithOneOf(Token[] endSuggestsInstall) {
        for (Token t : endSuggestsInstall) {
            if (laMatches(t)) {
                return true;
            }
        }
        return false;
    }

    boolean laMatches(Token expected) {
        return laMatches(expected.linePrefix);
    }

    boolean laMatches(String prefix) {
        return la.text != null && trim(la.text).startsWith(prefix);
    }

    void expect(Token expected) throws IOException {
        expect(expected.linePrefix);
    }

    void expect(String linePrefix) throws IOException {
        consumeLine();
        if (isEOF(curLine) || !trim(curLine.text).startsWith(linePrefix)) {
            throw parseError("Unexpected line " + (curLine.lineNr + 1) + " (expected \"" + linePrefix + "\" but was \"" + (isEOF(curLine) ? "<EOF>" : curLine.text) + "\"");
        }
    }

    LogFileParseException parseError(String message) {
        throw new LogFileParseException(message, pkg, getCurrentLocation());
    }

    void consumeLine() throws IOException {
        curLine = la;
        // skip empty lines
        do {
            la = new Line(lineNr++, reader.readLine());
        } while (la != null && la.text != null && la.isEmpty());
    }

    LogFile getLogFile() {
        return logFile;
    }

    Line getCurLine() {
        return curLine;
    }

    Line getLookahead() {
        return la;
    }

    static String trim(String l) {
        return l != null ? l.trim() : null;
    }

    static class Line {
        final int lineNr;
        final String text;

        protected Line(int lineNr, String text) {
            this.lineNr = lineNr;
            this.text = text;
        }

        public boolean isEmpty() {
            return text != null && text.isEmpty();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + lineNr;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Line other = (Line) obj;
            if (lineNr != other.lineNr) {
                return false;
            }
            if (text == null) {
                if (other.text != null) {
                    return false;
                }
            } else if (!text.equals(other.text)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("%d: %s", lineNr, text);
        }
    }

    public enum TestResult {
        OK,
        FAILED,
        INDETERMINATE;

        boolean toBoolean() {
            return OK.equals(this);
        }
    }

    public enum Token {

        BEGIN_INSTALL_TEST("BEGIN install/test with "),
        END_INSTALL_TEST("END install/test with "),
        BEGIN_CHECKING("BEGIN checking "),
        END_CHECKING("END checking "),
        TEST_STATUS("Test Status"),
        BEGIN_INSTALLATION("BEGIN package installation"),
        END_INSTALLATION("END package installation"),
        BEGIN_PROCESSING("BEGIN processing: "),
        END_PROCESSING("END processing: "),
        BEGIN_INSTALL_STATUS("BEGIN install status"),
        END_INSTALL_STATUS("END install status"),
        BEGIN_SUGGESTS_INSTALL("BEGIN suggests install"),
        END_SUGGESTS_INSTALL("END suggests install"),
        BEGIN_PACKAGE_TESTS("BEGIN package tests"),
        END_PACKAGE_TESTS("END package tests"),
        INSTALL_FAILED("install failed, not testing: "),
        BEGIN_TESTING("BEGIN testing: "),
        END_TESTING("END testing: "),
        TEST_TIME("TEST_TIME: "),
        FAIL_OUTPUT_GNUR("GnuR test had .fail outputs"),
        FAIL_OUTPUT_FASTR("FastR test had .fail outputs"),
        MISSING_OUTPUT_FILE("FastR is missing output file:"),
        CONTENT_MALFORMED("content malformed: "),
        OUTPUT_MISMATCH_FASTR("FastR output mismatch: "),

        TESTING_EXAMPLES("Testing examples for package"),
        RUNNING_SPECIFIC_TESTS("Running specific tests for package"),
        RUNNING_VIGNETTES("Running vignettes for package"),

        FAILED("FAILED"),
        INDETERMINATE("INDETERMINATE"),
        OK("OK");

        String linePrefix;

        Token(String prefix) {
            this.linePrefix = prefix;
        }

    }

    public abstract static class AbstractSection {
        private List<Section> sections = new LinkedList<>();
        private boolean success;

        public List<Section> getSections() {
            return sections;
        }

        public void addSection(Section sub) {
            sections.add(sub);
        }

        public abstract Collection<Problem> collectProblems();

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

    }

    public static class LogFile extends AbstractSection {
        public LogFile(Path path) {
            this.path = path;
        }

        private Path path;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("LogFile ").append(path);
            if (!getSections().isEmpty()) {
                sb.append("(");
                for (Section section : getSections()) {
                    sb.append(section).append(", ");
                }
                sb.append(")");
            }
            return sb.toString();
        }

        @Override
        public Collection<Problem> collectProblems() {
            return getSections().stream().flatMap(s -> s.collectProblems().stream()).collect(Collectors.toList());
        }

        public Path getPath() {
            return path;
        }

    }

    public static class Section extends AbstractSection {
        private String name;
        private String mode;
        private int startLine;
        private AbstractSection parent;
        Collection<Problem> problems;

        protected Section(AbstractSection parent, String name, int startLine) {
            this.parent = parent;
            this.startLine = startLine;
            this.name = name;
        }

        public AbstractSection getParent() {
            return parent;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return String.format("Section %s (start: %d, problems: %d)", name, startLine, problems != null ? problems.size() : 0);
        }

        @Override
        public Collection<Problem> collectProblems() {
            Collection<Problem> collected = new ArrayList<>();
            if (problems != null) {
                collected.addAll(problems);
            }
            for (Section child : getSections()) {
                collected.addAll(child.collectProblems());
            }
            return collected;
        }

    }

    public static class OutputMismatchProblem extends Problem {

        private final String details;

        protected OutputMismatchProblem(RPackageTestRun pkg, Detector<?> detector, Location location, String details) {
            super(pkg, detector, location);
            this.details = details;
        }

        @Override
        public String getSummary() {
            return Token.OUTPUT_MISMATCH_FASTR.linePrefix;
        }

        @Override
        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return getSummary() + details;
        }

        @Override
        public int getSimilarityTo(Problem other) {
            return 0;
        }

        @Override
        public boolean isSimilarTo(Problem other) {
            return true;
        }
    }

    public static class ContentMalformedProblem extends Problem {

        private final String details;

        protected ContentMalformedProblem(RPackageTestRun pkg, Detector<?> detector, Location location, String details) {
            super(pkg, detector, location);
            this.details = details;
        }

        @Override
        public String getSummary() {
            return Token.CONTENT_MALFORMED.linePrefix;
        }

        @Override
        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return getSummary() + details;
        }

        @Override
        public int getSimilarityTo(Problem other) {
            return 0;
        }

        @Override
        public boolean isSimilarTo(Problem other) {
            return true;
        }
    }

}
