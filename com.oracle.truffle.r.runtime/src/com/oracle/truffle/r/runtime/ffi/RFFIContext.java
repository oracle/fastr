/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 * Holds per RContext specific state of the RFFI. RFFI implementation agnostic data and methods are
 * implemented here, RFFI implementations may add their specifics.
 */
public abstract class RFFIContext extends RFFI {

    public final RFFIContextState rffiContextState;

    protected RFFIContext(RFFIContextState rffiContextState, CRFFI cRFFI, BaseRFFI baseRFFI, AltrepRFFI altrepRFFI, CallRFFI callRFFI, DLLRFFI dllRFFI, UserRngRFFI userRngRFFI, ZipRFFI zipRFFI,
                    PCRE2RFFI pcre2RFFI, LapackRFFI lapackRFFI, StatsRFFI statsRFFI,
                    ToolsRFFI toolsRFFI, REmbedRFFI rEmbedRFFI, MiscRFFI miscRFFI) {
        super(cRFFI, baseRFFI, altrepRFFI, callRFFI, dllRFFI, userRngRFFI, zipRFFI, pcre2RFFI, lapackRFFI, statsRFFI, toolsRFFI, rEmbedRFFI, miscRFFI);
        this.rffiContextState = rffiContextState;
        // forward constructor
    }

    // Used to protect the children from GC
    private static final class RFunctionChildren {
        @SuppressWarnings("unused") private Object body;
        @SuppressWarnings("unused") private Object args;
    }

    public static final class RFFIContextState {

        private int callDepth = 0;

        /**
         * @see #registerReferenceUsedInNative(Object)
         */
        private final Collections.ArrayListObj<Object> protectedNativeReferences = new Collections.ArrayListObj<>(256);

        /**
         * FastR equivalent of GNUR's special dedicated global list that is GC root and so any
         * vectors added to it will be guaranteed to be preserved.
         */
        public final EconomicMap<RBaseObject, AtomicInteger> preserveList = EconomicMap.create();

        public final WeakHashMap<RScalar, RAbstractVector> protectedMaterializedScalarVectors = new WeakHashMap<>();

        public final WeakHashMap<Object, RForeignObjectWrapper> protectedForeignWrappers = new WeakHashMap<>();

        public final WeakHashMap<RFunction, RFunctionChildren> protectedFunctionChildren = new WeakHashMap<>();

        public final WeakHashMap<RPromise, Object> protectedPromiseCode = new WeakHashMap<>();

        /**
         * Stack used by RFFI to implement the PROTECT/UNPROTECT functions. Objects registered on
         * this stack do necessarily not have to be {@link #registerReferenceUsedInNative}, but once
         * popped off, they must be put into that list. The initial size should "reasonably" big.
         * (Should a special FastR configuration property be introduced to control the size?)
         */
        public final Collections.ArrayListObj<RBaseObject> protectStack = new Collections.ArrayListObj<>(1000);

        public MaterializedFrame currentDowncallFrame = null;

        public boolean primFunBeingDispatched;
    }

    /**
     * The GC in GNUR is cooperative, which means that unless native code calls back to the R engine
     * (GNUR/FastR) one may assume (and unfortunately people do that) that GC will not run and will
     * not collect anything that may be not reachable anymore, including the result of the last
     * up-call and including the objects popped off the PROTECT/UNPROTECT stack! Moreover, some
     * R-API functions are known to be not calling GC, therefore people may (and do) count on them
     * not removing unreachable references, this is specifically true for {@code UNPROTECT} and its
     * variants and for macros like {@code INTEGER}. This behavior is required e.g. for
     * {@code C_parseRd}. We keep a list of all the objects that may not be reachable anymore, but
     * must not be collected, because no garbage collecting R-API function has been called since
     * they became unreachable.
     */
    public final void registerReferenceUsedInNative(Object obj) {
        // RSymbols are cached and never freed anyway -- dictated by GNU-R
        if (!(obj instanceof RSymbol)) {
            rffiContextState.protectedNativeReferences.add(obj);
        }
    }

