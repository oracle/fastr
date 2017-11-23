/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;

public abstract class SetDataAt extends Node {

    public abstract void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value);

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class Double extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RDoubleVector) vector, store, index, (double) value);
        }

        public final void setDataAt(RDoubleVector vector, Object store, int index, double value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RDoubleVector vector, Object store, int index, double value);

        @Specialization(guards = "!isNativeMirror(store)")
        protected void doManagedRVector(RDoubleVector vec, Object store, int index, double value) {
            ((double[]) store)[index] = value;
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RDoubleVector vec, Object store, int index, double value) {
            NativeDataAccess.setNativeMirrorData(store, index, value);
        }

        public static Double create() {
            return SetDataAtFactory.DoubleNodeGen.create();
        }
    }

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class Int extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RIntVector) vector, store, index, (int) value);
        }

        public final void setDataAt(RIntVector vector, Object store, int index, int value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RIntVector vector, Object store, int index, int value);

        @Specialization(guards = "!isNativeMirror(store)")
        protected void doManagedRVector(RIntVector vec, Object store, int index, int value) {
            ((int[]) store)[index] = value;
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RIntVector vec, Object store, int index, int value) {
            NativeDataAccess.setNativeMirrorData(store, index, value);
        }

        public static Int create() {
            return SetDataAtFactory.IntNodeGen.create();
        }
    }

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class Logical extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RLogicalVector) vector, store, index, (byte) value);
        }

        public final void setDataAt(RLogicalVector vector, Object store, int index, byte value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RLogicalVector vector, Object store, int index, byte value);

        @Specialization(guards = "!isNativeMirror(store)")
        protected void doManagedRVector(RLogicalVector vec, Object store, int index, byte value) {
            ((byte[]) store)[index] = value;
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RLogicalVector vec, Object store, int index, byte value) {
            NativeDataAccess.setNativeMirrorData(store, index, value);
        }

        public static Logical create() {
            return SetDataAtFactory.LogicalNodeGen.create();
        }
    }

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class Raw extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RRawVector) vector, store, index, (byte) value);
        }

        public final void setDataAt(RRawVector vector, Object store, int index, byte value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RRawVector vector, Object store, int index, byte value);

        @Specialization(guards = "!isNativeMirror(store)")
        protected void doManagedRVector(RRawVector vec, Object store, int index, byte value) {
            ((byte[]) store)[index] = value;
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RRawVector vec, Object store, int index, byte value) {
            NativeDataAccess.setNativeMirrorData(store, index, value);
        }

        public static Raw create() {
            return SetDataAtFactory.RawNodeGen.create();
        }
    }

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class Complex extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RComplexVector) vector, store, index, (RComplex) value);
        }

        public final void setDataAt(RComplexVector vector, Object store, int index, RComplex value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RComplexVector vector, Object store, int index, RComplex value);

        @Specialization(guards = "!isNativeMirror(storeObj)")
        protected void doManagedRVector(RComplexVector vec, Object storeObj, int index, RComplex value) {
            double[] store = (double[]) storeObj;
            store[index * 2] = value.getRealPart();
            store[index * 2 + 1] = value.getImaginaryPart();
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RComplexVector vec, Object store, int index, RComplex value) {
            throw RInternalError.unimplemented();
        }

        public static Complex create() {
            return SetDataAtFactory.ComplexNodeGen.create();
        }
    }

    @ImportStatic(NativeDataAccess.class)
    @SuppressWarnings("unused")
    public abstract static class String extends SetDataAt {

        @Override
        public final void setDataAtAsObject(RVector<?> vector, Object store, int index, Object value) {
            setDataAt((RStringVector) vector, store, index, (java.lang.String) value);
        }

        public final void setDataAt(RStringVector vector, Object store, int index, java.lang.String value) {
            execute(vector, store, index, value);
        }

        public abstract void execute(RStringVector vector, Object store, int index, java.lang.String value);

        @Specialization(guards = "!isNativeMirror(store)")
        protected void doManagedRVector(RStringVector vec, Object store, int index, java.lang.String value) {
            ((java.lang.String[]) store)[index] = value;
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected void doNativeDataRVector(RStringVector vec, Object store, int index, java.lang.String value) {
            throw RInternalError.unimplemented();
        }

        public static String create() {
            return SetDataAtFactory.StringNodeGen.create();
        }
    }
}
