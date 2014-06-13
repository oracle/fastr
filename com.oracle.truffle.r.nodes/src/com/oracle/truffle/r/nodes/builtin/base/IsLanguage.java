package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "is.language", kind = RBuiltinKind.PRIMITIVE)
public abstract class IsLanguage extends IsTypeNode {
    @Override
    @Specialization
    public byte isType(RSymbol value) {
        return RRuntime.LOGICAL_TRUE;
    }

    @Override
    @Specialization
    public byte isType(RExpression value) {
        return RRuntime.LOGICAL_TRUE;
    }

}
