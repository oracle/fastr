/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.ffi.impl.altrep.AltrepDownCallNodeFactoryImpl;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLL;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL.NFIHandle;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static com.oracle.truffle.r.runtime.context.FastROptions.TraceNativeCalls;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCall;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCallReturn;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logEnabled;

public class TruffleNFI_Context extends RFFIContext {

    private static ReentrantLock accessLock;

    public TruffleNFI_Context() {
        this(new RFFIContextState());
    }

    public TruffleNFI_Context(RFFIContextState rffiContextState) {
        super(rffiContextState, new TruffleNFI_C(), new BaseRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE, TruffleNFI_DownCallNodeFactory.INSTANCE),
                        new AltrepRFFI(AltrepDownCallNodeFactoryImpl.INSTANCE),
                        new TruffleNFI_Call(),
                        new TruffleNFI_DLL(),
                        new TruffleNFI_UserRng(),
                        new ZipRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE), new PCRERFFI(TruffleNFI_DownCallNodeFactory.INSTANCE), new LapackRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE),
                        new StatsRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE), new ToolsRFFI(), new REmbedRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE),
                        new MiscRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE));
        // forward constructor
    }

    /**
     * Last yet unhandled exception that happened during an up-call.
     */
    private RuntimeException lastException;

    /**
     * Memory allocated using Rf_alloc, which should be reclaimed at every down-call exit. Note:
     * this is less efficient than GNUR's version, we may need to implement it properly should the
     * performance be a problem.
     */
    public final ArrayDeque<ArrayList<Long>> transientAllocations = new ArrayDeque<>();

    public void setLastUpCallException(RuntimeException ex) {
        assert ex == null || lastException == null : "last up-call exception is already set";
        lastException = ex;
    }

    public RuntimeException getLastUpCallException() {
        return lastException;
    }

    @CompilationFinal(dimensions = 1) private final TruffleObject[] nativeFunctions = new TruffleObject[NativeFunction.values().length];

    @SuppressWarnings("unchecked")
    @Override
    public <C extends RFFIContext> C as(Class<C> rffiCtxClass) {
        assert rffiCtxClass == TruffleNFI_Context.class;
        return (C) this;
    }

    @Override
    public Type getDefaultRFFIType() {
        return Type.NFI;
    }

    /**
     * Looks up the given given function and returns the NFI function object.
     */
    @Override
    public TruffleObject lookupNativeFunction(NativeFunction function) {
        int index = function.ordinal();
        if (nativeFunctions[index] == null) {
            // The look-up is one-off thing
            CompilerDirectives.transferToInterpreterAndInvalidate();
            TruffleObject dllInfo;
            if (Utils.identityEquals(function.getLibrary(), NativeFunction.baseLibrary())) {
                dllInfo = TruffleNFI_Context.getInstance().defaultLibrary;
            } else if (Utils.identityEquals(function.getLibrary(), NativeFunction.anyLibrary())) {
                DLLInfo lib = DLL.findLibraryContainingSymbol(RContext.getInstance(), function.getCallName());
                if (lib == null) {
                    throw RInternalError.shouldNotReachHere("Could not find library containing symbol " + function.getCallName());
                }
                dllInfo = ((NFIHandle) lib.handle).libHandle;
            } else {
                DLLInfo lib = DLL.findLibrary(function.getLibrary());
                if (lib == null) {
                    throw RInternalError.shouldNotReachHere("Could not find library  " + function.getLibrary());
                }
                dllInfo = ((NFIHandle) lib.handle).libHandle;
            }
            try {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                TruffleObject symbol = ((TruffleObject) interop.readMember(dllInfo, function.getCallName()));
                TruffleObject target = (TruffleObject) interop.invokeMember(symbol, "bind", function.getSignature());
                nativeFunctions[index] = target;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        return nativeFunctions[index];
    }

    private static boolean variablesInitialized;

    public TruffleObject defaultLibrary;

    @Override
    public void initializeVariables(RContext context) {
        synchronized (TruffleNFI_Context.class) {
            if (!variablesInitialized) {
                variablesInitialized = true;
                RFFIVariables[] variables = RFFIVariables.initialize(context);
                for (int i = 0; i < variables.length; i++) {
                    RFFIVariables var = variables[i];
                    Object value = var.getValue();
                    if (value == null || var.alwaysUpCall) {
                        continue;
                    }
                    try {
                        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                        if (value instanceof Double) {
                            interop.execute(lookupNativeFunction(NativeFunction.initvar_double), i, value);
                        } else if (value instanceof Integer) {
                            interop.execute(lookupNativeFunction(NativeFunction.initvar_int), i, value);
                        } else if (value instanceof String) {
                            interop.execute(lookupNativeFunction(NativeFunction.initvar_string), i, value);
                        } else {
                            FFIDownCallWrap ffiWrap = new FFIDownCallWrap();
                            try {
                                interop.execute(lookupNativeFunction(NativeFunction.initvar_obj), i, ffiWrap.wrapUncached(value));
                            } catch (Exception ex) {
                                throw RInternalError.shouldNotReachHere(ex);
                            } finally {
                                ffiWrap.close();
                            }
                        }
                    } catch (InteropException ex) {
                        throw RInternalError.shouldNotReachHere(ex);
                    }
                }
            }
        }
    }

    @Override
    public void initializeEmbedded(RContext context) {
        pushCallbacks();
    }

    @TruffleBoundary
    private long initCallbacksAddress() {
        // get the address of the native thread local
        try {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            Object getCallbacksAddress = interop.readMember(getLibRHandle(), "Rinternals_getCallbacksAddress");
            TruffleObject getCallbacksAddressFunction = (TruffleObject) interop.invokeMember(getCallbacksAddress, "bind", "(): sint64");
            return (long) interop.execute(getCallbacksAddressFunction);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private void initCallbacks(RContext context) {
        if (context.getKind() == ContextKind.SHARE_NOTHING) {
            // create and fill a new callbacks table
            callbacks = NativeMemory.allocate(Callbacks.values().length * (long) Long.BYTES, "callbacks");
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            Object addCallback;
            try {
                addCallback = interop.readMember(getLibRHandle(), "Rinternals_addCallback");
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            try {
                Callbacks.createCalls(new TruffleNFI_UpCallsRFFIImpl());
                for (Callbacks callback : Callbacks.values()) {
                    String addCallbackSignature = String.format("(env, sint64, sint32, %s): void", callback.nfiSignature);
                    TruffleObject addCallbackFunction = (TruffleObject) interop.invokeMember(addCallback, "bind", addCallbackSignature);
                    interop.execute(addCallbackFunction, callbacks, callback.ordinal(), callback.call);
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        } else {
            // reuse the parent's callbacks table
            callbacks = context.getParent().getStateRFFI().as(TruffleNFI_Context.class).callbacks;
        }
    }

    private long callbacks;
    @CompilationFinal private boolean singleThreadOnly = true;
    @CompilationFinal private long callbacksAddressThread;
    @CompilationFinal private long callbacksAddress;
    private long lastCallbacksAddressThread;
    private long lastCallbacksAddress;

    private long pushCallbacks() {
        if (rlibDLLInfo == null) {
            return -1;
        }
        if (callbacksAddress == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callbacksAddress = initCallbacksAddress();
            callbacksAddressThread = Thread.currentThread().getId();
        }
        assert callbacks != 0L;
        if (singleThreadOnly && callbacksAddressThread == Thread.currentThread().getId()) {
            // Fast path for contexts used only from a single thread
            long oldCallbacks = NativeMemory.getLong(callbacksAddress);
            assert callbacksAddress != 0L;
            NativeMemory.putLong(callbacksAddress, callbacks);
            return oldCallbacks;
        }
        // Slow path: cache the address, but reinitialize it if the thread has changed, without
        // transfer to interpreter this time.
        boolean reinitialize = singleThreadOnly || lastCallbacksAddressThread != Thread.currentThread().getId();
        if (singleThreadOnly) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            singleThreadOnly = false;
        }
        if (reinitialize) {
            lastCallbacksAddress = initCallbacksAddress();
            lastCallbacksAddressThread = Thread.currentThread().getId();
        }
        long oldCallbacks = NativeMemory.getLong(lastCallbacksAddress);
        assert lastCallbacksAddress != 0L;
        assert lastCallbacksAddressThread == Thread.currentThread().getId();
        NativeMemory.putLong(lastCallbacksAddress, callbacks);
        return oldCallbacks;
    }

    private void popCallbacks(long beforeValue) {
        if (beforeValue < 0) {
            return;
        }
        assert !singleThreadOnly || NativeMemory.getLong(callbacksAddress) == callbacks : "invalid nesting of native calling contexts";
        assert singleThreadOnly || NativeMemory.getLong(lastCallbacksAddress) == callbacks : "invalid nesting of native calling contexts";
        NativeMemory.putLong(callbacksAddress, beforeValue);
    }

    protected void addLibRToDLLContextState(RContext context, DLLInfo libR) {
        context.stateDLL.addLibR(libR);
    }

    private DLLInfo rlibDLLInfo;

    private Object getLibRHandle() {
        if (rlibDLLInfo.handle instanceof TruffleMixed_DLL.MixedLLVM_Handle) {
            return ((NFIHandle) ((TruffleMixed_DLL.MixedLLVM_Handle) rlibDLLInfo.handle).nfiLibHandle).libHandle;
        }
        return ((NFIHandle) rlibDLLInfo.handle).libHandle;
    }

    @Override
    public ContextState initialize(RContext context) {
        if (RContext.getInstance().getOption(TraceNativeCalls)) {
            System.out.println("WARNING: The TraceNativeCalls option was discontinued!\n" +
                            "You can rerun FastR with --log.R." + RLogger.LOGGER_RFFI + ".level=FINE --log.file=<yourfile>.\n" +
                            "NOTE that stdout is problematic for embedded mode, when using this logger, also always specify a log file");
        }
        initializeLock();
        if (logEnabled()) {
            logDownCall("initialize");
        }
        acquireLock();
        try {
            String librffiPath = LibPaths.getBuiltinLibPath(context, "R");
            if (context.isInitial()) {
                rlibDLLInfo = DLL.loadLibR(context, librffiPath, path -> TruffleNFI_DLL.dlOpen(context, path, false, false));
                addLibRToDLLContextState(context, rlibDLLInfo);
            } else {
                rlibDLLInfo = DLL.ContextStateImpl.getLibR();
                // force initialization of NFI
                // TODO: still necessary? Maybe even buggy, what if it is mixed context?
                DLLRFFI.DLOpenRootNode.create(context).call(librffiPath, false, false);
            }
            switch (context.getKind()) {
                case SHARE_NOTHING:
                case SHARE_ALL:
                    // new thread, initialize properly
                    assert defaultLibrary == null;
                    defaultLibrary = (TruffleObject) RContext.getInstance().getEnv().parseInternal(Source.newBuilder("nfi", "default", "(load default)").build()).call();
                    initCallbacks(context);
                    break;
                case SHARE_PARENT_RO:
                case SHARE_PARENT_RW:
                    // these stay on the same thread
                    assert defaultLibrary != null && rlibDLLInfo != null;
                    break;
            }
            return this;
        } finally {
            if (logEnabled()) {
                logDownCallReturn("initialize", null);
            }
            releaseLock();
        }
    }

    private static synchronized void initializeLock() {
        if (accessLock == null) {
            accessLock = new ReentrantLock();
        }
    }

    @Override
    public void beforeDispose(RContext context) {
        switch (context.getKind()) {
            case SHARE_NOTHING:
                NativeMemory.free(callbacks, "callbacks");
                break;
            case SHARE_ALL:
            case SHARE_PARENT_RO:
            case SHARE_PARENT_RW:
                // these stay on the same thread
                break;
        }
        super.beforeDispose(context);
    }

    @Override
    public Object beforeDowncall(MaterializedFrame frame, RFFIFactory.Type rffiType) {
        Object tokenFromSuper = super.beforeDowncall(frame, RFFIFactory.Type.NFI);
        addTransientAllocationsFrame();
        acquireLock();
        return new Object[]{tokenFromSuper, pushCallbacks()};
    }

    @TruffleBoundary
    private void addTransientAllocationsFrame() {
        transientAllocations.push(new ArrayList<>());
    }

    @Override
    public void beforeUpcall(RContext context, boolean canRunGc, Type rffiType) {
        super.beforeUpcall(context, canRunGc, rffiType);
        // up-call is where we are returning from native code to Java, if we run GC now, any
        // unreferenced (and unprotected) R objects should be collected
        context.gcTorture.runGC();
    }

    @Override
    public void afterDowncall(Object beforeValue, Type rffiType, AfterDownCallProfiles profiles) {
        Object[] tokens = (Object[]) beforeValue;
        super.afterDowncall(tokens[0], rffiType, profiles);
        popCallbacks((long) tokens[1]);
        freeCurrentTransientAllocations();
        RuntimeException lastUpCallEx = getLastUpCallException();
        setLastUpCallException(null);
        releaseLock();
        if (lastUpCallEx != null) {
            CompilerDirectives.transferToInterpreter();
            throw lastUpCallEx;
        }
    }

    @TruffleBoundary
    private void freeCurrentTransientAllocations() {
        for (Long ptr : transientAllocations.pop()) {
            NativeMemory.free(ptr, "Rf_alloc");
        }
    }

    public static TruffleNFI_Context getInstance() {
        return RContext.getInstance().getStateRFFI(TruffleNFI_Context.class);
    }

    public DLLInfo getRLibDLLInfo() {
        return rlibDLLInfo;
    }

    @TruffleBoundary
    private static void acquireLock() {
        accessLock.lock();
    }

    @TruffleBoundary
    private static void releaseLock() {
        accessLock.unlock();
    }

}
