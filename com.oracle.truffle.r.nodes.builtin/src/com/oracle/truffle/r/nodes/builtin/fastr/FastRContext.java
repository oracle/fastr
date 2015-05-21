/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.io.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;

public class FastRContext {
    static int create(RStringVector args, boolean shared) {
        String[] argsArray = args.getDataCopy();
        RContext current = RContext.getInstance();
        RContext.ConsoleHandler consoleHandler = current.getConsoleHandler();
        RContext newContext;
        if (shared) {
            newContext = RContext.getRRuntimeASTAccess().createShared(current, argsArray, consoleHandler);
        } else {
            newContext = RContext.getRRuntimeASTAccess().create(current, argsArray, consoleHandler);
        }
        return (int) newContext.getId();
    }

    static void print(int contextId) {
        @SuppressWarnings("unused")
        RContext context = checkContext(contextId);
        try {
            StdConnections.getStdout().writeString("context: " + contextId, true);
        } catch (IOException ex) {
            throw RError.error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    static void eval(RIntVector contexts, RStringVector exprs, boolean par) {
        if (par) {
            RContext.EvalThread[] threads = new RContext.EvalThread[contexts.getLength()];
            for (int i = 0; i < threads.length; i++) {
                RContext context = checkContext(contexts.getDataAt(i));
                threads[i] = new RContext.EvalThread(context, Source.fromText(exprs.getDataAt(i % threads.length), "<context_eval>"));
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
            try {
                for (int i = 0; i < threads.length; i++) {
                    threads[i].join();
                }
            } catch (InterruptedException ex) {

            }
        } else {
            for (int i = 0; i < contexts.getLength(); i++) {
                RContext context = checkContext(contexts.getDataAt(i));
                try {
                    context.activate();
                    context.getThisEngine().parseAndEval(Source.fromText(exprs.getDataAt(i), "<context_eval>"), true, false);
                } finally {
                    context.destroy();
                }
            }
        }
    }

    private static RContext checkContext(int contextId) throws RError {
        RContext context = RContext.find(contextId);
        if (context == null) {
            throw RError.error(RError.Message.GENERIC, "no context: " + contextId);
        } else {
            return context;
        }
    }
}
