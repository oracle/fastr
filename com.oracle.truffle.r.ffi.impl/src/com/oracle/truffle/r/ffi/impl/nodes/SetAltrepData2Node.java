package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
// TODO: Optimize
public abstract class SetAltrepData2Node extends FFIUpCallNode.Arg2 {
    public static SetAltrepData2Node create() {
        return SetAltrepData2NodeGen.create();
    }

    @Specialization
    public Object doAltInt(RAltIntegerVec altIntVec, Object data2) {
        altIntVec.setData2(data2);
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
}
