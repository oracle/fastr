/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.RecallNodeGen.RecallHelperNodeGen;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.call.CallerFrameClosureProvider;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.nodes.function.call.SlowPathExplicitCall;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * The {@code Recall} {@code .Internal}.
 */
@RBuiltin(name = "Recall", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"..."}, nonEvalArgs = {0}, behavior = COMPLEX)
public abstract class Recall extends RBuiltinNode.Arg1 {

    @Child private LocalReadVariableNode readArgs = LocalReadVariableNode.create(ArgumentsSignature.VARARG_NAME, false);

    @Child private GetCallerFrameNode callerFrame = GetCallerFrameNode.create();

    @Child private RecallHelper recallHelper = RecallHelperNodeGen.create();

    static {
        Casts.noCasts(Recall.class);
    }

    @ImportStatic(DSLConfig.class)
    abstract static class RecallHelper extends CallerFrameClosureProvider {

        public abstract Object execute(VirtualFrame virtualFrame, RFunction function, RArgsValuesAndNames actualArgs, Object orginalFrame);

        static FrameDescriptor getFrameDescriptor(Object frame) {
            return ((Frame) frame).getFrameDescriptor();
        }

        @Specialization(guards = "getFrameDescriptor(orginalFrame) == fd", limit = "getCacheSize(10)")
        public Object recallFastPath(VirtualFrame frame, RFunction function, RArgsValuesAndNames actualArgs, Object orginalFrame,
                        @SuppressWarnings("unused") @Cached("getFrameDescriptor(orginalFrame)") FrameDescriptor fd,
                        @Cached("create()") RExplicitCallNode explicitCallNode,
                        @Cached("create()") GetVisibilityNode getVisibilityNode,
                        @Cached("create()") SetVisibilityNode setVisibilityNode) {
            VirtualEvalFrame clonedFrame = createVirtualEvalFrame(frame, function, orginalFrame);
            Object res = explicitCallNode.call(clonedFrame, function, actualArgs);
            setVisibilityNode.execute(frame, getVisibilityNode.execute(clonedFrame));
            return res;
        }

        @Specialization(replaces = "recallFastPath")
        public Object recallSlowPath(VirtualFrame frame, RFunction function, RArgsValuesAndNames actualArgs, Object orginalFrame,
                        @Cached("create()") SlowPathExplicitCall slowPathexplicitCallNode) {
            VirtualEvalFrame clonedFrame = createVirtualEvalFrame(frame, function, orginalFrame);
            Object res = slowPathexplicitCallNode.execute(clonedFrame, null, null, function, actualArgs);
            setVisibilitySlowPath(frame.materialize(), clonedFrame);
            return res;
        }

        @TruffleBoundary
        private static void setVisibilitySlowPath(MaterializedFrame frame, VirtualEvalFrame clonedFrame) {
            SetVisibilityNode.executeSlowPath(frame, GetVisibilityNode.executeSlowPath(clonedFrame));
        }

        private VirtualEvalFrame createVirtualEvalFrame(VirtualFrame frame, RFunction function, Object orginalFrame) {
            RCaller clonedCaller = RCaller.createForPromise(RArguments.getCall((Frame) orginalFrame), RArguments.getCall(frame), null);
            return VirtualEvalFrame.create((MaterializedFrame) orginalFrame, function, getCallerFrameObject(frame), clonedCaller);
        }
    }

    @Specialization
    protected Object recall(VirtualFrame frame, @SuppressWarnings("unused") RArgsValuesAndNames args) {
        Frame cframe = callerFrame.execute(frame);
        RFunction function = RArguments.getFunction(cframe);
        if (function == null) {
            throw error(RError.Message.RECALL_CALLED_OUTSIDE_CLOSURE);
        }
        /*
         * The args passed to the Recall internal are ignored, they are always "...". Instead, this
         * builtin looks at the arguments passed to the surrounding function.
         */
        RArgsValuesAndNames actualArgs = (RArgsValuesAndNames) readArgs.execute(frame);

        Frame originalFrame = callerFrame.execute((VirtualFrame) cframe);
        return recallHelper.execute(frame, function, actualArgs, originalFrame);
    }
}
