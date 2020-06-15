/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
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

    static boolean isRObjectDataPtr(Object x) {
        return x instanceof RObjectDataPtr;
    }

    @Specialization
    protected byte[] doRObjectDataPtr(@SuppressWarnings("unused") Object length, RObjectDataPtr x, @Cached BranchProfile rawVectorProfile) {
        TruffleObject vector = x.getVector();
        if (vector instanceof RRawVector) {
            rawVectorProfile.enter();
            return ((RRawVector) vector).getDataTemp();
        } else {
            assert vector instanceof RIntVector;
            return Utils.intArrayToByteArray(((RIntVector) vector).getDataTemp(), ByteOrder.LITTLE_ENDIAN);
        }
    }

    @Specialization(guards = {"!isRObjectDataPtr(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongByteArrayType"}, limit = "1")
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

    @Specialization(guards = {"!isRObjectDataPtr(x)", "interopLib.hasArrayElements(x)", "typeLib.getNativeType(x) == sulongIntArrayType"}, limit = "1")
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

    @Specialization(guards = {"!isRObjectDataPtr(x)", "!interopLib.hasArrayElements(x)", "interopLib.isPointer(x)"}, limit = "2")
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
