package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.altrep.AltrepDownCall;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;

public abstract class AltrepDownCallNode extends Node {
    public abstract Object execute(AltrepDownCall altrepDowncall, boolean unwrapFlag, Object[] args);

    public static AltrepDownCallNode create() {
        return AltrepRFFI.createDownCallNode();
    }

    public static AltrepDownCallNode getUncached() {
        return AltrepRFFI.createUncachedDownCallNode();
    }
}
