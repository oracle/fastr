package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.altrep.AltrepMethodDescriptor;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;

public abstract class AltrepDownCallNode extends Node {
    public abstract Object execute(AltrepMethodDescriptor altrepDowncall, boolean unwrapResult, boolean[] wrapArguments, Object[] args);

    public static AltrepDownCallNode create() {
        return AltrepRFFI.createDownCallNode();
    }

    public static AltrepDownCallNode getUncached() {
        return AltrepRFFI.createUncachedDownCallNode();
    }
}
