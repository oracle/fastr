/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * This implementation of the {@link VectorDataLibrary} serves as a generic wrapper that can coerce
 * vector data of any primitive type to look like vector data of given primitive type, which must be
 * different to the original type. The coercion takes place lazily.
 */
@ExportLibrary(VectorDataLibrary.class)
public abstract class VectorDataClosure implements RClosure, TruffleObject {
    final RAbstractVector delegate;
    final Object data;

    private VectorDataClosure(RAbstractVector delegate, Object data, RType targetType) {
        assert delegate == null || delegate.getData() == data;
        // Do not use closure to wrap int vector data as int vector data...
        assert targetType != VectorDataLibrary.getFactory().getUncached().getType(data);
        this.delegate = delegate;
        this.data = data;
    }

    // In order to support the legacy (pre VectorDataLibrary) vector closures,
    // the data object can hold the vector being wrapped as well so that it
    // can implement the RClosure interface
    static Object fromVector(RAbstractVector delegate, RType targetType) {
        if (delegate.getRType() == targetType) {
            return delegate.getData();
        } else {
            return create(delegate, delegate.getData(), targetType);
        }
    }

    static Object fromData(Object delegate, RType delegateType, RType targetType) {
        if (delegateType == targetType) {
            return delegate;
        } else {
            return create(null, delegate, targetType);
        }
    }

    private static Object create(RAbstractVector delegate, Object data, RType targetType) {
        switch (targetType) {
            case Integer:
                return new IntClosure(delegate, data, targetType);
            case Double:
                return new DoubleClosure(delegate, data, targetType);
            case Logical:
                return new LogicalClosure(delegate, data, targetType);
            case Complex:
                return new ComplexClosure(delegate, data, targetType);
            case Raw:
                return new RawClosure(delegate, data, targetType);
            case Character:
                return new StringClosure(delegate, data, targetType);
            case List:
                return new ListClosure(delegate, data, targetType);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    protected abstract RType getTargetType();

    protected abstract VectorDataClosure copyDataClosure();

    @ExportMessage
    public RType getType() {
        return getTargetType();
    }

    @ExportMessage
    public int getLength(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getLength(data);
    }

    @ExportMessage
    public Object materialize(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        switch (getType()) {
            case Integer:
                return new RIntArrayVectorData(getIntDataCopy(dataLib), dataLib.getNACheck(data).neverSeenNA());
            case Double:
                return new RDoubleArrayVectorData(getDoubleDataCopy(dataLib), dataLib.getNACheck(data).neverSeenNA());
            case Raw:
                return new RRawArrayVectorData(getRawDataCopy(dataLib));
            case Logical:
                return new RLogicalArrayVectorData(getLogicalDataCopy(dataLib), dataLib.getNACheck(data).neverSeenNA());
            case Complex:
                return new RComplexArrayVectorData(getComplexDataCopy(dataLib), dataLib.getNACheck(data).neverSeenNA());
            case Character:
                return new RStringArrayVectorData(getStringDataCopy(dataLib), dataLib.getNACheck(data).neverSeenNA());
            default:
                throw CompilerDirectives.shouldNotReachHere(getType().toString());
        }
    }

    @ExportMessage
    public VectorDataClosure copy(@SuppressWarnings("unused") boolean deep) {
        return copyDataClosure();
    }

    @ExportMessage
    public SeqIterator iterator(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.iterator(data);
    }

    @ExportMessage
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.nextImpl(data, it, loopCondition);
    }

    @ExportMessage
    public void nextWithWrap(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        dataLib.nextWithWrap(data, it);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.randomAccessIterator(data);
    }

    @ExportMessage
    public NACheck getNACheck() {
        // the contract is that the check is enabled if the vector may contain NA values,
        // we cannot say upfront that it does not, because NAs can occur as the result of type
        // conversions
        // NOTE: this method and "isComplete" can be improved for type conversions that are known to
        // never introduce NA values (e.g., int -> double)
        return NACheck.getEnabled();
    }

    // Integer

    @ExportMessage
    public int[] getIntDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Integer;
        int[] result = new int[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextInt(data, it);
        }
        return result;
    }

