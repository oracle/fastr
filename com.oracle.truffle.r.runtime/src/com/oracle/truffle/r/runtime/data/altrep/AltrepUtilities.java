package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RIntVectorData;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

public class AltrepUtilities {
    public static boolean isAltrep(Object object) {
        return object instanceof RBaseObject && ((RBaseObject) object).isAltRep();
    }

    private static RAltIntVectorData getAltIntVectorData(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return (RAltIntVectorData) altIntVector.getData();
    }

    private static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return getAltIntVectorData(altIntVector).getDescriptor();
    }

    public static AltRepClassDescriptor getDescriptorFromAltrepObj(RBaseObject altrepObject) {
        assert altrepObject.isAltRep();

        if (altrepObject instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) altrepObject);
        } else if (altrepObject instanceof RAltStringVector) {
            return ((RAltStringVector) altrepObject).getDescriptor();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    public static boolean hasCoerceMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isCoerceMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasDuplicateMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isDuplicateMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasEltMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }
        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isEltMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasMaxMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isMaxMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasMinMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isMinMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasSumMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isSumMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasGetRegionMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isGetRegionMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public static boolean hasSortedMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isIsSortedMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
        }
    }

    public abstract static class AltrepDuplicateMethodInvoker extends Node {
        public abstract Object execute(Object altrepVector, boolean deep);

        public static AltrepDuplicateMethodInvoker create() {
            return AltrepUtilitiesFactory.AltrepDuplicateMethodInvokerNodeGen.create();
        }

        protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
            return AltrepUtilities.getAltIntDescriptor(altIntVec);
        }

        @Specialization(limit = "3")
        Object doAltInt(RIntVector altIntVec, boolean deep,
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getDuplicateMethod()") InteropLibrary duplicateMethodInterop,
                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            assert getAltIntDescriptor(altIntVec).isDuplicateMethodRegistered();
            return getAltIntDescriptor(altIntVec).invokeDuplicateMethodCached(altIntVec, deep, duplicateMethodInterop, hasMirrorProfile);
        }
    }

    public abstract static class AltrepSumMethodInvoker extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepSumMethodInvoker create() {
            return AltrepUtilitiesFactory.AltrepSumMethodInvokerNodeGen.create();
        }

        protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
            return AltrepUtilities.getAltIntDescriptor(altIntVec);
        }

        @Specialization(limit = "3")
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getSumMethod()") InteropLibrary methodInterop,
                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            assert getAltIntDescriptor(altIntVec).isSumMethodRegistered();
            return getAltIntDescriptor(altIntVec).invokeSumMethodCached(altIntVec, naRm, methodInterop, hasMirrorProfile);
        }
    }

    public abstract static class AltrepMaxMethodInvoker extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepMaxMethodInvoker create() {
            return AltrepUtilitiesFactory.AltrepMaxMethodInvokerNodeGen.create();
        }

        protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
            return AltrepUtilities.getAltIntDescriptor(altIntVec);
        }

        @Specialization(limit = "3")
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getMaxMethod()") InteropLibrary methodInterop,
                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            assert getAltIntDescriptor(altIntVec).isMaxMethodRegistered();
            return getAltIntDescriptor(altIntVec).invokeMaxMethodCached(altIntVec, naRm, methodInterop, hasMirrorProfile);
        }
    }

    public abstract static class AltrepMinMethodInvoker extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        public static AltrepMinMethodInvoker create() {
            return AltrepUtilitiesFactory.AltrepMinMethodInvokerNodeGen.create();
        }

        protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
            return AltrepUtilities.getAltIntDescriptor(altIntVec);
        }

        @Specialization(limit = "3")
        Object doAltInt(RIntVector altIntVec, boolean naRm,
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getMinMethod()") InteropLibrary methodInterop,
                        @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            assert getAltIntDescriptor(altIntVec).isMinMethodRegistered();
            return getAltIntDescriptor(altIntVec).invokeMinMethodCached(altIntVec, naRm, methodInterop, hasMirrorProfile);
        }
    }

    public abstract static class AltrepReadArrayElement extends Node {
        public abstract Object execute(Object altrepVector, int index);

        public static AltrepReadArrayElement create() {
            return AltrepUtilitiesFactory.AltrepReadArrayElementNodeGen.create();
        }

        protected static boolean hasEltMethodRegistered(Object object) {
            return AltrepUtilities.hasEltMethodRegistered(object);
        }

        protected static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVec) {
            return AltrepUtilities.getAltIntDescriptor(altIntVec);
        }

        @Specialization(guards = "hasEltMethodRegistered(altIntVec)", limit = "1")
        Object doAltIntElt(RIntVector altIntVec, int index,
                           @CachedLibrary("getAltIntDescriptor(altIntVec).getEltMethod()") InteropLibrary eltMethodInterop,
                           @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            return getAltIntDescriptor(altIntVec).invokeEltMethodCached(altIntVec, index, eltMethodInterop, hasMirrorProfile);
        }

        @Specialization(replaces = "doAltIntElt", limit = "1")
        Object doAltIntWithoutElt(RIntVector altIntVec, int index,
                                  @CachedLibrary("getAltIntDescriptor(altIntVec).getDataptrMethod()") InteropLibrary dataptrMethodInterop,
                                  @CachedLibrary(limit = "1") InteropLibrary dataptrInterop,
                                  @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            long address = getAltIntDescriptor(altIntVec).invokeDataptrMethodCached(altIntVec, true, dataptrMethodInterop, dataptrInterop, hasMirrorProfile);
            return NativeMemory.getInt(address, index);
        }

        @Specialization(limit = "3")
        Object doAltString(RAltStringVector altStringVector, int index,
                           @CachedLibrary("altStringVector.getDescriptor().getEltMethod()") InteropLibrary eltMethodInterop,
                           @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            return altStringVector.getDescriptor().invokeEltMethodCached(altStringVector, index, eltMethodInterop, hasMirrorProfile);
        }
    }
}
