package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RAltStringVectorData;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;

public class AltrepUtilities {
    public static boolean isAltrep(Object object) {
        return object instanceof RBaseObject && ((RBaseObject) object).isAltRep();
    }

    private static RAltIntVectorData getAltIntVectorData(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return (RAltIntVectorData) altIntVector.getData();
    }

    private static RAltStringVectorData getAltStringVectorData(RStringVector altStringVector) {
        assert altStringVector.isAltRep();
        return (RAltStringVectorData) altStringVector.getData();
    }

    public static AltIntegerClassDescriptor getAltIntDescriptor(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        return getAltIntVectorData(altIntVector).getDescriptor();
    }

    public static AltStringClassDescriptor getAltStringDescriptor(RStringVector altStringVector) {
        assert altStringVector.isAltRep();
        return getAltStringVectorData(altStringVector).getDescriptor();
    }

    public static RAltRepData getAltrepData(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        RAltIntVectorData altIntVectorData = getAltIntVectorData(altIntVector);
        return altIntVectorData.getAltrepData();
    }

    public static RAltRepData getAltrepData(RStringVector altStringVector) {
        assert altStringVector.isAltRep();
        RAltStringVectorData data = getAltStringVectorData(altStringVector);
        return data.getAltrepData();
    }

    public static AltRepClassDescriptor getDescriptorFromAltrepObj(RBaseObject altrepObject) {
        assert altrepObject.isAltRep();

        if (altrepObject instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) altrepObject);
        } else if (altrepObject instanceof RStringVector) {
            return getAltStringDescriptor((RStringVector) altrepObject);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    public static RPairList getPairListDataFromVec(RIntVector altIntVec) {
        assert altIntVec.isAltRep();
        assert altIntVec.getData() instanceof RAltIntVectorData;
        RAltIntVectorData data = (RAltIntVectorData) altIntVec.getData();
        return data.getAltrepData().getDataPairList();
    }

    public static RPairList getPairListDataFromVec(RStringVector altStringVec) {
        assert altStringVec.isAltRep();
        assert altStringVec.getData() instanceof RAltStringVectorData;
        RAltStringVectorData data = (RAltStringVectorData) altStringVec.getData();
        return data.getAltrepData().getDataPairList();
    }

    public static AltrepDownCall getLengthMethodDownCall(RIntVector altIntVector) {
        assert altIntVector.isAltRep();
        AltIntegerClassDescriptor descriptor = getAltIntDescriptor(altIntVector);
        return descriptor.getLengthDownCall();
    }

    public static AltrepDownCall getEltMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getEltDownCall();
    }

    public static AltrepDownCall getDataptrMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getDataptrDownCall();
    }

    public static AltrepDownCall getIsSortedMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getIsSortedDownCall();
    }

    public static AltrepDownCall getNoNAMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getNoNADownCall();
    }

    public static AltrepDownCall getGetRegionMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getGetRegionDownCall();
    }

    public static AltrepDownCall getSumMethodDownCall(RIntVector altIntVector) {
        return getAltIntDescriptor(altIntVector).getSumDownCall();
    }

    public static AltrepDownCall getEltMethodDownCall(RStringVector altStringVector) {
        return getAltStringDescriptor(altStringVector).getEltDownCall();
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

    public static boolean hasNoNAMethodRegistered(Object object) {
        if (!isAltrep(object)) {
            return false;
        }

        if (object instanceof RIntVector) {
            return getAltIntDescriptor((RIntVector) object).isNoNAMethodRegistered();
        } else {
            throw RInternalError.shouldNotReachHere("Unexpected altrep type");
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
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getSumDownCall().method") InteropLibrary methodInterop,
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
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getMaxDownCall().method") InteropLibrary methodInterop,
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
                        @CachedLibrary("getAltIntDescriptor(altIntVec).getMinDownCall().method") InteropLibrary methodInterop,
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

        @Specialization(guards = "hasEltMethodRegistered(altIntVec)")
        Object doAltIntWithEltMethod(RIntVector altIntVec, int index,
                                     @Cached AltrepRFFI.AltIntEltNode eltNode) {
            return eltNode.execute(altIntVec, index);
        }

        @Specialization(guards = "!hasEltMethodRegistered(altIntVec)")
        Object doAltIntWithoutEltMethod(RIntVector altIntVec, int index,
                                        @Cached AltrepRFFI.AltIntDataptrNode dataptrNode) {
            long address = dataptrNode.execute(altIntVec, false);
            return NativeMemory.getInt(address, index);
        }

        @Specialization
        Object doAltString(RStringVector altStringVector, int index,
                           @Cached AltrepRFFI.AltStringEltNode eltNode) {
            return eltNode.execute(altStringVector, index);
        }
    }
}
