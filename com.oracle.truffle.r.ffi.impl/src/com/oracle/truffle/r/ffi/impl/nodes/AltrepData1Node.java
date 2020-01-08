package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;

@GenerateUncached
public abstract class AltrepData1Node extends FFIUpCallNode.Arg1 {
    public static AltrepData1Node create() {
        return AltrepData1NodeGen.create();
    }

    @Specialization(limit = "3")
    public Object data1ForAltrep(RAltIntegerVec altIntVec,
                                 @CachedLibrary("getPairListDataFromVec(altIntVec)") RPairListLibrary pairListLibrary) {
        return pairListLibrary.car(getPairListDataFromVec(altIntVec));
    }

    @Fallback
    public Object fallback(Object object) {
        throw RInternalError.shouldNotReachHere("Unknown type" + object.getClass().getSimpleName());
    }

    protected static RPairList getPairListDataFromVec(RAltIntegerVec altIntVec) {
        return altIntVec.getData().getDataPairList();
    }
}
