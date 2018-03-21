/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.FunctionBuiltin;
import com.oracle.truffle.r.nodes.function.FunctionExpressionNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Construct a call object ({@link RPairList}) from a name and optional arguments.
 *
 * Does not perform argument matching for first parameter "name".
 */
@RBuiltin(name = "call", kind = PRIMITIVE, parameterNames = {"", "..."}, behavior = PURE)
public abstract class Call extends RBuiltinNode.Arg2 {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY};
    }

    static {
        Casts casts = new Casts(Call.class);
        casts.arg("").mustBe(stringValue(), RError.Message.FIRST_ARG_MUST_BE_STRING).asStringVector().findFirst();
    }

    @Specialization
    @TruffleBoundary
    protected RPairList call(String name, RArgsValuesAndNames args) {
        return makeCall(getRLanguage(), RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, name, true), args.getArguments(), args.getSignature());
    }

    @TruffleBoundary
    public static RPairList makeCall(TruffleRLanguage language, RSyntaxNode target, Object[] arguments, ArgumentsSignature signature) {
        assert arguments.length == signature.getLength();
        if (target instanceof RSyntaxLookup && "function".equals(((RSyntaxLookup) target).getIdentifier()) && arguments.length >= 2) {
            // this optimization is not strictly necessary, `function` builtin is functional too.
            FunctionExpressionNode function = FunctionBuiltin.createFunctionExpressionNode(language, arguments[0], arguments[1]);
            return RDataFactory.createLanguage(Closure.createLanguageClosure(function.asRNode()));
        } else {
            return makeCall0(target, arguments, signature);
        }
    }

    @TruffleBoundary
    private static RPairList makeCall0(RSyntaxNode target, Object[] arguments, ArgumentsSignature signature) {
        RSyntaxNode[] args = new RSyntaxNode[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = (RSyntaxNode) RASTUtils.createNodeForValue(arguments[i]);
        }
        RNode call = RContext.getASTBuilder().call(RSyntaxNode.LAZY_DEPARSE, target, RCodeBuilder.createArgumentList(signature, args)).asRNode();
        return RDataFactory.createLanguage(Closure.createLanguageClosure(call));
    }
}
