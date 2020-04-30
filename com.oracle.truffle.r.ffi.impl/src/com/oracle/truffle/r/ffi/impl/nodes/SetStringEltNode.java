package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

@GenerateUncached
public abstract class SetStringEltNode extends FFIUpCallNode.Arg3 {
    public static SetStringEltNode create() {
        return SetStringEltNodeGen.create();
    }

    @Specialization(limit = "3")
    Object doStringVector(RStringVector vector, long index, CharSXPWrapper element,
                          @CachedLibrary("vector.getData()") VectorDataLibrary dataLibrary,
                          @Cached("createBinaryProfile()") ConditionProfile isAltrepProfile) {
        if (isAltrepProfile.profile(vector.isAltRep())) {
            dataLibrary.setStringAt(vector, (int) index, element.getContents());
        } else {
            if (RRuntime.isNA(element.getContents())) {
                vector.setComplete(false);
            }
            vector.setElement((int) index, element);
        }
        return null;
    }
}
