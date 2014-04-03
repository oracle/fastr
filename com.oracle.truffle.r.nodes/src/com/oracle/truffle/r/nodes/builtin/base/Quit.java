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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin({"quit", "q"})
public abstract class Quit extends RInvisibleBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"save", "status", "runLast"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create("default"), ConstantNode.create(0), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // status argument is at index 1
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false);
        return arguments;
    }

    @Specialization
    public Object doQuit(final String saveArg, int status, byte runLast) {
        controlVisibility();
        String save = saveArg;
        RContext.ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
        if (save.equals("default")) {
            if (RCmdOptions.NO_SAVE.getValue()) {
                save = "no";
            } else {
                if (consoleHandler.isInteractive()) {
                    save = "ask";
                } else {
                    // TODO options must be set, check
                }
            }
        }
        boolean doSave = false;
        if (save.equals("ask")) {
            W: while (true) {
                consoleHandler.setPrompt("");
                consoleHandler.print("Save workspace image? [y/n/c]: ");
                String response = consoleHandler.readLine();
                if (response == null) {
                    System.exit(status);
                }
                if (response.length() == 0) {
                    continue;
                }
                switch (response.charAt(0)) {
                    case 'c':
                        consoleHandler.setPrompt("> ");
                        return RNull.instance;
                    case 'y':
                        doSave = true;
                        break W;
                    case 'n':
                        doSave = false;
                        break W;
                    default:
                        continue;
                }
            }
        }

        if (doSave) {
            consoleHandler.println("Image saving is not implemented");
        }
        if (runLast != 0) {
            consoleHandler.println(".Last execution not implemented");
        }
        System.exit(status);
        return null;
    }

}
