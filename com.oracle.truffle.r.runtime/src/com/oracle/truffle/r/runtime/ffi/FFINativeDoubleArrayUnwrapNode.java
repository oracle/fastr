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
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeDoubleArrayUnwrapNode extends Node {

    public abstract double[] execute(Object length, Object x);

    static boolean isVectorRFFIWrapper(Object x) {
        return x instanceof VectorRFFIWrapper;
    }

    @Specialization
    protected double[] doVectorRFFIWrapper(@SuppressWarnings("unused") Object length, VectorRFFIWrapper x) {
        return ((RAbstractDoubleVector) x.getVector()).getDataTemp();
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interop.isPointer(x)"}, limit = "2")
    protected double[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interop) {
        try {
            long addr = interop.asPointer(x);
            return new NativeDoubleArray(addr, length).getDoubleArray();
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Fallback
    protected double[] doFallback(@SuppressWarnings("unused") Object length, Object x) {
        throw RError.error(this, RError.Message.GENERIC, "Invalid double array object " + x);
    }
}
