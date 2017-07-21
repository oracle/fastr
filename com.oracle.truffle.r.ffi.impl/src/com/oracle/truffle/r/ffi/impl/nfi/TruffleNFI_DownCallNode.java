package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.common.DownCallNode;

public abstract class TruffleNFI_DownCallNode extends DownCallNode<NFIFunction> {

    @Override
    protected final TruffleObject getTarget() {
        return getFunction().getFunction();
    }
}
