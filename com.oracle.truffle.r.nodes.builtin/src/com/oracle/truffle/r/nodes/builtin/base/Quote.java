/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;

@RBuiltin(name = "quote", nonEvalArgs = 0, kind = PRIMITIVE, parameterNames = {"expr"}, behavior = PURE)
public abstract class Quote extends RBuiltinNode {

    protected static final int LIMIT = 3;

    public abstract Object execute(RPromise expr);

    protected static Object createLanguage(Closure closure) {
        return Utils.makeShared(RASTUtils.createLanguageElement(closure.getExpr().asRSyntaxNode()));
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LIMIT", guards = "cachedClosure == expr.getClosure()")
    protected Object quoteCached(RPromise expr,
                    @Cached("expr.getClosure()") Closure cachedClosure,
                    @Cached("createLanguage(cachedClosure)") Object language) {
        return language;
    }

    @Specialization(contains = "quoteCached")
    protected Object quote(RPromise expr) {
        return RASTUtils.createLanguageElement(expr.getClosure().getExpr().asRSyntaxNode());
    }
}
