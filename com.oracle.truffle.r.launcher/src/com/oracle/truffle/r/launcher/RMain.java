/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.r.common.PrintHelp;
import com.oracle.truffle.r.common.PrintVersion;
import com.oracle.truffle.r.common.RCmdOptions;
import com.oracle.truffle.r.common.StartupTiming;
import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.common.RCmdOptions.Client;
import com.oracle.truffle.r.common.RCmdOptions.RCmdOption;
import com.oracle.truffle.r.launcher.REPL.REngineExecutor;

/**
 * Main entry point for the R engine. The first argument must be either {@code 'R'} or
 * {@code 'Rscript'} and decides which of the two R commands will be run.
 */
public final class RMain extends AbstractLanguageLauncher implements Closeable {

    public static void main(String[] args) {
        new RMain().launch(args);
    }

    public static int runR(String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream, int timeoutSecs) {
        return runROrRScript("R", args, inStream, outStream, errStream, timeoutSecs);
    }

    public static int runRscript(String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream, int timeoutSecs) {
        return runROrRScript("Rscript", args, inStream, outStream, errStream, timeoutSecs);
    }

    private static int runROrRScript(String command, String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream, int timeoutSecs) {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = command;
        try (RMain cmd = new RMain(false, inStream, outStream, errStream, timeoutSecs)) {
            cmd.launch(newArgs);
            return cmd.execute();
        }
    }

    protected final InputStream inStream;
    protected final OutputStream outStream;
    protected final OutputStream errStream;
    protected final int timeoutSecs;

    /**
     * In launcher mode {@link #launch(String[])} runs the command and uses {@link System#exit(int)}
     * to terminate and return the status. In non-launcher mode, {@link #launch(String[])} only
     * prepares the {@link Context} and then {@link #execute()} executes the command itself and
     * returns the status.
     */
    protected final boolean launcherMode;

    private Client client;
    private RCmdOptions options;
    private ConsoleHandler consoleHandler;
    private String[] rArguments;
    private Context preparedContext; // to transfer between launch and execute when !launcherMode

    private RMain(boolean launcherMode, InputStream inStream, OutputStream outStream, OutputStream errStream, int timeoutSecs) {
        this.launcherMode = launcherMode;
        this.inStream = inStream;
        this.outStream = outStream;
        this.errStream = errStream;
        this.timeoutSecs = timeoutSecs;
    }

    public RMain() {
        this.launcherMode = true;
        this.inStream = System.in;
        this.outStream = System.out;
        this.errStream = System.err;
        this.timeoutSecs = 0;
    }

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        boolean[] recognizedArgsIndices = new boolean[arguments.size()];
        options = RCmdOptions.parseArguments(arguments.toArray(new String[arguments.size()]), true, recognizedArgsIndices);
        client = options.getClient();

        if (options.getClient() == null) {
            System.err.printf("RMain: the first argument must be either 'R' or 'Rscript'. Given arguments: %s\n.", String.join(",", arguments.toArray(new String[0])));
            System.err.println("If you did not run RMain class explicitly, then this is a bug in launcher script, please report it at http://github.com/oracle/fastr.");
            if (launcherMode) {
                System.exit(1);
            }
            return arguments;
        }

        if (System.console() != null && client == Client.R) {
            options.addInteractive();
        }

