/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RScalarList;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

/**
 * A final step before an object is sent to native code or LLVM interpreter.
 * 
 * The resulting object must be able to live on the native side, i.e., {@link RBaseObject} that can
 * have its native mirror.
 *
 * The life-cycle of the resulting Java object should be tied to the argument, which is performance
 * (caching the result) and defensive measure. It is a defensive measure since any RFFI function
 * whose result's life-cycle should be tied with some other R object (e.g. it is retrieved from an
 * environment) should make sure it is tied by replacing the original compact representation with
 * the materialized one inside the referer.
 *
 * We cannot tie the life-cycle of wrappers of primitive values, they are at least protected with
 * {@link RFFIContext#registerReferenceUsedInNative(Object)} elsewhere.
 *
 * See documentation/dev/ffi.md for more details.
 */
@GenerateUncached
public abstract class FFIWrapNode extends Node {

    public abstract Object execute(Object value);

    // Scalar values:

    @Specialization
    protected static Object wrap(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(byte value) {
        return RDataFactory.createLogicalVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(String value) {
        return RDataFactory.createStringVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RString value) {
        return RDataFactory.createStringVectorFromScalar(value.getValue());
    }

    @Specialization
    protected static Object wrap(RRaw value) {
        return RDataFactory.createRawVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RComplex value) {
        return RDataFactory.createComplexVectorFromScalar(value);
    }

    // Scalar vectors: so far protected via weak hash map, should not occur here often
    // Maybe we should get rid of the scalar vectors altogether

    private static <T extends RScalar> RAbstractVector protectMaterialized(T scalar, Function<T, RAbstractVector> factory) {
        return RContext.getInstance().getRFFI().getOrCreateMaterialized(scalar, factory);
    }

    @Specialization
    protected static Object wrap(RInteger value) {
        return protectMaterialized(value, v -> RDataFactory.createIntVectorFromScalar(v.getValue()));
    }

    @Specialization
    protected static Object wrap(RDouble value) {
        return protectMaterialized(value, v -> RDataFactory.createDoubleVectorFromScalar(v.getValue()));
    }

    @Specialization
    protected static Object wrap(RLogical value) {
        return protectMaterialized(value, v -> RDataFactory.createLogicalVectorFromScalar(v.getValue()));
    }

    @Specialization
    protected static Object wrap(RScalarList value) {
        return protectMaterialized(value, v -> RDataFactory.createList(new Object[]{v.getValue()}));
    }

    // Sequences: life-cycle of the materialized vector is cached and tied with the sequence via a
    // field inside the sequence

    @Specialization
    protected static Object wrap(RSequence seq) {
        return seq.cachedMaterialize();
    }

    // VectorRFFIWrapper: held by a field in NativeMirror of the corresponding vector

    @Specialization
    protected static Object wrap(VectorRFFIWrapper value) {
        return value;
    }

    // No need to wrap other RObjects than sequences or scalars

    @Specialization(guards = "!isRScalarVectorOrSequence(value)")
    protected static Object wrap(RBaseObject value) {
        return value;
    }

    // Symbol holds the address as a field

    @Specialization
    protected static Object wrap(SymbolHandle sym) {
        return sym.asAddress();
    }

    // Wrappers for foreign objects are held in a weak hash map

    @Specialization(guards = "isForeignObject(value)")
    protected static Object wrapForeignObject(TruffleObject value) {
        return RContext.getInstance().getRFFI().getOrCreateForeignObjectWrapper(value);
    }

    @Fallback
    protected static Object wrap(Object value) {
        CompilerDirectives.transferToInterpreter();
        String name = value == null ? "null" : value.getClass().getSimpleName();
        throw RInternalError.shouldNotReachHere("invalid wrapping: " + name);
    }

    static boolean isForeignObject(Object value) {
        return !(value instanceof RTruffleObject);
    }

    protected static boolean isRScalarVectorOrSequence(RBaseObject value) {
        return value instanceof RScalarVector || value instanceof RSequence;
    }

    public static FFIWrapNode create() {
        return FFIWrapNodeGen.create();
    }

    public static FFIWrapNode[] create(int count) {
        FFIWrapNode[] result = new FFIWrapNode[count];
        for (int i = 0; i < count; i++) {
            result[i] = FFIWrapNode.create();
        }
        return result;
    }
}
