/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RAllNames;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "all.names", kind = INTERNAL, parameterNames = {"expr", "function", "max.names", "unique"})
public abstract class AllNames extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(1);
        casts.toInteger(2);
        casts.toLogical(3);
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RExpression exprs, byte functions, int maxNames, byte unique) {
        RAllNames.State state = createState(functions, maxNames, unique);
        for (int i = 0; i < exprs.getLength(); i++) {
            Object expr = exprs.getDataAt(i);
            if (expr instanceof RSymbol) {
                state.addName(((RSymbol) expr).getName());
            } else if (expr instanceof RLanguage) {
                RLanguage lang = (RLanguage) expr;
                doAST(RASTUtils.unwrap(lang.getRep()), state);
            }
        }
        return RDataFactory.createStringVector(state.getNames(), RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RLanguage expr, byte functions, int maxNames, byte unique) {
        RAllNames.State state = createState(functions, maxNames, unique);
        doAST(RASTUtils.unwrap(expr.getRep()), state);
        return RDataFactory.createStringVector(state.getNames(), RDataFactory.COMPLETE_VECTOR);
    }

    private static void doAST(RBaseNode expr, RAllNames.State state) {
        expr.asRSyntaxNode().allNamesImpl(state);
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RSymbol symbol, @SuppressWarnings("unused") byte functions, int maxNames, @SuppressWarnings("unused") byte unique) {
        if (maxNames == 0) {
            return RDataFactory.createStringVector(0);
        } else {
            return RDataFactory.createStringVectorFromScalar(symbol.getName());
        }
    }

    private static RAllNames.State createState(byte functions, int maxNames, byte unique) {
        return RAllNames.State.create(RRuntime.fromLogical(functions), maxNames, RRuntime.fromLogical(unique));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doAllNames(Object expr, Object functions, Object maxNames, Object unique) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
