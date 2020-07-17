package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.altrep.AltComplexClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRawClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;

@GenerateUncached
public abstract class NewAltRepNode extends FFIUpCallNode.Arg3 {
    public static NewAltRepNode create() {
        return NewAltRepNodeGen.create();
    }

    @Specialization
    public Object newIntAltRep(AltIntegerClassDescriptor classDescriptor, Object data1, Object data2) {
        RAltRepData altRepData = new RAltRepData(data1, data2);
        RLogger.getLogger(RLogger.LOGGER_ALTREP).fine(
                () -> "R_new_altrep: Returning vector with descriptor=" + classDescriptor.toString() + " to native."
        );
        return RDataFactory.createAltIntVector(classDescriptor, altRepData);
    }

    @Specialization
    public Object newRealAltRep(AltRealClassDescriptor classDescriptor, Object data1, Object data2) {
        RAltRepData altRepData = new RAltRepData(data1, data2);
        RLogger.getLogger(RLogger.LOGGER_ALTREP).fine(
                () -> "R_new_altrep: Returning vector with descriptor=" + classDescriptor.toString() + " to native."
        );
        return RDataFactory.createAltRealVector(classDescriptor, altRepData);
    }

    @Specialization
    public Object newStringAltRep(AltStringClassDescriptor classDescriptor, Object data1, Object data2) {
        return RDataFactory.createAltStringVector(classDescriptor, new RAltRepData(data1, data2));
    }

    @Specialization
    public Object newAltLogical(AltLogicalClassDescriptor descriptor, Object data1, Object data2) {
        return RDataFactory.createAltLogicalVector(descriptor, new RAltRepData(data1, data2));
    }

    @Specialization
    public Object newAltRaw(AltRawClassDescriptor descriptor, Object data1, Object data2) {
        throw RInternalError.unimplemented();
    }

    @Specialization
    public Object newAltComplex(AltComplexClassDescriptor descriptor, Object data1, Object data2) {
        throw RInternalError.unimplemented();
    }

    @Fallback
    public Object unknownAltrepType(Object classDescriptor, Object data1, Object data2) {
        throw RInternalError.shouldNotReachHere("Unknown class descriptor");
    }
}
