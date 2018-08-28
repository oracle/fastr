/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;

/**
 * Main entry point for the R engine. The first argument must be either {@code 'R'} or
 * {@code 'Rscript'} and decides which of the two R commands will be run.
 */
public final class RMain extends AbstractLanguageLauncher implements Closeable {

    public static void main(String[] args) {
        new RMain().launch(args);
    }

    public static int runR(String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        return runROrRScript("R", args, inStream, outStream, errStream);
    }

    public static int runRscript(String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        return runROrRScript("Rscript", args, inStream, outStream, errStream);
    }

    private static int runROrRScript(String command, String[] args, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = command;
        try (RMain cmd = new RMain(false, inStream, outStream, errStream)) {
            cmd.launch(newArgs);
            return cmd.execute();
        }
    }

    /**
     * Tells this R launcher to not process the {@code --jvm} and {@code --jvm.help}. Normally such
     * arguments are processed by the native launcher, but if there is no native launcher, we need
     * to explicitly process them in this class since the Truffle launcher does not count on this
     * eventuality.
     */
    private static final boolean ignoreJvmArguments = "true".equals(System.getProperty("fastr.internal.ignorejvmargs"));

    protected final InputStream inStream;
    protected final OutputStream outStream;
    protected final OutputStream errStream;

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
    private boolean useJVM;
    private Context preparedContext; // to transfer between launch and execute when !launcherMode

    private RMain(boolean launcherMode, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        this.launcherMode = launcherMode;
        this.inStream = inStream;
        this.outStream = outStream;
        this.errStream = errStream;
    }

