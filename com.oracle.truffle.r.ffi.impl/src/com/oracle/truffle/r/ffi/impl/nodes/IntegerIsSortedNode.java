package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;

@GenerateUncached
public abstract class IntegerIsSortedNode extends FFIUpCallNode.Arg1 {

    public static IntegerIsSortedNode create() {
        return IntegerIsSortedNodeGen.create();
    }

    @Specialization(guards = {"isIsSortedMethodRegistered(altIntVec)"})
    public int isSortedForAltIntVec(RAltIntegerVec altIntVec,
                                    @Cached(value = "createDispatchNode()", allowUncached = true) IsSortedDispatchNode dispatchNode) {
        return (int) dispatchNode.executeObject(altIntVec);
    }

    @Fallback
    public int isSortedFallback(Object x) {
        return AltrepSortedness.UNKNOWN_SORTEDNESS.getValue();
    }

    protected static boolean isIsSortedMethodRegistered(Object x) {
        if (x instanceof RAltIntegerVec) {
            return ((RAltIntegerVec) x).getDescriptor().isIsSortedMethodRegistered();
        } else {
            return false;
        }
    }

    protected static IsSortedDispatchNode createDispatchNode() {
        return IntegerIsSortedNode.IsSortedDispatchNode.create();
    }

    static abstract class IsSortedDispatchNode extends FFIUpCallNode.Arg1 {
        public static IsSortedDispatchNode create() {
            return IntegerIsSortedNodeGen.IsSortedDispatchNodeGen.create();
        }

        @Specialization(limit = "2")
        public int isSortedForAltIntVec(RAltIntegerVec altIntVec,
                                        @CachedLibrary("altIntVec.getDescriptor().getIsSortedMethod()") InteropLibrary isSortedMethodInterop) {
            return altIntVec.getDescriptor().invokeIsSortedMethod(altIntVec, isSortedMethodInterop);
        }
    }
}
