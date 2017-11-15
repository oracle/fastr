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
package com.oracle.truffle.r.runtime.interop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({Message.class, RRuntime.class})
public abstract class ForeignArray2R extends RBaseNode {

    @Child protected Node hasSize = Message.HAS_SIZE.createNode();
    @Child private Foreign2R foreign2R;
    @Child private ForeignArray2R foreignArray2R;
    @Child private Node read;
    @Child private Node isNull;
    @Child private Node isBoxed;
    @Child private Node unbox;

    public static ForeignArray2R create() {
        return ForeignArray2RNodeGen.create();
    }

    /**
     * Convert the provided foreign array to a vector. Multi dimensional arrays will be resolved
     * recursively.
     *
     * @param obj foreign array
     * @return a vector if obj is a foreign array, otherwise obj
     */
    public Object convert(Object obj) {
        return convert(obj, true);
    }

    /**
     * Convert the provided foreign array to a vector.
     *
     * @param obj foreign array
     * @param recursive determines whether a provided multi dimensional array should be resolved
     *            recursively or not.
     * @return a vector if obj is a foreign array, otherwise obj
     *
     */
    public Object convert(Object obj, boolean recursive) {
        Object result = execute(obj, recursive, null, 0);
        if (result instanceof ForeignArrayData) {
            ForeignArrayData arrayData = (ForeignArrayData) result;
            if (arrayData.elements.isEmpty()) {
                return RDataFactory.createList();
            }
            return asAbstractVector(arrayData);
        }
        return result;
    }

    protected abstract Object execute(Object obj, boolean recursive, ForeignArrayData arrayData, int depth);

