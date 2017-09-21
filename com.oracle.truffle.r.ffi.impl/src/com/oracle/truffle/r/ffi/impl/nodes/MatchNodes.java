package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public final class MatchNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class MatchNode extends FFIUpCallNode.Arg3 {

        @SuppressWarnings("unused")
        @Specialization
        Object match(Object itables, Object ix, int nmatch) {
            throw RInternalError.unimplemented("Rf_match");
        }

        public static MatchNode create() {
            return MatchNodesFactory.MatchNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class NonNullStringMatchNode extends FFIUpCallNode.Arg2 {

        @Specialization(guards = {"s.getLength() == 1", "t.getLength() == 1"})
        Object matchSingle(RAbstractStringVector s, RAbstractStringVector t) {
            if (s.getDataAt(0) == RRuntime.STRING_NA || t.getDataAt(0) == RRuntime.STRING_NA) {
                return RRuntime.LOGICAL_FALSE;
            }
            return RRuntime.asLogical(s.getDataAt(0).equals(t.getDataAt(0)));
        }

        @Fallback
        @SuppressWarnings("unused")
        Object match(Object s, Object t) {
            throw RInternalError.unimplemented("Rf_NonNullStringMatch");
        }

        public static NonNullStringMatchNode create() {
            return MatchNodesFactory.NonNullStringMatchNodeGen.create();
        }
    }

}