        List<String> unrecognizedArgs = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (!recognizedArgsIndices[i]) {
                unrecognizedArgs.add(arguments.get(i));
            }
        }
        return unrecognizedArgs;
    }

    @Override
    protected void validateArguments(Map<String, String> polyglotOptions) {
        if (options == null) {
            // preprocessArguments did not set the value
            return;
        }
        try {
            rArguments = client.processOptions(options);
        } catch (PrintHelp e) {
            printHelp(OptionCategory.USER);
        } catch (PrintVersion e) {
            System.out.println(client.getHelpMessage());
        }
    }

    @Override
    protected void launch(Builder contextBuilderIn) {
        StartupTiming.timestamp("RMain.launch");
        assert client != null;
        if (rArguments == null) {
            // validateArguments did not set the value
            return;
        }
        this.consoleHandler = ConsoleHandler.createConsoleHandler(options, null, inStream, outStream);
        Builder contextBuilder = contextBuilderIn;

        boolean isLLVMBackEnd = false;
        boolean debugLLVMLibs = false;
        for (String rArg : this.rArguments) {
            if (rArg.startsWith("--R.BackEnd=llvm") || rArg.startsWith("--R.BackEndLLVM=")) {
                isLLVMBackEnd = true;
                continue;
            }
            if (rArg.startsWith("--R.DebugLLVMLibs")) {
                debugLLVMLibs = true;
            }
        }
        if (isLLVMBackEnd) {
            System.setProperty("fastr.rffi.factory.type", "llvm");
        }

        Context context;
        if (debugLLVMLibs) {
            context = preparedContext = contextBuilder.allowExperimentalOptions(true).option("inspect.HideErrors", "true").option("inspect.Internal", "true").option(
                            "llvm.enableLVI", "true").arguments("R",
                                            rArguments).in(
                                                            consoleHandler.createInputStream()).out(outStream).err(errStream).build();
        } else {
            context = preparedContext = contextBuilder.arguments("R", rArguments).in(consoleHandler.createInputStream()).out(outStream).err(errStream).build();
        }

        this.consoleHandler.setContext(context);
        if (launcherMode) {
            int exitCode = 0;
            REngineExecutor executor = new REngineExecutor();
            try {
                exitCode = execute(context, executor);
            } finally {
                executor.run(context::close);
                System.exit(exitCode);
            }
        }
    }

    protected int execute() {
        StartupTiming.timestamp("RMain.execute");
        if (preparedContext == null) {
            // launch did not set the value
            return 1;
        }
        REngineExecutor executor = new REngineExecutor();
        int result = execute(preparedContext, executor);
        StartupTiming.printSummary();
        return result;
    }

    protected int execute(Context context, REngineExecutor executor) {
        StartupTiming.timestamp("RMain.execute");
        String fileOption = options.getString(RCmdOption.FILE);
        File srcFile = null;
        if (fileOption != null) {
            if (client == Client.RSCRIPT) {
                return executeFile(options.getBoolean(RCmdOption.VERBOSE), context, fileOption);
            }
            srcFile = new File(fileOption);
        }
        int result = REPL.readEvalPrint(context, consoleHandler, srcFile, executor);
        StartupTiming.printSummary();
        return result;
    }

    @Override
    protected String getLanguageId() {
        return "R";
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        assert client != null;
        RCmdOptions.printHelp(client);
    }

    @Override
    protected void collectArguments(Set<String> opts) {
        for (RCmdOption option : RCmdOption.values()) {
            if (option.shortName() != null) {
                opts.add(option.shortName());
            }
            if (option.plainName() != null) {
                opts.add(option.plainName());
            }
        }
    }

    @Override
    protected String[] getDefaultLanguages() {
        return new String[]{getLanguageId(), "llvm"};
    }

    @Override
    public void close() {
        if (preparedContext != null) {
            preparedContext.close();
        }
    }

    private static int executeFile(boolean verbose, Context context, String fileOption) {
        if (verbose) {
            System.out.println("[launcher] Running file: " + fileOption);
        }
        Source src;
        try {
            src = Source.newBuilder("R", new File(fileOption)).interactive(false).build();
        } catch (IOException ex) {
            System.err.printf("IO error while reading the source file '%s'.\nDetails: '%s'.", fileOption, ex.getLocalizedMessage());
            return 1;
        }
        int result = 0;
        try {
            context.eval(src);
        } catch (PolyglotException e) {
            result = 1;
            if (e.isExit()) {
                // usually from quit
                result = e.getExitStatus();
            } else {
                if (verbose) {
                    System.err.println("[launcher] Error: " + e.getClass().getSimpleName() + "; " + e.getMessage());
                    e.printStackTrace();
                }
                if (!e.isInternalError() && (e.isHostException() || e.isGuestException())) {
                    // Note: Internal exceptions are reported by the engine already
                    REPL.handleError(null, context, e);
                }
            }
        } catch (Throwable ex) {
            // Internal exceptions are reported by the engine already
            System.err.println("[launcher] Internal Error: " + ex.getClass().getSimpleName() + "; " + ex.getMessage());
            ex.printStackTrace();
            result = 1;
        }
        if (verbose) {
            System.out.println("[launcher] Exiting with code: " + result);
        }
        return result;
    }
}
