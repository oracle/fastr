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

    @GenerateUncached
    public abstract static class AltIntLengthNode extends Node {
        public abstract int execute(RIntVector altIntVector);

        @Specialization
        public int doIt(RIntVector altIntVector,
                        @Cached AltrepDownCallNode downCallNode,
                        @CachedLibrary(limit = "1") InteropLibrary returnValueInterop) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getLengthMethodDescriptor(altIntVector);
            Object retVal = downCallNode.execute(altrepMethodDescriptor, false, new Object[]{altIntVector});
            assert returnValueInterop.isNumber(retVal);
            try {
                return returnValueInterop.asInt(retVal);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
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
            Object retValue = downCallNode.execute(methodDescr, false, new Object[]{altIntVector, index});
            assert retValueInterop.isNumber(retValue);
            try {
                return retValueInterop.asInt(retValue);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    @GenerateUncached
    public abstract static class AltIntDataptrNode extends Node {
        public abstract long execute(RIntVector altIntVector, boolean writeable);

        @Specialization
        public long doIt(RIntVector altIntVector, boolean writeable,
                         @Cached AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            setDataptrCalled(altIntVector);
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getDataptrMethodDescriptor(altIntVector);
            Object ret = downCallNode.execute(methodDescr, false, new Object[]{altIntVector, writeable});
            InteropLibrary interop = InteropLibrary.getFactory().getUncached(ret);
            interop.toNative(ret);
            try {
                return interop.asPointer(ret);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere();
            }
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
                                     @Cached AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getIsSortedMethodDescriptor(altIntVector);
            int retValue = (int) downCallNode.execute(methodDescr, false, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(retValue);
        }
    }

    @GenerateUncached
    public abstract static class AltIntNoNANode extends Node {
        public abstract boolean execute(RIntVector altIntVector);

        @Specialization
        public boolean doIt(RIntVector altIntVector,
                            @Cached AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            AltrepMethodDescriptor methodDescr = AltrepUtilities.getNoNAMethodDescriptor(altIntVector);
            int retValue = (int) downCallNode.execute(methodDescr, false, new Object[]{altIntVector});
            return retValue != 0;
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
            Object ret = downCallNode.execute(methodDescr, false,
                    new Object[]{altIntVector, fromIdx, size, buffer});
            assert retValueInterop.isNumber(ret);
            try {
                return retValueInterop.asInt(ret);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @GenerateUncached
    public abstract static class AltIntSumNode extends Node {
        public abstract Object execute(RIntVector altIntVec, boolean naRm);

        @Specialization
        public Object doIt(RIntVector altIntVec, boolean naRm,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getSumMethodDescriptor(altIntVec);
            return downCallNode.execute(altrepMethodDescriptor, true, new Object[]{altIntVec, naRm});
        }
    }

    @GenerateUncached
    public abstract static class AltStringEltNode extends Node {
        public abstract Object execute(RStringVector altStringVec, int index);

        @Specialization
        public Object doIt(RStringVector altStringVec, int index,
                           @Cached AltrepDownCallNode downCallNode) {
            AltrepMethodDescriptor altrepMethodDescriptor = AltrepUtilities.getEltMethodDescriptor(altStringVec);
            return downCallNode.execute(altrepMethodDescriptor, true, new Object[]{altStringVec, index});
        }
    }
}
