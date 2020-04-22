package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;

@GenerateUncached
public abstract class IntegerIsSortedNode extends FFIUpCallNode.Arg1 {

    public static IntegerIsSortedNode create() {
        return IntegerIsSortedNodeGen.create();
    }

    @Specialization(guards = {"isAltrep(altIntVec)", "hasSortedMethod(altIntVec)"})
    public int doForAltIntVec(RIntVector altIntVec,
                              @Cached AltrepRFFI.AltIntIsSortedNode isSortedNode) {
        AltrepSortedness sortedness = isSortedNode.execute(altIntVec);
        return sortedness.getValue();
    }

    @Fallback
    public int isSortedFallback(Object x) {
        return AltrepSortedness.UNKNOWN_SORTEDNESS.getValue();
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static boolean hasSortedMethod(Object x) {
        return AltrepUtilities.hasSortedMethodRegistered(x);
    }
}
