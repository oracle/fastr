package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;

@GenerateUncached
public abstract class MakeAltComplexClassNode extends FFIUpCallNode.Arg3 {
    public static MakeAltComplexClassNode create() {
        return MakeAltComplexClassNodeGen.create();
    }

    @TruffleBoundary
    @Specialization
    public Object makeAltComplexClass(String className, String packageName, Object dllInfo) {
        AltRepContext altRepCtx = RContext.getInstance().altRepContext;
        return altRepCtx.registerNewAltComplexClass(className, packageName, dllInfo);
    }
}
