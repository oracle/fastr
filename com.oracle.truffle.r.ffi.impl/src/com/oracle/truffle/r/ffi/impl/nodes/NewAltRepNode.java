package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;

@GenerateUncached
public abstract class NewAltRepNode extends FFIUpCallNode.Arg3 {
    public static NewAltRepNode create() {
        return NewAltRepNodeGen.create();
    }

    @Specialization
    public Object newIntAltRep(AltIntegerClassDescriptor classDescriptor, Object data1, Object data2) {
        RAltRepData altRepData = new RAltRepData(data1, data2);
        RLogger.getLogger("altrep").fine(
                () -> "R_new_altrep: Returning vector with descriptor=" + classDescriptor.toString() + " to native."
        );
        return RDataFactory.createAltIntVector(classDescriptor, altRepData);
    }

    @Specialization
    public Object newRealAltRep(AltRealClassDescriptor classDescriptor, Object data1, Object data2) {
        throw RInternalError.unimplemented("newRealAltRep Not implemented");
    }

    @Specialization
    public Object newStringAltRep(AltStringClassDescriptor classDescriptor, Object data1, Object data2) {
        return new RAltStringVector(classDescriptor, new RAltRepData(data1, data2), true);
    }

    @Fallback
    public Object unknownAltrepType(Object classDescriptor, Object data1, Object data2) {
        throw RInternalError.shouldNotReachHere("Unknown class descriptor");
    }
}
