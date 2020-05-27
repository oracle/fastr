package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.altrep.AltrepDownCall;

public abstract class AltrepDownCallNode extends Node {
    public abstract Object execute(AltrepDownCall altrepDowncall, boolean unwrapFlag, Object[] args);
}
