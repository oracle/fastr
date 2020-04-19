package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;

@GenerateUncached
public abstract class MakeAltRealClassNode extends FFIUpCallNode.Arg3 {
    private static final TruffleLogger altrepLogger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    public static MakeAltRealClassNode create() {
        return MakeAltRealClassNodeGen.create();
    }

    @Specialization
    public Object makeAltRealClass(Object classNameObj, Object packageNameObj, Object dllInfo,
                                   @Cached("create()") NativeStringCastNode stringCastNode,
                                   @CachedContext(TruffleRLanguage.class) RContext context) {
        String className = stringCastNode.executeObject(classNameObj);
        String packageName = stringCastNode.executeObject(packageNameObj);
        AltRepContext altRepCtx = context.altRepContext;
        return altRepCtx.registerNewAltRealClass(className, packageName, dllInfo);
    }
}
