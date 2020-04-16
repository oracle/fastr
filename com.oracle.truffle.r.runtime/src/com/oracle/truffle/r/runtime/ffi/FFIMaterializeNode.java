/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;

/**
 * Converts values that can live on the FastR side to values that can be sent to the native code,
 * i.e., anything that is not {@link RBaseObject} and doesn't have a native mirror needs to be
 * wrapped to such object. Moreover, we need to convert compact read-only vectors to materialized
 * ones, since user may expect to be able to take their data-pointer and write into them.
 *
 * See documentation/dev/ffi.md for more details.
 */
@GenerateUncached
public abstract class FFIMaterializeNode extends Node {

    public Object materialize(Object value) {
        return execute(value, false);
    }

    public Object materializeProtected(Object value) {
        return execute(value, true);
    }

    protected abstract Object execute(Object value, boolean protect);

    public static Object uncachedMaterialize(Object value) {
        return FFIMaterializeNodeGen.getUncached().execute(value, false);
    }

    // Scalar values:

    @Specialization
    protected static Object wrap(int value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(double[] value, @SuppressWarnings("unused") boolean protect) {
        return new NativeDoubleArray(value);
    }

    @Specialization
    protected static Object wrap(byte value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createLogicalVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(String value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createStringVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RString value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createStringVectorFromScalar(value.getValue());
    }

    @Specialization
    protected static Object wrap(RRaw value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createRawVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RComplex value, @SuppressWarnings("unused") boolean protect) {
        return RDataFactory.createComplexVectorFromScalar(value);
    }

    // Sequences: life-cycle of the materialized vector is cached and tied with the sequence via a
    // field inside the sequence

    @Specialization
    protected static Object wrap(RSequence seq, @SuppressWarnings("unused") boolean protect) {
        return seq.cachedMaterialize();
    }

    // VectorRFFIWrapper: held by a field in NativeMirror of the corresponding vector

    @Specialization
    protected static Object wrap(VectorRFFIWrapper value, @SuppressWarnings("unused") boolean protect) {
        return value;
    }

    // No need to wrap other RObjects than sequences or scalars

    @Specialization(guards = "!isRScalarVectorOrSequenceOrHasVectorData(value)")
    protected static Object wrap(RBaseObject value, @SuppressWarnings("unused") boolean protect) {
        return value;
    }

    @Specialization
    protected static Object wrap(RIntVector value, @SuppressWarnings("unused") boolean protect) {
        // TODO specialize only for sequences (and maybe some other)
        return value.cachedMaterialize();
    }

    @Specialization
    protected static Object wrap(RDoubleVector value, @SuppressWarnings("unused") boolean protect) {
        // TODO specialize only for sequences (and maybe some other)
        return value.cachedMaterialize();
    }

    @Specialization
    protected static Object wrap(RRawVector value, @SuppressWarnings("unused") boolean protect) {
        return value.cachedMaterialize();
    }

    @Specialization
    protected static Object wrap(RLogicalVector value, @SuppressWarnings("unused") boolean protect) {
        return value.cachedMaterialize();
    }

    // Symbol holds the address as a field

    @Specialization
    protected static Object wrap(DLL.SymbolHandle sym, @SuppressWarnings("unused") boolean protect) {
        return sym.asAddress();
    }

    // Wrappers for foreign objects are held in a weak hash map

    @Specialization(guards = "isForeignObject(value)")
    protected static Object wrapForeignObject(TruffleObject value, @SuppressWarnings("unused") boolean protect) {
        return RContext.getInstance().getRFFI().getOrCreateForeignObjectWrapper(value);
    }

    @Fallback
    protected static Object wrap(Object value, @SuppressWarnings("unused") boolean protect) {
        CompilerDirectives.transferToInterpreter();
        String name = value == null ? "null" : value.getClass().getSimpleName();
        throw RInternalError.shouldNotReachHere("invalid wrapping: " + name);
    }

    static boolean isForeignObject(Object value) {
        // in case somebody calls wrap for an already wrapped RBaseObject
        assert value instanceof NativeDataAccess.NativeMirror : value.getClass().getSimpleName() + " has to be a NativeMirror";
        return RRuntime.isForeignObject(value);
    }

    protected static boolean isRScalarVectorOrSequenceOrHasVectorData(RBaseObject value) {
        return value instanceof RScalarVector || value instanceof RSequence || RRuntime.hasVectorData(value);
    }

    public static FFIMaterializeNode create() {
        return FFIMaterializeNodeGen.create();
    }

    public static FFIMaterializeNode[] create(int count) {
        FFIMaterializeNode[] result = new FFIMaterializeNode[count];
        for (int i = 0; i < count; i++) {
            result[i] = FFIMaterializeNode.create();
        }
        return result;
    }

    public static FFIMaterializeNode getUncached() {
        return FFIMaterializeNodeGen.getUncached();
    }
}
