package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
public abstract class AltrepSetElementAtNode extends RBaseNode {
    public abstract Object execute(Object altVec, Object element, int index);

    @Specialization(guards = {"isAltrep(altIntVec)"})
    public Object doAltInt(RIntVector altIntVec, int value, int index,
                         @Cached("create()") AltrepRFFI.AltIntDataptrNode dataptrNode) {
        writeViaDataptrNode(dataptrNode, altIntVec, index, value);
        return altIntVec;
    }

    protected static boolean isAltrep(Object vector) {
        return AltrepUtilities.isAltrep(vector);
    }

    private static void writeViaDataptrNode(AltrepRFFI.AltIntDataptrNode altIntDataptrNode, RIntVector altIntVec,
                                            int index, int value) {
        long dataptrAddr = altIntDataptrNode.execute(altIntVec, true);
        NativeMemory.putInt(dataptrAddr, index, value);
    }
}
