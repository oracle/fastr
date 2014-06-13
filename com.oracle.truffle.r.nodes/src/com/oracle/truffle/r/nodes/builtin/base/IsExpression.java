package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "is.expression", kind = PRIMITIVE)
public abstract class IsExpression extends IsTypeNode {

    @Override
    @Specialization
    public byte isType(RExpression expr) {
        return RRuntime.LOGICAL_TRUE;
    }
}
