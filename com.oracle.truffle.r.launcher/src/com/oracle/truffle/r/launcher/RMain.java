/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.launcher;

/**
 * Convenience class that allows the R/Rscript entry to be chosen by an initial argument.
 */
public class RMain {

    public static void main(String[] args) {
        boolean rscript = false;
        if (args.length > 0) {
            String arg = args[0];
            switch (arg) {
                case "R":
                case "r":
                    break;

                case "Rscript":
                case "rscript":
                    rscript = true;
                    break;

                default:
                    System.out.println(arg);
                    usage();
            }
            String[] xargs = shiftArgs(args);
            if (rscript) {
                RscriptCommand.main(xargs);
            } else {
                RCommand.main(xargs);
            }
        } else {
            usage();
        }
    }

    private static void usage() {
        System.out.println("usage: [R|Rscript] ...");
        System.exit(1);
    }

    private static String[] shiftArgs(String[] args) {
        String[] nargs = new String[args.length - 1];
        System.arraycopy(args, 1, nargs, 0, nargs.length);
        return nargs;
    }

}
