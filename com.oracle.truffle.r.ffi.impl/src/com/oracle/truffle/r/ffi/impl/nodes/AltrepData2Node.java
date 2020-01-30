package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
public abstract class AltrepData2Node extends FFIUpCallNode.Arg1 {
    public static AltrepData2Node create() {
        return AltrepData2NodeGen.create();
    }

    @Specialization(limit = "3")
    public Object doAltInt(RAltIntegerVec altIntVec,
                           @CachedLibrary("getPairListDataFromVec(altIntVec)") RPairListLibrary pairListLibrary) {
        return pairListLibrary.cdr(getPairListDataFromVec(altIntVec));
    }

    @Specialization(limit = "3")
    public Object doAltString(RAltStringVector altStringVec,
                              @CachedLibrary("getPairListDataFromVec(altStringVec)") RPairListLibrary pairListLibrary) {
        return pairListLibrary.cdr(getPairListDataFromVec(altStringVec));
    }

    @Fallback
    public Object fallback(Object object) {
        throw RInternalError.shouldNotReachHere("Unknown type " + object.getClass().getSimpleName() + " for R_altrep_data2");
    }

    protected static RPairList getPairListDataFromVec(RAltIntegerVec altIntVec) {
        return altIntVec.getData().getDataPairList();
    }

    protected static RPairList getPairListDataFromVec(RAltStringVector altStringVec) {
        return altStringVec.getData().getDataPairList();
    }
}
