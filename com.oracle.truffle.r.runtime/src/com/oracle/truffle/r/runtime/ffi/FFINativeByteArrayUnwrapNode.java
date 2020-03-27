package com.oracle.truffle.r.runtime.ffi;

import java.nio.ByteOrder;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeRawArray;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeByteArrayUnwrapNode extends RBaseNode {

    public abstract byte[] execute(Object length, Object x);

    static Object getSulongByteArrayType(ContextReference<RContext> ctxRef) {
        return ctxRef.get().getRFFI().getSulongArrayType((byte) 42);
    }

    static Object getSulongIntArrayType(ContextReference<RContext> ctxRef) {
        return ctxRef.get().getRFFI().getSulongArrayType(42);
    }

    static boolean isVectorRFFIWrapper(Object x) {
        return x instanceof VectorRFFIWrapper;
    }

    @Specialization
    protected byte[] doVectorRFFIWrapper(@SuppressWarnings("unused") Object length, VectorRFFIWrapper x, @Cached BranchProfile rawVectorProfile) {
        TruffleObject vector = x.getVector();
        if (vector instanceof RRawVector) {
            rawVectorProfile.enter();
            return ((RRawVector) vector).getDataTemp();
        } else {
            assert vector instanceof RIntVector;
            return Utils.intArrayToByteArray(((RIntVector) vector).getDataTemp(), ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongByteArrayType"}, limit = "1")
    protected byte[] doByteArrayInterop(@SuppressWarnings("unused") Object length, Object x,
                    @SuppressWarnings("unused") @CachedLibrary("x") NativeTypeLibrary typeLib,
                    @CachedLibrary("x") InteropLibrary interopLib,
                    @SuppressWarnings("unused") @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @SuppressWarnings("unused") @Cached(value = "getSulongByteArrayType(ctxRef)", uncached = "getSulongByteArrayType(ctxRef)") Object sulongByteArrayType) {
        try {
            int size = (int) interopLib.getArraySize(x);
            byte[] result = new byte[size];
            for (int i = 0; i < size; i++) {
                result[i] = (byte) interopLib.readArrayElement(x, i);
            }
            return result;
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongIntArrayType"}, limit = "1")
    protected byte[] doIntArrayInterop(@SuppressWarnings("unused") Object length, Object x,
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
            return Utils.intArrayToByteArray(result, ByteOrder.LITTLE_ENDIAN);
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "!interopLib.hasArrayElements(x)", "interopLib.isPointer(x)"}, limit = "2")
    protected byte[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interopLib) {
        try {
            interopLib.toNative(x);
            long addr = interopLib.asPointer(x);
            byte[] result = new byte[length];
            NativeMemory.copyMemory(addr, result, ElementType.INT, length);
            return result;
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Fallback
    protected byte[] doFallback(@SuppressWarnings("unused") Object length, Object x) {
        throw RError.error(this, RError.Message.GENERIC, "Invalid double array object " + x);
    }
}
