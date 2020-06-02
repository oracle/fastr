package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
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
    public abstract static class AltIntLengthNode extends Node {
        public abstract int execute(RIntVector altIntVector);

        @Specialization
        public int doIt(RIntVector altIntVector,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary returnValueInterop) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getLengthMethodDescriptor(altIntVector);
            Object retVal = downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.lengthMethodUnwrapResult,
                    AltIntegerClassDescriptor.lengthMethodWrapArguments, new Object[]{altIntVector});
            return expectInteger(returnValueInterop, retVal);
        }

    }

    @GenerateUncached
    public abstract static class AltIntEltNode extends Node {
        public abstract int execute(RIntVector altIntVector, int index);

        @Specialization
        public int doIt(RIntVector altIntVector, int index,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary retValueInterop) {
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getEltMethodDescriptor(altIntVector);
            Object retValue = downCallNode.execute(methodDescr, AltIntegerClassDescriptor.eltMethodUnwrapResult,
                    AltIntegerClassDescriptor.eltMethodWrapArguments, new Object[]{altIntVector, index});
            return expectInteger(retValueInterop, retValue);
        }
    }

    @GenerateUncached
    public abstract static class AltIntDataptrNode extends Node {
        public abstract long execute(RIntVector altIntVector, boolean writeable);

        @Specialization
        public long doIt(RIntVector altIntVector, boolean writeable,
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
    public abstract static class AltIntIsSortedNode extends Node {
        public abstract AltrepSortedness execute(RIntVector altIntVector);

        @Specialization
        public AltrepSortedness doIt(RIntVector altIntVector,
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
    public abstract static class AltIntNoNANode extends Node {
        public abstract boolean execute(RIntVector altIntVector);

        @Specialization
        public boolean doIt(RIntVector altIntVector,
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
    public abstract static class AltIntGetRegionNode extends Node {
        public abstract int execute(RIntVector altIntVector, int fromIdx, int size, Object buffer);

        @Specialization
        public int doIt(RIntVector altIntVector, int fromIdx, int size, Object buffer,
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
    public abstract static class AltIntSumNode extends Node {
        public abstract Object execute(RIntVector altIntVec, boolean naRm);

        @Specialization
        public Object doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getSumMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, AltIntegerClassDescriptor.sumMethodUnwrapResult,
                    AltIntegerClassDescriptor.sumMethodWrapArguments, new Object[]{altIntVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class AltStringEltNode extends Node {
        public abstract Object execute(RStringVector altStringVec, int index);

        @Specialization
        public Object doIt(RStringVector altStringVec, int index,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getEltMethodDescriptor(altStringVec);
            return downCallNode.execute(altrepMethodDescriptor, AltStringClassDescriptor.eltMethodUnwrapResult,
                    AltStringClassDescriptor.eltMethodWrapArguments, new Object[]{altStringVec, index});
        }
    }
}
