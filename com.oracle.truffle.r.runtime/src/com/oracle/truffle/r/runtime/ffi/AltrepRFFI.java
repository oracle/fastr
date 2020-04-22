package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory.DownCallNode;

public final class AltrepRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public AltrepRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    private static DownCallNode createDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.createDownCallNode();
    }

    private static DownCallNode getUncachedDownCallNode() {
        return RFFIFactory.getAltrepRFFI().downCallNodeFactory.getUncachedDownCallNode();
    }

    public static class AltIntEltNode extends NativeCallNode {
        private AltIntEltNode(DownCallNode downCallNode) {
            super(downCallNode);
        }

        public static AltIntEltNode create() {
            return new AltIntEltNode(createDownCallNode());
        }

        public static AltIntEltNode getUncached() {
            return new AltIntEltNode(getUncachedDownCallNode()) {
                @Override
                public boolean isAdoptable() {
                    return false;
                }
            };
        }

        public int execute(RIntVector altIntVector, int index) {
            assert AltrepUtilities.isAltrep(altIntVector);
            return (int) call(NativeFunction.AltInteger_Elt, new Object[]{altIntVector, index});
        }
    }

    public static class AltIntDataptrNode extends NativeCallNode {
        private AltIntDataptrNode(DownCallNode downCallNode) {
            super(downCallNode);
        }

        public static AltIntDataptrNode create() {
            return new AltIntDataptrNode(createDownCallNode());
        }

        public static AltIntDataptrNode getUncached() {
            return new AltIntDataptrNode(getUncachedDownCallNode()) {
                @Override
                public boolean isAdoptable() {
                    return false;
                }
            };
        }

        public long execute(RIntVector altIntVector, boolean writeable) {
            assert AltrepUtilities.isAltrep(altIntVector);
            setDataptrCalled(altIntVector);
            Object ret = call(NativeFunction.AltInteger_Dataptr, altIntVector, writeable);
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
}
