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
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeIntegerArray;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeIntArrayUnwrapNode extends RBaseNode {

    public abstract int[] execute(Object length, Object x);

    static boolean isVectorRFFIWrapper(Object x) {
        return x instanceof VectorRFFIWrapper;
    }

    static Object getSulongIntArrayType(ContextReference<RContext> ctxRef) {
        return ctxRef.get().getRFFI().getSulongArrayType(42);
    }

    @Specialization
    protected int[] doVectorRFFIWrapper(@SuppressWarnings("unused") Object length, VectorRFFIWrapper x) {
        return ((RIntVector) x.getVector()).getDataTemp();
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongIntArrayType"}, limit = "1")
    protected int[] doInterop(@SuppressWarnings("unused") Object length, Object x,
                    @SuppressWarnings("unused") @CachedLibrary("x") NativeTypeLibrary typeLib,
                    @CachedLibrary("x") InteropLibrary interopLib,
                    @SuppressWarnings("unused") @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @SuppressWarnings("unused") @Cached(value = "getSulongIntArrayType(ctxRef)", uncached = "getSulongIntArrayType(ctxRef)") Object sulongIntArrayType) {
        try {
            int size = (int) interopLib.getArraySize(x);
            int[] result = new int[size];
            for (int i = 0; i < size; i++) {
                result[i] = (int) interopLib.readArrayElement(x, i);
            }
            return result;
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "!interopLib.hasArrayElements(x)", "interopLib.isPointer(x)"}, limit = "2")
    protected int[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interopLib) {
        try {
            interopLib.toNative(x);
            long addr = interopLib.asPointer(x);
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
