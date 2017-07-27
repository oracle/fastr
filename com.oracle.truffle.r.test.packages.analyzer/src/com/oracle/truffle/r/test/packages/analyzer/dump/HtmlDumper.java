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
            PrintWriter writer = new PrintWriter(bw);

            writer.println(
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            writer.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
            writeTitle(writer, TITLE);
            writer.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n</head><body>");

            generateDistributionTable(writer, groupByType(problems));

            writer.println("</body>");
            writer.println("</html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateDistributionTable(PrintWriter pw, Map<Class<? extends Problem>, List<Problem>> groupByType) {

        List<Class<? extends Problem>> collect = groupByType.keySet().stream().sorted((a, b) -> Integer.compare(groupByType.get(b).size(), groupByType.get(a).size())).collect(Collectors.toList());
        pw.println("<table>");
        for (Class<? extends Problem> class1 : collect) {
            String problemClassFileName = dumpProblemClass(class1, groupByType);
            pw.println("<tr>");
            pw.print("<td><a href=\"");
            pw.print(problemClassFileName);
            pw.print("\">");
            pw.print(class1.getSimpleName());
            pw.print("</a></td>");
            pw.print("<td>");
            pw.print(groupByType.get(class1).size());
            pw.print("</td>");
            pw.println("</tr>");
        }
        pw.println("</table>");
    }

    private String dumpProblemClass(Class<? extends Problem> class1,
                    Map<Class<? extends Problem>, List<Problem>> groupByType) {

        Path problemClassFile = destDir.resolve(class1.getSimpleName() + ".html");
        try (BufferedWriter bw = Files.newBufferedWriter(problemClassFile, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            PrintWriter pw = new PrintWriter(bw);

            pw.println(
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
            writeTitle(pw, class1.getName());
            pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n</head><body>");

            pw.println("<table border=\"1\">");
            for (Problem p : groupByType.get(class1)) {
                pw.println("<tr>");
                pw.print("<td><a href=\"");
                Location loc = p.getLocation();
                pw.print(loc.file.toUri().toURL());
                pw.print("#L");
                pw.print(loc.lineNr);
                pw.print("\">");
                pw.print(loc.toString());
                pw.print("</a></td>");
                pw.print("<td>");
                pw.print(p.getSummary());
                pw.print("</td>");
                pw.print("<td>");
                pw.print(p.getDetails());
                pw.print("</td>");
                pw.println("</tr>");
            }
            pw.println("</table>");

            pw.println("</body>");
            pw.println("</html>");
            return problemClassFile.getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeTitle(PrintWriter writer, String title2) {
        writer.print("<title>");
        writer.print(title2);
        writer.println("</title>");

    }

}
