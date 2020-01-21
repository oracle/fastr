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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.Collections.StackLibrary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFILog;

@GenerateUncached
public abstract class UnprotectNode extends FFIUpCallNode.Arg1 {

    public static UnprotectNode create() {
        return UnprotectNodeGen.create();
    }

    public static UnprotectNode getUncached() {
        return UnprotectNodeGen.getUncached();
    }

    @Specialization(guards = "n == 0")
    Object unprotectNothing(@SuppressWarnings("unused") int n) {
        return RNull.instance;
    }

    @Specialization(guards = "n == 1")
    Object unprotectSingle(@SuppressWarnings("unused") int n,
                    @Cached BranchProfile registerNativeRefProfile,
                    @Cached("createBinaryProfile()") ConditionProfile registerNativeRefNopProfile,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @CachedLibrary(limit = "1") StackLibrary stacks) {
        RContext ctx = ctxRef.get();
        RFFIContext rffiCtx = ctx.getRFFI();
        try {
            popProtectedObject(ctx, rffiCtx, stacks, registerNativeRefNopProfile, registerNativeRefProfile);
        } catch (IndexOutOfBoundsException e) {
            debugWarning("mismatched protect/unprotect (unprotect with empty protect stack)");
        }
        return RNull.instance;
    }

    @Specialization(guards = {"n > 1", "n == nCached"}, limit = "5")
    @ExplodeLoop
    Object unprotectMultipleCached(@SuppressWarnings("unused") int n,
                    @Cached("n") int nCached,
                    @Cached("createBinaryProfile()") ConditionProfile registerNativeRefNopProfile,
                    @Cached BranchProfile registerNativeRefProfile,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @CachedLibrary(limit = "1") StackLibrary stacks) {
        RContext ctx = ctxRef.get();
        RFFIContext rffiCtx = ctx.getRFFI();
        try {
            for (int i = 0; i < nCached; i++) {
                popProtectedObject(ctx, rffiCtx, stacks, registerNativeRefNopProfile, registerNativeRefProfile);
            }
        } catch (IndexOutOfBoundsException e) {
            debugWarning("mismatched protect/unprotect (unprotect with empty protect stack)");
        }
        return RNull.instance;
    }

    @Specialization(guards = "n > 1", replaces = "unprotectMultipleCached")
    Object unprotectMultipleUnchached(int n,
                    @Cached("createBinaryProfile()") ConditionProfile registerNativeRefNopProfile,
                    @Cached BranchProfile registerNativeRefProfile,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @CachedLibrary(limit = "1") StackLibrary stacks) {
        RContext ctx = ctxRef.get();
        RFFIContext rffiCtx = ctx.getRFFI();
        try {
            for (int i = 0; i < n; i++) {
                popProtectedObject(ctx, rffiCtx, stacks, registerNativeRefNopProfile, registerNativeRefProfile);
            }
        } catch (IndexOutOfBoundsException e) {
            debugWarning("mismatched protect/unprotect (unprotect with empty protect stack)");
        }
        return RNull.instance;
    }

    private static void popProtectedObject(RContext ctx, RFFIContext rffiCtx, StackLibrary stacks, ConditionProfile nopProfile, BranchProfile registerNativeRefProfile) {
        Object removed = stacks.pop(ctx.getStateRFFI().rffiContextState.protectStack);
        // Developers expect the "unprotected" references to be still alive until next GNU-R
        // compatible GC cycle
        rffiCtx.registerReferenceUsedInNative(removed, nopProfile, registerNativeRefProfile);
        if (RFFILog.logEnabled()) {
            RFFILog.logRObject("Unprotected: ", removed);
        }
    }

    private static boolean debugWarning(String message) {
        CompilerDirectives.transferToInterpreter();
        RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, message);
        return true;
    }

}
