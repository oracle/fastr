/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

/**
 * Emulates the (Gnu)Rscript command as precisely as possible. in GnuR, Rscript is a genuine wrapper
 * to R, as evidenced by the script {@code print(commandArgs())}. We don't implement it quite that
 * way but the effect is similar.
 *
 */
public final class RscriptCommand extends RAbstractLauncher {

    private String[] rScriptArguments;

    RscriptCommand(String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        super(Client.RSCRIPT, env, inStream, outStream, errStream);
    }

    @Override
    protected void validateArguments(Map<String, String> polyglotOptions) {
        try {
            this.rScriptArguments = preprocessRScriptOptions(options);
        } catch (PrintHelp e) {
            printHelp(OptionCategory.USER);
        }
    }

    @Override
    protected String[] getArguments() {
        return rScriptArguments;
    }

    protected int execute(String[] args) {
        launch(args);
        if (context != null) {
            String fileOption = options.getString(RCmdOption.FILE);
            if (fileOption != null) {
                return executeFile(fileOption);
            } else {
                return RCommand.readEvalPrint(context, consoleHandler, null, true);
            }
        } else {
            return 0;
        }
    }

    private int executeFile(String fileOption) {
        Source src;
        try {
            src = Source.newBuilder("R", new File(fileOption)).interactive(false).build();
        } catch (IOException ex) {
            System.err.printf("IO error while reading the source file '%s'.\nDetails: '%s'.", fileOption, ex.getLocalizedMessage());
            return 1;
        }
        try {
            context.eval(src);
            return 0;
        } catch (Throwable ex) {
            if (ex instanceof PolyglotException && ((PolyglotException) ex).isExit()) {
                return ((PolyglotException) ex).getExitStatus();
            }
            // Internal exceptions are reported by the engine already
            return 1;
        }
    }

    // CheckStyle: stop system..print check

    private static String[] preprocessRScriptOptions(RCmdOptions options) throws PrintHelp {
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
                throw new PrintHelp();
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

    @SuppressWarnings("serial")
    static class PrintHelp extends Exception {
    }

    public static void main(String[] args) {
        System.exit(doMain(RCommand.prependCommand(args), null, System.in, System.out, System.err));
        throw RCommand.fatal("should not reach here");
    }

    public static int doMain(String[] args, String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        try (RscriptCommand rcmd = new RscriptCommand(env, inStream, outStream, errStream)) {
            return rcmd.execute(args);
        }
    }
}
