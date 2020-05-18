package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
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

    public static class AltIntIsSortedNode extends NativeCallNode {
        private AltIntIsSortedNode(DownCallNode downCallNode) {
            super(downCallNode);
        }

        public static AltIntIsSortedNode create() {
            return new AltIntIsSortedNode(createDownCallNode());
        }

        public static AltIntIsSortedNode getUncached() {
            return new AltIntIsSortedNode(getUncachedDownCallNode()) {
                @Override
                public boolean isAdoptable() {
                    return false;
                }
            };
        }

        public AltrepSortedness execute(RIntVector altIntVector) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            int retValue = (int) call(NativeFunction.AltInteger_Is_sorted, new Object[]{altIntVector});
            return AltrepSortedness.fromInt(retValue);
        }
    }

    public static class AltIntNoNANode extends NativeCallNode {
        private AltIntNoNANode(DownCallNode downCallNode) {
            super(downCallNode);
        }

        public static AltIntNoNANode create() {
            return new AltIntNoNANode(createDownCallNode());
        }

        public static AltIntNoNANode getUncached() {
            return new AltIntNoNANode(getUncachedDownCallNode()) {
                @Override
                public boolean isAdoptable() {
                    return false;
                }
            };
        }

        public boolean execute(RIntVector altIntVector) {
            assert AltrepUtilities.isAltrep(altIntVector);
            // TODO: Accept more return values - maybe use InteropLibrary?
            int retValue = (int) call(NativeFunction.AltInteger_No_NA, new Object[]{altIntVector});
            return retValue != 0;
        }
    }

    public static class AltIntGetRegionNode extends NativeCallNode {
        private AltIntGetRegionNode(DownCallNode downCallNode) {
            super(downCallNode);
        }

        public static AltIntGetRegionNode create() {
            return new AltIntGetRegionNode(createDownCallNode());
        }

        public static AltIntGetRegionNode getUncached() {
            return new AltIntGetRegionNode(getUncachedDownCallNode()) {
                @Override
                public boolean isAdoptable() {
                    return false;
                }
            };
        }

        public int execute(RIntVector altIntVector, int fromIdx, int size, int[] buffer) {
            assert AltrepUtilities.isAltrep(altIntVector);
            InteropLibrary interopLib = InteropLibrary.getUncached();
            Object ret = call(NativeFunction.AltInteger_Get_region, altIntVector, fromIdx, size, buffer);
            assert interopLib.isNumber(ret);
            try {
                return interopLib.asInt(ret);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }
}
