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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.dump.html.HTMLBuilder;
import com.oracle.truffle.r.test.packages.analyzer.dump.html.HTMLBuilder.Tag;

public class HtmlDumper extends AbstractDumper {

    private static final String TITLE = "FastR Package Test Dashboard";
    private Path destDir;

    public HtmlDumper(Path destDir) {
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
        return Files.isWritable(destDir);
    }

    @Override
    public void dump(Collection<Problem> problems) {
        try {
            createAndCheckOutDir();
            dumpIndexFile(problems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dumpIndexFile(Collection<Problem> problems) {
        Path indexFile = destDir.resolve("index.html");

        try (BufferedWriter bw = Files.newBufferedWriter(indexFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));

            Tag errorDistributionTable = generateDistributionTable(builder, groupByType(problems));
            Tag pkgDistributionTable = generatePkgDistributionTable(builder, groupByPkg(problems));

            builder.html(builder.head(builder.title(TITLE)),
                            builder.body(builder.h1(TITLE), builder.h2("Distribution by Problem Type"), errorDistributionTable, builder.h2("Distribution by Package"), pkgDistributionTable));
            builder.dump();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Tag generatePkgDistributionTable(HTMLBuilder builder, Map<RPackage, List<Problem>> groupByPkg) {
        List<RPackage> collect = groupByPkg.keySet().stream().sorted((a, b) -> Integer.compare(groupByPkg.get(b).size(), groupByPkg.get(a).size())).collect(Collectors.toList());

        Tag table = builder.table();
        for (RPackage pkg : collect) {
            String pkgFileName = dumpRPackage(pkg, groupByPkg);
            table.addChild(builder.tr(builder.td(builder.a(pkgFileName, pkg.toString())),
                            builder.td(Integer.toString(groupByPkg.get(pkg).size()))));
        }
        return table;
    }

    private Tag generateDistributionTable(HTMLBuilder builder, Map<Class<? extends Problem>, List<Problem>> groupByType) {
        List<Class<? extends Problem>> collect = groupByType.keySet().stream().sorted((a, b) -> Integer.compare(groupByType.get(b).size(), groupByType.get(a).size())).collect(Collectors.toList());

        Tag table = builder.table();
        for (Class<? extends Problem> class1 : collect) {
            String problemClassFileName = dumpProblemClass(class1, groupByType);
            table.addChild(builder.tr(builder.td(builder.a(problemClassFileName, class1.getSimpleName())), builder.td(Integer.toString(groupByType.get(class1).size()))));
        }
        return table;
    }

    private String dumpRPackage(RPackage pkg, Map<RPackage, List<Problem>> groupByPkg) {

        Path problemClassFile = destDir.resolve(pkg.toString() + ".html");
        try (BufferedWriter bw = Files.newBufferedWriter(problemClassFile, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));

            Tag table = dumpProblemTable(pkg, groupByPkg, builder);

            builder.html(builder.head(builder.title(pkg.toString())), builder.body(table));
            builder.dump();

            return problemClassFile.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String dumpProblemClass(Class<? extends Problem> type, Map<Class<? extends Problem>, List<Problem>> groupByType) {

        Path problemClassFile = destDir.resolve(type.getSimpleName() + ".html");
        try (BufferedWriter bw = Files.newBufferedWriter(problemClassFile, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            HTMLBuilder builder = new HTMLBuilder(new PrintWriter(bw));

            Tag table = dumpProblemTable(type, groupByType, builder);

            builder.html(builder.head(builder.title(type.getName())), builder.body(table));
            builder.dump();

            return problemClassFile.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> Tag dumpProblemTable(T pkg, Map<T, List<Problem>> groupByPkg, HTMLBuilder builder) throws MalformedURLException {
        Tag table = builder.table();
        table.addAttribute("border", "1");
        for (Problem p : groupByPkg.get(pkg)) {
            Location loc = p.getLocation();
            table.addChild(builder.tr(builder.td(builder.a(loc.file.toUri().toURL().toString(), loc.toString())),
                            builder.td(builder.escape(p.getSummary())),
                            builder.td(builder.escape(p.getDetails()))));
        }
        return table;
    }

}
