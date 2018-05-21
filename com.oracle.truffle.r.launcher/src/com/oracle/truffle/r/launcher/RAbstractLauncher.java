/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RCmdOptions.RCmdOption;
import java.io.File;

public abstract class RAbstractLauncher extends AbstractLanguageLauncher implements Closeable {

    private final Client client;
    protected final InputStream inStream;
    protected final OutputStream outStream;
    protected final OutputStream errStream;
    protected RCmdOptions options;
    private boolean useJVM;
    protected ConsoleHandler consoleHandler;
    protected Context context;

    RAbstractLauncher(Client client, String[] env, InputStream inStream, OutputStream outStream, OutputStream errStream) {
        this.client = client;
        assert env == null : "re-enble environment variables";
        this.inStream = inStream;
        this.outStream = outStream;
        this.errStream = errStream;
    }

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        boolean[] recognizedArgsIndices = new boolean[arguments.size()];
        this.options = RCmdOptions.parseArguments(client, arguments.toArray(new String[arguments.size()]), true, recognizedArgsIndices);
        List<String> unrecognizedArgs = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if ("--jvm.help".equals(arguments.get(i))) {
                // This condition should be removed when the Launcher handles --jvm.help
                // correctly.
                printJvmHelp();
                throw exit();
            } else if ("--jvm".equals(arguments.get(i))) {
                useJVM = true;
            } else if (!recognizedArgsIndices[i]) {
                unrecognizedArgs.add(arguments.get(i));
            }
        }

        return unrecognizedArgs;
    }

    protected abstract String[] getArguments();

    @Override
    protected void launch(Builder contextBuilder) {
        this.consoleHandler = RCommand.createConsoleHandler(options, null, inStream, outStream);
        this.context = contextBuilder.allowAllAccess(true).allowHostAccess(useJVM).arguments("R", getArguments()).in(consoleHandler.createInputStream()).out(
                        outStream).err(errStream).build();
        this.consoleHandler.setContext(context);
    }

    @Override
    protected String getLanguageId() {
        return "R";
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
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
        if (context != null) {
            context.close();
        }
    }

    // The following code is copied from org.graalvm.launcher.Launcher and it should be removed
    // when the Launcher handles --jvm.help correctly.

    private static void printJvmHelp() {
        System.out.print("JVM options:");
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
