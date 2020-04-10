package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
public abstract class AltrepData2Node extends FFIUpCallNode.Arg1 {
    public static AltrepData2Node create() {
        return AltrepData2NodeGen.create();
    }

    @Specialization(limit = "3", guards = "isAltrep(altIntVec)")
    public Object doAltInt(RIntVector altIntVec,
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

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static RPairList getPairListDataFromVec(RIntVector altIntVec) {
        return AltrepUtilities.getPairListDataFromVec(altIntVec);
    }

    protected static RPairList getPairListDataFromVec(RAltStringVector altStringVec) {
        return AltrepUtilities.getPairListDataFromVec(altStringVec);
    }
}
