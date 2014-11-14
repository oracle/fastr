package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

@RBuiltin(name = "interactive", kind = RBuiltinKind.PRIMITIVE, parameterNames = {})
public abstract class Interactive extends RBuiltinNode {
    @Specialization
    protected byte interactive() {
        controlVisibility();
        return RRuntime.asLogical(!RContext.isHeadless());
    }
}
