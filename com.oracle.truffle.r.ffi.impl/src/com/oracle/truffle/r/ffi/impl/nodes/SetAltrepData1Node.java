package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;

@GenerateUncached
@ImportStatic({AltrepUtilities.class, DSLConfig.class})
public abstract class SetAltrepData1Node extends FFIUpCallNode.Arg2 {
    public static SetAltrepData1Node create() {
        return SetAltrepData1NodeGen.create();
    }

    @Specialization(guards = "altrepVec == cachedAltrepVec", limit = "getGenericDataLibraryCacheSize()")
    public Object setData1Cached(@SuppressWarnings("unused") RAbstractAtomicVector altrepVec, Object data1,
                           @Cached("altrepVec") @SuppressWarnings("unused") RAbstractAtomicVector cachedAltrepVec,
                           @Cached("getPairListData(altrepVec)") RPairList pairListData,
                           @CachedLibrary("pairListData") RPairListLibrary pairListLibrary) {
        pairListLibrary.setCar(pairListData, data1);
        return null;
    }

    @Specialization(replaces = "setData1Cached")
    public Object setData1Uncached(RAbstractAtomicVector altrepVec, Object data1) {
        RPairList pairListData = AltrepUtilities.getPairListData(altrepVec);
        pairListData.setCar(data1);
        return null;
    }

    @Fallback
    public Object fallback(Object vector, @SuppressWarnings("unused") Object data1) {
        throw RInternalError.shouldNotReachHere("R_set_altrep_data1: Unknown type = " + vector.getClass().getSimpleName());
    }
}
