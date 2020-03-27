package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeDoubleArrayUnwrapNode extends RBaseNode {

    public abstract double[] execute(Object length, Object x);

    static boolean isVectorRFFIWrapper(Object x) {
        return x instanceof VectorRFFIWrapper;
    }

    static Object getSulongDoubleArrayType(ContextReference<RContext> ctxRef) {
        return ctxRef.get().getRFFI().getSulongArrayType(42.2);
    }

    @Specialization
    protected double[] doVectorRFFIWrapper(@SuppressWarnings("unused") Object length, VectorRFFIWrapper x) {
        return ((RAbstractDoubleVector) x.getVector()).getDataTemp();
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongDoubleArrayType"}, limit = "1")
    protected double[] doInterop(@SuppressWarnings("unused") Object length, Object x,
                    @SuppressWarnings("unused") @CachedLibrary("x") NativeTypeLibrary typeLib,
                    @CachedLibrary("x") InteropLibrary interopLib,
                    @SuppressWarnings("unused") @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @SuppressWarnings("unused") @Cached(value = "getSulongDoubleArrayType(ctxRef)", uncached = "getSulongDoubleArrayType(ctxRef)") Object sulongDoubleArrayType) {
        try {
            int size = (int) interopLib.getArraySize(x);
            double[] result = new double[size];
            for (int i = 0; i < size; i++) {
                result[i] = (double) interopLib.readArrayElement(x, i);
            }
            return result;
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "!interopLib.hasArrayElements(x)", "interopLib.isPointer(x)"}, limit = "2")
    protected double[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interopLib) {
        try {
            interopLib.toNative(x);
            long addr = interopLib.asPointer(x);
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
