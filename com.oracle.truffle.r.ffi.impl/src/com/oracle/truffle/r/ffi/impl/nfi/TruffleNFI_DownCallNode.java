package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.ffi.impl.common.DownCallNode;
import com.oracle.truffle.r.ffi.impl.interop.NativeNACheck;

public abstract class TruffleNFI_DownCallNode extends DownCallNode<NFIFunction> {

    @Override
    protected final TruffleObject getTarget() {
        return getFunction().getFunction();
    }

    @SuppressWarnings("cast")
    @Override
    @ExplodeLoop
    protected void wrapArguments(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            if (obj instanceof double[]) {
                args[i] = JavaInterop.asTruffleObject((double[]) obj);
            } else if (obj instanceof int[] || obj == null) {
                args[i] = JavaInterop.asTruffleObject((int[]) obj);
            }
        }
    }

    @Override
    @ExplodeLoop
    protected void finishArguments(Object[] args) {
        for (Object obj : args) {
            if (obj instanceof NativeNACheck<?>) {
                ((NativeNACheck<?>) obj).close();
            }
        }
    }
}
