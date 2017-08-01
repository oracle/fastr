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
package com.oracle.truffle.r.test.packages.analyzer.dump;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.dump.HTMLBuilder.Tag;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParseException;

public class HTMLDumper extends AbstractDumper {

    private static final String TITLE = "FastR Package Test Dashboard";
    private Path destDir;

    public HTMLDumper(Path destDir) {
        this.destDir = Objects.requireNonNull(destDir);
    }

    /**
     * Creates the output directory if it does not exists and checks if the directory is writable.
     * This method may throw an {@link IOException} if it cannot create the directory.
     */
    public boolean createAndCheckOutDir() throws IOException {
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }
        // test creating a file in the output directory
        Path testFile = destDir.resolve("test.html");
        Files.createFile(testFile);
        Files.deleteIfExists(testFile);
        return true;
    }

    @Override
    public void dump(Collection<RPackage> packages, Collection<LogFileParseException> parseErrors) {
        try {
            createAndCheckOutDir();
            dumpIndexFile(packages, parseErrors);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpIndexFile(Collection<RPackage> packages, Collection<LogFileParseException> parseErrors) {
        Path indexFile = destDir.resolve("index.html");

        try (BufferedWriter bw = Files.newBufferedWriter(indexFile, CREATE, TRUNCATE_EXISTING, WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));

            Collection<Problem> allProblems = collectAllProblems(packages);
            Collection<RPackageTestRun> allTestRuns = collectTestRuns(packages);

            allProblems = eliminateRedundantProblems(allProblems);

            Tag errorDistributionTable = generateTypeDistributionTable(builder, groupByType(allProblems));
            Tag pkgDistributionTable = generateTestRunDistributionTable(builder, groupByTestRuns(allTestRuns, allProblems));
            Tag distrinctProblemDistributionTable = generateDistinctProblemDistribution(builder, groupByProblemContent(allProblems));

            builder.html(builder.head(builder.title(TITLE)), builder.body(
                            builder.h1(TITLE),
                            builder.p("Total number of analysis candidates: " + allTestRuns.size()),
                            builder.p(builder.a(generateParseErrorsList(parseErrors), "Test runs failed to analyze: " + parseErrors.size())),
                            builder.h2("Distribution by Problem Type"), errorDistributionTable,
                            builder.h2("Distribution by Package Test Run"), pkgDistributionTable,
                            builder.h2("Distinct Problem Distribution"), distrinctProblemDistributionTable));
            builder.dump();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Collection<RPackageTestRun> collectTestRuns(Collection<RPackage> pkgs) {
        Collection<RPackageTestRun> problems = new ArrayList<>();
        for (RPackage pkg : pkgs) {
            for (RPackageTestRun run : pkg.getTestRuns()) {
                problems.add(run);
            }
        }
        return problems;
    }

    private static Collection<Problem> collectAllProblems(Collection<RPackage> pkgs) {
        Collection<Problem> problems = new ArrayList<>();
        for (RPackage pkg : pkgs) {
            for (RPackageTestRun run : pkg.getTestRuns()) {
                problems.addAll(run.getProblems());
            }
        }
        return problems;
    }

    private Tag generateTestRunDistributionTable(HTMLBuilder builder, Map<RPackageTestRun, List<Problem>> groupByPkg) {
        List<RPackageTestRun> collect = groupByPkg.keySet().stream().sorted((a, b) -> Integer.compare(groupByPkg.get(b).size(), groupByPkg.get(a).size())).collect(Collectors.toList());

        Tag table = builder.table(builder.tr(
                        builder.th("Package"),
                        builder.th("Test Run"),
                        builder.th("Result"),
                        builder.th("Problem Count")));
        table.addAttribute("border", "1");

        for (RPackageTestRun testRun : collect) {
            String pkgFileName = dumpTableFile(testRun.getPackage().toString() + "_" + testRun.getNumber() + ".html", testRun.toString(), testRun, groupByPkg);
            int n = groupByPkg.get(testRun).size();
            Tag tableRow = builder.tr(
                            builder.td(builder.a(pkgFileName, testRun.getPackage().toString())),
                            builder.td(Integer.toString(testRun.getNumber())),
                            builder.td(testRun.isSuccess() ? "OK" : "FAILED"),
                            builder.td(Integer.toString(n)));
            if (testRun.isSuccess() && n > 0) {
                tableRow.addAttribute("bgcolor", "#fdae61");
            }
            table.addChild(tableRow);
        }
        return table;
    }

    private Tag generateDistinctProblemDistribution(HTMLBuilder builder, Map<ProblemContent, List<Problem>> groupByPkg) {
        List<ProblemContent> collect = groupByPkg.keySet().stream().sorted((a, b) -> Integer.compare(groupByPkg.get(b).size(), groupByPkg.get(a).size())).collect(Collectors.toList());

        Tag table = builder.table(builder.tr(
                        builder.th("Problem"),
                        builder.th("Problem Count"),
                        builder.th("Representitive Message")));
        table.addAttribute("border", "1");

        int i = 0;
        for (ProblemContent problem : collect) {
            String pkgFileName = dumpTableFile("problem" + i + ".html", problem.toString(), problem, groupByPkg);
            ++i;

            int n = groupByPkg.get(problem).size();
            Tag tableRow = builder.tr(
                            builder.td(builder.a(pkgFileName, problem.representitive.getSummary())),
                            builder.td(Integer.toString(n)),
                            builder.td(problem.representitive.getDetails()));
            table.addChild(tableRow);
        }
        return table;
    }

    private Tag generateTypeDistributionTable(HTMLBuilder builder, Map<Class<? extends Problem>, List<Problem>> groupByType) {
        List<Class<? extends Problem>> collect = groupByType.keySet().stream().sorted((a, b) -> Integer.compare(groupByType.get(b).size(), groupByType.get(a).size())).collect(Collectors.toList());

        Tag table = builder.table();
        table.addAttribute("border", "1");
        for (Class<? extends Problem> type : collect) {
            String problemClassFileName = dumpTableFile(type.getSimpleName() + ".html", type.getName(), type, groupByType);
            table.addChild(builder.tr(
                            builder.td(builder.a(problemClassFileName, type.getSimpleName())),
                            builder.td(Integer.toString(groupByType.get(type).size()))));
        }
        return table;
    }

    private <T> String dumpTableFile(String htmlFileName, String title, T key, Map<T, List<Problem>> groupingTable) {

        Path problemClassFile = destDir.resolve(htmlFileName);
        try (BufferedWriter bw = Files.newBufferedWriter(problemClassFile, CREATE, TRUNCATE_EXISTING, WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));

            Tag table = generateProblemTable(problemClassFile, key, groupingTable, builder);

            builder.html(builder.head(builder.title(title)), builder.body(table));
            builder.dump();

            return problemClassFile.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> Tag generateProblemTable(Path file, T key, Map<T, List<Problem>> groupingTable, HTMLBuilder builder) {
        Tag table = builder.table();
        table.addAttribute("border", "1");
        for (Problem p : groupingTable.get(key)) {
            Location loc = p.getLocation();
            Path relativeLocation = file.relativize(loc.file);
            // remove nearest path element because relativizing also considers file names
            relativeLocation = relativeLocation.subpath(1, relativeLocation.getNameCount());
            table.addChild(builder.tr(
                            builder.td(builder.a(relativeLocation.toString() + "#" + loc.lineNr, loc.toString())),
                            builder.td(builder.escape(p.getSummary())),
                            builder.td(builder.escape(p.getDetails()))));
        }
        return table;
    }

    private String generateParseErrorsList(Collection<LogFileParseException> parseErrors) throws IOException {

        Path problemClassFile = destDir.resolve("failedToAnalyze.html");
        try (BufferedWriter bw = Files.newBufferedWriter(problemClassFile, CREATE, TRUNCATE_EXISTING, WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));
            Tag table = builder.table(builder.tr(
                            builder.th("Package Test Run"),
                            builder.th("Location"),
                            builder.th("Message")));
            table.addAttribute("border", "1");
            for (LogFileParseException e : parseErrors) {
                table.addChild(builder.tr(
                                builder.td(e.getTestRun().toString()),
                                builder.td(e.getLocation().toString()),
                                builder.td(e.getMessage())));
            }
            builder.html(builder.head(builder.title("Test Runs Failed to Analyze")), builder.body(table));
            builder.dump();
        }
        return problemClassFile.getFileName().toString();
    }

}
