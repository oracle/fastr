package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

@GenerateUncached
public abstract class IntegerNoNANode extends FFIUpCallNode.Arg1 {
    public static IntegerNoNANode create() {
        return IntegerNoNANodeGen.create();
    }

    @Specialization(limit = "getGenericDataLibraryCacheSize()")
    public Object doContainer(RAbstractContainer container,
                              @CachedLibrary("container.getData()") VectorDataLibrary dataLibrary) {
        if (dataLibrary.noNA(container.getData())) {
            return RRuntime.LOGICAL_TRUE;
        } else {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static boolean hasNoNAMethod(Object object) {
        return AltrepUtilities.hasNoNAMethodRegistered(object);
    }
}