    public final void registerReferenceUsedInNative(Object obj, ConditionProfile nopProfile, BranchProfile stackResizeProfile) {
        // RSymbols are cached and never freed anyway -- dictated by GNU-R
        if (nopProfile.profile(!(obj instanceof RSymbol))) {
            rffiContextState.protectedNativeReferences.add(obj, stackResizeProfile);
        }
    }

    public abstract TruffleObject lookupNativeFunction(NativeFunction function, RContext ctx);

    public abstract <C extends RFFIContext> C as(Class<C> rffiCtxClass);

    /**
     * Calls a native function in slow-path. This method is a simpler alternative to
     * {@link com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory.DownCallNode} machinery.
     * Use it once the native function is not known during FastR build, i.e., whenever you
     * cannot use {@link NativeFunction}.
     *
     * @param nativeFunc (Truffle) object representing the native function to be called.
     * @param nativeFuncType Type of the native function - can be either {@code LLVM} or {@code NFI}.
     * @param signature Signature of the function. Makes sense only for {@code NFI} function type.
     * @param args Arguments passed to the native function, and processed with
     *             {@link com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap}.
     * @return Return value directly from the native function, processed with {@link FFIUnwrapNode}.
     */
    public Object callNativeFunction(Object nativeFunc, RFFIFactory.Type nativeFuncType, String signature, Object[] args) {
        throw RInternalError.shouldNotReachHere("Implemented only for NFI and LLVM");
    }

    /**
     * @param context
     * @param canRunGc {@code true} if this upcall can cause a gc on GNU R, and therefore can clear
     */
    public void beforeUpcall(RContext context, boolean canRunGc, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        // empty by default
    }

