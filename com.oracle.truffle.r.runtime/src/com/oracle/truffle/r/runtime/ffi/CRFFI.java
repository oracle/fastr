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
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
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
import com.oracle.truffle.r.runtime.data.RVector;
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
                return receiver.asPointer();
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
    protected boolean reuseVector = false;

    TemporaryWrapper(RAbstractAtomicVector vector) {
        this.vector = vector;
    }

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

    public long asPointer() {
        // if the vector is temporary, we can re-use it. We turn it into native memory backed
        // vector, keep it so and reuse it as the result.
        if (vector instanceof RVector<?> && ((RVector<?>) vector).isTemporary()) {
            NativeDataAccess.asPointer(vector);
            reuseVector = true;
            address = allocateNative();
        } else {
            reuseVector = false;
            address = allocate();
        }
        return address;
    }

    public final RAbstractAtomicVector cleanup() {
        if (address == 0 || reuseVector) {
            return vector;
        } else {
            return copyBack();
        }
    }

    protected abstract long allocate();

    protected abstract long allocateNative();

    protected abstract RAbstractAtomicVector copyBack();
}

// TODO: fortran only takes a pointer to the first string
final class StringWrapper extends TemporaryWrapper {

    StringWrapper(RAbstractStringVector vector) {
        super(vector);
    }

    @Override
    public long asPointer() {
        address = allocate();
        return address;
    }

    @Override
    protected long allocateNative() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    @TruffleBoundary
    public long allocate() {
        return allocateNativeStringVector((RAbstractStringVector) vector);
    }

    public static long allocateNativeStringVector(RAbstractStringVector vector) {
        // We allocate contiguous memory that we'll use to store both the array of pointers (char**)
        // and the arrays of characters (char*). Given vector of size N, we allocate memory for N
        // adresses (long) and after those we put individual strings character by character, the
        // pointers from the first segment of this memory will be pointing to the starts of those
        // strings.
        int length = vector.getLength();
        int size = length * Long.BYTES;
        byte[][] bytes = new byte[length][];
        for (int i = 0; i < length; i++) {
            String element = vector.getDataAt(i);
            bytes[i] = element.getBytes(StandardCharsets.US_ASCII);
            size += bytes[i].length + 1;
        }
        long memory = UnsafeAdapter.UNSAFE.allocateMemory(size);
        long ptr = memory + length * Long.BYTES; // start of the actual character data
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
        boolean reuseResult = result.isTemporary() && !result.hasNativeMemoryData();
        String[] data = reuseResult ? result.getInternalManagedData() : new String[result.getLength()];
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
        if (reuseResult) {
            return result;
        } else {
            RStringVector newResult = RDataFactory.createStringVector(data, true);
            newResult.copyAttributesFrom(result);
            return newResult;
        }
    }
}

final class IntWrapper extends TemporaryWrapper {

    IntWrapper(RAbstractIntVector vector) {
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
    protected long allocateNative() {
        return ((RIntVector) vector).allocateNativeContents();
    }

    @Override
    @TruffleBoundary
    protected RIntVector copyBack() {
        RIntVector result = RDataFactory.createIntVectorFromNative(address, vector.getLength());
        result.copyAttributesFrom(vector);
        return result;
    }
}

final class LogicalWrapper extends TemporaryWrapper {

    LogicalWrapper(RAbstractLogicalVector vector) {
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
    protected long allocateNative() {
        return ((RLogicalVector) vector).allocateNativeContents();
    }

    @Override
    @TruffleBoundary
    protected RLogicalVector copyBack() {
        RLogicalVector result = RDataFactory.createLogicalVectorFromNative(address, vector.getLength());
        result.copyAttributesFrom(vector);
        return result;
    }
}

final class DoubleWrapper extends TemporaryWrapper {

    DoubleWrapper(RAbstractDoubleVector vector) {
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
    protected long allocateNative() {
        return ((RDoubleVector) vector).allocateNativeContents();
    }

    @Override
    @TruffleBoundary
    protected RDoubleVector copyBack() {
        RDoubleVector result = RDataFactory.createDoubleVectorFromNative(address, vector.getLength());
        result.copyAttributesFrom(vector);
        return result;
    }
}

final class ComplexWrapper extends TemporaryWrapper {

    ComplexWrapper(RAbstractComplexVector vector) {
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
    protected long allocateNative() {
        return ((RComplexVector) vector).allocateNativeContents();
    }

    @Override
    @TruffleBoundary
    protected RComplexVector copyBack() {
        RComplexVector result = RDataFactory.createComplexVectorFromNative(address, vector.getLength());
        result.copyAttributesFrom(vector);
        return result;
    }
}

final class RawWrapper extends TemporaryWrapper {

    RawWrapper(RAbstractRawVector vector) {
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
    protected long allocateNative() {
        return ((RRawVector) vector).allocateNativeContents();
    }

    @Override
    @TruffleBoundary
    protected RRawVector copyBack() {
        RRawVector result = RDataFactory.createRawVectorFromNative(address, vector.getLength());
        result.copyAttributesFrom(vector);
        return result;
    }
}

/**
 * Support for the {.C} and {.Fortran} calls. Arguments of these calls are only arrays of primitive
 * types, in the case character vectors, only the first string. The vectors coming from the R side
 * are duplicated (if not temporary) with all their attributes and then the pointer to the data of
 * the new fresh vectors is passed to the function. The result is a list of all those new vectors
 * (or the original vectors if they are temporary).
 *
 * Note: seems that symbols in GnuR may declare: expected types of their args (and other types
 * should be coerced), whether an argument is only input (RNull is in its place in the result list)
 * and whether the argument value must always be copied. We do not implement those as they do not
 * seem necessary?
 */
public interface CRFFI {

    abstract class InvokeCNode extends RBaseNode {

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

            RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
            long before = stateRFFI.beforeDowncall();
            try {
                execute(nativeCallInfo, array);

                // we have to assume that the native method updated everything
                Object[] results = new Object[array.length];
                for (int i = 0; i < array.length; i++) {
                    results[i] = ((TemporaryWrapper) array[i]).cleanup();
                }
                return RDataFactory.createList(results, validateArgNames(array.length, args.getSignature()));
            } finally {
                stateRFFI.afterDowncall(before);
            }
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
