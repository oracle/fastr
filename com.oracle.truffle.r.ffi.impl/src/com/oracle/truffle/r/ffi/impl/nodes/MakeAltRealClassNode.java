package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;

@GenerateUncached
public abstract class MakeAltRealClassNode extends FFIUpCallNode.Arg3 {

    public static MakeAltRealClassNode create() {
        return MakeAltRealClassNodeGen.create();
    }

    @Specialization
    public Object makeAltRealClass(String className, String packageName, Object dllInfo,
                    @CachedContext(TruffleRLanguage.class) RContext context) {
        AltRepContext altRepCtx = context.altRepContext;
        return altRepCtx.registerNewAltRealClass(className, packageName, dllInfo);
    }
}
