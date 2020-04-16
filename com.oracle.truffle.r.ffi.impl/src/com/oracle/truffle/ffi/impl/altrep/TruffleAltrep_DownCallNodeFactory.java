package com.oracle.truffle.ffi.impl.altrep;

import com.oracle.truffle.r.ffi.impl.llvm.AltrepLLVMDownCallNodeGen;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;

public class TruffleAltrep_DownCallNodeFactory extends DownCallNodeFactory{

    public static final TruffleAltrep_DownCallNodeFactory INSTANCE = new TruffleAltrep_DownCallNodeFactory();

    private TruffleAltrep_DownCallNodeFactory() {
    }

    /**
     * TODO: Currently only LLVM is supported for altrep.
     */
    @Override
    public DownCallNode createDownCallNode() {
        return AltrepLLVMDownCallNodeGen.create();
    }
}
