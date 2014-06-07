package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "is.name", kind = PRIMITIVE)
public abstract class IsName extends IsTypeNode {

    @Specialization
    @Override
    public byte isType(RSymbol value) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    @Override
    public byte isType(Object value) {
        controlVisibility();
        return RRuntime.asLogical(value instanceof RSymbol);
    }

}
