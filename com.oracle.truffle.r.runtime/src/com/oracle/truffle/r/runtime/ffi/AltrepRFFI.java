package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;

public final class AltrepRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public AltrepRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public AltIntEltNode createAltIntEltNode() {
        return new AltIntEltNode(downCallNodeFactory);
    }

    public final static class AltIntEltNode extends NativeCallNode {
        private AltIntEltNode(DownCallNodeFactory downCallNodeFactory) {
            super(downCallNodeFactory.createDownCallNode());
        }

        public static AltIntEltNode create() {
            return RFFIFactory.getAltrepRFFI().createAltIntEltNode();
        }

        public int execute(RIntVector altIntVector, int index) {
            assert AltrepUtilities.isAltrep(altIntVector);
            return (int) call(NativeFunction.AltInteger_Elt, new Object[]{altIntVector, index});
        }
    }
}
