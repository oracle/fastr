/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfo.EvalMode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Creates a {@code FunctionInfo} instance for a recognized function call. The function can be
 * represented either by a {@link RFunction function} object or by a {@link RSymbol symbol}.
 */
@ImportStatic(DSLConfig.class)
public abstract class CallInfoFactoryNode extends Node {

    protected static final int CACHE_SIZE = 100;

    static CallInfoFactoryNode create() {
        return CallInfoFactoryNodeGen.create();
    }

    abstract CallInfo execute(Object fun, Object argList, REnvironment env);

    @Specialization(limit = "getCacheSize(CACHE_SIZE)", guards = {"cachedFunName.equals(funSym.getName())", "env.getFrame(frameAccessProfile).getFrameDescriptor() == cachedFrameDesc"})
    CallInfo createFunctionInfoFromSymbolCached(@SuppressWarnings("unused") RSymbol funSym, RPairList argList, REnvironment env,
                    @SuppressWarnings("unused") @Cached("funSym.getName()") String cachedFunName,
                    @SuppressWarnings("unused") @Cached("env.getFrame().getFrameDescriptor()") FrameDescriptor cachedFrameDesc,
                    @Cached("createFunctionLookup(cachedFunName)") ReadVariableNode readFunNode,
                    @Cached("createClassProfile()") ValueProfile frameProfile,
                    @Cached("createClassProfile()") ValueProfile frameAccessProfile,
                    @Cached("create()") BranchProfile ignoredProfile,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        Object fun = readFunNode.execute(frameProfile.profile(env.getFrame(frameAccessProfile)));
        if (fun instanceof RFunction) {
            return new CallInfo((RFunction) fun, funSym.getName(), argList, env, plLib);
        } else {
            ignoredProfile.enter();
            return null;
        }
    }

    @Specialization(replaces = "createFunctionInfoFromSymbolCached")
    CallInfo createFunctionInfoFromSymbolUncached(RSymbol funSym, RPairList argList, REnvironment env,
                    @Cached("createClassProfile()") ValueProfile frameProfile,
                    @Cached("createClassProfile()") ValueProfile frameAccessProfile,
                    @Cached("create()") BranchProfile ignoredProfile,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        Object fun = ReadVariableNode.lookupFunction(funSym.getName(), frameProfile.profile(env.getFrame(frameAccessProfile)));
        if (fun instanceof RFunction) {
            return new CallInfo((RFunction) fun, funSym.getName(), argList, env, plLib);
        } else {
            ignoredProfile.enter();
            return null;
        }
    }

    @Specialization
    CallInfo createFunctionInfo(RFunction fun, @SuppressWarnings("unused") RNull argList, REnvironment env,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        return new CallInfo(fun, fun.getName(), null, env, plLib, EvalMode.FAST);
    }

    @Specialization
    CallInfo createFunctionInfo(RFunction fun, RPairList argList, REnvironment env,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib) {
        return new CallInfo(fun, fun.getName(), argList, env, plLib, EvalMode.FAST);
    }

    @SuppressWarnings("unused")
    @Fallback
    CallInfo fallback(Object fun, Object argList, REnvironment env) {
        return null;
    }
}
