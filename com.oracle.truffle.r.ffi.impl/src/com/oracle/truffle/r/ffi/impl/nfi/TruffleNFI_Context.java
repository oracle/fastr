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
package com.oracle.truffle.r.ffi.impl.nfi;

import static com.oracle.truffle.r.runtime.context.FastROptions.TraceNativeCalls;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCall;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCallReturn;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logEnabled;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_DLL.NFIHandle;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

import sun.misc.Unsafe;

class UnsafeAdapter {
    public static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }
}

public class TruffleNFI_Context extends RFFIContext {

    @CompilationFinal private boolean hasAccessLock;
    private static ReentrantLock accessLock;

    public TruffleNFI_Context() {
        this(new RFFIContextState());
    }

    public TruffleNFI_Context(RFFIContextState rffiContextState) {
        super(rffiContextState, new TruffleNFI_C(), new BaseRFFI(TruffleNFI_DownCallNodeFactory.INSTANCE, TruffleNFI_DownCallNodeFactory.INSTANCE), new TruffleNFI_Call(), new TruffleNFI_DLL(),
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

    private final EnumMap<NativeFunction, TruffleObject> nativeFunctions = new EnumMap<>(NativeFunction.class);

    @SuppressWarnings("unchecked")
    @Override
    public <C extends RFFIContext> C as(Class<C> rffiCtxClass) {
        assert rffiCtxClass == TruffleNFI_Context.class;
        return (C) this;
    }

    /**
     * Looks up the given given function and returns the NFI function object.
     */
    @Override
    public TruffleObject lookupNativeFunction(NativeFunction function) {
        CompilerAsserts.neverPartOfCompilation();
        if (!nativeFunctions.containsKey(function)) {
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
                nativeFunctions.put(function, target);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        return nativeFunctions.get(function);
    }

    private static boolean variablesInitialized;

    public TruffleObject defaultLibrary;

    @Override
    public void initializeVariables(RContext context) {
        synchronized (TruffleNFI_Context.class) {
            if (!variablesInitialized) {
                variablesInitialized = true;
                RFFIVariables[] variables = RFFIVariables.initialize(context);
                boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
                try {
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
                                interop.execute(lookupNativeFunction(NativeFunction.initvar_obj), i, value);
                            }
                        } catch (InteropException ex) {
                            throw RInternalError.shouldNotReachHere(ex);
                        }
                    }
                } finally {
                    RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
                }
            }
        }
    }

    @Override
    public void initializeEmbedded(RContext context) {
        pushCallbacks();
    }

