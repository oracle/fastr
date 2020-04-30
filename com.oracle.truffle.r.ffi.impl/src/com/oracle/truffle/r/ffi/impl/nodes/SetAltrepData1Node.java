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
public abstract class SetAltrepData1Node extends FFIUpCallNode.Arg2 {
    public static SetAltrepData1Node create() {
        return SetAltrepData1NodeGen.create();
    }

    @Specialization(guards = "isAltrep(altIntVec)", limit = "3")
    public Object doAltInt(RIntVector altIntVec, Object data1,
                           @CachedLibrary("getPairListDataFromVec(altIntVec)") RPairListLibrary pairListLibrary) {
        pairListLibrary.setCar(getPairListDataFromVec(altIntVec), data1);
        return null;
    }

    @Specialization(limit = "3", guards = "isAltrep(altStringVec)")
    public Object doAltString(RStringVector altStringVec, Object data1,
                              @CachedLibrary("getPairListDataFromVec(altStringVec)") RPairListLibrary pairListLibrary) {
        pairListLibrary.setCar(getPairListDataFromVec(altStringVec), data1);
        return null;
    }

    @Fallback
    public Object fallback(Object vector, Object data1) {
        throw RInternalError.shouldNotReachHere("R_set_altrep_data1: Unknown type = " + vector.getClass().getSimpleName());
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
