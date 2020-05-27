package com.oracle.truffle.ffi.impl.altrep;

import com.oracle.truffle.r.runtime.ffi.AltrepDownCallNodeFactory;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDownCallNode;

public final class AltrepDownCallNodeFactoryImpl implements AltrepDownCallNodeFactory {
    public static final AltrepDownCallNodeFactory INSTANCE = new AltrepDownCallNodeFactoryImpl();

    @Override
    public AltrepDownCallNode createDownCallNode() {
        return AltrepDownCallNodeImpl.create();
    }

    @Override
    public AltrepDownCallNode getUncached() {
        return AltrepDownCallNodeImplNodeGen.getUncached();
    }
}
