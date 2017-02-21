/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.methods.SubstituteDirect;
import com.oracle.truffle.r.nodes.RASTUtils;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.nodes.control.IfNode;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ENVIRONMENT_SPECIFIED;
import com.oracle.truffle.r.runtime.RSubstitute;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "substitute", kind = PRIMITIVE, parameterNames = {"expr", "env"}, nonEvalArgs = 0, behavior = COMPLEX)
public abstract class Substitute extends RBuiltinNode {

    @Child private Quote quote;

    static {
        Casts casts = new Casts(Substitute.class);
        casts.arg(1).allowNullAndMissing().defaultError(INVALID_ENVIRONMENT_SPECIFIED).mustBe(instanceOf(RAbstractListVector.class).or(instanceOf(REnvironment.class)));
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RMissing envMissing) {
        return doSubstituteWithEnv(expr, REnvironment.frameToEnvironment(frame.materialize()));
    }

    @Specialization
    protected Object doSubstitute(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") RNull env) {
        return doSubstituteWithEnv(expr, REnvironment.frameToEnvironment(frame.materialize()));
    }

    @Specialization
    protected Object doSubstitute(RPromise expr, REnvironment env) {
        return doSubstituteWithEnv(expr, env);
    }

    @Specialization(guards = {"list.getNames() == null || list.getNames().getLength() == 0"})
    protected Object doSubstitute(RPromise expr, @SuppressWarnings("unused") RList list) {
        return doSubstituteWithEnv(expr, SubstituteDirect.createNewEnvironment());
    }

    @Specialization(guards = {"list.getNames() != null", "list.getNames().getLength() > 0"})
    protected Object doSubstitute(RPromise expr, RList list,
                    @Cached("createList2EnvNode()") RList2EnvNode list2Env) {
        return doSubstituteWithEnv(expr, SubstituteDirect.createEnvironment(list, list2Env));
    }

    /**
     * Handles all above specializations. Transforms an AST into another AST, with the appropriate
     * substitutions. The incoming AST will either denote a symbol, constant or function call
     * (because in R everything else is a call). So in general, both the input and output is a call(
     * language element). E.g. {@link IfNode} is a special case because it is not (currently)
     * represented as a function, as are several other nodes.
     *
     * @param expr
     * @param env {@code null} if the {@code env} argument was {@code RMissing} to avoid always
     *            materializing the current frame.
     * @return in general an {@link RLanguage} instance, but simple cases could be a constant value
     *         or {@link RSymbol}
     */
    private Object doSubstituteWithEnv(RPromise expr, REnvironment env) {
        // In the global environment, substitute behaves like quote
        // TODO It may be too early to do this check, GnuR doesn't work this way (re promises)
        if (env == REnvironment.globalEnv()) {
            if (quote == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                quote = insert(QuoteNodeGen.create());
            }
            return quote.execute(expr);
        }

        // The "expr" promise comes from the no-evalarg aspect of the builtin,
        // so get the actual expression (AST) from that
        return RASTUtils.createLanguageElement(RSubstitute.substitute(env, expr.getRep()));
    }

    protected static RList2EnvNode createList2EnvNode() {
        return new RList2EnvNode(true);
    }

}
