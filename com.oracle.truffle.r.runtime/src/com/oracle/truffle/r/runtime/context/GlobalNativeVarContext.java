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
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

import java.util.HashMap;
import java.util.Map;

public class GlobalNativeVarContext implements RContext.ContextState {
    private static final Collections.ArrayListObj<GlobalVarDescriptor> globalNativeVarDescriptors = new Collections.ArrayListObj<>(64);

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
     * Allocates descriptor structure for native global var.
     */
    @CompilerDirectives.TruffleBoundary
    public GlobalVarDescriptor allocGlobalVarDescr() {
        var descr = new GlobalVarDescriptor();
        globalNativeVarDescriptors.add(descr);
        return descr;
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
    public void beforeFinalize(RContext context) {
        callAllDestructors(context, InteropLibrary.getUncached());
    }

    /**
     * Removes {@code context} from all teh global native variable descriptors.
     * 
     * @param context Context used as the key for global native variable descriptors.
     */
    @Override
    public void beforeDispose(RContext context) {
        for (int i = 0; i < globalNativeVarDescriptors.size(); i++) {
            GlobalVarDescriptor descriptor = globalNativeVarDescriptors.get(i);
            // Some descriptors were created only for a particular context, so it is possible
            // that some descriptors do not have any information about some contexts.
            // Therefore, we have to check for `containsKey`.
            if (descriptor.containsKey(context.getId())) {
                try {
                    descriptor.removeHashEntry(context.getId());
                } catch (UnknownKeyException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }
    }

    public void callAllDestructors(RContext context, InteropLibrary interop) {
        // Do not wrap the argument to the native function.
        boolean[] whichArgToWrap = {false};
        for (int i = 0; i < destructors.size(); i++) {
            var destructor = destructors.get(i);
            Object ptrForDestructor = getGlobalVar(destructor.globalVarDescr, interop);
            interop.toNative(ptrForDestructor);
            assert interop.isPointer(ptrForDestructor);
            Object ptrForDestructorNative;
            try {
                ptrForDestructorNative = interop.asPointer(ptrForDestructor);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            Object ret = context.getRFFI().callNativeFunction(destructor.nativeFunc, destructor.nativeFuncType, Destructor.SIGNATURE,
                            new Object[]{ptrForDestructorNative}, whichArgToWrap);
            assert interop.isNull(ret);
        }
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
        boolean hasHashEntries() {
            return true;
        }

        @ExportMessage
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
            return contextIndexes.get((Integer) key);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        boolean isHashEntryModifiable(Object key) {
            return containsKey(key);
        }

        @ExportMessage
        boolean isHashEntryInsertable(Object key) {
            return true;
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        void writeHashEntry(Object key, Object value) {
            contextIndexes.put(((Integer) key), (Integer) value);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        boolean isHashEntryRemovable(Object key) {
            return containsKey(key);
        }

        @ExportMessage
        @CompilerDirectives.TruffleBoundary
        void removeHashEntry(Object key) throws UnknownKeyException {
            Integer previousValue = contextIndexes.remove((Integer) key);
            if (previousValue == null) {
                throw UnknownKeyException.create(key);
            }
        }

        @ExportMessage
        Object getHashEntriesIterator() {
            return null;
        }

        private boolean containsKey(Object key) {
            assert key instanceof Integer;
            return contextIndexes.containsKey((Integer) key);
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
