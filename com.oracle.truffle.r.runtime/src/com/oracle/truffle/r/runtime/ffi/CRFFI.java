/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

import sun.misc.Unsafe;

@MessageResolution(receiverType = TemporaryWrapper.class)
class TemporaryWrapperMR {

    @Resolve(message = "IS_POINTER")
    public abstract static class TemporaryWrapperIsPointerNode extends Node {
        protected Object access(@SuppressWarnings("unused") TemporaryWrapper receiver) {
            return true;
        }
    }

    @Resolve(message = "AS_POINTER")
    public abstract static class TemporaryWrapperAsPointerNode extends Node {
        private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

        protected Object access(TemporaryWrapper receiver) {
            long address = receiver.address;
            if (profile.profile(address == 0)) {
                receiver.address = address = receiver.allocate();
            }
            return address;
        }
    }

    @Resolve(message = "READ")
    public abstract static class TemporaryWrapperReadNode extends Node {
        protected Object access(TemporaryWrapper receiver, long index) {
            return receiver.read(index);
        }

        protected Object access(TemporaryWrapper receiver, int index) {
            return receiver.read(index);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class TemporaryWrapperWriteNode extends Node {
        protected Object access(TemporaryWrapper receiver, long index, Object value) {
            receiver.write(index, value);
            return value;
        }

        protected Object access(TemporaryWrapper receiver, int index, Object value) {
            receiver.write(index, value);
            return value;
        }
    }

    @CanResolve
    public abstract static class TemporaryWrapperCheck extends Node {
        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof TemporaryWrapper;
        }
    }
}

abstract class TemporaryWrapper implements TruffleObject {

    protected long address;
    protected RAbstractAtomicVector vector;

    public TemporaryWrapper(RAbstractAtomicVector vector) {
        this.vector = vector;
    }

    public abstract long allocate();

    public Object read(long index) {
        throw RInternalError.unimplemented("read at " + index);
    }

    public void write(long index, Object value) {
        throw RInternalError.unimplemented("write of value " + value + " at index " + index);
    }

    @Override
    public final ForeignAccess getForeignAccess() {
        return TemporaryWrapperMRForeign.ACCESS;
    }

    public final RAbstractAtomicVector cleanup() {
        if (address == 0) {
            return vector;
        } else {
            return copyBack();
        }
    }

    protected abstract RAbstractAtomicVector copyBack();
}

final class StringWrapper extends TemporaryWrapper {

