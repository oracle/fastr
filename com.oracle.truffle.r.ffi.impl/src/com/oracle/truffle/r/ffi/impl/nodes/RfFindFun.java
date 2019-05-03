/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class RfFindFun extends FFIUpCallNode.Arg2 {

    protected static final int SYM_CACHE_SIZE = DSLConfig.getCacheSize(20);

    private final ValueProfile frameProfile = ValueProfile.createClassProfile();
    private final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();

    protected RfFindFun() {
    }

    public static RfFindFun create() {
        return RfFindFunNodeGen.create();
    }

    @Specialization(limit = "SYM_CACHE_SIZE", guards = {"cachedFunName.equals(funSym.getName())", "env.getFrame().getFrameDescriptor() == cachedFrameDesc"})
    Object findFunCached(@SuppressWarnings("unused") RSymbol funSym, REnvironment env,
                    @SuppressWarnings("unused") @Cached("funSym.getName()") String cachedFunName,
                    @SuppressWarnings("unused") @Cached("env.getFrame().getFrameDescriptor()") FrameDescriptor cachedFrameDesc,
                    @Cached("createFunctionLookup(cachedFunName)") ReadVariableNode readFunNode) {
        return readFunNode.execute(frameProfile.profile(env.getFrame(frameAccessProfile)));
    }

    @Specialization(replaces = "findFunCached")
    Object findFunUncached(RSymbol funSym, REnvironment env) {
        return ReadVariableNode.lookupFunction(funSym.getName(), frameProfile.profile(env.getFrame(frameAccessProfile)));
    }
}
