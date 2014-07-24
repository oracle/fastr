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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

// TODO Implement completely
@RBuiltin(name = "substitute", kind = PRIMITIVE, nonEvalArgs = {0})
public abstract class Substitute extends RBuiltinNode {
    private static final String[] PARAMETER_NAMES = new String[]{"expr", "env"};

    @Child Quote quote;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private Quote checkQuote() {
        if (quote == null) {
            quote = insert(QuoteFactory.create(new RNode[1], getBuiltin()));
        }
        return quote;
    }

    @Specialization
    public Object doSubstitute(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envMissing) {
        controlVisibility();
        // In the global environment, substitute behaves like quote
        REnvironment env = EnvFunctions.checkNonFunctionFrame(frame);
        if (env == REnvironment.globalEnv()) {
            return checkQuote().execute(expr);
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
            if (env == null) {
                env = EnvFunctions.frameToEnvironment(frame);
            }
            Object val = env.get(name);
            if (val == null) {
                // not bound in env
                return RDataFactory.createSymbol(name);
            } else {
                return val;
            }
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
    public String doSubstitute(VirtualFrame frame, RPromise expr, REnvironment env) {
        controlVisibility();
        throw RError.nyi(getEncapsulatingSourceSection(), "substitute(expr, env)");
    }
}
