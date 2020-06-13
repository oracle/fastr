package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntArrayVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltrepDuplicateNode extends RBaseNode {
    public abstract Object execute(Object altVec, boolean deep);

    @Specialization(guards = {"hasDuplicateMethod(altIntVec)"})
    protected Object doAltIntWithDuplicateMethod(RIntVector altIntVec, boolean deep,
                                              @Cached("createBinaryProfile()") ConditionProfile duplicateReturnsNullProfile,
                                              @Cached FFIUnwrapNode unwrapNode,
                                              @Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                              @Cached AltrepRFFI.AltIntLengthNode lengthNode,
                                              @Cached AltrepRFFI.AltIntDuplicateNode duplicateNode) {
        assert AltrepUtilities.isAltrep(altIntVec);
        Object duplicatedObject = duplicateNode.execute(altIntVec, deep);
        if (duplicateReturnsNullProfile.profile(duplicatedObject == null)) {
            return doStandardDuplicate(altIntVec, deep, dataptrNode, lengthNode);
        } else {
            RIntVector duplicatedVector = (RIntVector) unwrapNode.execute(duplicatedObject);
            // We have to return data, not the whole vector. However, even if the Duplicate method returns the whole vector.
            return duplicatedVector.getData();
        }
    }

    @Specialization(replaces = {"doAltIntWithDuplicateMethod"})
    protected Object doStandardDuplicate(RIntVector altIntVec, @SuppressWarnings("unused") boolean deep,
                                      @Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                      @Cached AltrepRFFI.AltIntLengthNode lengthNode) {
        assert AltrepUtilities.isAltrep(altIntVec);
        int length = lengthNode.execute(altIntVec);
        long dataptrAddr = dataptrNode.execute(altIntVec, false);
        int[] newData = new int[length];
        NativeMemory.copyMemory(dataptrAddr, newData, ElementType.INT, length);
        return new RIntArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
    }

    protected static boolean hasDuplicateMethod(RIntVector altIntVec) {
        return AltrepUtilities.getAltIntDescriptor(altIntVec).isDuplicateMethodRegistered();
    }
}
