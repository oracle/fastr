package com.oracle.truffle.r.runtime.ffi;

import java.nio.ByteOrder;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.ffi.interop.NativeRawArray;

@GenerateUncached
@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class FFINativeByteArrayUnwrapNode extends Node {

    public abstract byte[] execute(Object length, Object x);

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

    @Specialization(guards = {"!isVectorRFFIWrapper(x)", "interop.isPointer(x)"}, limit = "2")
    protected byte[] doPointer(int length, Object x, @CachedLibrary("x") InteropLibrary interop) {
        try {
            long addr = interop.asPointer(x);
            return new NativeRawArray(addr, length).getByteArray();
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Fallback
    protected byte[] doFallback(@SuppressWarnings("unused") Object length, Object x) {
        throw RError.error(this, RError.Message.GENERIC, "Invalid double array object " + x);
    }
}
