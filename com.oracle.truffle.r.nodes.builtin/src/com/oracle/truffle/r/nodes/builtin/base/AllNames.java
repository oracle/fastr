/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.ArrayList;
import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

@RBuiltin(name = "all.names", kind = INTERNAL, parameterNames = {"expr", "functions", "max.names", "unique"}, behavior = PURE)
public abstract class AllNames extends RBuiltinNode.Arg4 {

    static {
        Casts casts = new Casts(AllNames.class);
        casts.arg("functions").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).replaceNA(RRuntime.LOGICAL_FALSE);
        casts.arg("max.names").asIntegerVector().findFirst(0).replaceNA(0);
        casts.arg("unique").asLogicalVector().findFirst(RRuntime.LOGICAL_TRUE).replaceNA(RRuntime.LOGICAL_TRUE);
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RExpression exprs, byte functions, int maxNames, byte unique) {
        AllNamesVisitor visitor = new AllNamesVisitor(functions == RRuntime.LOGICAL_TRUE, maxNames, unique == RRuntime.LOGICAL_TRUE);
        for (int i = 0; i < exprs.getLength(); i++) {
            Object expr = exprs.getDataAt(i);
            if (expr instanceof RSymbol) {
                visitor.accept(RSyntaxLookup.createDummyLookup(null, ((RSymbol) expr).getName(), false));
            } else if (expr instanceof RLanguage) {
                RLanguage lang = (RLanguage) expr;
                visitor.accept(RASTUtils.unwrap(lang.getRep()).asRSyntaxNode());
            }
        }
        return visitor.getResult();
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RLanguage expr, byte functions, int maxNames, byte unique) {
        AllNamesVisitor visitor = new AllNamesVisitor(functions == RRuntime.LOGICAL_TRUE, maxNames, unique == RRuntime.LOGICAL_TRUE);
        visitor.accept(RASTUtils.unwrap(expr.getRep()).asRSyntaxNode());
        return visitor.getResult();
    }

    /**
     * This visitor recursively traverses the syntax tree, collecting names according to the
     * semantics of the all.names builtin.
     */
    private static final class AllNamesVisitor extends RSyntaxVisitor<Void> {

        private final boolean functions;
        private final int maxNames;
        private final HashSet<String> unique;
        private final ArrayList<String> result;

        AllNamesVisitor(boolean functions, int maxNames, boolean unique) {
            this.functions = functions;
            this.maxNames = maxNames == -1 ? Integer.MAX_VALUE : maxNames;
            this.unique = unique ? new HashSet<>() : null;
            this.result = new ArrayList<>();
        }

        public RStringVector getResult() {
            return RDataFactory.createStringVector(result.toArray(new String[result.size()]), true);
        }

        @Override
        protected Void visit(RSyntaxCall element) {
            accept(element.getSyntaxLHS());
            for (RSyntaxElement arg : element.getSyntaxArguments()) {
                // unmatched arguments may still be null
                if (arg != null) {
                    accept(arg);
                }
            }
            return null;
        }

        @Override
        protected Void visit(RSyntaxConstant element) {
            return null;
        }

        @Override
        protected Void visit(RSyntaxLookup element) {
            if (functions || !element.isFunctionLookup()) {
                if (result.size() < maxNames) {
                    String identifier = element.getIdentifier();
                    if (unique == null || unique.add(identifier)) {
                        result.add(identifier);
                    }
                }
            }
            return null;
        }

        @Override
        protected Void visit(RSyntaxFunction element) {
            accept(RSyntaxLookup.createDummyLookup(RSyntaxNode.INTERNAL, "function", true));
            accept(element.getSyntaxBody());
            // functions do not recurse into the arguments
            return null;
        }
    }

    @Specialization
    @TruffleBoundary
    protected Object doAllNames(RSymbol symbol, @SuppressWarnings("unused") byte functions, int maxNames, @SuppressWarnings("unused") byte unique) {
        if (maxNames > 0 || maxNames == -1) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return RDataFactory.createStringVectorFromScalar(symbol.getName());
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doAllNames(Object expr, Object functions, Object maxNames, Object unique) {
        return RDataFactory.createEmptyStringVector();
    }
}
