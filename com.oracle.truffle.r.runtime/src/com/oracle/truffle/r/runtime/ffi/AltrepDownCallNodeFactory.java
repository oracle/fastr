package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDownCallNode;

public interface AltrepDownCallNodeFactory {
    AltrepDownCallNode createDownCallNode();

    AltrepDownCallNode getUncached();
}
