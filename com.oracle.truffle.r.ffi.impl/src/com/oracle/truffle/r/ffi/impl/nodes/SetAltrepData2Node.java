package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;

@GenerateUncached
@ImportStatic({AltrepUtilities.class, DSLConfig.class})
public abstract class SetAltrepData2Node extends FFIUpCallNode.Arg2 {
    public static SetAltrepData2Node create() {
        return SetAltrepData2NodeGen.create();
    }

    // TODO: There is not a cached version because RPairListLibrary does not yet implement
    //  setCdr message.

    @Specialization
    public Object setData2Uncached(RAbstractAtomicVector altrepVec, Object data2) {
        RPairList pairListData = AltrepUtilities.getPairListData(altrepVec);
        pairListData.setCdr(data2);
        return null;
    }

    @Fallback
    public Object fallback(Object vector, @SuppressWarnings("unused") Object data2) {
        throw RInternalError.shouldNotReachHere("R_set_altrep_data2: Unknown type = " + vector.getClass().getSimpleName());
    }
}
