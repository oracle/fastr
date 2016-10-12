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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctionsFactory.ParentFrameNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.SaveArgumentsNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * The {@code args} builtin.
 *
 * Unlike {@code formals}, a character string is not coerced in the closure, so we have to do that
 * here.
 *
 */
@RBuiltin(name = "args", kind = INTERNAL, parameterNames = {"name"}, behavior = COMPLEX)
public abstract class Args extends RBuiltinNode {

    @Child private GetFunctions.Get getNode;
    @Child private FrameFunctions.ParentFrame parentFrameNode;

    @Specialization
    protected Object args(VirtualFrame frame, RAbstractStringVector funName) {
        if (funName.getLength() == 0) {
            return RNull.instance;
        }
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create());
            parentFrameNode = insert(ParentFrameNodeGen.create());
        }
        return args((RFunction) getNode.execute(frame, funName.getDataAt(0), parentFrameNode.execute(frame, 1), RType.Function.getName(), true));
    }

    @Specialization
    @TruffleBoundary
    protected Object args(RFunction fun) {
        if (fun.isBuiltin()) {
            return RNull.instance;
        }
        RRootNode rootNode = (RRootNode) fun.getTarget().getRootNode();
        FormalArguments formals = rootNode.getFormalArguments();
        String newDesc = "args(" + rootNode.getDescription() + ")";
        FunctionDefinitionNode newNode = FunctionDefinitionNode.create(RSyntaxNode.EAGER_DEPARSE, rootNode.getFrameDescriptor(), null, SaveArgumentsNode.NO_ARGS,
                        ConstantNode.create(RSyntaxNode.EAGER_DEPARSE, RNull.instance), formals, newDesc, null);
        RDeparse.ensureSourceSection(newNode);
        return RDataFactory.createFunction(newDesc, Truffle.getRuntime().createCallTarget(newNode), null, REnvironment.globalEnv().getFrame());
    }

    @Specialization(guards = {"!isRFunction(fun)", "!isRAbstractStringVector(fun)"})
    protected Object args(@SuppressWarnings("unused") Object fun) {
        return RNull.instance;
    }
}
