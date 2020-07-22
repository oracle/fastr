package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

@GenerateUncached
public abstract class StringEltNode extends FFIUpCallNode.Arg2 {
    public static StringEltNode create() {
        return StringEltNodeGen.create();
    }

    // TODO: Make just one specialization that handles both altrep and non-altrep via
    // VectorDataLibrary
    @Specialization(limit = "1")
    Object doStringVector(RStringVector stringVector, long index,
                    @CachedLibrary("stringVector.getData()") VectorDataLibrary dataLibrary,
                    @Cached("createBinaryProfile()") ConditionProfile isAltrepProfile) {
        if (isAltrepProfile.profile(stringVector.isAltRep())) {
            return dataLibrary.getStringAt(stringVector, (int) index);
        } else {
            stringVector.wrapStrings();
            return stringVector.getWrappedDataAt((int) index);
        }
    }
}
