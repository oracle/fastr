/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

import java.util.HashMap;
import java.util.Map;

public final class GlobalNativeVarContext implements RContext.ContextState {
    /**
     * We have to wrap {@link GlobalVarDescriptor descriptors} in {@link RForeignObjectWrapper} and
     * {@link com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror} in this class,
     * because we expect all the descriptors to have static life cycle, whereas, usually, foreign
     * object wrappers and native mirrors have a life cycle tied to the life cycle of a specific
     * context.
     *
     * Note that the static life cycle of descriptors is expected, because the descriptors are
     * allocated once per every DLL load.
     */
    private static final Collections.ArrayListObj<RForeignObjectWrapper> globalNativeVarDescriptors = new Collections.ArrayListObj<>(64);

    /**
     * RContext-specific array of global native variables.
     */
    private final Collections.ArrayListObj<Object> globalNativeVars = new Collections.ArrayListObj<>();
    private final Collections.ArrayListObj<Destructor> destructors = new Collections.ArrayListObj<>();
    private final RContext context;

    private GlobalNativeVarContext(RContext context) {
        this.context = context;
    }

    public static GlobalNativeVarContext newContextState(RContext context) {
        return new GlobalNativeVarContext(context);
    }

    /**
     * Allocates descriptor structure for native global var, and wraps it in a foreign object
     * wrapper, and assigns it a native mirror.
     */
    @CompilerDirectives.TruffleBoundary
    public static RForeignObjectWrapper allocGlobalVarDescr() {
        var foreignWrapper = new RForeignObjectWrapper(new GlobalVarDescriptor());
        NativeDataAccess.createNativeMirror(foreignWrapper);
        globalNativeVarDescriptors.add(foreignWrapper);
        return foreignWrapper;
    }

    /**
     * Allocates space for a new native global variable and assigns its index into the descriptor.
     */
    public void initGlobalVar(Object descr, InteropLibrary interop) {
        assert interop.hasHashEntries(descr);
        int currGlobVarIdx = globalNativeVars.size();
        globalNativeVars.add(null);
        setGlobVarIdxForContext(context, currGlobVarIdx, descr, interop);
    }

    public void initGlobalVarWithDtor(Object descr, Object destructorNativeFunc, RFFIFactory.Type nativeFuncType, InteropLibrary interop) {
        initGlobalVar(descr, interop);
        destructors.add(new Destructor(descr, destructorNativeFunc, nativeFuncType));
    }

    public void setGlobalVar(Object descr, Object value, InteropLibrary interop) {
        assert interop.hasHashEntries(descr);
        int globVarIdx = getGlobVarIdxForContext(context, descr, interop);
        assert 0 <= globVarIdx && globVarIdx < globalNativeVars.size();
        globalNativeVars.set(globVarIdx, value);
    }

    public Object getGlobalVar(Object descr, InteropLibrary interop) {
        int globVarIdx = getGlobVarIdxForContext(context, descr, interop);
        assert 0 <= globVarIdx && globVarIdx < globalNativeVars.size();
        return globalNativeVars.get(globVarIdx);
    }

    @Override
    public void beforeFinalize(RContext ctx) {
        callAllDestructors(ctx, InteropLibrary.getUncached());
    }

