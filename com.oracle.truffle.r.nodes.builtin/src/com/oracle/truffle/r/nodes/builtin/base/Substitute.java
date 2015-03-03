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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "substitute", kind = PRIMITIVE, parameterNames = {"expr", "env"}, nonEvalArgs = {0})
public abstract class Substitute extends RBuiltinNode {

    @Child private Quote quote;
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    protected abstract Object executeObject(VirtualFrame frame, RPromise promise, Object env);

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private Quote checkQuote() {
        if (quote == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            quote = insert(QuoteFactory.create(new RNode[1], getBuiltin(), getSuppliedSignature()));
        }
        return quote;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doSubstitute(Object expr, Object x) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ENVIRONMENT);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envMissing) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, null);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, REnvironment env) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, env);
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, RList list) {
        controlVisibility();
        return doSubstituteWithEnv(frame, expr, REnvironment.createFromList(attrProfiles, list, REnvironment.baseEnv()));
    }

    /**
     * Handles all above specializations. Transforms an AST into another AST, with the appropriate
     * substitutions. The incoming AST will either denote a symbol, constant or function call
     * (because in R everything else is a call). So in general, both the input and output is a call(
     * language element). E.g. {@link IfNode} is a special case because it is not (currently)
     * represented as a function, as are several other nodes.
     *
     * We clear the {@link SourceSection} on the substituted tree as it would need to be modified
     * anyway and GnuR appears to. E.g. {@code c(z+2)} when substituted prints as {@code z + 2}.
     *
     * As per {@code deparse}, it might make sense to have each node handle its own substitution
     * through an appropriate interface.
     *
     * @param frame
     * @param expr
     * @param envArg {@code null} if the {@code env} argument was {@code RMissing} to avoid always
     *            materializing the current frame.
     * @return in general an {@link RLanguage} instance, but simple cases could be a constant value
     *         or {@link RSymbol}
     */
    private Object doSubstituteWithEnv(VirtualFrame frame, RPromise expr, REnvironment envArg) {
        // In the global environment, substitute behaves like quote
        // TODO It may be too early to do this check, GnuR doesn't work this way (re promises)
        if (envArg == null && REnvironment.isGlobalEnvFrame(frame) || envArg == REnvironment.globalEnv()) {
            return checkQuote().execute(frame, expr);
        }
        // Check for missing env, which means current
        REnvironment env = envArg != null ? envArg : REnvironment.frameToEnvironment(frame.materialize());

        return doSubstituteInternal(expr, env);
    }

    @TruffleBoundary
    private static Object doSubstituteInternal(RPromise expr, REnvironment env) {
        // We have to examine all the names in the expression:
        // 1. Ordinary variable, replace by value (if bound), else unchanged
        // 2. promise (aka function argument): replace by expression associated with the promise
        // 3. ..., replace by contents of ... (if bound)

        // The "expr" promise comes from the no-evalarg aspect of the builtin,
        // so get the actual expression (AST) from that
        Node node = RASTUtils.unwrap(expr.getRep());
        // substitution is destructive so clone the tree
        RNode rNode = (RNode) NodeUtil.cloneNode(node);
        // NodeUtil.printTree(System.out, node);
        return RASTUtils.createLanguageElement(clearSourceSection(rNode.substitute(env)));
    }

    private static Node clearSourceSection(Node node) {
        node.accept(n -> {
            n.clearSourceSection();
            return true;
        });
        return node;
    }

}
