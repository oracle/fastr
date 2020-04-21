package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltrepLengthNode extends RBaseNode {
    public abstract Object execute(Object altVec);

    public static AltrepLengthNode getUncached() {
        return AltrepLengthNodeGen.getUncached();
    }

    /**
     * Specializes only on one descriptor (which is one altinteger vector), therefore the limit is set to 1.
     */
    @Specialization(guards = {"isAltrep(altIntVec)", "getAltIntDescriptor(altIntVec).equals(cachedDescriptor)"},
        limit = "1")
    public int doAltIntCachedDescriptor(RIntVector altIntVec,
                                        @Cached(value = "getAltIntDescriptor(altIntVec)", allowUncached = true) AltIntegerClassDescriptor cachedDescriptor,
                                        // TODO: Muze se CachedLibrary odkazovat na parameter z Cached?
                                        @CachedLibrary("getLengthMethodFromDescriptor(altIntVec)") InteropLibrary lengthMethodInterop,
                                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
        return cachedDescriptor.invokeLengthMethodCached(altIntVec, lengthMethodInterop, hasMirrorProfile);
    }

    @Specialization(guards = "isAltrep(altIntVec)", limit = "3")
    public int doAltIntUncachedDescriptor(RIntVector altIntVec,
                                          @CachedLibrary("getLengthMethodFromDescriptor(altIntVec)") InteropLibrary lengthMethodInterop,
                                          @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
        AltIntegerClassDescriptor descriptor = getAltIntDescriptor(altIntVec);
        return descriptor.invokeLengthMethodCached(altIntVec, lengthMethodInterop, hasMirrorProfile);
    }

    @Specialization(guards = "isAltrep(altIntVec)", replaces = {"doAltIntCachedDescriptor", "doAltIntUncachedDescriptor"})
    public int doAltIntUncached(RIntVector altIntVec,
                                @CachedLibrary(limit = "1") InteropLibrary lengthMethodInterop,
                                @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
        AltIntegerClassDescriptor descriptor = getAltIntDescriptor(altIntVec);
        return descriptor.invokeLengthMethodCached(altIntVec, lengthMethodInterop, hasMirrorProfile);
    }

    protected static boolean isAltrep(Object vector) {
        return AltrepUtilities.isAltrep(vector);
    }

    protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
        return AltrepUtilities.getAltIntDescriptor(altIntVec);
    }

    protected static Object getLengthMethodFromDescriptor(RIntVector altIntVec) {
        return getAltIntDescriptor(altIntVec).getLengthMethod();
    }
}
