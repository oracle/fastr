package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class MaterializeNode extends Node {

    protected static final int LIMIT = 10;

    public abstract Object execute(VirtualFrame frame, Object arg);

    @Specialization(limit = "LIMIT", guards = {"vec.getClass() == cachedClass"})
    protected RAbstractContainer doAbstractContainerCached(RAbstractContainer vec,
                    @SuppressWarnings("unused") @Cached("vec.getClass()") Class<?> cachedClass) {
        return vec.materialize();
    }

    @Specialization(replaces = "doAbstractContainerCached")
    protected RAbstractContainer doAbstractContainer(RAbstractContainer vec) {
        return vec.materialize();
    }

    @Fallback
    protected Object doGeneric(Object o) {
        return o;
    }

    public static MaterializeNode create() {
        return MaterializeNodeGen.create();
    }

}
