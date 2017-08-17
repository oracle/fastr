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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

import jline.console.UserInterruptException;

class RLauncher extends Launcher {

    private final Client client;

    RLauncher(Client client) {
        this.client = client;
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        RCmdOptions.printHelp(client);
    }

    @Override
    protected void printVersion() {
        RCmdOptions.printVersion();
    }

    @Override
    protected void collectArguments(Set<String> options) {
        for (RCmdOption option : RCmdOption.values()) {
            if (option.shortName != null) {
                options.add(option.shortName);
            }
            if (option.plainName != null) {
                options.add(option.plainName);
            }
        }
    }
}

/*
 * TODO:
 *
 * - create a replacement for "executor"
 *
 * - fatal needs to be handled more carefully in nested/spawned contexts
 */

/**
 * Emulates the (Gnu)R command as precisely as possible.
 */
public class RCommand {

    // CheckStyle: stop system..print check
    public static RuntimeException fatal(String message, Object... args) {
        System.out.println("FATAL: " + String.format(message, args));
        System.exit(-1);
        return null;
    }

    public static RuntimeException fatal(Throwable t, String message, Object... args) {
        t.printStackTrace();
        System.out.println("FATAL: " + String.format(message, args));
        System.exit(-1);
        return null;
    }

    public static void main(String[] args) {
        try {
            System.exit(doMain(prependCommand(args), null, System.in, System.out, System.err));
            // never returns
            throw fatal("main should never return");
        } catch (Throwable t) {
            throw fatal(t, "error during REPL execution");
        }
    }

