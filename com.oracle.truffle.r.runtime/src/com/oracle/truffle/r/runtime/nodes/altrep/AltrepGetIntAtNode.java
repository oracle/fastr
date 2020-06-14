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
public abstract class AltrepGetIntAtNode extends RBaseNode {
    public abstract Object execute(Object altVec, int index);

    @Specialization(guards = {"isAltrep(altIntVector)", "hasEltMethodRegistered(altIntVector)"})
    public int doAltIntWithElt(RIntVector altIntVector, int index,
                               @Cached("create()") AltrepRFFI.EltNode altIntEltNode) {
        return altIntEltNode.execute(altIntVector, index);
    }

    @Specialization(guards = {"isAltrep(altIntVector)", "!hasEltMethodRegistered(altIntVector)"})
    public int doAltIntWithoutElt(RIntVector altIntVector, int index,
                                  @Cached("create()") AltrepRFFI.DataptrNode altIntDataptrNode) {
        long dataptrAddr = altIntDataptrNode.execute(altIntVector, false);
        return NativeMemory.getInt(dataptrAddr, index);
    }

    @Fallback
    public int unexpected(@SuppressWarnings("unused") Object object,
                          @SuppressWarnings("unused") int index) {
        throw RInternalError.shouldNotReachHere("Unexpected: AltrepGetIntAtNode, object = " + object.toString());
    }

    protected static boolean isAltrep(Object vector) {
        return AltrepUtilities.isAltrep(vector);
    }

    protected static boolean hasEltMethodRegistered(RIntVector altIntVector) {
        return AltrepUtilities.hasEltMethodRegistered(altIntVector);
    }
}
