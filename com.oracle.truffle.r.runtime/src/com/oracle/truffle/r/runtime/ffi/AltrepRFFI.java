package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltStringClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepMethodDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDownCallNode;

public final class AltrepRFFI {
    private final AltrepDownCallNodeFactory downCallNodeFactory;

    public AltrepRFFI(AltrepDownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static AltrepDownCallNode createDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.createDownCallNode();
    }

    public static AltrepDownCallNode createUncachedDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.getUncached();
    }

    private static boolean expectBoolean(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isBoolean(value), "Value from downcall should be boolean");
        try {
            return interop.asBoolean(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static int expectInteger(InteropLibrary interop, Object value) {
        RInternalError.guarantee(interop.isNumber(value), "Value from downcall should be an integer");
        try {
            return interop.asInt(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static long expectPointer(InteropLibrary interop, Object value) {
        if (!interop.isPointer(value)) {
            interop.toNative(value);
        }
        try {
            return interop.asPointer(value);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }


    @GenerateUncached
    public abstract static class LengthNode extends Node {
        public abstract int execute(Object altrepVector);

        @Specialization
        protected int lengthOfAltInt(RIntVector altIntVector,
                                     @Shared("downCallNode") @Cached AltrepDownCallNode downCallNode,
                                     @Shared("returnValInterop") @CachedLibrary(limit = "1") InteropLibrary returnValueInterop) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getLengthMethodDescriptor(altIntVector);
            Object retVal = downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.lengthMethodUnwrapResult,
                    AltIntegerClassDescriptor.lengthMethodWrapArguments, new Object[]{altIntVector});
            return expectInteger(returnValueInterop, retVal);
        }

        @Specialization
        protected int lengthOfAltString(RStringVector altStringVector,
                                        @Shared("downCallNode") @Cached AltrepDownCallNode downCallNode,
                                        @Shared("returnValInterop") @CachedLibrary(limit = "1") InteropLibrary returnValueInterop) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getLengthMethodDescriptor(altStringVector);
            Object retVal = downCallNode.execute(altrepMethodDescriptor, AltStringClassDescriptor.lengthMethodUnwrapResult,
                    AltStringClassDescriptor.lengthMethodWrapArguments, new Object[]{altStringVector});
            return expectInteger(returnValueInterop, retVal);
        }
    }

    @GenerateUncached
    public abstract static class EltNode extends Node {
        public abstract int execute(Object altrepVector, int index);

        @Specialization
        protected int doIt(RIntVector altIntVector, int index,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getEltMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.eltMethodUnwrapResult,
                    AltIntegerClassDescriptor.eltMethodWrapArguments, new Object[]{altIntVector, index});
            return expectInteger(retValueInterop, retValue);
        }
    }

    @GenerateUncached
    public abstract static class DataptrNode extends Node {
        public abstract long execute(Object altrepVector, boolean writeable);

        @Specialization
        protected long doIt(RIntVector altIntVector, boolean writeable,
                         @Cached AltrepDownCallNode downCallNode,
                         @CachedLibrary(limit = "1") InteropLibrary interop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            setDataptrCalled(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getDataptrMethodDescriptor(altIntVector);
            Object ret = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.dataptrMethodUnwrapResult,
                    AltIntegerClassDescriptor.dataptrMethodWrapArguments, new Object[]{altIntVector, writeable});
            return expectPointer(interop, ret);
        }

        private static void setDataptrCalled(RIntVector altIntVec) {
            assert AltrepUtilities.isAltrep(altIntVec);
            RAltIntVectorData altIntVectorData = (RAltIntVectorData) altIntVec.getData();
            altIntVectorData.setDataptrCalled();
        }
    }

    @GenerateUncached
    public abstract static class IsSortedNode extends Node {
        public abstract AltrepSortedness execute(Object altrepVector);

        @Specialization
        protected AltrepSortedness doIt(RIntVector altIntVector,
                                     @Cached AltrepDownCallNode downCallNode,
                                     @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getIsSortedMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.isSortedMethodUnwrapResult,
                    AltIntegerClassDescriptor.isSortedMethodWrapArguments, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(expectInteger(retValueInterop, retValue));
        }
    }

    @GenerateUncached
    public abstract static class NoNANode extends Node {
        public abstract boolean execute(Object altrepVector);

        @Specialization
        protected boolean doIt(RIntVector altIntVector,
                            @Cached AltrepDownCallNode downCallNode,
                            @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.noNAMethodUnwrapResult,
                    AltIntegerClassDescriptor.noNAMethodWrapArguments, new Object[]{altIntVector});
            return expectInteger(retValueInterop, retValue) == 1;
        }
    }

    @GenerateUncached
    public abstract static class GetRegionNode extends Node {
        public abstract int execute(Object altrepVector, int fromIdx, int size, Object buffer);

        @Specialization
        protected int doIt(RIntVector altIntVector, int fromIdx, int size, Object buffer,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            assert AltrepUtilities.isAltrep(altIntVector);
            assert InteropLibrary.getUncached().hasArrayElements(buffer);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getGetRegionMethodDescriptor(altIntVector);
            Object ret = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.getRegionMethodUnwrapResult,
                    AltIntegerClassDescriptor.getRegionMethodWrapArguments, new Object[]{altIntVector, fromIdx, size, buffer});
            return expectInteger(retValueInterop, ret);
        }
    }

    @GenerateUncached
    public abstract static class DuplicateNode extends Node {
        public abstract Object execute(Object altrepVector, boolean deep);

        @Specialization
        protected Object doIt(RIntVector altIntVector, boolean deep,
                           @Cached AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            AltrepMethodDescriptor methodDescriptor = AltrepUtilities.getDuplicateMethodDescriptor(altIntVector);
            return downCallNode.execute(methodDescriptor, AltIntegerClassDescriptor.duplicateMethodUnwrapResult,
                    AltIntegerClassDescriptor.duplicateMethodWrapArguments, new Object[]{altIntVector, deep});
        }
    }

    @GenerateUncached
    public abstract static class SumNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getSumMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.sumMethodUnwrapResult,
                    AltIntegerClassDescriptor.sumMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class MaxNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getMaxMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.maxMethodUnwrapResult,
                    AltIntegerClassDescriptor.maxMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class MinNode extends Node {
        public abstract Object execute(Object altrepVector, boolean naRm);

        @Specialization
        protected Object doIt(RIntVector altIntVec, boolean naRm,
                              @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getMinMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.minMethodUnwrapResult,
                    AltIntegerClassDescriptor.minMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }
}
