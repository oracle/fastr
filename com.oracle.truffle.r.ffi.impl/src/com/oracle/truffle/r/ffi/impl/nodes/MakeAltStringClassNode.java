package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.context.RContext;

@GenerateUncached
public abstract class MakeAltStringClassNode extends FFIUpCallNode.Arg3 {
    private static final TruffleLogger altrepLogger = RLogger.getLogger("altrep");

    public static MakeAltStringClassNode create() {
        return MakeAltStringClassNodeGen.create();
    }

    @TruffleBoundary
    @Specialization
    public Object makeAltStringClass(Object classNameObj, Object packageNameObj, Object dllInfo,
                                      @Cached("create()") NativeStringCastNode stringCastNode) {
        String className = stringCastNode.executeObject(classNameObj);
        String packageName = stringCastNode.executeObject(packageNameObj);
        AltRepContext altRepCtx = RContext.getInstance().altRepContext;
        return altRepCtx.registerNewAltStringClass(className, packageName, dllInfo);
    }
}
