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
public abstract class AltrepData2Node extends FFIUpCallNode.Arg1 {
    public static AltrepData2Node create() {
        return AltrepData2NodeGen.create();
    }

    @Specialization(guards = "altrepVec == cachedAltrepVec", limit = "getGenericDataLibraryCacheSize()")
    public Object getData2FromAltrepCached(@SuppressWarnings("unused") RAbstractAtomicVector altrepVec,
                    @Cached("altrepVec") @SuppressWarnings("unused") RAbstractAtomicVector cachedAltrepVec,
                    @Cached("getPairListData(altrepVec)") RPairList pairListData,
                    @CachedLibrary("pairListData") RPairListLibrary pairListLibrary) {
        return pairListLibrary.cdr(pairListData);
    }

    @Specialization(replaces = "getData2FromAltrepCached")
    public Object getData2FromAltrepUncached(RAbstractAtomicVector altrepVec) {
        RPairList pairListData = AltrepUtilities.getPairListData(altrepVec);
        return pairListData.cdr();
    }

    @Fallback
    public Object fallback(Object object) {
        throw RInternalError.shouldNotReachHere("Unknown type " + object.getClass().getSimpleName() + " for R_altrep_data2");
    }
}
