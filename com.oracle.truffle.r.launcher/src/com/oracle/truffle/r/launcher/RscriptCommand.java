/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

/**
 * Emulates the (Gnu)Rscript command as precisely as possible. in GnuR, Rscript is a genuine wrapper
 * to R, as evidenced by the script {@code print(commandArgs())}. We don't implement it quite that
 * way but the effect is similar.
 *
 */
public class RscriptCommand {

    // CheckStyle: stop system..print check

    private static String[] preprocessRScriptOptions(RLauncher launcher, RCmdOptions options) {
        String[] arguments = options.getArguments();
        int resultArgsLength = arguments.length;
        int firstNonOptionArgIndex = options.getFirstNonOptionArgIndex();
        // Now reformat the args, setting --slave and --no-restore as per the spec
        ArrayList<String> adjArgs = new ArrayList<>(resultArgsLength + 1);
        adjArgs.add(arguments[0]);
        adjArgs.add("--slave");
        options.setValue(RCmdOption.SLAVE, true);
        adjArgs.add("--no-restore");
        options.setValue(RCmdOption.NO_RESTORE, true);
        // Either -e options are set or first non-option arg is a file
        if (options.getStringList(RCmdOption.EXPR) == null) {
            if (firstNonOptionArgIndex == resultArgsLength) {
                launcher.setHelpCategory(OptionCategory.USER);
                // does not return
                if (launcher.runPolyglotAction()) {
                    System.exit(1);
                }
            } else {
                options.setValue(RCmdOption.FILE, arguments[firstNonOptionArgIndex]);
            }
        }
        String defaultPackagesArg = options.getString(RCmdOption.DEFAULT_PACKAGES);
        String defaultPackagesEnv = System.getenv("R_DEFAULT_PACKAGES");
        if (defaultPackagesArg == null && defaultPackagesEnv == null) {
            defaultPackagesArg = "datasets,utils,grDevices,graphics,stats";
        }
        if (defaultPackagesEnv == null) {
            options.setValue(RCmdOption.DEFAULT_PACKAGES, defaultPackagesArg);
        }
        // copy up to non-option args
        int rx = 1;
        while (rx < firstNonOptionArgIndex) {
            adjArgs.add(arguments[rx]);
            rx++;
        }
        if (options.getString(RCmdOption.FILE) != null) {
            adjArgs.add("--file=" + options.getString(RCmdOption.FILE));
            rx++; // skip over file arg
            firstNonOptionArgIndex++;
        }

        if (firstNonOptionArgIndex < resultArgsLength) {
            adjArgs.add("--args");
            while (rx < resultArgsLength) {
                adjArgs.add(arguments[rx++]);
            }
        }
        return adjArgs.toArray(new String[adjArgs.size()]);
    }

    public static void main(String[] args) {
        System.exit(doMain(RCommand.prependCommand(args), null, System.in, System.out, System.err));
        throw RCommand.fatal("should not reach here");
    }

    public static int doMain(String[] args, String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        assert env == null : "re-enble environment variables";

        ArrayList<String> argsList = new ArrayList<>(Arrays.asList(args));
        RLauncher launcher = new RLauncher(Client.RSCRIPT) {
            @Override
            protected void printVersion() {
                System.out.print("R scripting front-end version ");
                System.out.println(RVersionNumber.FULL);
            }
        };
        Map<String, String> polyglotOptions = new HashMap<>();
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (launcher.parsePolyglotOption("R", polyglotOptions, arg)) {
                iter.remove();
            }
        }
        if (launcher.runPolyglotAction()) {
            return 0;
        }
        RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, argsList.toArray(new String[argsList.size()]), false);
        String[] arguments = preprocessRScriptOptions(launcher, options);

        ConsoleHandler consoleHandler = RCommand.createConsoleHandler(options, false, inStream, outStream);
        try (Context context = Context.newBuilder().options(polyglotOptions).arguments("R", arguments).in(consoleHandler.createInputStream()).out(outStream).err(errStream).build()) {
            consoleHandler.setContext(context);
            return RCommand.readEvalPrint(context, consoleHandler);
        }
    }
}