    private RMain() {
        this.launcherMode = true;
        this.inStream = System.in;
        this.outStream = System.out;
        this.errStream = System.err;
    }

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        String clientName = arguments.size() > 0 ? arguments.get(0).trim() : null;
        if ("R".equals(clientName)) {
            client = Client.R;
        } else if ("Rscript".equals(clientName)) {
            client = Client.RSCRIPT;
        } else {
            System.err.printf("RMain: the first argument must be either 'R' or 'Rscript'. Given was '%s'.\n", clientName);
            System.err.println("If you did not run RMain class explicitly, then this is a bug in launcher script, please report it at http://github.com/oracle/fastr.");
            if (launcherMode) {
                System.exit(1);
            }
            return arguments;
        }
        boolean[] recognizedArgsIndices = new boolean[arguments.size()];
        options = RCmdOptions.parseArguments(client, arguments.toArray(new String[arguments.size()]), true, recognizedArgsIndices);
        if (System.console() != null && client == Client.R) {
            options.addInteractive();
        }
        List<String> unrecognizedArgs = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (!ignoreJvmArguments && "--jvm.help".equals(arguments.get(i))) {
                // This condition should be removed when FastR always ships with native launcher
                // that handles this option for us
                printJvmHelp();
                throw exit();
            } else if (!ignoreJvmArguments && "--jvm".equals(arguments.get(i))) {
                useJVM = true;
            } else

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
        if (client == Client.RSCRIPT) {
            try {
                rArguments = preprocessRScriptOptions(options);
            } catch (PrintHelp e) {
                printHelp(OptionCategory.USER);
            }
        } else {
            rArguments = this.options.getArguments();
        }
    }

    @Override
    protected void launch(Builder contextBuilder) {
        assert client != null;
        if (rArguments == null) {
            // validateArguments did not set the value
            return;
        }
        this.consoleHandler = ConsoleHandler.createConsoleHandler(options, null, inStream, outStream);
        Builder contextBuilderAllowAll = contextBuilder.allowAllAccess(true);
        if (ignoreJvmArguments) {
            contextBuilderAllowAll = contextBuilderAllowAll.allowHostAccess(useJVM);
        }
        Context context = preparedContext = contextBuilderAllowAll.arguments("R", rArguments).in(consoleHandler.createInputStream()).out(outStream).err(errStream).build();
        this.consoleHandler.setContext(context);
        Source src = Source.newBuilder("R", ".fastr.set.consoleHandler", "<set-console-handler>").internal(true).buildLiteral();
        context.eval(src).execute(consoleHandler.getPolyglotWrapper());
        if (launcherMode) {
            try {
                System.exit(execute(context));
            } finally {
                context.close();
            }
        }
    }

    protected int execute() {
        if (preparedContext == null) {
            // launch did not set the value
            return 1;
        }
        return execute(preparedContext);
    }

    protected int execute(Context context) {
        String fileOption = options.getString(RCmdOption.FILE);
        File srcFile = null;
        if (fileOption != null) {
            if (client == Client.RSCRIPT) {
                return executeFile(context, fileOption);
            }
            srcFile = new File(fileOption);
        }
        return REPL.readEvalPrint(context, consoleHandler, srcFile, true);
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
            if (option.shortName != null) {
                opts.add(option.shortName);
            }
            if (option.plainName != null) {
                opts.add(option.plainName);
            }
        }
    }

    @Override
    protected String[] getDefaultLanguages() {
        if ("llvm".equals(System.getenv("FASTR_RFFI"))) {
            return new String[]{getLanguageId(), "llvm"};
        }
        return super.getDefaultLanguages();
    }

    @Override
    public void close() {
        if (preparedContext != null) {
            preparedContext.close();
        }
    }

    private static int executeFile(Context context, String fileOption) {
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
    public static RuntimeException fatal(String message, Object... args) {
        System.out.println("FATAL: " + String.format(message, args));
        System.exit(-1);
        return new RuntimeException();
    }

    public static RuntimeException fatal(Throwable t, String message, Object... args) {
        t.printStackTrace();
        System.out.println("FATAL: " + String.format(message, args));
        System.exit(-1);
        return null;
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
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    // The following code is copied from org.graalvm.launcher.Launcher and it should be removed
    // when the R launcher always ships native version that handles --jvm.help for us.

    private static void printJvmHelp() {
        System.out.println("JVM options:");
        printOption("--jvm.classpath <...>", "A " + File.pathSeparator + " separated list of classpath entries that will be added to the JVM's classpath");
        printOption("--jvm.D<name>=<value>", "Set a system property");
        printOption("--jvm.esa", "Enable system assertions");
        printOption("--jvm.ea[:<packagename>...|:<classname>]", "Enable assertions with specified granularity");
        printOption("--jvm.agentlib:<libname>[=<options>]", "Load native agent library <libname>");
        printOption("--jvm.agentpath:<pathname>[=<options>]", "Load native agent library by full pathname");
        printOption("--jvm.javaagent:<jarpath>[=<options>]", "Load Java programming language agent");
        printOption("--jvm.Xbootclasspath/a:<...>", "A " + File.pathSeparator + " separated list of classpath entries that will be added to the JVM's boot classpath");
        printOption("--jvm.Xmx<size>", "Set maximum Java heap size");
        printOption("--jvm.Xms<size>", "Set initial Java heap size");
        printOption("--jvm.Xss<size>", "Set java thread stack size");
    }

    private static void printOption(String option, String description, int indentation) {
        String indent = spaces(indentation);
        String desc = wrap(description != null ? description : "");
        String nl = System.lineSeparator();
        String[] descLines = desc.split(nl);
        int optionWidth = 45;
        if (option.length() >= optionWidth && description != null) {
            System.out.println(indent + option + nl + indent + spaces(optionWidth) + descLines[0]);
        } else {
            System.out.println(indent + option + spaces(optionWidth - option.length()) + descLines[0]);
        }
        for (int i = 1; i < descLines.length; i++) {
            System.out.println(indent + spaces(optionWidth) + descLines[i]);
        }
    }

    static void printOption(String option, String description) {
        printOption(option, description, 2);
    }

    private static String spaces(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }

    private static String wrap(String s) {
        final int width = 120;
        StringBuilder sb = new StringBuilder(s);
        int cursor = 0;
        while (cursor + width < sb.length()) {
            int i = sb.lastIndexOf(" ", cursor + width);
            if (i == -1 || i < cursor) {
                i = sb.indexOf(" ", cursor + width);
            }
            if (i != -1) {
                sb.replace(i, i + 1, System.lineSeparator());
                cursor = i;
            } else {
                break;
            }
        }
        return sb.toString();
    }

    // End of copied code
}