    @ExportMessage
    public int getIntAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getIntAt(data, index);
    }

    @ExportMessage
    public int getNextInt(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextInt(data, it);
    }

    @ExportMessage
    public int getInt(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getInt(data, it, index);
    }

    // Double

    @ExportMessage
    public double[] getDoubleDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Double;
        double[] result = new double[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextDouble(data, it);
        }
        return result;
    }

    @ExportMessage
    public double getDoubleAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getDoubleAt(data, index);
    }

    @ExportMessage
    public double getNextDouble(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextDouble(data, it);
    }

    @ExportMessage
    public double getDouble(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getDouble(data, it, index);
    }

    // Logical

    @ExportMessage
    public byte[] getLogicalDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Logical;
        byte[] result = new byte[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextLogical(data, it);
        }
        return result;
    }

    @ExportMessage
    public byte getLogicalAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getLogicalAt(data, index);
    }

    @ExportMessage
    public byte getNextLogical(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextLogical(data, it);
    }

    @ExportMessage
    public byte getLogical(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getLogical(data, it, index);
    }

    // Raw

    @ExportMessage
    public byte[] getRawDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Raw;
        byte[] result = new byte[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextRaw(data, it);
        }
        return result;
    }

    @ExportMessage
    public byte getRawAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getRawAt(data, index);
    }

    @ExportMessage
    public byte getNextRaw(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextRaw(data, it);
    }

    @ExportMessage
    public byte getRaw(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getRaw(data, it, index);
    }

    // Complex

    @ExportMessage
    public double[] getComplexDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Complex;
        double[] result = new double[getLength(dataLib) * 2];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            RComplex val = dataLib.getNextComplex(data, it);
            result[it.getIndex() * 2] = val.getRealPart();
            result[it.getIndex() * 2 + 1] = val.getImaginaryPart();
        }
        return result;
    }

    @ExportMessage
    public RComplex getComplexAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getComplexAt(data, index);
    }

    @ExportMessage
    public RComplex getNextComplex(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextComplex(data, it);
    }

    @ExportMessage
    public RComplex getComplex(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getComplex(data, it, index);
    }

    // String

    @ExportMessage
    public String[] getStringDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Character;
        String[] result = new String[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextString(data, it);
        }
        return result;
    }

    @ExportMessage
    public String getStringAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getStringAt(data, index);
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextString(data, it);
    }

    @ExportMessage
    public String getString(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getString(data, it, index);
    }

    // CharSXP

    @ExportMessage
    public CharSXPWrapper[] getCharSXPDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Char || getTargetType() == RType.Character;
        CharSXPWrapper[] result = new CharSXPWrapper[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextCharSXP(data, it);
        }
        return result;
    }

    @ExportMessage
    public RStringCharSXPData materializeCharSXPStorage(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.Char || getTargetType() == RType.Character;
        return new RStringCharSXPData(getCharSXPDataCopy(dataLib));
    }

    @ExportMessage
    public CharSXPWrapper getCharSXPAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getCharSXPAt(data, index);
    }

    @ExportMessage
    public CharSXPWrapper getNextCharSXP(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextCharSXP(data, it);
    }

    @ExportMessage
    public CharSXPWrapper getCharSXP(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getCharSXP(data, it, index);
    }

    // List

    @ExportMessage
    public Object[] getListDataCopy(@CachedLibrary("this.data") VectorDataLibrary dataLib) {
        assert getTargetType() == RType.List;
        Object[] result = new Object[getLength(dataLib)];
        SeqIterator it = dataLib.iterator(data);
        while (dataLib.next(data, it)) {
            result[it.getIndex()] = dataLib.getNextElement(data, it);
        }
        return result;
    }

    @ExportMessage
    public Object getElementAt(int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getElementAt(data, index);
    }

    @ExportMessage
    public Object getNextElement(SeqIterator it,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getNextElement(data, it);
    }

    @ExportMessage
    public Object getElement(RandomAccessIterator it, int index,
                    @CachedLibrary("this.data") VectorDataLibrary dataLib) {
        return dataLib.getElement(data, it, index);
    }

    // Support of the RClosure interface

    @Override
    public Object getDelegateDataAt(int idx) {
        return delegate.getDataAtAsObject(idx);
    }

    // NOTE: Legacy method!
    // TODO: We should be able to remove this method and the need for the "delegate" field once
    // string vectors are converted to "storage startegy" pattern
    @Override
    public RAbstractVector getDelegate() {
        assert delegate != null;
        return delegate;
    }

    // Subclasses that specialize on target type, so that getType message gives constant value

    static class IntClosure extends VectorDataClosure {
        IntClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Integer;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new IntClosure(delegate, data, RType.Integer);
        }
    }

    static final class DoubleClosure extends VectorDataClosure {
        DoubleClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Double;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new IntClosure(delegate, data, RType.Double);
        }
    }

    static final class LogicalClosure extends VectorDataClosure {
        LogicalClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Logical;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new LogicalClosure(delegate, data, RType.Logical);
        }
    }

    static final class RawClosure extends VectorDataClosure {
        RawClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Raw;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new RawClosure(delegate, data, RType.Raw);
        }
    }

    static final class ComplexClosure extends VectorDataClosure {
        ComplexClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Complex;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new ComplexClosure(delegate, data, RType.Complex);
        }
    }

    static final class StringClosure extends VectorDataClosure {
        StringClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.Character;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new StringClosure(delegate, data, RType.Character);
        }
    }

    static final class ListClosure extends VectorDataClosure {
        ListClosure(RAbstractVector delegate, Object data, RType targetType) {
            super(delegate, data, targetType);
        }

        @Override
        protected RType getTargetType() {
            return RType.List;
        }

        @Override
        protected VectorDataClosure copyDataClosure() {
            return new ListClosure(delegate, data, RType.Integer);
        }
    }
}