    static String[] prependCommand(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "R";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    public static int doMain(String[] args, String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        StartupTiming.timestamp("Main Entered");
        ArrayList<String> argsList = new ArrayList<>(Arrays.asList(args));
        if (System.console() != null) {
            // add "--interactive" to force interactive mode
            boolean addArg = true;
            for (String arg : args) {
                if ("--interactive".equals(arg)) {
                    addArg = false;
                    break;
                }
            }
            if (addArg) {
                if (argsList.size() == 0) {
                    argsList.add("--interactive");
                } else {
                    argsList.add(1, "--interactive");
                }
            }
        }

        RLauncher launcher = new RLauncher(Client.R);
        Map<String, String> polyglotOptions = new HashMap<>();
        for (int i = 1; i < argsList.size(); i++) {
            String arg = argsList.get(i);
            if (launcher.parsePolyglotOption("R", polyglotOptions, arg)) {
                argsList.remove(i);
            }
        }
        if (launcher.runPolyglotAction()) {
            return 0;
        }
        RCmdOptions options = RCmdOptions.parseArguments(Client.R, argsList.toArray(new String[argsList.size()]), false);
        assert env == null || env.length == 0 : "re-enable setting environments";
        ConsoleHandler consoleHandler = createConsoleHandler(options, false, inStream, outStream);
        try (Context context = Context.newBuilder().options(polyglotOptions).arguments("R", options.getArguments()).in(consoleHandler.createInputStream()).out(outStream).err(errStream).build()) {
            consoleHandler.setContext(context);
            StartupTiming.timestamp("VM Created");
            StartupTiming.printSummary();
            return readEvalPrint(context, consoleHandler);
        }
    }

    public static ConsoleHandler createConsoleHandler(RCmdOptions options, boolean embedded, InputStream inStream, OutputStream outStream) {
        /*
         * Whether the input is from stdin, a file (-f), or an expression on the command line (-e)
         * it goes through the console. N.B. -f and -e can't be used together and this is already
         * checked.
         */
        RStartParams rsp = new RStartParams(options, false);
        String fileArgument = rsp.getFileArgument();
        if (fileArgument != null) {
            List<String> lines;
            try {
                /*
                 * If initial==false, ~ expansion will not have been done and the open will fail.
                 * It's harmless to always do it.
                 */
                File file = fileArgument.startsWith("~") ? new File(System.getProperty("user.home") + fileArgument.substring(1)) : new File(fileArgument);
                lines = Files.readAllLines(file.toPath());
            } catch (IOException e) {
                throw fatal("cannot open file '%s': No such file or directory", fileArgument);
            }
            return new StringConsoleHandler(lines, outStream);
        } else if (options.getStringList(RCmdOption.EXPR) != null) {
            List<String> exprs = options.getStringList(RCmdOption.EXPR);
            for (int i = 0; i < exprs.size(); i++) {
                exprs.set(i, unescapeSpace(exprs.get(i)));
            }
            return new StringConsoleHandler(exprs, outStream);
        } else {
            boolean isInteractive = options.getBoolean(RCmdOption.INTERACTIVE);
            if (!isInteractive && rsp.askForSave()) {
                fatal("you must specify '--save', '--no-save' or '--vanilla'");
            }
            if (embedded) {
                /*
                 * If we are in embedded mode, the creation of ConsoleReader and the ConsoleHandler
                 * should be lazy, as these may not be necessary and can cause hangs if stdin has
                 * been redirected.
                 */
                throw fatal("embedded mode disabled");
                // consoleHandler = new EmbeddedConsoleHandler(rsp, engine);
            } else {
                boolean useReadLine = !rsp.noReadline();
                if (useReadLine) {
                    return new JLineConsoleHandler(inStream, outStream, rsp.isSlave());
                } else {
                    return new DefaultConsoleHandler(inStream, outStream);
                }
            }
        }
    }

    /**
     * The standard R script escapes spaces to "~+~" in "-e" and "-f" commands.
     */
    static String unescapeSpace(String input) {
        return input.replace("~+~", " ");
    }

    private static final Source GET_ECHO = Source.newBuilder("R", ".Internal(getOption('echo'))", "<echo>").internal(true).buildLiteral();
    private static final Source QUIT_EOF = Source.newBuilder("R", ".Internal(quit('default', 0L, TRUE))", "<quit-on-eof>").internal(true).buildLiteral();
    private static final Source GET_PROMPT = Source.newBuilder("R", ".Internal(getOption('prompt'))", "<prompt>").internal(true).buildLiteral();
    private static final Source GET_CONTINUE_PROMPT = Source.newBuilder("R", ".Internal(getOption('continue'))", "<continue-prompt>").internal(true).buildLiteral();

    /**
     * The read-eval-print loop, which can take input from a console, command line expression or a
     * file. There are two ways the repl can terminate:
     * <ol>
     * <li>A {@code quit} command is executed successfully.</li>
     * <li>EOF on the input.</li>
     * </ol>
     * In case 2, we must implicitly execute a {@code quit("default, 0L, TRUE} command before
     * exiting. So,in either case, we never return.
     */
    public static int readEvalPrint(Context context, ConsoleHandler consoleHandler) {
        int lastStatus = 0;
        try {
            while (true) { // processing inputs
                boolean doEcho = doEcho(context);
                consoleHandler.setPrompt(doEcho ? getPrompt(context) : null);
                try {
                    String input = consoleHandler.readLine();
                    if (input == null) {
                        throw new EOFException();
                    }
                    String trInput = input.trim();
                    if (trInput.equals("") || trInput.charAt(0) == '#') {
                        // nothing to parse
                        continue;
                    }

                    String continuePrompt = null;
                    StringBuilder sb = new StringBuilder(input);
                    while (true) { // processing subsequent lines while input is incomplete
                        lastStatus = 0;
                        try {
                            context.eval(Source.newBuilder("R", sb.toString(), "<REPL>").interactive(true).buildLiteral());
                        } catch (PolyglotException e) {
                            if (continuePrompt == null) {
                                continuePrompt = doEcho ? getContinuePrompt(context) : null;
                            }
                            if (e.isIncompleteSource()) {
                                // read another line of input
                                consoleHandler.setPrompt(continuePrompt);
                                String additionalInput = consoleHandler.readLine();
                                if (additionalInput == null) {
                                    throw new EOFException();
                                }
                                sb.append('\n');
                                sb.append(additionalInput);
                                // The only continuation in the while loop
                                continue;
                            } else if (e.isExit()) {
                                // usually from quit
                                throw new ExitException(e.getExitStatus());
                            } else if (e.isHostException() || e.isInternalError()) {
                                // we continue the repl even though the system may be broken
                                lastStatus = 1;
                            } else if (e.isGuestException()) {
                                // drop through to continue REPL and remember last eval was an error
                                lastStatus = 1;
                            }
                        }
                        break;
                    }
                } catch (UserInterruptException e) {
                    // interrupted by ctrl-c
                }
            }
        } catch (EOFException e) {
            try {
                context.eval(QUIT_EOF);
            } catch (PolyglotException e2) {
                if (e2.isExit()) {
                    return e2.getExitStatus();
                }
                throw fatal(e, "error while calling quit");
            }
        } catch (ExitException e) {
            return e.code;
        }
        return lastStatus;
    }

    @SuppressWarnings("serial")
    private static final class ExitException extends RuntimeException {
        private final int code;

        ExitException(int code) {
            this.code = code;
        }
    }

    private static boolean doEcho(Context context) {
        try {
            return context.eval(GET_ECHO).asBoolean();
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw fatal(e, "error while retrieving echo");
        }
    }

    private static String getPrompt(Context context) {
        try {
            return context.eval(GET_PROMPT).asString();
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw fatal(e, "error while retrieving prompt");
        }
    }

    private static String getContinuePrompt(Context context) {
        try {
            return context.eval(GET_CONTINUE_PROMPT).asString();
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw fatal(e, "error while retrieving continue prompt");
        }
    }
}
