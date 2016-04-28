/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctions;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctionsFactory.TopEnvNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctions;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctionsFactory.ParentFrameNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctions;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.TraceFunctions;
import com.oracle.truffle.r.nodes.builtin.base.TraceFunctionsFactory.PrimTraceNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.TraceFunctionsFactory.PrimUnTraceNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This a FastR-specific version of the standard {@code trace} function which uses the
 * instrumentation framework to implement the trace semantics instead of the standard (GnuR)
 * approach of creating a modified function.
 *
 * The only required argument is the function, either as a closure value or a string.
 */
public class FastRTrace {

    protected abstract static class Helper extends RBuiltinNode {
        @Child private GetFunctions.Get getNode;
        @Child private EnvFunctions.TopEnv topEnv;
        @Child private FrameFunctions.ParentFrame parentFrame;

        protected Object getWhere(VirtualFrame frame) {
            if (topEnv == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                topEnv = insert(TopEnvNodeGen.create(null));
                parentFrame = insert(ParentFrameNodeGen.create(null));
            }
            return topEnv.execute(frame, parentFrame.execute(frame, 1), RNull.instance);
        }

        protected Object getFunction(VirtualFrame frame, Object what, Object where) {
            if (getNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNode = insert(GetNodeGen.create(null));
            }
            return getNode.execute(frame, what, where, RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        }

        protected void checkWhat(Object what) {
            if (what == RMissing.instance) {
                throw RError.error(this, RError.Message.ARGUMENT_MISSING, "what");
            }
        }

        protected RFunction checkFunction(Object what) {
            if (what instanceof RFunction) {
                RFunction func = (RFunction) what;
                if (func.isBuiltin()) {
                    throw RError.error(this, RError.Message.GENERIC, "builtin functions cannot be traced");
                } else {
                    return func;
                }
            } else {
                throw RError.error(this, RError.Message.ARG_MUST_BE_CLOSURE);
            }
        }

    }

    @RBuiltin(name = ".fastr.trace", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"what", "tracer", "exit", "at", "print", "signature", "where"})
    public abstract static class Trace extends Helper {

        @Child private TraceFunctions.PrimTrace primTrace;

        @Specialization
        protected Object trace(VirtualFrame frame, Object whatObj, Object tracer, Object exit, Object at, Object print, Object signature, Object whereObj) {
            controlVisibility();
            Object what = whatObj;
            checkWhat(what);
            Object where = whereObj;
            if (where == RMissing.instance) {
                where = getWhere(frame);
            }
            String funcName = RRuntime.asString(what);
            if (funcName != null) {
                what = getFunction(frame, what, where);
            }
            RFunction func = checkFunction(what);

            if (tracer == RMissing.instance && exit == RMissing.instance && at == RMissing.instance && print == RMissing.instance && signature == RMissing.instance) {
                // simple case, nargs() == 1
                if (primTrace == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    primTrace = insert(PrimTraceNodeGen.create(null));
                }
                return primTrace.execute(frame, func);
            } else {
                complexCase(func, tracer, exit, what, print, signature);
            }

            // supposed to return the function name
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        private void complexCase(RFunction func, Object tracer, Object exit, Object at, Object print, Object signature) {
            // the complex case
            RSyntaxNode tracerNode;
            if (tracer instanceof RFunction) {
                tracerNode = RASTUtils.createCall(tracer, false, ArgumentsSignature.empty(0));
            } else if (tracer instanceof RLanguage) {
                tracerNode = ((RLanguage) tracer).getRep().asRSyntaxNode();
            } else {
                throw RError.error(this, RError.Message.GENERIC, "tracer is unexpected type");
            }
            RContext.getRRuntimeASTAccess().enableStatementTrace(func, tracerNode, at);
        }

    }

    @RBuiltin(name = ".fastr.untrace", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"what", "signature", "where"})
    public abstract static class Untrace extends Helper {

        @Child private TraceFunctions.PrimUnTrace primUnTrace;

        @Specialization
        protected Object untrace(VirtualFrame frame, Object whatObj, Object signature, Object whereObj) {
            forceVisibility(false);
            Object what = whatObj;
            checkWhat(what);
            Object where = whereObj;
            if (where == RMissing.instance) {
                where = getWhere(frame);
            }
            String funcName = RRuntime.asString(what);
            if (funcName != null) {
                what = getFunction(frame, what, where);
            }
            RFunction func = checkFunction(what);
            if (signature == RMissing.instance) {
                if (primUnTrace == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    primUnTrace = insert(PrimUnTraceNodeGen.create(null));
                }
                primUnTrace.execute(frame, func);
            } else {
                throw RError.nyi(this, "method tracing");
            }
            // supposed to return the function name
            return RNull.instance;
        }
    }
}
