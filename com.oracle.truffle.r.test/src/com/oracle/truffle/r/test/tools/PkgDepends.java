/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.oracle.truffle.r.runtime.DCF;
import com.oracle.truffle.r.runtime.DCF.Fields;

/**
 * Java version of code that reads the CRAN PACKAGES (DCF) file and produces a package install order
 * based on the stated dependencies.
 */
public class PkgDepends {

    private static final String[] defaultPkgs = new String[]{"base", "grid", "splines", "utils",
                    "compiler", "grDevices", "methods", "stats", "stats4",
                    "datasets", "graphics", "parallel", "tools", "tcltk"};

    private static final Set<String> defaultPkgsSet = new HashSet<>();
    private static final SortedMap<String, Package> packageMap = new TreeMap<>();

    public static void main(String[] args) {
        String packagesFile = null;
        Path packagesFilePath = null;
        boolean listTotalDepends = false;
        String pkgInstallOrder = null;
        boolean allInstallOrder = false;
        boolean listLeaves = false;
        boolean listUnknown = false;
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("--PACKAGES")) {
                packagesFile = args[++i];
            } else if (arg.equals("--list-total-depends")) {
                listTotalDepends = true;
            } else if (arg.equals("--install-order")) {
                pkgInstallOrder = args[++i];
            } else if (arg.equals("--all-install-order")) {
                allInstallOrder = true;
            } else if (arg.equals("--list-leaves")) {
                listLeaves = true;
            } else if (arg.equals("--list-unknown")) {
                listUnknown = true;
            }
            i++;
        }
        if (packagesFile == null) {
            String localCranRepoPath = System.getenv("LOCAL_CRAN_REPO_PATH");
            if (localCranRepoPath == null) {
                System.err.println("must set LOCAL_CRAN_REPO_PATH or provide --PACKAGES");
                System.exit(1);
            }
            packagesFilePath = FileSystems.getDefault().getPath(localCranRepoPath, "PACKAGES");
        } else {
            packagesFilePath = FileSystems.getDefault().getPath(packagesFile);
        }

        for (String defaultPkg : defaultPkgs) {
            defaultPkgsSet.add(defaultPkg);
        }

        try {
            List<String> linesList = Files.readAllLines(packagesFilePath);
            String[] linesArray = new String[linesList.size()];
            linesList.toArray(linesArray);
            DCF dcf = DCF.read(linesArray, null);
            List<Fields> fieldList = dcf.getRecords();
            for (Fields fields : fieldList) {
                String pkgName = fields.getFields().get("Package");
                packageMap.put(pkgName, new Package(pkgName));
            }
            for (Fields fields : fieldList) {
                Package pkg = packageMap.get(fields.getFields().get("Package"));
                for (String kind : new String[]{"Depends", "Imports", "LinkingTo"}) {
                    processDependencies(pkg, fields, kind);
                }
            }
            for (Package pkg : packageMap.values()) {
                pkg.computeTotalDepends();
            }
            if (listTotalDepends) {
                listTotalDepends();
            }
            if (pkgInstallOrder != null) {
                List<String> installOrder = installOrder(pkgInstallOrder);
                if (installOrder == null) {
                    System.err.printf("no package: %s%n", pkgInstallOrder);
                    System.exit(1);
                }
                printInstallOrder(pkgInstallOrder, installOrder);
            }
            if (allInstallOrder) {
                for (Package pkg : packageMap.values()) {
                    if (!pkg.isCran) {
                        continue;
                    }
                    printInstallOrder(pkg.name, installOrder(pkg.name));
                }
            }
            if (listLeaves) {
                System.out.println("Leaf packages");
                for (Package pkg : packageMap.values()) {
                    if (pkg.directDepends.size() == 0) {
                        System.out.println(pkg.name);
                    }
                }
            }
            if (listUnknown) {
                System.out.println("Packages not found on CRAN");
                for (Package pkg : packageMap.values()) {
                    if (!pkg.isCran) {
                        System.out.printf("Package: %s%n", pkg.name);
                        System.out.println("Reason: Bioconductor\n");
                    }
                }
            }
            System.console();
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

    private static void processDependencies(Package pkg, Fields fields, String kind) {
        String depends = fields.getFields().get(kind);
        if (depends != null && depends.length() > 0) {
            String[] list = depends.split(",");
            for (String entry : list) {
                String dependee = trimVersion(entry);
                if (dependee.equals("R")) {
                    continue;
                }
                if (defaultPkgsSet.contains(dependee)) {
                    continue;
                }
                Package dependeePkg = packageMap.get(dependee);
                if (dependeePkg == null) {
                    System.out.printf("WARNING: package %s depends on unknown package %s%n", pkg.name, dependee);
                    dependeePkg = new Package(dependee, false);
                    packageMap.put(dependee, dependeePkg);
                }
                pkg.addDependency(dependeePkg);
            }
        }
    }

    private static void printInstallOrder(String pkgName, List<String> installOrder) {
        System.out.printf("Install order for package %s: ", pkgName);
        for (String dpkgName : installOrder) {
            System.out.printf("%s ", dpkgName);
        }
        System.out.println();

    }

    private static String trimVersion(String entry) {
        String result = entry;
        int ix = entry.indexOf('(');
        if (ix > 0) {
            result = result.substring(0, ix);
        }
        return result.trim();
    }

    private static void listTotalDepends() {
        for (Package pkg : packageMap.values()) {
            if (!pkg.isCran) {
                continue;
            }
            System.out.printf("Package: %s\nDepends: ", pkg.name);
            int p = 0;
            int s = pkg.totalDepends.size();
            if (s > 0) {
                for (Package dpkg : pkg.totalDepends) {
                    System.out.printf("%s", dpkg.name);
                    p++;
                    if (p < s) {
                        System.out.print(", ");
                    } else {
                        System.out.println();
                    }
                }
            } else {
                System.out.println();
            }
            System.out.println();
        }
    }

    private static List<String> installOrder(String pkgName) {
        Package pkg = packageMap.get(pkgName);
        if (pkg == null) {
            return null;
        } else {
            return installOrder(pkg);
        }
    }

    private static List<String> installOrder(Package pkg) {
        List<String> order = new ArrayList<>();
        for (Package dpkg : pkg.directDepends) {
            List<String> dpkgOrder = installOrder(dpkg);
            for (String s : dpkgOrder) {
                checkAdd(order, s);
            }
        }
        order.add(pkg.name);
        return order;
    }

    private static void checkAdd(List<String> order, String pkgName) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).equals(pkgName)) {
                return;
            }
        }
        order.add(pkgName);
    }

    private static final class Package {
        private final String name;
        private final List<Package> directDepends = new ArrayList<>();
        private Set<Package> totalDepends;
        private final boolean isCran;

        private Package(String name) {
            this(name, true);
        }

        private Package(String name, boolean isCran) {
            this.name = name;
            this.isCran = isCran;
        }

        private void addDependency(Package pkg) {
            directDepends.add(pkg);
        }

        @Override
        public String toString() {
            return name;
        }

        private void computeTotalDepends() {
            if (totalDepends == null) {
                totalDepends = new HashSet<>();
                for (Package pkg : directDepends) {
                    totalDepends.add(pkg);
                    pkg.computeTotalDepends();
                    for (Package dpkg : pkg.totalDepends) {
                        totalDepends.add(dpkg);
                    }
                }
            }
        }
    }
}
