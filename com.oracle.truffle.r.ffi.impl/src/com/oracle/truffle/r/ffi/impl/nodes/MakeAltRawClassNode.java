package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;

@GenerateUncached
public abstract class MakeAltRawClassNode extends FFIUpCallNode.Arg3 {

    public static MakeAltRawClassNode create() {
        return MakeAltRawClassNodeGen.create();
    }

    @TruffleBoundary
    @Specialization
    public Object makeAltRawClass(String className, String packageName, Object dllInfo) {
        AltRepContext altRepCtx = RContext.getInstance().altRepContext;
        return altRepCtx.registerNewAltRawClass(className, packageName, dllInfo);
    }
}
