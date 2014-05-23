/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.shell;

import static com.oracle.truffle.r.runtime.RCmdOptions.*;

import java.util.*;

import com.oracle.truffle.r.runtime.*;

/**
 * Emulates the (Gnu)Rscript command as precisely as possible. in GnuR, Rscript is a genuine wrapper
 * to R, as evidenced by the script {@code print(commandArgs())}. We don't implement it quite that
 * way but the effect is similar.
 *
 * TODO support {@code --default-packages} option.
 */
public class RscriptCommand {
    // CheckStyle: stop system..print check
    public static void main(String[] args) {
        // Since many of the options are shared parse them from an RSCRIPT perspective.
        // Handle --help and --version specially, as they exit.
        RCmdOptionsParser.Result result = RCmdOptionsParser.parseArguments(RCmdOptions.Client.RSCRIPT, args);
        int resultArgsLength = result.args.length;
        int firstNonOptionArgIndex = result.firstNonOptionArgIndex;
        if (HELP.getValue()) {
            RCmdOptionsParser.printHelp(RCmdOptions.Client.RSCRIPT, 0);
            Utils.exit(0);
        } else if (VERSION.getValue()) {
            printVersionAndExit();
        }
        // Now reformat the args, setting --slave and --no-restore as per the spec
        // and invoke RCommand.subMain
        ArrayList<String> adjArgs = new ArrayList<>(resultArgsLength + 1);
        adjArgs.add(result.args[0]);
        adjArgs.add("--slave");
        SLAVE.setValue(true);
        adjArgs.add("--no-restore");
        NO_RESTORE.setValue(true);
        // Either -e options are set or first non-option arg is a file
        if (EXPR.getValue() == null) {
            if (firstNonOptionArgIndex == resultArgsLength) {
                System.err.println("filename is missing");
                Utils.exit(2);
            } else {
                FILE.setValue(result.args[firstNonOptionArgIndex]);
            }
        }
        // copy up to non-option args
        int rx = 1;
        while (rx < firstNonOptionArgIndex) {
            adjArgs.add(result.args[rx]);
            rx++;
        }
        if (FILE.getValue() != null) {
            adjArgs.add("--file=" + FILE.getValue());
            rx++; // skip over file arg
            firstNonOptionArgIndex++;
        }

        if (firstNonOptionArgIndex < resultArgsLength) {
            adjArgs.add("--args");
            while (rx < resultArgsLength) {
                adjArgs.add(result.args[rx++]);
            }
        }
        String[] adjArgsArray = new String[adjArgs.size()];
        adjArgs.toArray(adjArgsArray);
        RCommand.subMain(adjArgsArray);
    }

    private static void printVersionAndExit() {
        System.out.print("R scripting front-end version ");
        System.out.println(RVersionNumber.FULL);
        Utils.exit(0);
    }

}
