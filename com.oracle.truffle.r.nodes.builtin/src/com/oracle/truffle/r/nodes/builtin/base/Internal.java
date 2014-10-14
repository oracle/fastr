/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The {@code .Internal} builtin. In {@code .Internal(func(args))} we have an AST where the
 * RCallNode.Uninitialized and the function child should be a {@link ReadVariableNode} node with
 * symbol {@code func}. We could just eval this, but eval has a lot of unnecessary overhead given
 * that we know that {@code func} is either a builtin or it's an error. We want to rewrite the AST
 * as if the {@code func} had been called directly.
 */
@RBuiltin(name = ".Internal", kind = PRIMITIVE, parameterNames = {"call"}, nonEvalArgs = {0})
public abstract class Internal extends RBuiltinNode {

    protected final BranchProfile errorProfile = new BranchProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    protected Object doInternal(@SuppressWarnings("unused") RMissing x) {
        errorProfile.enter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
    }

    @Specialization
    protected Object doInternal(VirtualFrame frame, RPromise x) {
        controlVisibility();
        RNode call = (RNode) x.getRep();
        RNode operand = ((WrapArgumentNode) call).getOperand();

        if (!(operand instanceof RootCallNode)) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INTERNAL);
        }

        RootCallNode callNode = (RootCallNode) operand;
        RNode func = callNode.getFunctionNode();
        Symbol symbol = ((ReadVariableNode) func).getSymbol();
        RFunction function = RContext.getEngine().lookupBuiltin(symbol.getName());
        if (function == null || function.getRBuiltin() != null && function.getRBuiltin().kind() != RBuiltinKind.INTERNAL) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INTERNAL, symbol);
        }

        // .Internal function is validated
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // Replace the original call; we can't just use callNode as that will cause recursion!
        RCallNode internalCallNode = RCallNode.createInternalCall(frame, call.getSourceSection(), callNode, function, symbol);
        this.getParent().replace(internalCallNode);
        // evaluate the actual builtin this time, next time we won't get here!
        Object result = internalCallNode.execute(frame);
        return result;
    }

}