    public StringWrapper(RAbstractStringVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractStringVector v = (RAbstractStringVector) vector;
        int length = v.getLength();
        int size = length * 8;
        byte[][] bytes = new byte[length][];
        for (int i = 0; i < length; i++) {
            String element = v.getDataAt(i);
            bytes[i] = element.getBytes(StandardCharsets.US_ASCII);
            size += bytes[i].length + 1;
        }
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(size);
        long ptr = memory + length * 8; // start of the actual character data
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putLong(memory + i * 8, ptr);
            UnsafeAdapter.UNSAFE.copyMemory(bytes[i], Unsafe.ARRAY_BYTE_BASE_OFFSET, null, ptr, bytes[i].length);
            ptr += bytes[i].length;
            UnsafeAdapter.UNSAFE.putByte(ptr++, (byte) 0);
        }
        assert ptr == memory + size : "should have filled everything";
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RStringVector copyBack() {
        RStringVector result = ((RAbstractStringVector) vector).materialize();
        String[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        for (int i = 0; i < data.length; i++) {
            long ptr = UnsafeAdapter.UNSAFE.getLong(address + i * 8);
            int length = 0;
            while (UnsafeAdapter.UNSAFE.getByte(ptr + length) != 0) {
                length++;
            }
            byte[] bytes = new byte[length];
            UnsafeAdapter.UNSAFE.copyMemory(null, ptr, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
            data[i] = new String(bytes, StandardCharsets.US_ASCII);
        }
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createStringVector(data, true);
    }
}

final class IntWrapper extends TemporaryWrapper {

    public IntWrapper(RAbstractIntVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractIntVector v = (RAbstractIntVector) vector;
        int length = v.getLength();
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(length * Unsafe.ARRAY_INT_INDEX_SCALE);
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putInt(memory + (i * Unsafe.ARRAY_INT_INDEX_SCALE), v.getDataAt(i));
        }
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RIntVector copyBack() {
        RIntVector result = ((RAbstractIntVector) vector).materialize();
        int[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_INT_BASE_OFFSET, vector.getLength() * Unsafe.ARRAY_INT_INDEX_SCALE);
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createIntVector(data, false);
    }
}

final class LogicalWrapper extends TemporaryWrapper {

    public LogicalWrapper(RAbstractLogicalVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractLogicalVector v = (RAbstractLogicalVector) vector;
        int length = v.getLength();
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(length * Unsafe.ARRAY_INT_INDEX_SCALE);
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putInt(memory + (i * Unsafe.ARRAY_INT_INDEX_SCALE), RRuntime.logical2int(v.getDataAt(i)));
        }
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RLogicalVector copyBack() {
        RLogicalVector result = ((RAbstractLogicalVector) vector).materialize();
        byte[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        int length = vector.getLength();
        for (int i = 0; i < length; i++) {
            data[i] = RRuntime.int2logical(UnsafeAdapter.UNSAFE.getInt(address + (i * Unsafe.ARRAY_INT_INDEX_SCALE)));
        }
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createLogicalVector(data, false);
    }
}

final class DoubleWrapper extends TemporaryWrapper {

    public DoubleWrapper(RAbstractDoubleVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractDoubleVector v = (RAbstractDoubleVector) vector;
        int length = v.getLength();
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putDouble(memory + (i * Unsafe.ARRAY_DOUBLE_INDEX_SCALE), v.getDataAt(i));
        }
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RDoubleVector copyBack() {
        RDoubleVector result = ((RAbstractDoubleVector) vector).materialize();
        double[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, vector.getLength() * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createDoubleVector(data, false);
    }
}

final class ComplexWrapper extends TemporaryWrapper {

    public ComplexWrapper(RAbstractComplexVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractComplexVector v = (RAbstractComplexVector) vector;
        int length = v.getLength();
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE * 2);
        for (int i = 0; i < length; i++) {
            RComplex element = v.getDataAt(i);
            UnsafeAdapter.UNSAFE.putDouble(memory + (i * Unsafe.ARRAY_DOUBLE_INDEX_SCALE * 2), element.getRealPart());
            UnsafeAdapter.UNSAFE.putDouble(memory + (i * Unsafe.ARRAY_DOUBLE_INDEX_SCALE * 2) + 8, element.getImaginaryPart());
        }
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RComplexVector copyBack() {
        RComplexVector result = ((RAbstractComplexVector) vector).materialize();
        double[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, vector.getLength() * 2 * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createComplexVector(data, false);
    }
}

final class RawWrapper extends TemporaryWrapper {

    public RawWrapper(RAbstractRawVector vector) {
        super(vector);
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        RAbstractRawVector v = (RAbstractRawVector) vector;
        int length = v.getLength();
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(length * Unsafe.ARRAY_BYTE_INDEX_SCALE);
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putByte(memory + (i * Unsafe.ARRAY_BYTE_INDEX_SCALE), v.getRawDataAt(i));
        }
        return memory;
    }

    @Override
    @TruffleBoundary
    protected RRawVector copyBack() {
        RRawVector result = ((RAbstractRawVector) vector).materialize();
        byte[] data = result.isTemporary() ? result.getDataWithoutCopying() : result.getDataCopy();
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_BYTE_BASE_OFFSET, vector.getLength() * Unsafe.ARRAY_BYTE_INDEX_SCALE);
        UnsafeAdapter.UNSAFE.freeMemory(address);
        return RDataFactory.createRawVector(data);
    }
}

/**
 * Support for the {.C} and {.Fortran} calls.
 */
public interface CRFFI {

    public static abstract class InvokeCNode extends RBaseNode {

        /**
         * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
         * {@code args}. The values in {@code args} should be support the IS_POINTER/AS_POINTER
         * messages.
         */
        protected abstract void execute(NativeCallInfo nativeCallInfo, Object[] args);

        @TruffleBoundary
        protected TemporaryWrapper getNativeArgument(int index, Object vector) {
            if (vector instanceof RAbstractDoubleVector) {
                return new DoubleWrapper((RAbstractDoubleVector) vector);
            } else if (vector instanceof RAbstractIntVector) {
                return new IntWrapper((RAbstractIntVector) vector);
            } else if (vector instanceof RAbstractLogicalVector) {
                return new LogicalWrapper((RAbstractLogicalVector) vector);
            } else if (vector instanceof RAbstractComplexVector) {
                return new ComplexWrapper((RAbstractComplexVector) vector);
            } else if (vector instanceof RAbstractStringVector) {
                return new StringWrapper((RAbstractStringVector) vector);
            } else if (vector instanceof RAbstractRawVector) {
                return new RawWrapper((RAbstractRawVector) vector);
            } else if (vector instanceof String) {
                return new StringWrapper(RDataFactory.createStringVectorFromScalar((String) vector));
            } else if (vector instanceof Double) {
                return new DoubleWrapper(RDataFactory.createDoubleVectorFromScalar((double) vector));
            } else if (vector instanceof Integer) {
                return new IntWrapper(RDataFactory.createIntVectorFromScalar((int) vector));
            } else if (vector instanceof Byte) {
                return new LogicalWrapper(RDataFactory.createLogicalVectorFromScalar((byte) vector));
            } else {
                throw error(RError.Message.UNIMPLEMENTED_ARG_TYPE, index + 1);
            }
        }

        @TruffleBoundary
        public final RList dispatch(NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args) {
            @SuppressWarnings("unused")
            boolean dupArgs = RRuntime.fromLogical(dup);
            @SuppressWarnings("unused")
            boolean checkNA = RRuntime.fromLogical(naok);

            // Analyze the args, making copies (ignoring dup for now)
            Object[] array = new Object[args.getLength()];
            for (int i = 0; i < array.length; i++) {
                array[i] = getNativeArgument(i, args.getArgument(i));
            }

            execute(nativeCallInfo, array);

            // we have to assume that the native method updated everything
            Object[] results = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                results[i] = ((TemporaryWrapper) array[i]).cleanup();
            }

            return RDataFactory.createList(results, validateArgNames(array.length, args.getSignature()));
        }

        private static RStringVector validateArgNames(int argsLength, ArgumentsSignature signature) {
            String[] listArgNames = new String[argsLength];
            for (int i = 0; i < argsLength; i++) {
                String name = signature.getName(i);
                if (name == null) {
                    name = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                }
                listArgNames[i] = name;
            }
            return RDataFactory.createStringVector(listArgNames, RDataFactory.COMPLETE_VECTOR);
        }
    }

    InvokeCNode createInvokeCNode();
}
