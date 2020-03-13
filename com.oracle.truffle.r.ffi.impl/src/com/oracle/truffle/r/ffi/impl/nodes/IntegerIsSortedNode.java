package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.llvm.AltrepLLVMDownCallNode;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

@GenerateUncached
public abstract class IntegerIsSortedNode extends FFIUpCallNode.Arg1 {

    public static IntegerIsSortedNode create() {
        return IntegerIsSortedNodeGen.create();
    }

    @Specialization(guards = {"isAltrep(altIntVec)", "isIsSortedMethodRegistered(altIntVec)"})
    public int isSortedForAltIntVec(RIntVector altIntVec,
                                    @Cached("createDispatchNode()") IsSortedDispatchNode dispatchNode) {
        return (int) dispatchNode.executeObject(altIntVec);
    }

    @Fallback
    public int isSortedFallback(Object x) {
        return AltrepSortedness.UNKNOWN_SORTEDNESS.getValue();
    }

    protected static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    protected static boolean isIsSortedMethodRegistered(Object x) {
        return AltrepUtilities.hasSortedMethodRegistered(x);
    }

    protected static IsSortedDispatchNode createDispatchNode() {
        return IntegerIsSortedNode.IsSortedDispatchNode.create();
    }

    @GenerateUncached
    static abstract class IsSortedDispatchNode extends FFIUpCallNode.Arg1 {
        public static IsSortedDispatchNode create() {
            return IntegerIsSortedNodeGen.IsSortedDispatchNodeGen.create();
        }

        @Specialization
        public int isSortedForAltIntVec(RAltIntVectorData altIntVec,
                                        @Cached(value = "create()", allowUncached = true) AltrepLLVMDownCallNode altrepLLVMDownCallNode) {
            Object ret = altrepLLVMDownCallNode.call(NativeFunction.AltInteger_Is_sorted, altIntVec);
            assert ret instanceof Integer;
            return (int) ret;
        }
    }
}
