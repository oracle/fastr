package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;

@GenerateUncached
public abstract class MakeAltLogicalClassNode extends FFIUpCallNode.Arg3 {
    private static final TruffleLogger altrepLogger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    public static MakeAltLogicalClassNode create() {
        return MakeAltLogicalClassNodeGen.create();
    }

    @TruffleBoundary
    @Specialization
    public Object makeAltLogicalClass(String className, String packageName, Object dllInfo) {
        AltRepContext altRepCtx = RContext.getInstance().altRepContext;
        return altRepCtx.registerNewAltLogicalClass(className, packageName, dllInfo);
    }
}