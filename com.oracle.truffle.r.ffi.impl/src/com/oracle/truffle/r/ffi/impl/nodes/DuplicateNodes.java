package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodesFactory.DuplicateNodeGen;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class DuplicateNodes {

    public abstract static class DuplicateNode extends FFIUpCallNode.Arg2 {

        @Specialization
        public Object duplicateShareable(RShareable x, int deep) {
            assert !isReusableForDuplicate(x);
            return deep == 1 ? x.deepCopy() : x.copy();
        }

        @Specialization
        public Object duplicateSequence(RSequence x, @SuppressWarnings("unused") int deep) {
            return x.materialize();
        }

        @Specialization
        public Object duplicateExternalPtr(RExternalPtr x, @SuppressWarnings("unused") int deep) {
            return x.copy();
        }

        @Specialization(guards = "isReusableForDuplicate(val)")
        public Object returnReusable(Object val, @SuppressWarnings("unused") int deep) {
            return val;
        }

        protected static boolean isReusableForDuplicate(Object o) {
            return o == RNull.instance || o instanceof REnvironment || o instanceof RSymbol;
        }

        public static DuplicateNode create() {
            return DuplicateNodeGen.create();
        }
    }

}
