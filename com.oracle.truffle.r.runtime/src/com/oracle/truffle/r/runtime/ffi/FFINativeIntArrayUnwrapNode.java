package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeIntegerArray;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeIntArrayUnwrapNode extends Node {

    public abstract int[] execute(Object length, Object x);

    static boolean isVectorRFFIWrapper(Object x) {
        return x instanceof VectorRFFIWrapper;
    }

    @Specialization
    protected int[] doVectorRFFIWrapper(@SuppressWarnings("unused") Object length, VectorRFFIWrapper x) {
        return ((RIntVector) x.getVector()).getDataTemp();
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interop.isPointer(x)"}, limit = "2")
    protected int[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interop) {
        try {
            long addr = interop.asPointer(x);
            return new NativeIntegerArray(addr, length).getIntegerArray();
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Fallback
    protected int[] doFallback(@SuppressWarnings("unused") Object length, Object x) {
        throw RError.error(this, RError.Message.GENERIC, "Invalid double array object " + x);
    }
}
