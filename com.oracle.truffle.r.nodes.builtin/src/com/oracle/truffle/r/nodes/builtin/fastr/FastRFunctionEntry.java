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

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The entry point to all the {@code fastr.xxx} functions called by the {@code .FastR} primitive.
 */
public class FastRFunctionEntry {
    public static Object invoke(String name, Object[] argValues, RBuiltinNode fastRNode) {
        Object arg0 = argValues[0];
        if (name.equals("typeof")) {
            return arg0.getClass().getSimpleName();
        } else if (name.equals("stacktrace")) {
            fastRNode.forceVisibility(false);
            return FastRStackTrace.printStackTrace(checkLogical(argValues[0], fastRNode));
        } else if (name.equals("debug")) {
            fastRNode.forceVisibility(false);
            FastROptions.debugUpdate(checkString(argValues[0], fastRNode));
            return RNull.instance;
        } else if (name.equals("inspect")) {
            fastRNode.forceVisibility(false);
            return FastRInspect.inspect(argValues);
        } else if (name.contains("pkgsource")) {
            switch (name) {
                case "pkgsource.pre":
                    return FastRPkgSource.preLoad(RRuntime.asString(arg0), RRuntime.asString(argValues[1]));

                case "pkgsource.post":
                    return FastRPkgSource.postLoad(RRuntime.asString(arg0), RRuntime.asString(argValues[1]), argValues[2]);

                case "pkgsource.done":
                    return FastRPkgSource.done();

            }
        } else if (name.equals("comparefilesizes")) {
            return FastRFileSizeCompare.compare(RRuntime.asString(arg0), RRuntime.asString(argValues[1]));
        } else if (name.equals("createcontext")) {
            return FastRCreateContext.createContext((RStringVector) RRuntime.asAbstractVector(arg0), RRuntime.fromLogical(checkLogical(argValues[1], fastRNode)));
        }
        // The remainder all take a func argument
        RFunction func = checkFunction(arg0, fastRNode);
        switch (name) {
            case "createcc":
                fastRNode.forceVisibility(false);
                return FastRCallCounting.createCallCounter(func);
            case "getcc":
                return FastRCallCounting.getCallCount(func);

            case "compile":
                return FastRCompile.compileFunction(func, checkLogical(argValues[1], fastRNode));

            case "dumptrees":
                fastRNode.forceVisibility(false);
                return FastRDumpTrees.dump(func, checkLogical(argValues[1], fastRNode), checkLogical(argValues[2], fastRNode));

            case "source":
                return FastRSource.debugSource(func);

            case "tree":
                return FastRTree.printTree(func, checkLogical(argValues[1], fastRNode));

            case "syntaxtree":
                fastRNode.forceVisibility(false);
                return FastRSyntaxTree.printTree(func, checkLogical(argValues[1], fastRNode));

            default:
                throw RInternalError.shouldNotReachHere();
        }

    }

    private static RFunction checkFunction(Object arg, RBuiltinNode fastRNode) throws RError {
        if (arg instanceof RFunction) {
            return (RFunction) arg;
        } else {
            throw RError.error(fastRNode.getEncapsulatingSourceSection(), RError.Message.TYPE_EXPECTED, "function");
        }
    }

    private static byte checkLogical(Object arg, RBuiltinNode fastRNode) throws RError {
        if (arg instanceof Byte) {
            return (byte) arg;
        } else {
            throw RError.error(fastRNode.getEncapsulatingSourceSection(), RError.Message.TYPE_EXPECTED, "logical");
        }
    }

    private static String checkString(Object arg, RBuiltinNode fastRNode) throws RError {
        String s = RRuntime.asString(arg);
        if (s != null) {
            return s;
        } else {
            throw RError.error(fastRNode.getEncapsulatingSourceSection(), RError.Message.TYPE_EXPECTED, "character");
        }
    }

}
