package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;

@GenerateUncached
@ImportStatic(DSLConfig.class)
public abstract class IntegerIsSortedNode extends FFIUpCallNode.Arg1 {

    public static IntegerIsSortedNode create() {
        return IntegerIsSortedNodeGen.create();
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    public int doForVector(RIntVector intVec,
                           @CachedLibrary("intVec.getData()") VectorDataLibrary dataLibrary) {
        Object vecData = intVec.getData();
        // The translation between (boolean descending, boolean naLast) to AltrepSortedness enum is
        // inspired by code in GNU-R (sort.c : makeSortEnum).
        AltrepSortedness sortedness = null;
        if (dataLibrary.isSorted(vecData, false, false)) {
            sortedness = AltrepSortedness.SORTED_INCR_NA_1ST;
        } else if (dataLibrary.isSorted(vecData, false, true)) {
            sortedness = AltrepSortedness.SORTED_INCR;
        } else if (dataLibrary.isSorted(vecData, true, false)) {
            sortedness = AltrepSortedness.SORTED_DECR_NA_1ST;
        } else if (dataLibrary.isSorted(vecData, true, true)) {
            sortedness = AltrepSortedness.SORTED_DECR;
        } else {
            sortedness = AltrepSortedness.UNKNOWN_SORTEDNESS;
        }
        return sortedness.getValue();
    }

    @Fallback
    public int isSortedFallback(@SuppressWarnings("unused") Object x) {
        return AltrepSortedness.UNKNOWN_SORTEDNESS.getValue();
    }
}
