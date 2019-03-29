/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.helpers.BrowserInteractNode;
import com.oracle.truffle.r.nodes.builtin.helpers.BrowserInteractNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.BrowserState;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.BrowserState.HelperState;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class BrowserFunctions {

    @RBuiltin(name = "browser", visibility = OFF, kind = PRIMITIVE, nonEvalArgs = 2, parameterNames = {"text", "condition", "expr", "skipCalls"}, behavior = COMPLEX)
    public abstract static class BrowserNode extends RBuiltinNode.Arg4 {

        // Note: this node could be used to trigger external debugger via the
        // DebuggerTags.AlwaysHalt tag, but for that to work, we would need to make this node
        // instrumentable and also all its parents all the way to RCallNode

        @Child private BrowserInteractNode browserInteractNode = BrowserInteractNodeGen.create();
        @Child private GetCallerFrameNode getCallerFrame;
        @Child private CastNode castExprNode = newCastBuilder().asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).map(toBoolean()).buildCastNode();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"", RNull.instance, RRuntime.LOGICAL_TRUE, 0};
        }

        static {
            Casts casts = new Casts(BrowserNode.class);
            casts.arg("skipCalls").asIntegerVector().findFirst(0);
        }

        @Specialization
        protected RNull browser(VirtualFrame frame, Object text, Object condition, RPromise expr, @SuppressWarnings("unused") int skipCalls) {
            RBaseNode exprExpr = expr.getClosure().getExpr();
            Object doBreak;
            if (exprExpr instanceof ConstantNode) {
                doBreak = ((ConstantNode) exprExpr).getValue();
            } else {
                doBreak = evalClosure(frame.materialize(), expr);
            }
            if ((boolean) castExprNode.doCast(doBreak)) {
                RContext curContext = RContext.getInstance();
                if (!curContext.isInitial() && curContext.getKind() == ContextKind.SHARE_ALL && curContext.getParent().getKind() == ContextKind.SHARE_NOTHING) {
                    return RNull.instance;
                }

                BrowserState browserState = curContext.stateInstrumentation.getBrowserState();
                try {
                    browserState.push(new HelperState(text, condition));
                    MaterializedFrame mFrame = frame.materialize();
                    RCaller caller = RArguments.getCall(mFrame);
                    RFunction fun = RArguments.getFunction(mFrame);
                    VirtualFrame actualFrame = frame;
                    if (fun != null && fun.isBuiltin() && fun.getRBuiltin().getBuiltinNodeClass() == BrowserNode.class) {
                        if (getCallerFrame == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            getCallerFrame = insert(GetCallerFrameNode.create());
                        }
                        actualFrame = getCallerFrame.execute(mFrame);
                        caller = caller.getPrevious();
                    }
                    doPrint(caller);
                    browserInteractNode.execute(actualFrame, caller);
                } finally {
                    browserState.pop();
                }
            }
            return RNull.instance;
        }

        @TruffleBoundary
        private static Object evalClosure(MaterializedFrame frame, RPromise expr) {
            return expr.getClosure().eval(frame);
        }

        @TruffleBoundary
        private static void doPrint(RCaller caller) {
            String callerString;
            if (caller == null || (!caller.isValidCaller() && caller.getDepth() == 0 && caller.getPrevious() == null)) {
                callerString = "top level";
            } else {
                callerString = RContext.getRRuntimeASTAccess().getCallerSource(caller);
            }
            RContext.getInstance().getConsole().printf("Called from: %s%n", callerString);
        }
    }

    protected static void casts(Class<? extends RBuiltinNode> builtinClass) {
        Casts casts = new Casts(builtinClass);
        casts.arg("n").asIntegerVector().findFirst(0).mustBe(gt(0), Message.POSITIVE_CONTEXTS);
    }

    /**
     * GnuR objects to indices <= 0 but allows positive indices that are out of range.
     */
    protected static HelperState getHelperState(int n) {
        BrowserState helperState = RContext.getInstance().stateInstrumentation.getBrowserState();
        return helperState.get(n);
    }

    @RBuiltin(name = "browserText", kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserText extends RBuiltinNode.Arg1 {

        static {
            casts(BrowserText.class);
        }

        @Specialization
        @TruffleBoundary
        protected Object browserText(int n) {
            return getHelperState(n).text;
        }
    }

    @RBuiltin(name = "browserCondition", kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserCondition extends RBuiltinNode.Arg1 {

        static {
            casts(BrowserCondition.class);
        }

        @Specialization
        @TruffleBoundary
        protected Object browserCondition(int n) {
            return getHelperState(n).condition;
        }
    }

    @RBuiltin(name = "browserSetDebug", visibility = OFF, kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class BrowserSetDebug extends RBuiltinNode.Arg1 {

        static {
            casts(BrowserSetDebug.class);
        }

        @Specialization
        @TruffleBoundary
        protected RNull browserSetDebug(@SuppressWarnings("unused") int n) {
            // TODO implement
            return RNull.instance;
        }
    }
}
