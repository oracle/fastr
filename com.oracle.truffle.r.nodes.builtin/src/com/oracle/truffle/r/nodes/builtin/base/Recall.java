/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RWrapperBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.RecallFactory.RecallFunctionNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseNode.InlineVarArgsNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The {@code Recall} {@code .Internal}.
 */
@RBuiltin(name = "Recall", kind = INTERNAL, parameterNames = {"..."})
public final class Recall extends RWrapperBuiltinNode {

    public Recall(RNode[] arguments, RBuiltinFactory builtin, ArgumentsSignature suppliedSignature) {
        super(arguments, builtin, suppliedSignature);
    }

    @Override
    protected RNode createDelegate() {
        /*
         * TODO (chumer) ideally this node would just create an RCallNode and calls execute with
         * evaluated arguments on it. Until this is possible we use this rather hacky solution.
         */
        InlineVarArgsNode inlineVarArgs = (InlineVarArgsNode) getArguments()[0];
        return RCallNode.createCall(getSourceSection(), RecallFunctionNodeGen.create(),
                        CallArgumentsNode.create(false, false, new RNode[]{((WrapArgumentNode) inlineVarArgs.getVarargs()[0]).getOperand()}, inlineVarArgs.getSignature()), null);
    }

    protected abstract static class RecallFunctionNode extends RNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Specialization
        protected RFunction findFunction(VirtualFrame frame) {
            Frame cframe = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
            RFunction function = RArguments.getFunction(cframe);
            if (function == null) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.RECALL_CALLED_OUTSIDE_CLOSURE);
            }
            return function;
        }
    }

}
