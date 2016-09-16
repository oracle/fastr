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
package com.oracle.truffle.r.nodes.builtin.base.system;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;

public abstract class SystemFunctionFactory {
    private static String kind;
    private static SystemFunctionFactory theInstance;

    static {
        kind = System.getProperty("fastr.systemfunction.factory.kind", "Process");
        String className = "com.oracle.truffle.r.nodes.builtin.base.system." + kind + "SystemFunctionFactory";
        try {
            theInstance = (SystemFunctionFactory) Class.forName(className).newInstance();
        } catch (Exception ex) {
            // CheckStyle: stop system..print check
            Utils.rSuicide("Failed to instantiate class: " + className);
        }
    }

    @TruffleBoundary
    public static SystemFunctionFactory getInstance() {
        return theInstance;
    }

    /**
     * Implements the system {@code .Internal}. If {@code intern} is {@code true} the result is a
     * character vector containing the output of the process, with a {@code status} attribute
     * carrying the return code, else it is just the return code.
     *
     * {@code command} is a string with args separated by spaces with the first element enclosed in
     * single quotes.
     */
    public abstract Object execute(VirtualFrame frame, String command, boolean intern);

    @TruffleBoundary
    protected void log(String command, String useKind) {
        if (RContext.getInstance().stateREnvVars.getMap().get("FASTR_LOG_SYSTEM") != null) {
            System.out.printf("FastR system (%s): %s%n", useKind, command);
        }

    }

    @TruffleBoundary
    protected void log(String command) {
        log(command, kind);
    }

    /**
     * Encodes information collected by analyzing the command pass to {@code .Internal(system)}.
     */
    public static final class CommandInfo {
        public final String[] envDefs;
        public final String command;
        public final String[] args;

        public CommandInfo(String[] envDefs, String command, String[] args) {
            this.envDefs = envDefs;
            this.command = command;
            this.args = args;
        }
    }

    /**
     * Analyzes {@code command} to see if it is an R command that we can execute in a context.
     *
     * @return a {@link CommandInfo} object if the command can be executed in a context, else
     *         {@code null}.
     */
    @TruffleBoundary
    public static CommandInfo checkRCommand(String command) {
        CommandInfo commandInfo = null;
        String[] parts = command.split(" ");
        /* The actual command may be prefixed by environment variable settings of the form X=Y */
        int i = 0;
        while (parts[i].contains("=")) {
            i++;
        }
        String rcommand = isFastR(parts[i]);
        if (rcommand == null) {
            return null;
        } else {
            String[] envDefs = new String[i];
            if (i != 0) {
                System.arraycopy(parts, 0, envDefs, 0, i);
            }
            String[] args = new String[parts.length - i - 1];
            if (args.length > 0) {
                System.arraycopy(parts, i + 1, args, 0, args.length);
            }
            commandInfo = new CommandInfo(envDefs, command, args);
        }
        // check for and emulate selected R CMD cmd commands
        if (commandInfo.args.length > 0) {
            if (commandInfo.args[0].equals("CMD")) {
                switch (commandInfo.args[1]) {
                    case "INSTALL":
                        // INSTALL pipes in "tools:::.install_packages()"
                        // We use "-e tools:::.install_packages()" as its simpler
                        ArrayList<String> newArgsList = new ArrayList<>();
                        newArgsList.add("--no-restore");
                        newArgsList.add("--slave");
                        newArgsList.add("-e");
                        newArgsList.add("tools:::.install_packages()");
                        newArgsList.add("--args");
                        StringBuffer sb = new StringBuffer();
                        i = 2;
                        while (i < commandInfo.args.length) {
                            String arg = commandInfo.args[i];
                            if (arg.equals("<") || arg.contains(">")) {
                                break;
                            }
                            sb.append("nextArg");
                            sb.append(arg);
                            i++;
                        }
                        if (sb.length() > 0) {
                            newArgsList.add(sb.toString());
                        }
                        while (i < commandInfo.args.length) {
                            newArgsList.add(commandInfo.args[i]);
                            i++;
                        }
                        String[] newArgs = new String[newArgsList.size()];
                        newArgsList.toArray(newArgs);
                        commandInfo = new CommandInfo(commandInfo.envDefs, commandInfo.command, newArgs);
                        break;

                    default:
                        commandInfo = null;

                }
            }
        }
        return commandInfo;
    }

    /**
     * Returns {@code true} iff, {@code command} is {@code R} or {@code Rscript}.
     */
    private static String isFastR(String command) {
        // strip off quotes
        String xc = Utils.unShQuote(command);
        if (xc.equals("R") || xc.equals("Rscript")) {
            return xc;
        }
        // often it is an absolute path
        String rhome = REnvVars.rHome();
        if (isFullPath(rhome, "Rscript", xc)) {
            return "Rscript";
        }
        if (isFullPath(rhome, "R", xc)) {
            return "R";
        }
        return null;
    }

    private static boolean isFullPath(String rhome, String rcmd, String command) {
        try {
            String rpath = FileSystems.getDefault().getPath(rhome, "bin", rcmd).toString();
            String cpath = FileSystems.getDefault().getPath(command).toRealPath().toString();
            if (cpath.equals(rpath)) {
                return true;
            }
        } catch (IOException ex) {
            // should not happen but just return false
        }
        return false;
    }

}
