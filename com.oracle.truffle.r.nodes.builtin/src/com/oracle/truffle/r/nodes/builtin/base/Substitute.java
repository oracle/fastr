/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgPromiseNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

// TODO Implement completely
@RBuiltin(name = "substitute", kind = PRIMITIVE, parameterNames = {"expr", "env"}, nonEvalArgs = {0})
public abstract class Substitute extends RBuiltinNode {

    @Child private Quote quote;
    @Child private Substitute substituteRecursive;

    protected abstract Object executeObject(VirtualFrame frame, RPromise promise, Object env);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private Quote checkQuote() {
        if (quote == null) {
            quote = insert(QuoteFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
        }
        return quote;
    }

    private Substitute checkSubstituteRecursive() {
        if (substituteRecursive == null) {
            substituteRecursive = insert(SubstituteFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        return substituteRecursive;
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, RMissing envMissing) {
        controlVisibility();
        // In the global environment, substitute behaves like quote
        if (REnvironment.isGlobalEnvFrame(frame)) {
            return checkQuote().execute(frame, expr);
        }
        // We have to examine all the names in the expression:
        // 1. Ordinary variable, replace by value (if bound)
        // 2. promise (aka function argument): replace by expression associated with the promise
        // 3. ..., replace by contents of ...
        // TODO handle trees other than simple variables
        RNode node = (RNode) expr.getRep();
        RNode unode = ((WrapArgumentNode) node).getOperand();
        SourceSection ss = node.getSourceSection();
        if (unode instanceof ConstantNode) {
            return ((ConstantNode) node).getValue();
        } else if (unode instanceof ReadVariableNode) {
            String name = ss.toString();
            if (isFunctionArg(name)) {
                // Without real promises this is essentially impossible, as we need the expression
                // that was uttered in the caller of the function this substitute call occurs in,
                // and that may itself be a promise from an earlier call.
                // N.B. In many cases it is ok to just return the value, which is what we do to
                // allow progress on package loading
            }
            REnvironment env = REnvironment.frameToEnvironment(frame.materialize());
            Object val = env.get(name);
            if (val == null) {
                // not bound in env
                return RDataFactory.createSymbol(name);
            } else {
                return val;
            }
        } else if (unode instanceof UninitializedCallNode) {
            UninitializedCallNode callNode = (UninitializedCallNode) unode;
            ReadVariableNode funReadNode = (ReadVariableNode) callNode.getFunctionNode();
            REnvironment env = REnvironment.frameToEnvironment(frame.materialize());
            Object fun = env.get(funReadNode.getSymbol().getName());
            if (fun == null) {
                fun = RDataFactory.createSymbol(funReadNode.getSymbol().getName());
            }
            CallArgumentsNode arguments = callNode.getClonedArgs();
            UnrolledVariadicArguments unrolledArgs = arguments.executeFlatten(frame);
            RNode[] argValues = unrolledArgs.getArguments();
            Object[] listData = new Object[argValues.length + 1];
            listData[0] = fun;
            for (int i = 0; i < argValues.length; i++) {
                if (argValues[i] instanceof VarArgPromiseNode) {
                    RPromise p = ((VarArgPromiseNode) argValues[i]).getPromise();
                    listData[i + 1] = checkSubstituteRecursive().executeObject(frame, p, envMissing);
                } else {
                    ReadVariableNode argReadNode = (ReadVariableNode) ((WrapArgumentNode) argValues[i]).getOperand();
                    Object arg = env.get(argReadNode.getSymbol().getName());
                    if (arg == null) {
                        arg = RDataFactory.createSymbol(argReadNode.getSymbol().getName());
                    }
                    listData[i + 1] = arg;
                }
            }
            return RDataFactory.createLanguage(RDataFactory.createExpression(RDataFactory.createList(listData)));
        } else {
            throw RError.nyi(getEncapsulatingSourceSection(), "substitute(expr), unsupported arg");
        }
    }

    /**
     * Locate the {@link FunctionDefinitionNode} that this call occurs in an check signature for
     * {code name}.
     */
    private boolean isFunctionArg(String name) {
        Node node = this;
        while (node != null && !(node instanceof FunctionDefinitionNode)) {
            node = node.getParent();
        }
        assert node != null;
        FunctionDefinitionNode fdNode = (FunctionDefinitionNode) node;
        Object[] paramNames = fdNode.getParameterNames();
        for (Object paramName : paramNames) {
            if (paramName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected String doSubstitute(VirtualFrame frame, RPromise expr, REnvironment env) {
        controlVisibility();
        throw RError.nyi(getEncapsulatingSourceSection(), "substitute(expr, env)");
    }
}
