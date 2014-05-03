/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tools;

import java.io.*;
import java.util.*;

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Analyzes the {@link RBuiltin} classes against GnuR. The starting points are:
 * <ul>
 * <li>The set of classes in FastR annotated by {@link RBuiltin}.</li>
 * <li>A file containing a distilled version of the {@code FUNTAB} table in GnuR.</li>
 * </ul>
 * {@code FUNTAB} is first processed into {@link #rInfoList} and {@link #rInfoMap}. Then the classes
 * that are annotated with {@link RBuiltin}, from the list the file argument passed in by {@code mx}
 * , are analyzed. The function specified with the {@link RBuiltin} is looked up in
 * {@link #rInfoMap} and, if found, the {@code RInfo.classInfo} field is filled in. The special
 * treatment of {@code .Internal} in current FastR is handled and any {@link RBuiltin} that is
 * specified as a {@code .Internal.xxx} is indicated by the boolean
 * {@code ClassInfo.dotInternal == true}.
 * <p>
 * The following options can be set to perform specific checks:
 * <ul>
 * <li>--check-internal: Every function in {@code FUNTAB} that is defined to be called using
 * {@code .Internal} is checked against the {@link RBuiltin} that implements it. Those which are not
 * specified as {@code .Internal.xxx} are listed.</li>
 * <li>--todo: Every function in {@code FUNTAB} that is not implemented by a FastR {@link RBuiltin}
 * is listed.</li>
 * <li>--unknown-to-gnur: Any {@link RBuiltin} specifying a function that does not appear in
 * {@code FUNTAB} is listed.</li>
 * <li>--no-eval-args: Functions in {@code FUNTAB} that are specified to not evaluate their
 * arguments are listed.</li>
 * <li>--visibility: The visibility specification of the functions in {@code FUNTAB} is listed.</li>
 * </ul>
 * If no options are provided, it is as if they all were.
 */
public class AnalyzeRBuiltin {

    private static class ClassInfo {
        String name;
        @SuppressWarnings("unused") String path;
        boolean dotInternal;

        ClassInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    enum Visibility {
        ON("Force ON"),
        OFF("Force OFF"),
        ON_UPDATE("Force ON, implementation may UPDATE");

        final String printName;

        Visibility(String printName) {
            this.printName = printName;
        }
    }

    enum Kind {
        Primitive,
        Internal
    }

    private static class RInfo {
        String name;
        Visibility visibility;
        Kind kind;
        boolean evalArgs;
        ClassInfo classInfo;

        RInfo(String[] info) {
            name = info[0];
            visibility = Visibility.valueOf(info[1]);
            kind = Kind.valueOf(info[2]);
            evalArgs = Boolean.parseBoolean(info[3]);
        }
    }

    private static SortedMap<String, RInfo> rInfoMap = new TreeMap<>();
    private static ArrayList<RInfo> rInfoList = new ArrayList<>();
    private static ArrayList<String> unkownToGnuR = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Checkstyle: stop system print check

        boolean checkInternal = false;
        boolean checkUnknownToGnuR = false;
        boolean toDo = false;
        boolean noEvalArgs = false;
        boolean visibility = false;

        String classFilePath = null;

        if (args.length == 1) {
            classFilePath = args[0];
            checkInternal = true;
            checkUnknownToGnuR = true;
            toDo = true;
            noEvalArgs = true;
            visibility = true;
        } else {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--all":
                        break;
                    case "--check-internal":
                        checkInternal = true;
                        break;
                    case "--unknown-to-gnur":
                        checkUnknownToGnuR = true;
                        break;
                    case "--todo":
                        toDo = true;
                        break;
                    case "--no-eval-args":
                        noEvalArgs = true;
                        break;
                    case "--visibility":
                        visibility = true;
                        break;
                    default:
                        classFilePath = arg;
                        break;
                }
            }
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(ResourceHandlerFactory.getHandler().getResourceAsStream(AnalyzeRBuiltin.class, "FUNTAB")))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] items = line.split(",");
                RInfo rInfo = new RInfo(items);
                rInfoMap.put(rInfo.name, rInfo);
                rInfoList.add(rInfo);
            }
        }
        try (BufferedReader r = new BufferedReader(new FileReader(classFilePath))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] pair = line.split(",");
                ClassInfo classInfo = new ClassInfo(pair[0], pair[1]);
                Class<?> klass = Class.forName(classInfo.name);
                RBuiltin rb = klass.getAnnotation(RBuiltin.class);
                if (rb != null) {
                    String[] functions = rb.value();
                    for (String function : functions) {
                        String cleanFunction = stripInternal(function);
                        if (!cleanFunction.equals(function)) {
                            classInfo.dotInternal = true;
                        }
                        RInfo rInfo = rInfoMap.get(cleanFunction);
                        if (rInfo == null) {
                            unkownToGnuR.add(function);
                            continue;
                        }
                        if (rInfo.classInfo != null) {
                            System.err.printf("function %s defined in %s is redefined by %s%n", cleanFunction, stripPackage(rInfo.classInfo.name), stripPackage(classInfo.name));
                            continue;
                        }
                        rInfo.classInfo = classInfo;
                    }
                }
            }
        }

        if (checkInternal) {
            checkInternals();
        }

        if (checkUnknownToGnuR) {
            checkUnknownToGnuR();
        }

        if (toDo) {
            listToDo();
        }

        if (noEvalArgs) {
            listNoEvalArgs();
        }

        if (visibility) {
            listVisibility();
        }
    }

    /**
     * For each .Internal in {@link #rInfoList} check how FastR implements it.
     */
    private static void checkInternals() {
        System.out.println("Functions defined as .Internal that are not implemented as .Internal.xxx in FastR");
        for (RInfo rInfo : rInfoList) {
            if (rInfo.classInfo != null && rInfo.kind == Kind.Internal) {
                if (!rInfo.classInfo.dotInternal) {
                    System.out.println(rInfo.name);
                }
            }
        }
        System.out.println();
    }

    private static void checkUnknownToGnuR() {
        System.out.println("FastR builtins not in FUNTAB");
        for (String f : unkownToGnuR) {
            System.out.println(f);
        }
        System.out.println();
    }

    private static void listToDo() {
        System.out.println("Functions not implemented by FastR (as an RBuiltin)");
        for (RInfo rInfo : rInfoList) {
            if (rInfo.classInfo == null) {
                System.out.printf("%s (%s)%n", rInfo.name, rInfo.kind);
            }
        }
        System.out.println();
    }

    private static void listNoEvalArgs() {
        System.out.println("Functions defined to not evaluate their arguments");
        for (RInfo rInfo : rInfoList) {
            if (!rInfo.evalArgs) {
                System.out.println(rInfo.name);
            }
        }
        System.out.println();
    }

    private static void listVisibility() {
        System.out.println("Visibility specification");
        for (RInfo rInfo : rInfoList) {
            System.out.printf("%s: %s%n", rInfo.name, rInfo.visibility.printName);
        }
        System.out.println();
    }

    private static final String DOT_INTERNAL = ".Internal.";
    private static final String PACKAGE_PREFIX = "com.oracle.truffle.r.nodes.builtin.";

    private static String stripInternal(String name) {
        if (name.startsWith(DOT_INTERNAL)) {
            return name.replace(DOT_INTERNAL, "");
        } else {
            return name;
        }
    }

    private static String stripPackage(String s) {
        return s.replace(PACKAGE_PREFIX, "");
    }

}