    /**
     * Removes {@code context} from all teh global native variable descriptors.
     * 
     * @param ctx Context used as the key for global native variable descriptors.
     */
    @Override
    public void beforeDispose(RContext ctx) {
        for (int i = 0; i < globalNativeVarDescriptors.size(); i++) {
            GlobalVarDescriptor descriptor = getDesriptorAt(i);
            // Some descriptors were created only for a particular context, so it is possible
            // that some descriptors do not have any information about some contexts.
            // Therefore, we have to check for `containsKey`.
            if (descriptor.containsKey(ctx.getId())) {
                try {
                    descriptor.removeHashEntry(ctx.getId());
                } catch (UnknownKeyException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }
    }

    public void callAllDestructors(RContext ctx, InteropLibrary interop) {
        // Do not wrap the argument to the native function.
        boolean[] whichArgToWrap = {false};
        for (int i = 0; i < destructors.size(); i++) {
            var destructor = destructors.get(i);
            Object ptrForDestructor = getGlobalVar(destructor.globalVarDescr, interop);
            interop.toNative(ptrForDestructor);
            assert interop.isPointer(ptrForDestructor);
            Object ret = ctx.getRFFI().callNativeFunction(destructor.nativeFunc, destructor.nativeFuncType, Destructor.SIGNATURE,
                            new Object[]{ptrForDestructor}, whichArgToWrap);
            assert interop.isNull(ret);
        }
    }

    /**
     * Method for debugging purposes.
     */
    public static void printAllDescriptors(RContext context) {
        assert globalNativeVarDescriptors.size() == globalNativeVarDescriptors.size();
        context.getConsole().println("All foreign wrappers for descriptors: [");
        for (int i = 0; i < globalNativeVarDescriptors.size(); i++) {
            var descrWrapper = globalNativeVarDescriptors.get(i);
            context.getConsole().println(
                            String.format("  RForeignObjectWrapper{nativeMirror = %s, delegate = %s}, ",
                                            descrWrapper.getNativeMirror(), descrWrapper.getDelegate()));
        }
        context.getConsole().println("]");
    }

    private static GlobalVarDescriptor getDesriptorAt(int idx) {
        RForeignObjectWrapper foreignWrapper = globalNativeVarDescriptors.get(idx);
        assert foreignWrapper.getDelegate() instanceof GlobalVarDescriptor;
        assert foreignWrapper.getNativeMirror() != null;
        return (GlobalVarDescriptor) foreignWrapper.getDelegate();
    }

    private static int getGlobVarIdxForContext(RContext context, Object descr, InteropLibrary interop) {
        assert interop.hasHashEntries(descr);
        try {
            return (int) interop.readHashValue(descr, context.getId());
        } catch (UnsupportedMessageException | ClassCastException | UnknownKeyException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    /**
     *
     * @param idx Index into {@link #globalNativeVars}.
     * @param descr Global var descriptor (represented by {@link GlobalVarDescriptor}).
     */
    private static void setGlobVarIdxForContext(RContext context, int idx, Object descr, InteropLibrary interop) {
        assert interop.hasHashEntries(descr);
        try {
            interop.writeHashEntry(descr, context.getId(), idx);
        } catch (UnsupportedMessageException | UnsupportedTypeException | UnknownKeyException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class GlobalVarDescriptor implements TruffleObject {
        /**
         * contextIndexes[i] denotes an index to an array of global variables in i-th RContext.
         */
        private final Map<Integer, Integer> contextIndexes = new HashMap<>();

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean hasHashEntries() {
            return true;
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        long getHashSize() {
            return contextIndexes.size();
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        boolean isHashEntryReadable(Object key) {
            return containsKey(key);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        Object readHashValue(Object key) {
            return contextIndexes.get(key);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        boolean isHashEntryModifiable(Object key) {
            return containsKey(key);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        boolean isHashEntryInsertable(@SuppressWarnings("unused") Object key) {
            return true;
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        void writeHashEntry(Object key, Object value) {
            contextIndexes.put((Integer) key, (Integer) value);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        boolean isHashEntryRemovable(Object key) {
            return containsKey(key);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        void removeHashEntry(Object key) throws UnknownKeyException {
            Integer previousValue = contextIndexes.remove(key);
            if (previousValue == null) {
                throw UnknownKeyException.create(key);
            }
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        Object getHashEntriesIterator() {
            return null;
        }

        private boolean containsKey(Object key) {
            assert key instanceof Integer;
            return contextIndexes.containsKey(key);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("GlobalVarDescriptor{");
            contextIndexes.forEach((key, value) -> {
                sb.append("RContext(").append(key).append("):").append(value).append(", ");
            });
            if (!contextIndexes.isEmpty()) {
                sb.delete(sb.length() - 2, sb.length());
            }
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Represents a native function callback that is called during the context finalization. Needed
     * for native array global variables for which there was heap memory allocated.
     */
    private static final class Destructor {
        static final String SIGNATURE = "(pointer): void";

        final Object globalVarDescr;
        /**
         * The signature of the native function is {@code void dtor(void *ptr)}, where {@code ptr}
         * is the underlying pointer fetched via {@code FASTR_GlobalVarGetPtr}.
         */
        final Object nativeFunc;
        final RFFIFactory.Type nativeFuncType;

        private Destructor(Object globalVarDescr, Object nativeFunc, RFFIFactory.Type nativeFuncType) {
            this.globalVarDescr = globalVarDescr;
            this.nativeFunc = nativeFunc;
            this.nativeFuncType = nativeFuncType;
        }

        @Override
        public String toString() {
            return String.format("Destructor{nativeFunc = %s (%s), descr = %s}", nativeFunc, nativeFuncType, globalVarDescr);
        }
    }
}
