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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFILog;

@GenerateUncached
public abstract class UnprotectPtrNode extends FFIUpCallNode.Arg1 {

    public static UnprotectPtrNode create() {
        return UnprotectPtrNodeGen.create();
    }

    public static UnprotectPtrNode getUncached() {
        return UnprotectPtrNodeGen.getUncached();
    }

    @Specialization
    Object unprotect(RBaseObject x,
                    @Cached("createBinaryProfile()") ConditionProfile registerNativeRefNopProfile,
                    @Cached BranchProfile registerNativeRefProfile,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        RFFIContext ctx = ctxRef.get().getStateRFFI();
        Collections.ArrayListObj<RBaseObject> stack = ctx.rffiContextState.protectStack;
        for (int i = stack.size() - 1; i >= 0; i--) {
            RBaseObject current = stack.get(i);
            if (current == x) {
                if (RFFILog.logEnabled()) {
                    RFFILog.logRObject("Unprotected: ", current);
                }
                ctx.registerReferenceUsedInNative(current, registerNativeRefNopProfile, registerNativeRefProfile);
                stack.remove(i);
                return null;
            }
        }
        return null;
    }
}
