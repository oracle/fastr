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
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
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
        return ((RDoubleVector) x.getVector()).getDataTemp();
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
            double[] result = new double[length];
            NativeMemory.copyMemory(addr, result, ElementType.DOUBLE, length);
            return result;
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Fallback
    protected double[] doFallback(@SuppressWarnings("unused") Object length, Object x) {
        throw RError.error(this, RError.Message.GENERIC, "Invalid double array object " + x);
    }
}
