package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
// TODO: Optimize
public abstract class SetAltrepData2Node extends FFIUpCallNode.Arg2 {
    public static SetAltrepData2Node create() {
        return SetAltrepData2NodeGen.create();
    }

    @Specialization(guards = "isAltrep(altIntVec)")
    public Object doAltInt(RIntVector altIntVec, Object data2) {
        assert altIntVec.getData() instanceof RAltIntVectorData;
        ((RAltIntVectorData) altIntVec.getData()).getData().setData2(data2);
        return null;
    }

    @Specialization
    public Object doAltString(RAltStringVector altStringVec, Object data2) {
        altStringVec.setData2(data2);
        return null;
    }

    @Fallback
    public Object fallback(Object vector, Object data1) {
        throw RInternalError.shouldNotReachHere("R_set_altrep_data1: Unknown type = " + vector.getClass().getSimpleName());
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }
}
