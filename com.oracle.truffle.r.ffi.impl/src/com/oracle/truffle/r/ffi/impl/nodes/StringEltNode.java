package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;


@GenerateUncached
public abstract class StringEltNode extends FFIUpCallNode.Arg2 {
    public static StringEltNode create() {
        return StringEltNodeGen.create();
    }

    @Specialization
    Object doStringVector(RStringVector stringVector, long index) {
        stringVector.wrapStrings();
        return stringVector.getWrappedDataAt((int) index);
    }

    @Specialization(limit = "1")
    Object doAltStringVector(RAltStringVector altStringVector, long index,
                             @CachedLibrary("altStringVector.getDescriptor().getEltMethod()") InteropLibrary eltMethodInterop,
                             @Cached("createBinaryProfile()")ConditionProfile hasMirrorProfile) {
        return altStringVector.getDescriptor().invokeEltMethodCached(altStringVector, (int) index, eltMethodInterop, hasMirrorProfile);
    }
}
