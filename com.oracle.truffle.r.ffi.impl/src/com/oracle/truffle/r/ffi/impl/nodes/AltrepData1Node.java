package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;

@GenerateUncached
public abstract class AltrepData1Node extends FFIUpCallNode.Arg1 {
    public static AltrepData1Node create() {
        return AltrepData1NodeGen.create();
    }

    @Specialization(limit = "3", guards = "isAltrep(altIntVec)")
    public Object doAltInt(RIntVector altIntVec,
                           @CachedLibrary("getPairListDataFromVec(altIntVec)") RPairListLibrary pairListLibrary) {
        return pairListLibrary.car(getPairListDataFromVec(altIntVec));
    }

    @Specialization(limit = "3", guards = "isAltrep(altStringVec)")
    public Object doAltString(RStringVector altStringVec,
                              @CachedLibrary("getPairListDataFromVec(altStringVec)") RPairListLibrary pairListLibrary) {
        return pairListLibrary.car(getPairListDataFromVec(altStringVec));
    }

    @Fallback
    public Object fallback(Object object) {
        throw RInternalError.shouldNotReachHere("Unknown type " + object.getClass().getSimpleName() + " for R_altrep_data1");
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static RPairList getPairListDataFromVec(RIntVector altIntVec) {
        return AltrepUtilities.getPairListDataFromVec(altIntVec);
    }

    protected static RPairList getPairListDataFromVec(RStringVector altStringVec) {
        return AltrepUtilities.getPairListDataFromVec(altStringVec);
    }
}
