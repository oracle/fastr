/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.RCallNode.LeafCallNode;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.nodes.instrument.wrappers.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The {@code .Internal} builtin. In {@code .Internal(func(args))} we have an AST where the
 * RCallNode.Uninitialized and the function child should be a {@link ReadVariableNode} node with
 * symbol {@code func}. We want to rewrite the AST as if the {@code func} had been called directly.
 * However, we must do this in a non-destructive way otherwise deparsing and serialization will
 * fail.
 *
 * A note on {@link RInstrumentableNode}. Since both the {@code .Internal} and the argument are
 * {@link RCallNode}s both may have been wrapped. The call to {@link RASTUtils#unwrap} will go
 * through any {@link RNodeWrapper} and the rewrite will remove one level of wrapping. However the
 * parent of the the {@code .Internal}, which will be an {@link RNodeWrapper}, will remain so any
 * instrumentation at that level will remain in place.
 */
@NodeInfo(cost = NodeCost.NONE)
@RBuiltin(name = ".Internal", kind = PRIMITIVE, parameterNames = {"call"}, nonEvalArgs = 0)
public abstract class Internal extends RBuiltinNode {

    protected final BranchProfile errorProfile = BranchProfile.create();

    @Child private LeafCallNode builtinCallNode;
    @CompilationFinal private RFunction builtinFunction;

    @Specialization
    protected Object doInternal(@SuppressWarnings("unused") RMissing x) {
        errorProfile.enter();
        throw RError.error(this, RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
    }

    @Specialization
    protected Object doInternal(VirtualFrame frame, RPromise x) {
        controlVisibility();
        if (builtinCallNode == null) {
            RNode call = (RNode) x.getRep();
            RNode operand = (RNode) RASTUtils.unwrap(call);

            if (!(operand instanceof RCallNode)) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.INVALID_INTERNAL);
            }

            RCallNode callNode = (RCallNode) operand;
            RNode func = callNode.getFunctionNode();
            String name = ((ReadVariableNode) func).getIdentifier();
            RFunction function = RContext.lookupBuiltin(name);
            if (function == null || function.isBuiltin() && function.getRBuiltin().getKind() != RBuiltinKind.INTERNAL) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.NO_SUCH_INTERNAL, name);
            }

            // .Internal function is validated
            CompilerDirectives.transferToInterpreterAndInvalidate();
            builtinCallNode = insert(RCallNode.createInternalCall(frame, operand.getSourceSection(), callNode, function, name));
            builtinFunction = function;
        }
        return builtinCallNode.execute(frame, builtinFunction, null);
    }

}