    @Specialization(guards = {"isForeignArray(obj)"})
    @TruffleBoundary
    protected ForeignArrayData doArray(TruffleObject obj, boolean recursive, ForeignArrayData arrayData, int depth,
                    @Cached("GET_SIZE.createNode()") Node getSize) {
        try {
            return collectArrayElements(arrayData == null ? new ForeignArrayData() : arrayData, obj, recursive, getSize, depth);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Specialization(guards = "isJavaIterable(obj)")
    @TruffleBoundary
    protected ForeignArrayData doJavaIterable(TruffleObject obj, boolean recursive, ForeignArrayData arrayData, int depth,
                    @Cached("createExecute(0).createNode()") Node execute) {

        try {
            return getIterableElements(arrayData == null ? new ForeignArrayData() : arrayData, obj, recursive, execute, depth);
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException e) {
            throw error(RError.Message.GENERIC, "error while casting external object to list: " + e.getMessage());
        }
    }

    @Fallback
    protected Object doObject(Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") ForeignArrayData arrayData, @SuppressWarnings("unused") int depth) {
        return obj;
    }

    private ForeignArrayData collectArrayElements(ForeignArrayData arrayData, TruffleObject obj, boolean recursive, Node getSize, int depth)
                    throws UnsupportedMessageException, UnknownIdentifierException {
        int size = (int) ForeignAccess.sendGetSize(getSize, obj);

        if (arrayData.dims != null) {
            if (arrayData.dims.size() == depth) {
                arrayData.dims.add(size);
            } else if (depth < arrayData.dims.size()) {
                if (arrayData.dims.get(depth) != size) {
                    // had previously on the same depth an array with different length
                    // -> not rectangular, skip the dimensions
                    arrayData.dims = null;
                }
            }
        }

        if (size == 0) {
            return arrayData;
        }
        for (int i = 0; i < size; i++) {
            if (read == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                read = insert(Message.READ.createNode());
            }
            Object element = ForeignAccess.sendRead(read, obj, i);
            if (recursive && (isForeignArray(element, hasSize) || isJavaIterable(element))) {
                recurse(arrayData, element, depth);
            } else {
                arrayData.add(element, this::getIsNull, this::getIsBoxed, this::getUnbox, this::getForeign2R);
            }
        }
        return arrayData;
    }

    private ForeignArrayData getIterableElements(ForeignArrayData arrayData, TruffleObject obj, boolean recursive, Node execute, int depth)
                    throws UnknownIdentifierException, ArityException, UnsupportedMessageException, UnsupportedTypeException {
        if (read == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            read = insert(Message.READ.createNode());
        }
        TruffleObject itFunction = (TruffleObject) ForeignAccess.sendRead(read, obj, "iterator");
        TruffleObject it = (TruffleObject) ForeignAccess.sendExecute(execute, itFunction);
        TruffleObject hasNextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "hasNext");

        while ((boolean) ForeignAccess.sendExecute(execute, hasNextFunction)) {
            TruffleObject nextFunction = (TruffleObject) ForeignAccess.sendRead(read, it, "next");
            Object element = ForeignAccess.sendExecute(execute, nextFunction);
            if (recursive && (isJavaIterable(element) || isForeignArray(element, hasSize))) {
                recurse(arrayData, element, depth);
            } else {
                arrayData.add(element, this::getIsNull, this::getIsBoxed, this::getUnbox, this::getForeign2R);
            }
        }
        return arrayData;
    }

    private void recurse(ForeignArrayData arrayData, Object element, int depth) {
        if (foreignArray2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreignArray2R = insert(create());
        }
        foreignArray2R.execute(element, true, arrayData, depth + 1);
    }

    private Foreign2R getForeign2R() {
        if (foreign2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2R = insert(Foreign2RNodeGen.create());
        }
        return foreign2R;
    }

    private Node getUnbox() {
        if (unbox == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unbox = insert(Message.UNBOX.createNode());
        }
        return unbox;
    }

    private Node getIsBoxed() {
        if (isBoxed == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isBoxed = insert(Message.IS_BOXED.createNode());
        }
        return isBoxed;
    }

    private Node getIsNull() {
        if (isNull == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isNull = insert(Message.IS_NULL.createNode());
        }
        return isNull;
    }

    /**
     * Converts the elements collected from a foreign array or java iterable into a vector or list.
     *
     * @param arrayData foreign array data
     * @return a vector
     */
    @TruffleBoundary
    public static RAbstractVector asAbstractVector(final ForeignArrayData arrayData) {
        InteropTypeCheck.RType type = arrayData.typeCheck.getType();
        int size = arrayData.elements.size();

        int[] dims = arrayData.dims != null && arrayData.dims.size() > 1 ? arrayData.dims.stream().mapToInt((i) -> i.intValue()).toArray() : null;
        assert dims == null || sizeByDims(dims) == size : sizeByDims(dims) + " " + size;

        switch (type) {
            case NONE:
                return RDataFactory.createList(arrayData.elements.toArray(new Object[arrayData.elements.size()]));
            case BOOLEAN:
                WriteArray<byte[]> wba = (byte[] array, int resultIdx, int sourceIdx, boolean[] complete) -> {
                    array[resultIdx] = ((Number) arrayData.elements.get(sourceIdx)).byteValue();
                    complete[0] &= RRuntime.isNA(array[resultIdx]);
                };
                byte[] byteArray = new byte[size];
                if (dims != null) {
                    return createVector(dims, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete, dims));
                } else {
                    return createFlatVector(size, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete));
                }
            case DOUBLE:
                WriteArray<double[]> wda = (array, resultIdx, sourceIdx, complete) -> {
                    array[resultIdx] = ((Number) arrayData.elements.get(sourceIdx)).doubleValue();
                    complete[0] &= RRuntime.isNA(array[resultIdx]);
                };
                double[] doubleArray = new double[size];
                if (dims != null) {
                    return createVector(dims, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete, dims));
                } else {
                    return createFlatVector(size, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete));
                }
            case INTEGER:
                WriteArray<int[]> wia = (array, resultIdx, sourceIdx, complete) -> {
                    array[resultIdx] = ((Number) arrayData.elements.get(sourceIdx)).intValue();
                    complete[0] &= RRuntime.isNA(array[resultIdx]);
                };
                int[] intArray = new int[size];
                if (dims != null) {
                    return createVector(dims, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete, dims));
                } else {
                    return createFlatVector(size, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete));
                }
            case STRING:
                WriteArray<String[]> wsa = (array, resultIdx, sourceIdx, complete) -> {
                    array[resultIdx] = String.valueOf(arrayData.elements.get(sourceIdx));
                    complete[0] &= RRuntime.isNA(array[resultIdx]);
                };
                String[] stringArray = new String[size];
                if (dims != null) {
                    return createVector(dims, stringArray, wsa, (complete) -> RDataFactory.createStringVector(stringArray, complete, dims));
                } else {
                    return createFlatVector(size, stringArray, wsa, (complete) -> RDataFactory.createStringVector(stringArray, complete));
                }
            default:
                assert false : "did not handle properly: " + type;
        }

        return RDataFactory.createList(arrayData.elements.toArray(new Object[arrayData.elements.size()]));
    }

    private static int sizeByDims(int[] dims) {
        return Arrays.stream(dims).reduce(1, (x, y) -> x * y);
    }

    @FunctionalInterface
    private interface WriteArray<A> {
        void apply(A array, int resultIdx, int sourceIdx, boolean[] complete);
    }

    private static <A> RAbstractVector createFlatVector(int length, A resultArray, WriteArray<A> writeResultArray, Function<Boolean, RVector<?>> createResult) {
        boolean[] complete = new boolean[]{true};
        for (int i = 0; i < length; i++) {
            writeResultArray.apply(resultArray, i, i, complete);
        }
        return createResult.apply(complete[0]);
    }

    private static <A> RAbstractVector createVector(int[] dims, A resultArray, WriteArray<A> writeResultArray, Function<Boolean, RVector<?>> createResult) {
        boolean[] complete = new boolean[]{true};
        assert dims.length > 1;
        populateResultArray(dims, new int[dims.length], 0, new int[]{0}, resultArray, writeResultArray, complete);
        return createResult.apply(complete[0]);
    }

    private static <A> int populateResultArray(int[] dims, int[] currentCoordinates, int depth, int[] sourceIdx, A resultArray, WriteArray<A> writeResultArray, boolean[] complete) {
        int[] cor = new int[currentCoordinates.length];
        System.arraycopy(currentCoordinates, 0, cor, 0, currentCoordinates.length);
        for (int i = 0; i < dims[depth]; i++) {
            cor[depth] = i;
            if (depth < dims.length - 1) {
                populateResultArray(dims, cor, depth + 1, sourceIdx, resultArray, writeResultArray, complete);
            } else {
                int resultIdx = getResultIdx(cor, dims);
                writeResultArray.apply(resultArray, resultIdx, sourceIdx[0], complete);
                sourceIdx[0]++;
            }
        }
        return -1;
    }

    /**
     * Computes index in vector given by the element coordinates. <br>
     * cor[0] + cor[1] * dims[0] + ... + cor[n] * dim[0] * ... * dim[n-1]
     *
     * @param cor coordinates to compute the index from
     * @param dims vector dimensions
     * @return the index
     */
    private static int getResultIdx(int[] cor, int[] dims) {
        int idx = 0;
        for (int c = 0; c < cor.length; c++) {
            int dp = 1;
            for (int d = 0; d < c; d++) {
                dp *= dims[d];
            }
            idx += cor[c] * dp;
        }
        return idx;
    }

    protected boolean isForeignArray(Object obj) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    public boolean isForeignVector(Object obj) {
        return isJavaIterable(obj) || isForeignArray(obj, hasSize);
    }

    public static boolean isForeignArray(Object obj, Node hasSize) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    public static boolean isJavaIterable(Object obj) {
        return RRuntime.isForeignObject(obj) && JavaInterop.isJavaObject(Iterable.class, (TruffleObject) obj);
    }

    public static boolean isForeignVector(Object obj, Node hasSize) {
        return isJavaIterable(obj) || isForeignArray(obj, hasSize);
    }

    public static class InteropTypeCheck {
        public enum RType {
            BOOLEAN,
            DOUBLE,
            INTEGER,
            STRING,
            NONE;
        }

        private RType type = null;

        public RType checkForeign(Object value) {
            if (value instanceof Boolean) {
                setType(RType.BOOLEAN);
            } else if (value instanceof Byte || value instanceof Integer || value instanceof Short) {
                setType(RType.INTEGER);
            } else if (value instanceof Double || value instanceof Float || value instanceof Long) {
                setType(RType.DOUBLE);
            } else if (value instanceof Character || value instanceof String) {
                setType(RType.STRING);
            } else {
                this.type = RType.NONE;
            }
            return this.type;
        }

        public RType checkVector(RAbstractVector value) {
            if (value instanceof RAbstractLogicalVector) {
                setType(RType.BOOLEAN);
            } else if (value instanceof RAbstractIntVector) {
                setType(RType.INTEGER);
            } else if (value instanceof RAbstractDoubleVector) {
                setType(RType.DOUBLE);
            } else if (value instanceof RAbstractStringVector) {
                setType(RType.STRING);
            } else {
                this.type = RType.NONE;
            }
            return this.type;
        }

        private void setType(RType check) {
            if (this.type != RType.NONE) {
                if (this.type == null) {
                    this.type = check;
                } else if (this.type == RType.INTEGER && check == RType.DOUBLE || check == RType.INTEGER && this.type == RType.DOUBLE) {
                    this.type = RType.DOUBLE;
                } else if (this.type != check) {
                    this.type = RType.NONE;
                }
            }
        }

        public RType getType() {
            return type != null ? type : RType.NONE;
        }
    }

    public static class ForeignArrayData {
        private InteropTypeCheck typeCheck = new InteropTypeCheck();
        private List<Object> elements = new ArrayList<>();
        private List<Integer> dims = new ArrayList<>();

        public void add(Object value, Supplier<Node> getIsNull, Supplier<Node> getIsBoxed, Supplier<Node> getUnbox, Supplier<Foreign2R> getForeign2R) throws UnsupportedMessageException {
            typeCheck.checkForeign(value);

            Object unboxedValue = value;
            if (unboxedValue instanceof TruffleObject) {
                if (ForeignAccess.sendIsNull(getIsNull.get(), (TruffleObject) unboxedValue)) {
                    unboxedValue = RNull.instance;
                } else {
                    if (ForeignAccess.sendIsBoxed(getIsBoxed.get(), (TruffleObject) unboxedValue)) {
                        unboxedValue = ForeignAccess.sendUnbox(getUnbox.get(), (TruffleObject) unboxedValue);
                    }
                }
            }
            typeCheck.checkForeign(unboxedValue);
            unboxedValue = getForeign2R.get().execute(unboxedValue);

            elements.add(unboxedValue);
        }
    }
}
