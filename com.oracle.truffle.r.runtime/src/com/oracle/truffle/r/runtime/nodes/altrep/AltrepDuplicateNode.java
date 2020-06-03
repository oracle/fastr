package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.RIntArrayVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltrepDuplicateNode extends RBaseNode {
    public abstract Object execute(Object altVec, boolean deep);

    @Specialization(guards = {"isAltrep(altIntVec)", "hasDuplicateMethod(altIntVec)"}, limit = "1")
    public Object doAltIntWithDuplicateMethod(RIntVector altIntVec, boolean deep,
                                              @CachedLibrary("getDuplicateMethod(altIntVec)") InteropLibrary duplicateMethodInterop,
                                              @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile,
                                              @Cached("createBinaryProfile()") ConditionProfile duplicateReturnsNullProfile,
                                              @Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                              @Cached AltrepRFFI.AltIntLengthNode lengthNode) {
        AltIntegerClassDescriptor descriptor = getAltIntDescriptor(altIntVec);
        Object duplicatedObject = descriptor.invokeDuplicateMethodCached(altIntVec, deep, duplicateMethodInterop, hasMirrorProfile);
        if (duplicateReturnsNullProfile.profile(duplicatedObject == null)) {
            return doStandardDuplicate(altIntVec, deep, dataptrNode, lengthNode);
        } else {
            assert duplicatedObject instanceof NativeMirror; // TODO: debug this line
            assert ((NativeMirror) duplicatedObject).getDelegate() instanceof RIntVector;
            RIntVector duplicatedVector = (RIntVector) ((NativeMirror) duplicatedObject).getDelegate();
            // We have to return data, not the whole vector. However, the Duplicate method returns whole vector.
            return duplicatedVector.getData();
        }
    }

    @Specialization(guards = {"isAltrep(altIntVec)"}, replaces = {"doAltIntWithDuplicateMethod"}, limit = "3")
    public Object doStandardDuplicate(RIntVector altIntVec, @SuppressWarnings("unused") boolean deep,
                                      @Cached AltrepRFFI.AltIntDataptrNode dataptrNode,
                                      @Cached AltrepRFFI.AltIntLengthNode lengthNode) {
        int length = lengthNode.execute(altIntVec);
        long dataptrAddr = dataptrNode.execute(altIntVec, false);
        int[] newData = new int[length];
        NativeMemory.copyMemory(dataptrAddr, newData, ElementType.INT, length);
        // TODO: complete=true?
        return new RIntArrayVectorData(newData, true);
    }

    protected static boolean isAltrep(Object vector) {
        return AltrepUtilities.isAltrep(vector);
    }

    protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
        return AltrepUtilities.getAltIntDescriptor(altIntVec);
    }

    protected static boolean hasDuplicateMethod(RIntVector altIntVec) {
        return AltrepUtilities.getAltIntDescriptor(altIntVec).isDuplicateMethodRegistered();
    }

    protected static Object getDuplicateMethod(RIntVector altIntVec) {
        return getAltIntDescriptor(altIntVec).getDuplicateMethodDescriptor().method;
    }
}
