package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.Collections;
import com.oracle.truffle.r.runtime.RInternalError;
import org.graalvm.collections.EconomicMap;

public class GlobalNativeVarContext implements RContext.ContextState {
    // TODO: Remove?
    private static Collections.ArrayListObj<GlobalVarDescriptor> globalNativeVarDescriptors = new Collections.ArrayListObj<>(64);

    private final Collections.ArrayListObj<Object> globalNativeVars = new Collections.ArrayListObj<>();
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
        assert interop.hasHashEntries(descr);
        int currGlobVarIdx = globalNativeVars.size();
        globalNativeVars.add(null);
        setGlobVarIdxForContext(context, currGlobVarIdx, descr, interop);
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

    private static int getGlobVarIdxForContext(RContext context, Object descr, InteropLibrary interop) {
        int globVarIdx = -1;
        try {
            globVarIdx = (int) interop.readHashValue(descr, context.getId());
        } catch (UnsupportedMessageException | UnknownKeyException | ClassCastException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
        return globVarIdx;
    }

    private static void setGlobVarIdxForContext(RContext context, int idx, Object descr, InteropLibrary interop) {
        assert interop.hasHashEntries(descr);
        try {
            interop.writeHashEntry(descr, context.getId(), idx);
        } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class GlobalVarDescriptor implements TruffleObject {
        // Maps ID of RContext to index into RContext-specific array containing values of global variables.
        // TODO: Get rid of hash table, because it is not PE-friendly.
        private final EconomicMap<Integer, Integer> contextToIndexMap = EconomicMap.create();

        @ExportMessage
        boolean hasHashEntries() {
            return true;
        }

        @ExportMessage
        long getHashSize() {
            return contextToIndexMap.size();
        }

        @ExportMessage
        boolean isHashEntryReadable(Object key) {
            return true;
        }

        @ExportMessage
        boolean isHashEntryModifiable(Object key) {
            return true;
        }

        @ExportMessage
        boolean isHashEntryInsertable(Object key) {
            return true;
        }

        @ExportMessage
        Object readHashValue(Object key) {
            assert key instanceof Integer;
            return contextToIndexMap.get((Integer) key);
        }

        @ExportMessage
        void writeHashEntry(Object key, Object value) {
            assert key instanceof Integer;
            assert value instanceof Integer;
            contextToIndexMap.put((Integer) key, (Integer) value);
        }

        @ExportMessage
        Object getHashEntriesIterator() {
            throw new UnsupportedOperationException("unimplemented");
        }
    }

}