    @TruffleBoundary
    private static long initCallbacksAddress() {
        // get the address of the native thread local
        try {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            TruffleObject getCallbacksAddressFunction = (TruffleObject) interop.invokeMember(DLL.findSymbol("Rinternals_getCallbacksAddress", null).asTruffleObject(), "bind", "(): sint64");
            return (long) interop.execute(getCallbacksAddressFunction);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private void initCallbacks(RContext context) {
        if (context.getKind() == ContextKind.SHARE_NOTHING) {
            // create and fill a new callbacks table
            callbacks = UnsafeAdapter.UNSAFE.allocateMemory(Callbacks.values().length * Unsafe.ARRAY_LONG_INDEX_SCALE);
            SymbolHandle symbolHandle = DLL.findSymbol("Rinternals_addCallback", null);
            try {
                Callbacks.createCalls(new TruffleNFI_UpCallsRFFIImpl());
                for (Callbacks callback : Callbacks.values()) {
                    String addCallbackSignature = String.format("(env, sint64, sint32, %s): void", callback.nfiSignature);
                    InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                    TruffleObject addCallbackFunction = (TruffleObject) interop.invokeMember(symbolHandle.asTruffleObject(), "bind", addCallbackSignature);
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
            long oldCallbacks = UnsafeAdapter.UNSAFE.getLong(callbacksAddress);
            assert callbacksAddress != 0L;
            UnsafeAdapter.UNSAFE.putLong(callbacksAddress, callbacks);
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
        long oldCallbacks = UnsafeAdapter.UNSAFE.getLong(lastCallbacksAddress);
        assert lastCallbacksAddress != 0L;
        assert lastCallbacksAddressThread == Thread.currentThread().getId();
        UnsafeAdapter.UNSAFE.putLong(lastCallbacksAddress, callbacks);
        return oldCallbacks;
    }

    private void popCallbacks(long beforeValue) {
        if (beforeValue < 0) {
            return;
        }
        assert !singleThreadOnly || UnsafeAdapter.UNSAFE.getLong(callbacksAddress) == callbacks : "invalid nesting of native calling contexts";
        assert singleThreadOnly || UnsafeAdapter.UNSAFE.getLong(lastCallbacksAddress) == callbacks : "invalid nesting of native calling contexts";
        UnsafeAdapter.UNSAFE.putLong(callbacksAddress, beforeValue);
    }

    private DLLInfo rlibDLLInfo;

    @Override
    public ContextState initialize(RContext context) {
        if (RContext.getInstance().getOption(TraceNativeCalls)) {
            System.out.println("WARNING: The TraceNativeCalls option was discontinued!\n" +
                            "You can rerun FastR with --log.R.com.oracle.truffle.r.nativeCalls.level=FINE --log.file=<yourfile>.\n" +
                            "NOTE that stdout is problematic for embedded mode, when using this logger, also always specify a log file");
        }
        initializeLock();
        if (logEnabled()) {
            logDownCall("initialize");
        }
        if (hasAccessLock) {
            acquireLock();
        }
        try {
            String librffiPath = LibPaths.getBuiltinLibPath("R");
            if (context.isInitial()) {
                loadLibR(context, librffiPath);
            } else {
                // force initialization of NFI
                DLLRFFI.DLOpenRootNode.create(context).call(librffiPath, false, false);
            }
            switch (context.getKind()) {
                case SHARE_NOTHING:
                case SHARE_ALL:
                    // new thread, initialize properly
                    assert defaultLibrary == null && rlibDLLInfo == null;
                    rlibDLLInfo = DLL.findLibraryContainingSymbol(context, "dot_call0");
                    defaultLibrary = (TruffleObject) RContext.getInstance().getEnv().parse(Source.newBuilder("nfi", "default", "(load default)").build()).call();
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
            if (hasAccessLock) {
                releaseLock();
            }
        }
    }

    private synchronized void initializeLock() {
        hasAccessLock = RContext.getInstance().getOption(FastROptions.SynchronizeNativeCode);
        if (hasAccessLock && accessLock == null) {
            accessLock = new ReentrantLock();
        }
    }

    @Override
    public void beforeDispose(RContext context) {
        switch (context.getKind()) {
            case SHARE_NOTHING:
                UnsafeAdapter.UNSAFE.freeMemory(callbacks);
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
    public Object beforeDowncall(VirtualFrame frame, RFFIFactory.Type rffiType) {
        Object tokenFromSuper = super.beforeDowncall(frame, RFFIFactory.Type.NFI);
        transientAllocations.push(new ArrayList<>());
        if (hasAccessLock) {
            acquireLock();
        }
        return new Object[]{tokenFromSuper, pushCallbacks()};
    }

    @Override
    public void afterDowncall(Object beforeValue, RFFIFactory.Type rffiType) {
        Object[] tokens = (Object[]) beforeValue;
        super.afterDowncall(tokens[0], rffiType);
        popCallbacks((long) tokens[1]);
        for (Long ptr : transientAllocations.pop()) {
            UnsafeAdapter.UNSAFE.freeMemory(ptr);
        }
        RuntimeException lastUpCallEx = getLastUpCallException();
        setLastUpCallException(null);
        if (hasAccessLock) {
            releaseLock();
        }
        if (lastUpCallEx != null) {
            CompilerDirectives.transferToInterpreter();
            throw lastUpCallEx;
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
