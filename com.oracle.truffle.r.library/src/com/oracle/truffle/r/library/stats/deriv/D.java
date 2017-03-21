/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.stats.deriv;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

public abstract class D extends RExternalBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(D.class);
        casts.arg(0, "expr").mustBe(instanceOf(RExpression.class).or(instanceOf(RLanguage.class)).or(instanceOf(RSymbol.class)).or(numericValue()).or(complexValue()),
                        RError.Message.INVALID_EXPRESSION_TYPE, typeName());
        casts.arg(1, "namevec").mustBe(stringValue()).asStringVector().mustBe(notEmpty(), RError.Message.GENERIC, "variable must be a character string").shouldBe(size(1),
                        RError.Message.ONLY_FIRST_VARIABLE_NAME).findFirst();
    }

    public static D create() {
        return DNodeGen.create();
    }

    protected static boolean isConstant(Object expr) {
        return !(expr instanceof RLanguage || expr instanceof RExpression || expr instanceof RSymbol);
    }

    @Specialization(guards = "isConstant(expr)")
    @TruffleBoundary
    protected Object doD(Object expr, String var) {
        return doD(ConstantNode.create(expr), var);
    }

    @Specialization
    @TruffleBoundary
    protected Object doD(RSymbol expr, String var) {
        return doD(RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, expr.getName(), false), var);
    }

    @Specialization
    @TruffleBoundary
    protected Object doD(RLanguage expr, String var) {
        return doD((RSyntaxElement) expr.getRep(), var);
    }

    @Specialization
    @TruffleBoundary
    protected Object doD(RExpression expr, String var,
                    @Cached("create()") D dNode) {
        if (expr.getLength() == 0) {
            return RRuntime.DOUBLE_NA;
        }
        return dNode.execute(expr.getDataAt(0), var);
    }

    private static Object doD(RSyntaxElement elem, String var) {
        RSyntaxVisitor<RSyntaxElement> vis = new DerivVisitor(var);
        RSyntaxElement dExpr = vis.accept(elem);
        dExpr = Deriv.addParens(dExpr);
        return RASTUtils.createLanguageElement(dExpr);
    }
}
