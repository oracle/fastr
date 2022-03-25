/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.Argument;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "function", kind = PRIMITIVE, nonEvalArgs = 1, parameterNames = {"args", "body"}, behavior = COMPLEX)
public final class FunctionBuiltin extends RBuiltinNode.Arg2 {

    static {
        Casts.noCasts(FunctionBuiltin.class);
    }

    @Child private CreateAndExecuteFunctionExpr createFunNode;

    @Override
    public Object execute(VirtualFrame frame, Object args, Object body) {
        if (createFunNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            createFunNode = insert(new CreateAndExecuteFunctionExpr());
        }
        return createFunNode.execute(frame.materialize(), getRLanguage(), args, body);
    }

    public static FunctionBuiltin create() {
        return new FunctionBuiltin();
    }

    protected static final class CreateAndExecuteFunctionExpr extends TruffleBoundaryNode {
        @Child private FunctionExpressionNode funExprNode;

        @TruffleBoundary
        public Object execute(MaterializedFrame frame, TruffleRLanguage language, Object args, Object body) {
            funExprNode = insert(createFunctionExpressionNode(language, args, body));
            return funExprNode.execute(frame);
        }
    }

    public static FunctionExpressionNode createFunctionExpressionNode(TruffleRLanguage language, Object args, Object body) {
        List<Argument<RSyntaxNode>> finalArgs = RContext.getASTBuilder().getFunctionExprArgs(args);
        // TODO: Search for local variables?
        return (FunctionExpressionNode) RContext.getASTBuilder().function(language, RSyntaxNode.LAZY_DEPARSE, finalArgs, RASTUtils.createNodeForValue(body).asRSyntaxNode(), null, null);
    }
}
