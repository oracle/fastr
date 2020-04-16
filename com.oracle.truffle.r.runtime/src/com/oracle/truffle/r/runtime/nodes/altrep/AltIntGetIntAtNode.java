package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltIntGetIntAtNode extends RBaseNode {
    public abstract Object execute(RIntVector altIntVector, int index);

    @Specialization(guards = "hasEltMethodRegistered(altIntVector)")
    public int doWithElt(RIntVector altIntVector, int index,
                         @Cached("create()") AltrepRFFI.AltIntEltNode altIntEltNode) {
        return altIntEltNode.execute(altIntVector, index);
    }

    @Specialization(guards = "!hasEltMethodRegistered(altIntVector)")
    public int doWithoutElt(RIntVector altIntVector, int index,
                            @Cached("create()") AltrepRFFI.AltIntDataptrNode altIntDataptrNode) {
        long dataptrAddr = altIntDataptrNode.execute(altIntVector, false);
        return NativeMemory.getInt(dataptrAddr, index);
    }

    @Fallback
    public int unexpected(@SuppressWarnings("unused") RIntVector altIntVector,
                          @SuppressWarnings("unused") int index) {
        throw RInternalError.shouldNotReachHere("Unexpected: AltIntGetIntAtNode");
    }

    protected static boolean hasEltMethodRegistered(RIntVector altIntVector) {
        return AltrepUtilities.hasEltMethodRegistered(altIntVector);
    }
}
