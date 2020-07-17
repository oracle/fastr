package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleArrayVectorData;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntArrayVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic(AltrepUtilities.class)
public abstract class AltrepDuplicateNode extends RBaseNode {
    public abstract Object execute(Object altVec, boolean deep);

    @Specialization(guards = "hasDuplicateMethodRegistered(altrepVec)")
    protected Object duplicateAltrepWithDuplicateMethod(RAbstractAtomicVector altrepVec, boolean deep,
                        @Cached ConditionProfile duplicateReturnsNullProfile,
                        @Cached FFIUnwrapNode unwrapNode,
                        @Cached AltrepRFFI.DataptrNode dataptrNode,
                        @Cached AltrepRFFI.LengthNode lengthNode,
                        @Cached AltrepRFFI.DuplicateNode duplicateNode) {
        assert altrepVec.isAltRep();
        Object duplicatedObject = duplicateNode.execute(altrepVec, deep);
        if (duplicateReturnsNullProfile.profile(duplicatedObject == null)) {
            return doStandardDuplicate(altrepVec, deep, dataptrNode, lengthNode);
        } else {
            RAbstractAtomicVector duplicatedVector = (RAbstractAtomicVector) unwrapNode.execute(duplicatedObject);
            // We have to return data, not the whole vector.
            return duplicatedVector.getData();
        }
    }

    @Specialization(replaces = {"duplicateAltrepWithDuplicateMethod"})
    protected Object doStandardDuplicate(RAbstractAtomicVector altrepVec, @SuppressWarnings("unused") boolean deep,
                                      @Cached AltrepRFFI.DataptrNode dataptrNode,
                                      @Cached AltrepRFFI.LengthNode lengthNode) {
        assert AltrepUtilities.isAltrep(altrepVec);
        int length = lengthNode.execute(altrepVec);
        long dataptrAddr = dataptrNode.execute(altrepVec, false);
        if (altrepVec instanceof RIntVector) {
            int[] newData = new int[length];
            NativeMemory.copyMemory(dataptrAddr, newData, ElementType.INT, length);
            return new RIntArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
        } else if (altrepVec instanceof RDoubleVector) {
            double[] newData = new double[length];
            NativeMemory.copyMemory(dataptrAddr, newData, ElementType.DOUBLE, length);
            return new RDoubleArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            throw RInternalError.unimplemented();
        }
    }
}
