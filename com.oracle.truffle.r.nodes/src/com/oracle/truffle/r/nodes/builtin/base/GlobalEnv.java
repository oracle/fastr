package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

@RBuiltin({"globalenv"})
public abstract class GlobalEnv extends RBuiltinNode {

    @Specialization
    public Object globalenv() {
        return RRuntime.GLOBAL_ENV;
    }

}
