/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.ffi.interop.NativeDoubleArray;

/**
 * Converts values that can live on the FastR side to values that can be sent to the native code,
 * i.e., anything that is not {@link RBaseObject} and doesn't have a native mirror needs to be
 * wrapped to such object.
 *
 * See documentation/dev/ffi.md for more details.
 */
@ImportStatic(DSLConfig.class)
@GenerateUncached
public abstract class FFIMaterializeNode extends Node {

    public Object materialize(Object value) {
        return execute(value);
    }

    protected abstract Object execute(Object value);

    public static Object uncachedMaterialize(Object value) {
        return FFIMaterializeNodeGen.getUncached().execute(value);
    }

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
    protected static Object wrap(double[] value) {
        return new NativeDoubleArray(value);
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
    protected static Object wrap(RRaw value) {
        return RDataFactory.createRawVectorFromScalar(value);
    }

    @Specialization
    protected static Object wrap(RComplex value) {
        return RDataFactory.createComplexVectorFromScalar(value);
    }

    // RObjectDataPtr: held by a field in NativeMirror of the corresponding vector

    @Specialization
    protected static Object wrap(RObjectDataPtr value) {
        return value;
    }

    // No need to wrap any RObjects
    @Specialization
    protected static Object wrap(RBaseObject value) {
        return value;
    }

    // Symbol holds the address as a field

    @Specialization
    protected static Object wrap(DLL.SymbolHandle sym) {
        return sym.asAddress();
    }

    // Wrappers for foreign objects are held in a weak hash map

    @Specialization(guards = "isForeignObject(value)")
    protected Object wrapForeignObject(TruffleObject value) {
        return RContext.getInstance(this).getRFFI().getOrCreateForeignObjectWrapper(value);
    }

    @Fallback
    protected static Object wrap(Object value) {
        CompilerDirectives.transferToInterpreter();
        String name = value == null ? "null" : value.getClass().getSimpleName();
        throw RInternalError.shouldNotReachHere("invalid wrapping: " + name);
    }

    static boolean isForeignObject(Object value) {
        return RRuntime.isForeignObject(value);
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
