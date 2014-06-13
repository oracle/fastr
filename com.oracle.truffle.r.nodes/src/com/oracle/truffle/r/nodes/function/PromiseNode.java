package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

public final class PromiseNode extends RNode {
    RPromise promise;

    private PromiseNode(RPromise promise) {
        this.promise = promise;
    }

    public static PromiseNode create(RPromise promise) {
        return new PromiseNode(promise);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return promise;
    }

}
