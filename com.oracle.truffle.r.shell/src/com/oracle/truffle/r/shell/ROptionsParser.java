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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RCmdOptions.Option;
import com.oracle.truffle.r.runtime.RCmdOptions.OptionType;

/**
 * Implements the standard R command line syntax.
 * 
 * R supports {@code --arg=value} or {@code -arg value} for string-valued options.
 * 
 * The spec for {@code commandArgs()} states that it returns the executable by which R was invoked
 * in element 0, which is consistent with the C {@code main} function, but defines the exact form to
 * be platform independent. Java does not provide the executable (for obvious reasons) so we use
 * "FastR".
 */
public class ROptionsParser {
    // CheckStyle: stop system..print check

    /**
     * Parse the arguments, setting the corresponding {@code Option values}.
     * 
     * @return an array with element zero set to "FastR", followed by {@code args}.
     */
    public static String[] parseArguments(String[] args) {
        int i = 0;
        while (i < args.length) {
            final String arg = args[i];
            Option<?> option = RCmdOptions.matchOption(arg);
            if (option == null) {
                // GnuR does not abort, simply issues a warning
                System.out.printf("ARGUMENT '%s' __ignored__%n", arg);
                i++;
                continue;
            } else if (option.matchedShort() && i == args.length - 1) {
                System.out.println("usage:");
                printHelp(1);
            }
            // check implemented
            if (!option.implemented) {
                System.out.println("WARNING: option: " + arg + " is not implemented");
            }
            if (option.matchedShort()) {
                i++;
                option.setValue(args[i]);
            } else {
                if (option.type == OptionType.BOOLEAN) {
                    option.setValue(true);
                } else if (option.type == OptionType.STRING) {
                    int eqx = arg.indexOf('=');
                    option.setValue(arg.substring(eqx + 1));
                }
            }
            i++;
            // check for --args, in which case stop parsing
            if (option == RCmdOptions.ARGS) {
                break;
            }
        }
        String[] result = new String[args.length + 1];
        result[0] = "FastR";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    public static void printHelp(int exitCode) {
        System.out.println("\nUsage: R [options] [< infile] [> outfile]\n" + "   or: R CMD command [arguments]\n\n" + "Start R, a system for statistical computation and graphics, with the\n"
                        + "specified options, or invoke an R tool via the 'R CMD' interface.\n");
        System.out.println("Options:");
        for (Option<?> option : RCmdOptions.optionList()) {
            System.out.printf("  %-22s  %s%n", option.getHelpName(), option.help);
        }
        System.out.println("\nFILE may contain spaces but not shell metacharacters.\n");
        if (exitCode >= 0) {
            System.exit(exitCode);
        }
    }

}
