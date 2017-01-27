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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "formals", kind = INTERNAL, parameterNames = {"fun"}, behavior = PURE)
public abstract class Formals extends RBuiltinNode {

    @SuppressWarnings("unused")
    @Specialization(limit = "3", guards = "fun == cachedFunction")
    protected Object formalsCached(RFunction fun,
                    @Cached("fun") RFunction cachedFunction,
                    @Cached("createFormals(fun)") Object formals) {
        return formals;
    }

    @Specialization(replaces = "formalsCached")
    protected Object formals(RFunction fun) {
        return createFormals(fun);
    }

    @Fallback
    protected Object formals(@SuppressWarnings("unused") Object fun) {
        // for anything that is not a function, GnuR returns NULL
        return RNull.instance;
    }

    @TruffleBoundary
    protected Object createFormals(RFunction fun) {
        if (fun.isBuiltin()) {
            return RNull.instance;
        }
        FunctionDefinitionNode fdNode = (FunctionDefinitionNode) fun.getTarget().getRootNode();
        if (fdNode.getParameterCount() == 0) {
            return RNull.instance;
        }
        FormalArguments formalArgs = fdNode.getFormalArguments();
        Object succ = RNull.instance;
        for (int i = formalArgs.getSignature().getLength() - 1; i >= 0; i--) {
            RNode argument = formalArgs.getDefaultArgument(i);
            Object lang = argument == null ? RSymbol.MISSING : RASTUtils.createLanguageElement(argument.asRSyntaxNode());
            RSymbol name = RDataFactory.createSymbol(formalArgs.getSignature().getName(i));
            succ = RDataFactory.createPairList(lang, succ, name);
        }
        return succ;
    }
}
