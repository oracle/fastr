/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RObject;

/**
 * Holds per RContext specific state of the RFFI. RFFI implementation agnostic data and methods are
 * implemented here, RFFI implementations may add their specifics.
 */
public abstract class RFFIContext extends RFFI {

    public final RFFIContextState rffiContextState;

    protected RFFIContext(RFFIContextState rffiContextState, CRFFI cRFFI, BaseRFFI baseRFFI, CallRFFI callRFFI, DLLRFFI dllRFFI, UserRngRFFI userRngRFFI, ZipRFFI zipRFFI, PCRERFFI pcreRFFI,
                    LapackRFFI lapackRFFI, StatsRFFI statsRFFI,
                    ToolsRFFI toolsRFFI, REmbedRFFI rEmbedRFFI, MiscRFFI miscRFFI) {
        super(cRFFI, baseRFFI, callRFFI, dllRFFI, userRngRFFI, zipRFFI, pcreRFFI, lapackRFFI, statsRFFI, toolsRFFI, rEmbedRFFI, miscRFFI);
        this.rffiContextState = rffiContextState;
        // forward constructor
    }

    public static final class RFFIContextState {

        private int callDepth = 0;

        /**
         * @see #registerReferenceUsedInNative(Object)
         */
        private final ArrayList<Object> protectedNativeReferences = new ArrayList<>();

        /**
         * FastR equivalent of GNUR's special dedicated global list that is GC root and so any
         * vectors added to it will be guaranteed to be preserved.
         */
        public final IdentityHashMap<RObject, AtomicInteger> preserveList = new IdentityHashMap<>();

        private final WeakHashMap<Object, Set<Object>> protectedChildren = new WeakHashMap<>();
        /**
         * Stack used by RFFI to implement the PROTECT/UNPROTECT functions. Objects registered on
         * this stack do necessarily not have to be {@linke #registerReferenceUsedInNative}, but
         * once popped off, they must be put into that list.
         */
        public final ArrayList<RObject> protectStack = new ArrayList<>();

    }

    /**
     * The GC in GNUR is cooperative, which means that unless native code calls back to the R engine
     * (GNUR/FastR) one may assume (and unfortunately people do that) that GC will not run and will
     * not collect anything that may be not reachable anymore, including the result of the last
     * up-call and including the objects popped off the PROTECT/UNPROTECT stack! Moreover, some
     * R-API functions are known to be not calling GC, therefore people may (and do) count on them
     * not removing unreachable references, this is specifically true for {@code UPROTECT} and its
     * variants and for macros like {@code INTEGER}. This behavior is required e.g. for
     * {@code C_parseRd}. We keep a list of all the objects that may not be reachable anymore, but
     * must not be collected, because no garbage collecting R-API function has been called since
     * they became unreachable.
     */
    public final void registerReferenceUsedInNative(Object obj) {
        rffiContextState.protectedNativeReferences.add(obj);
    }

    public abstract TruffleObject lookupNativeFunction(NativeFunction function);

    protected void loadLibR(RContext context, String librffiPath) {
        DLL.loadLibR(context, librffiPath);
    }

    public abstract <C extends RFFIContext> C as(Class<C> rffiCtxClass);

    /**
     * @param canRunGc {@code true} if this upcall can cause a gc on GNU R, and therefore can clear
     *            the list of preserved objects.
     */
    public void beforeUpcall(boolean canRunGc, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        // empty by default
    }

    /**
     * @param canRunGc Causes that the list of additionally protected objects is cleared so that
     *            Java GC can collect the objects. See
     *            {@link RFFIContext#registerReferenceUsedInNative(Object)}.
     */
    public void afterUpcall(boolean canRunGc, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        if (canRunGc) {
            cooperativeGc();
        }
    }

    /**
     * Invoked during RContext initialization, but after the global environment is set up.
     */
    public void initializeVariables(@SuppressWarnings("unused") RContext context) {
        // empty by default
    }

    /**
     * Invoked as part of the R embedded initialization just before returning back to the C user
     * code. Should do any set-up necessary for the RFFI to be fully functional even outside the
     * context of a down-call. At the moment the assumption is that embedded code is always single
     * threaded and always creates exactly one context. This method shall be invoked after
     * {@link #initialize(RContext)} and {@link #initializeVariables(RContext)}.
     */
    public void initializeEmbedded(@SuppressWarnings("unused") RContext context) {
        throw RInternalError.unimplemented("R Embedding not supported with " + this.getClass().getSimpleName() + " RFFI backend.");
    }

    public long beforeDowncall(@SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        rffiContextState.callDepth++;
        return 0;
    }

    /**
     * @param before the value returned by the corresponding call to
     *            {@link #beforeDowncall(com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type)}.
     */
    public void afterDowncall(long before, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        rffiContextState.callDepth--;
        if (rffiContextState.callDepth == 0) {
            cooperativeGc();
        }
    }

    public final int getCallDepth() {
        return rffiContextState.callDepth;
    }

    // this emulates GNUR's cooperative GC
    @TruffleBoundary
    private void cooperativeGc() {
        rffiContextState.protectedNativeReferences.clear();
    }

    /**
     * Establish a weak relationship between an object and its owner to prevent a premature garbage
     * collecting of the object. See <code>com.oracle.truffle.r.ffi.processor.RFFIResultOwner</code>
     * for more commentary.
     *
     * Note: It is meant to be applied only on certain return values from upcalls.
     *
     * @param parent
     * @param child
     * @return the child
     *
     */
    public final Object protectChild(Object parent, Object child) {
        Set<Object> children = rffiContextState.protectedChildren.get(parent);
        if (children == null) {
            children = new HashSet<>();
            rffiContextState.protectedChildren.put(parent, children);
        }
        children.add(child);
        return child;
    }

    private RFFI instance;

    public final RFFI getRFFI() {
        if (instance == null) {
            instance = RFFIFactory.create();
        }
        return instance;
    }
}
