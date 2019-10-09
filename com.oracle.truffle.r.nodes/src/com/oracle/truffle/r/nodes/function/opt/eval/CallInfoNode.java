/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.function.opt.eval;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Extracts information about a function call in an expression. Currently, two situations are
 * recognized: a simple function call and a function call wrapped in the try-catch envelope.
 */
public abstract class CallInfoNode extends Node {

    private static final String TRYCATCH = "tryCatch";
    private static final String EVALQ = "evalq";

    public abstract CallInfo execute(RPairList expr, REnvironment env);

    static CallInfoNode create() {
        return CallInfoNodeGen.create();
    }

    @Specialization(guards = "isSimpleCall(expr, plLib)")
    CallInfo handleSimpleCall(RPairList expr, REnvironment env,
                    @Cached("create()") CallInfoFactoryNode callInfoFactory,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        return callInfoFactory.execute(plLib.car(expr), plLib.cdr(expr), env);
    }

    @Specialization(guards = "isTryCatchWrappedCall(expr, plLib)")
    CallInfo handleTryCatchWrappedCall(RPairList expr, @SuppressWarnings("unused") REnvironment env,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @Cached("create()") BranchProfile branchProfile1,
                    @Cached("create()") BranchProfile branchProfile2,
                    @Cached("create()") BranchProfile branchProfile3,
                    @Cached("create()") BranchProfile branchProfile4,
                    @Cached("create()") CallInfoFactoryNode callInfoFactory) {
        Object pp = plLib.cdr(expr);
        pp = plLib.car(pp);
        if (pp instanceof RPairList) {
            branchProfile1.enter();
            Object p = plLib.car(pp);
            if (p instanceof RSymbol && EVALQ.equals(((RSymbol) p).getName())) {
                branchProfile2.enter();
                pp = plLib.cdr(pp);
                if (pp instanceof RPairList) {
                    branchProfile3.enter();
                    REnvironment evalEnv = (REnvironment) plLib.car(plLib.cdr(pp));
                    pp = plLib.car(pp);
                    if (pp instanceof RPairList) {
                        branchProfile4.enter();
                        p = plLib.car(pp);
                        Object argList = plLib.cdr(pp);
                        return callInfoFactory.execute(p, argList instanceof RPairList ? (RPairList) argList : null, evalEnv);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @Fallback
    CallInfo handleOther(RPairList expr, REnvironment env) {
        return null;
    }

    boolean isSimpleCall(RPairList expr, RPairListLibrary plLib) {
        Object fn = plLib.car(expr);
        return fn instanceof RFunction || (fn instanceof RSymbol && !TRYCATCH.equals(((RSymbol) fn).getName()));
    }

    boolean isTryCatchWrappedCall(RPairList expr, RPairListLibrary plLib) {
        Object p = plLib.car(expr);
        return p instanceof RSymbol && TRYCATCH.equals(((RSymbol) p).getName());
    }

}
