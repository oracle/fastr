package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.nodes.CopyWithAttributes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltrepDuplicateNode extends RBaseNode {
    public abstract Object execute(Object altVec, boolean deep);

    @Specialization(guards = {"isAltrep(altIntVec)", "hasDuplicateMethod(altIntVec)"}, limit = "1")
    public Object doAltIntWithDuplicateMethod(RIntVector altIntVec, boolean deep,
                                              @CachedLibrary("getDuplicateMethod(altIntVec)") InteropLibrary duplicateMethodInterop,
                                              @CachedLibrary("altIntVec") AbstractContainerLibrary containerLibrary,
                                              @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile,
                                              @Cached("createBinaryProfile()") ConditionProfile duplicateReturnsNullProfile,
                                              @Cached CopyWithAttributes copyWithAttributes) {
        AltIntegerClassDescriptor descriptor = getAltIntDescriptor(altIntVec);
        Object duplicatedObject = descriptor.invokeDuplicateMethodCached(altIntVec, deep, duplicateMethodInterop, hasMirrorProfile);
        if (duplicateReturnsNullProfile.profile(duplicatedObject == null)) {
            return doStandardDuplicate(altIntVec, deep, copyWithAttributes, containerLibrary);
        } else {
            // TODO: Duplicate also attributes (as in GNU-R's DuplicateEX)
            return duplicatedObject;
        }
    }

    @Specialization(replaces = {"doAltIntWithDuplicateMethod"}, limit = "3")
    public Object doStandardDuplicate(RIntVector intVector, boolean deep,
                                      @Cached(allowUncached = true) CopyWithAttributes copyWithAttributes,
                                      @CachedLibrary("intVector") AbstractContainerLibrary containerLibrary) {
        return copyWithAttributes.execute(containerLibrary, intVector);
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
        return getAltIntDescriptor(altIntVec).getDuplicateMethod();
    }
}