    /**
     * @param canRunGc Causes that the list of additionally protected objects is cleared so that
     *            Java GC can collect the objects. See
     *            {@link RFFIContext#registerReferenceUsedInNative(Object)}.
     */
    public void afterUpcall(boolean canRunGc, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        if (canRunGc && rffiType == RFFIFactory.Type.NFI) {
            cooperativeGc(AfterDownCallProfiles.getUncached());
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

    /**
     * @param frame the last FastR frame before the downcall or null if the this call is beyond the
     *            truffle boundary
     * @param rffiType the type of the RFFI backend
     */
    public Object beforeDowncall(MaterializedFrame frame, @SuppressWarnings("unused") RFFIFactory.Type rffiType) {
        rffiContextState.callDepth++;
        MaterializedFrame savedDowncallFrame = rffiContextState.currentDowncallFrame;
        rffiContextState.currentDowncallFrame = frame == null || !RArguments.isRFrame(frame) ? null : frame;
        return savedDowncallFrame;
    }

    /**
     * @param before the value returned by the corresponding call to
     *            {@link #beforeDowncall(MaterializedFrame, RFFIFactory.Type)} .
     * @param profiles
     */
    public void afterDowncall(Object before, @SuppressWarnings("unused") RFFIFactory.Type rffiType, AfterDownCallProfiles profiles) {
        rffiContextState.currentDowncallFrame = (MaterializedFrame) before;
        rffiContextState.callDepth--;
        if (rffiContextState.callDepth == 0) {
            cooperativeGc(profiles);
        }
    }

    public final int getCallDepth() {
        return rffiContextState.callDepth;
    }

    // this emulates GNUR's cooperative GC
    private void cooperativeGc(AfterDownCallProfiles profiles) {
        rffiContextState.protectedNativeReferences.clear(profiles);
    }

    /**
     * Maintains a weak-reference 1:1 relationship between the foreign object and its wrapper.
     */
    @TruffleBoundary
    final RForeignObjectWrapper getOrCreateForeignObjectWrapper(TruffleObject foreignObj) {
        // If parent instanceof RForeignObjectWrapper, we could have a reference cycle
        assert !(foreignObj instanceof RForeignObjectWrapper) : foreignObj;
        RForeignObjectWrapper result = rffiContextState.protectedForeignWrappers.get(foreignObj);
        if (result == null) {
            result = new RForeignObjectWrapper(foreignObj);
            rffiContextState.protectedForeignWrappers.put(foreignObj, result);
        }
        return result;
    }

    /**
     * Maintains a weak-reference 1:1 relationship between the function object and its formal
     * arguments materialized as a pair-list.
     */
    @TruffleBoundary
    public final Object getOrCreateFunctionFormals(RFunction fun, Function<RFunction, Object> factory) {
        RFunctionChildren entry = getFunctionChildrenHolder(fun);
        if (entry.args == null) {
            Object args = factory.apply(fun);
            // assert checks reference cycle that could cause memory leak
            assert !contains(args, fun) : fun;
            assert args instanceof RPairList || args == RNull.instance : "not a pairlist: " + args;
            entry.args = args;
        }
        return entry.args;
    }

    /**
     * Maintains a weak-reference 1:1 relationship between the function object and its body
     * materialized as a language object.
     */
    @TruffleBoundary
    public final Object getOrCreateFunctionBody(RFunction fun, Function<RFunction, Object> factory) {
        RFunctionChildren entry = getFunctionChildrenHolder(fun);
        if (entry.body == null) {
            Object body = factory.apply(fun);
            // assert checks reference cycle that could cause memory leak
            assert !contains(body, fun) : fun;
            entry.body = body;
        }
        return entry.body;
    }

    /**
     * Removes existing mapping from function to its formals materialized as a pair-list.
     */
    public final void removeFunctionFormals(RFunction fun) {
        RFunctionChildren entry = rffiContextState.protectedFunctionChildren.get(fun);
        if (entry != null) {
            entry.args = null;
        }
    }

    /**
     * Removes existing mapping from function to its formals materialized as a language object.
     */
    public final void removeFunctionBody(RFunction fun) {
        RFunctionChildren entry = rffiContextState.protectedFunctionChildren.get(fun);
        if (entry != null) {
            entry.body = null;
        }
    }

    /**
     * Maintains a weak-reference 1:1 relationship between the promise and its closure presented as
     * pair-list to the native code.
     */
    @TruffleBoundary
    public final Object getOrCreateCode(RPromise promise, Function<RPromise, Object> factory) {
        Object code = rffiContextState.protectedPromiseCode.get(promise);
        if (code == null) {
            code = factory.apply(promise);
            // this can in theory introduce reference cycle
            assert !contains(code, promise) : code;
            rffiContextState.protectedPromiseCode.put(promise, code);
        }
        return code;
    }

    public Object getSulongArrayType(@SuppressWarnings("unused") Object arrayElement) {
        // TODO: this is here because TruffleLLVM_Context is not visible from "runtime" project
        // where we implement RObjectDataPtr which needs this
        throw RInternalError.shouldNotReachHere("getSulongArrayType");
    }

    public abstract RFFIFactory.Type getDefaultRFFIType();

    private RFunctionChildren getFunctionChildrenHolder(RFunction parent) {
        RFunctionChildren children = rffiContextState.protectedFunctionChildren.get(parent);
        if (children == null) {
            children = new RFunctionChildren();
            rffiContextState.protectedFunctionChildren.put(parent, children);
        }
        return children;
    }

    // Basic check for reference cycles that would lead to memory leaks
    private static boolean contains(Object child, Object parent) {
        if (child instanceof RPairList) {
            for (RPairList attr : (RPairList) child) {
                if (attr.car() == parent || attr.getTag() == parent) {
                    return true;
                }
            }
        } else if (child instanceof RList) {
            RList list = (RList) child;
            for (int i = 0; i < list.getLength(); i++) {
                if (list.getDataAt(i) == parent) {
                    return true;
                }
            }
        } else if (child instanceof REnvironment) {
            Frame frame = ((REnvironment) child).getFrame();

            for (Object identifier : FrameSlotChangeMonitor.getIdentifiers(frame.getFrameDescriptor())) {
                if (FrameSlotChangeMonitor.getObject(frame, identifier) == parent) {
                    return true;
                }
            }
        }
        if (child instanceof RAttributable) {
            DynamicObject attrs = ((RAttributable) child).getAttributes();
            if (attrs != null) {
                for (Object key : attrs.getShape().getKeys()) {
                    if (DynamicObjectLibrary.getUncached().getOrDefault(attrs, key, null) == parent) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
