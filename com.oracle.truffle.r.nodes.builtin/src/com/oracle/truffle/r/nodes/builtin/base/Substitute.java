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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.ResolvePromiseNode;
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
        // 1. Ordinary variable, replace by value (if bound), else unchanged
        // 2. promise (aka function argument): replace by expression associated with the promise
        // 3. ..., replace by contents of ...
        // TODO handle generalized expression trees
        RNode node = (RNode) expr.getRep();
        RNode unode = ((WrapArgumentNode) node).getOperand();
        if (unode instanceof ConstantNode) {
            return ((ConstantNode) node).getValue();
        } else if (unode instanceof ReadVariableNode) {
            // The common case (1, 2)
            String name = ((ReadVariableNode) unode).getSymbol().getName();
            if (unode instanceof ResolvePromiseNode) {
                // Case 2
                // Access the promise to get the expression
                RPromise promise = ((ResolvePromiseNode) unode).getPromise(frame);
                WrapArgumentNode rep = (WrapArgumentNode) promise.getRep();
                RNode paramNode = rep.getOperand();
                if (paramNode instanceof ConstantNode) {
                    return ((ConstantNode) paramNode).getValue();
                } else if (paramNode instanceof ReadVariableNode) {
                    return RDataFactory.createSymbol(((ReadVariableNode) paramNode).getSymbol().getName());
                } else {
                    // an expression
                    return RDataFactory.createLanguage(rep);
                }
            } else {
                // Case 1
                // N.B. do not need to look in parent
                REnvironment env = REnvironment.frameToEnvironment(frame.materialize());
                Object val = env.get(name);
                if (val == null) {
                    // not bound in env,
                    return RDataFactory.createSymbol(name);
                } else {
                    return val;
                }
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
                } else if (argValues[i] instanceof ConstantNode) {
                    listData[i + 1] = ((ConstantNode) argValues[i]).getValue();
                } else {
                    Object argNode = ((WrapArgumentNode) argValues[i]).getOperand();
                    Object arg;
                    if (argNode instanceof ReadVariableNode) {
                        ReadVariableNode argReadNode = (ReadVariableNode) argNode;
                        arg = env.get(argReadNode.getSymbol().getName());
                        if (arg == null) {
                            arg = RDataFactory.createSymbol(argReadNode.getSymbol().getName());
                        }
                    } else {
                        arg = ((ConstantNode) argNode).getValue();
                    }
                    listData[i + 1] = arg;
                }
            }
            if (unrolledArgs.getNames() != null) {
                String[] argNames = unrolledArgs.getNames();
                String[] names = new String[listData.length];
                names[0] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                for (int i = 1; i < names.length; i++) {
                    names[i] = argNames[i - 1] == null ? RRuntime.NAMES_ATTR_EMPTY_VALUE : argNames[i - 1];
                }
                return RDataFactory.createLanguage(RDataFactory.createList(listData, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR)), RLanguage.Type.FUNCALL);

            } else {
                return RDataFactory.createLanguage(RDataFactory.createList(listData), RLanguage.Type.FUNCALL);
            }
        } else {
            throw RError.nyi(getEncapsulatingSourceSection(), "substitute(expr), unsupported arg");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected String doSubstitute(VirtualFrame frame, RPromise expr, REnvironment env) {
        controlVisibility();
        throw RError.nyi(getEncapsulatingSourceSection(), "substitute(expr, env)");
    }
}
