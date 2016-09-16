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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.base.system.ContextSystemFunctionFactoryFactory.ContextRSystemFunctionNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.system.ContextSystemFunctionFactoryFactory.ContextRscriptSystemFunctionNodeGen;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRContext;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRContextFactory;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * A variant that uses {@code .fastr.context.r} for R sub-processes and throws an error otherwise.
 * Used in systems that do not support {@code ProcessBuilder}.
 */
public class ContextSystemFunctionFactory extends SystemFunctionFactory {
    private abstract static class ContextSystemFunctionNode extends RBaseNode {
        public abstract Object execute(VirtualFrame frame, Object args, Object env, Object intern);

    }

    public abstract static class ContextRSystemFunctionNode extends ContextSystemFunctionNode {
        @Child FastRContext.R contextRNode;

        private void initContextRNode() {
            if (contextRNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                contextRNode = insert(FastRContextFactory.RNodeGen.create(null));
            }

        }

        @Specialization
        protected Object systemFunction(VirtualFrame frame, RAbstractStringVector args, RAbstractStringVector env, boolean intern) {
            initContextRNode();
            Object result = contextRNode.execute(frame, args, env, intern);
            return result;
        }
    }

    public abstract static class ContextRscriptSystemFunctionNode extends ContextSystemFunctionNode {
        @Child FastRContext.Rscript contextRscriptNode;

        private void initContextRscriptNode() {
            if (contextRscriptNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                contextRscriptNode = insert(FastRContextFactory.RscriptNodeGen.create(null));
            }

        }

        @Specialization
        protected Object systemFunction(VirtualFrame frame, RAbstractStringVector args, RAbstractStringVector env, boolean intern) {
            initContextRscriptNode();
            Object result = contextRscriptNode.execute(frame, args, env, intern);
            return result;
        }
    }

    @Override
    Object execute(VirtualFrame frame, String command, boolean intern) {
        log(command, "Context");
        CommandInfo commandInfo = checkRCommand(command);
        if (commandInfo != null) {
            // check for and emulate R CMD cmd
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
                            int i = 2;
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
                            commandInfo.args = newArgs;
                            break;

                        default:
                            throw cannotExecute(command);
                    }
                }
            }
            ContextSystemFunctionNode node = commandInfo.command.equals("R") ? ContextRSystemFunctionNodeGen.create() : ContextRscriptSystemFunctionNodeGen.create();
            Object result = node.execute(frame, RDataFactory.createStringVector(commandInfo.args, RDataFactory.COMPLETE_VECTOR),
                            RDataFactory.createStringVector(commandInfo.envDefs, RDataFactory.COMPLETE_VECTOR), intern);
            return result;
        } else if (isMv(command)) {
            return 0;
        } else {
            throw cannotExecute(command);
        }
    }

    private static boolean isMv(String command) {
        String[] parts = command.split(" ");
        if (Utils.unShQuote(parts[0]).equals("mv")) {
            // during package installation backup, R uses mv to rename a directory
            try {
                Files.move(Paths.get(Utils.unShQuote(parts[1])), Paths.get(Utils.unShQuote(parts[2])));
            } catch (IOException ex) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static RError cannotExecute(String command) throws RError {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, command + " cannot be executed in a context");
    }

    private static class CommandInfo {
        private String[] envDefs;
        private String command;
        private String[] args;
    }

    @TruffleBoundary
    private static CommandInfo checkRCommand(String command) {
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
            CommandInfo result = new CommandInfo();
            result.command = rcommand;
            result.envDefs = new String[i];
            if (i != 0) {
                System.arraycopy(parts, 0, result.envDefs, 0, i);
            }
            result.args = new String[parts.length - i - 1];
            if (result.args.length > 0) {
                System.arraycopy(parts, i + 1, result.args, 0, result.args.length);
            }
            return result;
        }

    }

}
