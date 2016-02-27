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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctionsFactory.ParentFrameNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * The {@code args} builtin.
 *
 * Unlike {@code formals}, a character string is not coerced in the closure, so we have to do that
 * here.
 *
 */
@RBuiltin(name = "args", kind = RBuiltinKind.INTERNAL, parameterNames = {"name"})
public abstract class Args extends RBuiltinNode {
    private static final FunctionStatementsNode nullBody = new FunctionStatementsNode(RSyntaxNode.INTERNAL, ConstantNode.create(RNull.instance));
    @Child private GetFunctions.Get getNode;
    @Child private FrameFunctions.ParentFrame parentFrameNode;

    @Specialization
    protected Object args(VirtualFrame frame, RAbstractStringVector funName) {
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create(new RNode[4], null, null));
            parentFrameNode = insert(ParentFrameNodeGen.create(new RNode[1], null, null));

        }
        return args((RFunction) getNode.execute(frame, funName, (REnvironment) parentFrameNode.execute(frame, 1), RType.Function.getName(), RRuntime.LOGICAL_TRUE));
    }

    @Specialization
    @TruffleBoundary
    protected RFunction args(RFunction fun) {
        controlVisibility();
        RRootNode rootNode = (RRootNode) fun.getTarget().getRootNode();
        FormalArguments formals = rootNode.getFormalArguments();
        FunctionBodyNode newBody = new FunctionBodyNode(SaveArgumentsNode.NO_ARGS, nullBody);
        String newDesc = "args(" + rootNode.getDescription() + ")";
        FunctionDefinitionNode newNode = new FunctionDefinitionNode(RSyntaxNode.INTERNAL, rootNode.getFrameDescriptor(), newBody, formals, newDesc, false, null);
        return RDataFactory.createFunction(newDesc, Truffle.getRuntime().createCallTarget(newNode), null, REnvironment.globalEnv().getFrame(), null, false);
    }

    @Specialization(guards = {"!isRFunction(fun)", "!isRAbstractStringVector(fun)"})
    protected Object args(@SuppressWarnings("unused") Object fun) {
        return RNull.instance;
    }
}
