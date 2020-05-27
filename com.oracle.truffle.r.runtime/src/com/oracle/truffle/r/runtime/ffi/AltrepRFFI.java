package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepDownCall;
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

    protected abstract static class AltBaseNode extends Node {
        protected static AltrepDownCallNode createDownCallNode() {
            return AltrepRFFI.createDownCallNode();
        }

        protected static AltrepDownCallNode createUncachedDownCallNode() {
            return AltrepRFFI.createUncachedDownCallNode();
        }
    }

    @GenerateUncached
    public abstract static class AltIntEltNode extends AltBaseNode {
        public abstract int execute(RIntVector altIntVector, int index);

        @Specialization
        public int doIt(RIntVector altIntVector, int index,
                        @Cached(value="createDownCallNode()", uncached="createUncachedDownCallNode()") AltrepDownCallNode downCallNode) {
            // TODO: Get this from altIntVector
            AltrepDownCall altrepDowncall = null;


            boolean unwrapFlag = false;
            return (int) downCallNode.execute(altrepDowncall, unwrapFlag, new Object[]{altIntVector, index});
        }

    }

    @GenerateUncached
    public abstract static class AltIntDataptrNode extends AltBaseNode {
        public abstract long execute(RIntVector altIntVector, boolean writeable);

        @Specialization
        public long doIt(RIntVector altIntVector, boolean writeable,
                         @Cached(value="createDownCallNode()", uncached="createUncachedDownCallNode()") AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            setDataptrCalled(altIntVector);
            // TODO: Get this from altIntVector
            AltrepDownCall altrepDowncall = null;
            boolean unwrapFlag = false;
            Object ret = downCallNode.execute(altrepDowncall, unwrapFlag, new Object[]{altIntVector, writeable});
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
    public abstract static class AltIntIsSortedNode extends AltBaseNode {
        public abstract AltrepSortedness execute(RIntVector altIntVector);

        @Specialization
        public AltrepSortedness doIt(RIntVector altIntVector,
                                     @Cached(value="createDownCallNode()", uncached="createUncachedDownCallNode()") AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            // TODO: Get this from altIntVector
            AltrepDownCall altrepDowncall = null;
            boolean unwrapFlag = false;
            int retValue = (int) downCallNode.execute(altrepDowncall, unwrapFlag, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(retValue);
        }
    }

    @GenerateUncached
    public abstract static class AltIntNoNANode extends AltBaseNode {
        public abstract boolean execute(RIntVector altIntVector);

        @Specialization
        public boolean doIt(RIntVector altIntVector,
                            @Cached(value="createDownCallNode()", uncached="createUncachedDownCallNode()") AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            // TODO: Get this from altIntVector
            AltrepDownCall altrepDowncall = null;
            boolean unwrapFlag = false;
            int retValue = (int) downCallNode.execute(altrepDowncall, unwrapFlag, new Object[]{altIntVector});
            return retValue != 0;
        }
    }

    @GenerateUncached
    public abstract static class AltIntGetRegionNode extends AltBaseNode {
        public abstract int execute(RIntVector altIntVector, int fromIdx, int size, Object buffer);

        @Specialization
        public int doIt(RIntVector altIntVector, int fromIdx, int size, Object buffer,
                        @Cached(value="createDownCallNode()", uncached="createUncachedDownCallNode()") AltrepDownCallNode downCallNode) {
            assert AltrepUtilities.isAltrep(altIntVector);
            InteropLibrary interopLib = InteropLibrary.getUncached();
            AltrepDownCall altrepDowncall = null;
            boolean unwrapFlag = false;
            Object ret = downCallNode.execute(altrepDowncall, unwrapFlag,
                    new Object[]{altIntVector, fromIdx, size, buffer});
            assert interopLib.isNumber(ret);
            try {
                return interopLib.asInt(ret);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }
}
