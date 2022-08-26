package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import org.graalvm.collections.EconomicMap;

public class GlobalNativeVarContext implements RContext.ContextState {
    // TODO: Remove?
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
    public GlobalVarDescriptor allocGlobalVarDescr() {
        var descr = new GlobalVarDescriptor();
        globalNativeVarDescriptors.add(descr);
        return descr;
    }

    /**
     * Allocates space for a new native global variable and assigns its index into the descriptor.
     */
    public void initGlobalVar(Object descr, InteropLibrary interop) {
        assert interop.hasArrayElements(descr);
        int currGlobVarIdx = globalNativeVars.size();
        globalNativeVars.add(null);
        setGlobVarIdxForContext(context, currGlobVarIdx, descr, interop);
    }

    public void initGlobalVarWithDtor(Object descr, Object destructorNativeFunc, RFFIFactory.Type nativeFuncType, InteropLibrary interop) {
        initGlobalVar(descr, interop);
        destructors.add(new Destructor(descr, destructorNativeFunc, nativeFuncType));
    }

    public void setGlobalVar(Object descr, Object value, InteropLibrary interop) {
        assert interop.hasArrayElements(descr);
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
        callAllDestructors(context);
    }

    public void callAllDestructors(RContext context) {
        for (int i = 0; i < destructors.size(); i++) {
            var destructor = destructors.get(i);
            Object ret = context.getRFFI().callNativeFunction(destructor.nativeFunc, destructor.nativeFuncType, Destructor.SIGNATURE, new Object[]{destructor.globalVarDescr});
            assert InteropLibrary.getUncached().isNull(ret);
        }
    }

    private static int getGlobVarIdxForContext(RContext context, Object descr, InteropLibrary interop) {
        try {
            return (int) interop.readArrayElement(descr, context.getId());
        } catch (UnsupportedMessageException | ClassCastException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    /**
     *
     * @param idx Index into {@link #globalNativeVars}.
     * @param descr Global var descriptor (represented by {@link GlobalVarDescriptor}).
     */
    private static void setGlobVarIdxForContext(RContext context, int idx, Object descr, InteropLibrary interop) {
        assert interop.hasArrayElements(descr);
        try {
            interop.writeArrayElement(descr, context.getId(), idx);
        } catch (UnsupportedMessageException | UnsupportedTypeException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class GlobalVarDescriptor implements TruffleObject {
        /**
         * contextIndexes[i] denotes an index to an array of global variables in i-th RContext.
         */
        private final Collections.ArrayListInt contextIndexes = new Collections.ArrayListInt();

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return contextIndexes.size();
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return isIndexInBounds(index);
        }

        @ExportMessage
        public boolean isArrayElementModifiable(long index) {
            return isIndexInBounds(index);
        }

        @ExportMessage
        public boolean isArrayElementInsertable(long index) {
            return isIndexInBounds(index);
        }

        @ExportMessage
        void writeArrayElement(long index, Object value) throws UnsupportedTypeException, InvalidArrayIndexException {
            if (!(value instanceof Integer)) {
                throw UnsupportedTypeException.create(new Object[]{value});
            }
            if (isIndexInBounds(index)) {
                contextIndexes.set((int) index, (int) value);
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        }

        @ExportMessage
        Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (isIndexInBounds(index)) {
                return contextIndexes.get((int) index);
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        }

        private boolean isIndexInBounds(long index) {
            return 0 <= index && index < contextIndexes.size();
        }
    }

    /**
     * Represents a native function callback that is called during the context finalization.
     * Needed for native array global variables for which there was heap memory allocated.
     */
    private static final class Destructor {
        static final String SIGNATURE = "(pointer): void";

        final Object globalVarDescr;
        final Object nativeFunc;
        final RFFIFactory.Type nativeFuncType;

        private Destructor(Object globalVarDescr, Object nativeFunc, RFFIFactory.Type nativeFuncType) {
            this.globalVarDescr = globalVarDescr;
            this.nativeFunc = nativeFunc;
            this.nativeFuncType = nativeFuncType;
        }
    }
}
