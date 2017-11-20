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
package com.oracle.truffle.r.runtime.ffi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RObject;

/**
 * Holds per RContext specific state of the RFFI. RFFI implementation agnostic data and methods are
 * implemented here, RFFI implementations may add their specifics.
 */
public abstract class RFFIContext extends RFFI {

    protected RFFIContext(CRFFI cRFFI, BaseRFFI baseRFFI, CallRFFI callRFFI, DLLRFFI dllRFFI, UserRngRFFI userRngRFFI, ZipRFFI zipRFFI, PCRERFFI pcreRFFI, LapackRFFI lapackRFFI, StatsRFFI statsRFFI,
                    ToolsRFFI toolsRFFI, REmbedRFFI rEmbedRFFI, MiscRFFI miscRFFI) {
        super(cRFFI, baseRFFI, callRFFI, dllRFFI, userRngRFFI, zipRFFI, pcreRFFI, lapackRFFI, statsRFFI, toolsRFFI, rEmbedRFFI, miscRFFI);
        // forward constructor
    }

    private int callDepth = 0;

    /**
     * @see #registerReferenceUsedInNative(Object)
     */
    private final ArrayList<Object> protectedNativeReferences = new ArrayList<>();

    /**
     * Stack used by RFFI to implement the PROTECT/UNPROTECT functions. Objects registered on this
     * stack do necessarily not have to be {@linke #registerReferenceUsedInNative}, but once popped
     * off, they must be put into that list.
     */
    public final ArrayList<RObject> protectStack = new ArrayList<>();

    /**
     * The GC in GNUR is cooperative, which means that unless native code calls back to the R engine
     * (GNUR/FastR) it may assume (and unfortunately people do that) that GC will not run and will
     * not collect anything that may be not reachable anymore, including the result of the last
     * up-call and including the objects popped off the PROTECT/UNPROTECT stack! Moreover, some
     * R-API functions are known to be not calling GC, therefore people may (and do) count on them
     * not removing unreachable references, this is specifically true for {@code UPROTECT} and its
     * variants and for macros like {@code INTEGER}. This behavior is required e.g. for
     * {@code C_parseRd}. We keep a list of all the objects that may not be reachable anymore, but
     * must not be collected, because no garbage collecting R-API function has been called since
     * they became unreachable. This method clears this list so that Java GC can collect the
     * objects.
     */
    public final void registerReferenceUsedInNative(Object obj) {
        protectedNativeReferences.add(obj);
    }

    /**
     * FastR equivalent of GNUR's special dedicated global list that is GC root and so any vectors
     * added to it will be guaranteed to be preserved.
     */
    public final IdentityHashMap<RObject, AtomicInteger> preserveList = new IdentityHashMap<>();

    public abstract TruffleObject lookupNativeFunction(NativeFunction function);

    /**
     * @param canRunGc {@code true} if this upcall can cause a gc on GNU R, and therefore can clear
     *            the list of preserved objects.
     */
    public void beforeUpcall(boolean canRunGc) {
    }

    public void afterUpcall(boolean canRunGc) {
        if (canRunGc) {
            cooperativeGc();
        }
    }

    /**
     * Invoked during RContext initialization, but after the global environment is set up.
     */
    public void initializeVariables(RContext context) {
    }

    public long beforeDowncall() {
        callDepth++;
        return 0;
    }

    /**
     * @param before the value returned by the corresponding call to {@link #beforeDowncall()}.
     */
    public void afterDowncall(long before) {
        callDepth--;
        cooperativeGc();
    }

    public int getCallDepth() {
        return callDepth;
    }

    // this emulates GNUR's cooperative GC
    @TruffleBoundary
    private void cooperativeGc() {
        protectedNativeReferences.clear();
    }

    private RFFI instance;

    public RFFI getRFFI() {
        if (instance == null) {
            instance = RFFIFactory.create();
        }
        return instance;
    }
}
