/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
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
    @Child protected Node getSize = Message.GET_SIZE.createNode();
    @Child private Foreign2R foreign2R;
    @Child private ForeignArray2R foreignArray2R;
    @Child private Node read = Message.READ.createNode();
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
    public Object convert(TruffleObject obj) {
        return convert(obj, true);
    }

    /**
     * Convert the provided foreign array to a vector.
     *
     * @param truffleObject foreign array
     * @param recursive determines whether a provided multi dimensional array should be resolved
     *            recursively or not.
     * @return a vector if obj is a foreign array, otherwise obj
     *
     */
    public Object convert(TruffleObject truffleObject, boolean recursive) {
        if (FastROptions.ForeignObjectWrappers.getBooleanValue() && isForeignArray(truffleObject)) {
            try {
                int size = (int) ForeignAccess.sendGetSize(getSize, truffleObject);
                if (size == 0) {
                    // TODO not yet ready to use RForeignListWrapper
                    // as an alternative to RList
                    // return new RForeignListWrapper(truffleObject);
                    return RDataFactory.createList();
                } else {
                    Object result = execute(truffleObject, recursive, null, 0, true);
                    if (result instanceof ForeignArrayData) {
                        ForeignArrayData arrayData = (ForeignArrayData) result;
                        if (arrayData.isOneDim()) { // can't deal with multidims
                            InteropTypeCheck.RType type = arrayData.typeCheck.getType();
                            switch (type) {
                                case NONE:
                                case NULL:
                                    // TODO not yet ready to use RForeignListWrapper
                                    // as an alternative to RList
                                    // return new RForeignListWrapper(truffleObject);
                                    return copy(truffleObject, recursive);
                                case BOOLEAN:
                                    return new RForeignBooleanWrapper(truffleObject);
                                case DOUBLE:
                                    return new RForeignDoubleWrapper(truffleObject);
                                case INTEGER:
                                    return new RForeignIntWrapper(truffleObject);
                                case STRING:
                                    return new RForeignStringWrapper(truffleObject);
                                default:
                                    assert false : "did not handle properly: " + type;
                            }
                        }
                    }
                }
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        return copy(truffleObject, recursive);
    }

    /**
     * Creates a complete copy of all foreign array elements into a R vector/list.
     * 
     * @param obj
     * @param recursive
     * @return a vector or list
     */
    public Object copy(TruffleObject obj, boolean recursive) {
        Object result = execute(obj, recursive, null, 0, false);
        if (result instanceof ForeignArrayData) {
            ForeignArrayData arrayData = (ForeignArrayData) result;
            if (arrayData.elements.isEmpty()) {
                return RDataFactory.createList();
            }
            return asAbstractVector(arrayData);
        }
        return result;
    }

    protected abstract Object execute(Object obj, boolean recursive, ForeignArrayData arrayData, int depth, boolean onlyInspect);

    @Specialization(guards = {"isForeignArray(obj)"})
    @TruffleBoundary
    protected ForeignArrayData doArray(TruffleObject obj, boolean recursive, ForeignArrayData arrayData, int depth, boolean onlyInspect) {
        try {
            return collectArrayElements(arrayData == null ? new ForeignArrayData() : arrayData, obj, recursive, depth, onlyInspect);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw error(RError.Message.GENERIC, "error while converting array: " + e.getMessage());
        }
    }

    @Specialization(guards = "isJavaIterable(obj)")
    @TruffleBoundary
    protected ForeignArrayData doJavaIterable(TruffleObject obj, @SuppressWarnings("unused") boolean recursive, ForeignArrayData arrayData, @SuppressWarnings("unused") int depth, boolean onlyInspect,
                    @Cached("EXECUTE.createNode()") Node execute) {

        try {
            return getIterableElements(arrayData == null ? new ForeignArrayData() : arrayData, obj, execute, onlyInspect);
        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException e) {
            throw error(RError.Message.GENERIC, "error while casting polyglot value to list: " + e.getMessage());
        }
    }

    @Fallback
    protected Object doObject(Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") ForeignArrayData arrayData, @SuppressWarnings("unused") int depth,
                    @SuppressWarnings("unused") boolean onlyInspect) {
        return obj;
    }

    private ForeignArrayData collectArrayElements(ForeignArrayData arrayData, TruffleObject obj, boolean recursive, int depth, boolean onlyInspect)
                    throws UnsupportedMessageException, UnknownIdentifierException {
        int size = (int) ForeignAccess.sendGetSize(getSize, obj);

        arrayData.addDimension(depth, size);

        if (size == 0) {
            return arrayData;
        }
        for (int i = 0; i < size; i++) {
            Object element = ForeignAccess.sendRead(read, obj, i);
            if (recursive && (isForeignArray(element, hasSize) || isJavaIterable(element))) {
                recurse(arrayData, element, depth, onlyInspect);
            } else {
                arrayData.process(element, this::getIsNull, this::getIsBoxed, this::getUnbox, this::getForeign2R, onlyInspect);
            }
        }
        return arrayData;
    }

    private ForeignArrayData getIterableElements(ForeignArrayData arrayData, TruffleObject obj, Node execute, boolean onlyInspect)
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
            arrayData.process(element, this::getIsNull, this::getIsBoxed, this::getUnbox, this::getForeign2R, onlyInspect);
        }
        return arrayData;
    }

    private void recurse(ForeignArrayData arrayData, Object element, int depth, boolean onlyInspect) {
        if (foreignArray2R == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreignArray2R = insert(create());
        }
        foreignArray2R.execute(element, true, arrayData, depth + 1, onlyInspect);
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

        int[] dims = arrayData.isMultiDim() ? arrayData.dims.stream().mapToInt((i) -> i.intValue()).toArray() : null;

        switch (type) {
            case NONE:
            case NULL:
                return RDataFactory.createList(arrayData.elements.toArray(new Object[arrayData.elements.size()]));
            case BOOLEAN:
                WriteArray<byte[]> wba = (byte[] array, int resultIdx, int sourceIdx, boolean[] complete) -> {
                    Object value = arrayData.elements.get(sourceIdx);
                    array[resultIdx] = value == RNull.instance ? RRuntime.LOGICAL_NA : ((Number) value).byteValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                byte[] byteArray = new byte[size];
                if (dims != null) {
                    return createVector(dims, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete, dims));
                } else {
                    return createFlatVector(size, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete));
                }
            case DOUBLE:
                WriteArray<double[]> wda = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = arrayData.elements.get(sourceIdx);
                    array[resultIdx] = value == RNull.instance ? RRuntime.DOUBLE_NA : ((Number) value).doubleValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                double[] doubleArray = new double[size];
                if (dims != null) {
                    return createVector(dims, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete, dims));
                } else {
                    return createFlatVector(size, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete));
                }
            case INTEGER:
                WriteArray<int[]> wia = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = arrayData.elements.get(sourceIdx);
                    array[resultIdx] = value == RNull.instance ? RRuntime.INT_NA : ((Number) value).intValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                int[] intArray = new int[size];
                if (dims != null) {
                    return createVector(dims, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete, dims));
                } else {
                    return createFlatVector(size, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete));
                }
            case STRING:
                WriteArray<String[]> wsa = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = arrayData.elements.get(sourceIdx);
                    array[resultIdx] = value == RNull.instance ? RRuntime.STRING_NA : String.valueOf(value);
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
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

    public boolean isForeignArray(Object obj) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    public boolean isForeignVector(Object obj) {
        return isJavaIterable(obj) || isForeignArray(obj, hasSize);
    }

    public static boolean isForeignArray(Object obj, Node hasSize) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSize, (TruffleObject) obj);
    }

    public static boolean isJavaIterable(Object obj) {
        if (RRuntime.isForeignObject(obj)) {
            TruffleLanguage.Env env = RContext.getInstance().getEnv();
            return env.isHostObject(obj) && env.asHostObject(obj) instanceof Iterable;
        }
        return false;
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
            NULL,
            NONE;
        }

        private RType type = null;

        public static RType determineType(Object value) {
            if (value instanceof Boolean) {
                return RType.BOOLEAN;
            } else if (value instanceof Byte || value instanceof Integer || value instanceof Short) {
                return RType.INTEGER;
            } else if (value instanceof Double || value instanceof Float || value instanceof Long) {
                return RType.DOUBLE;
            } else if (value instanceof Character || value instanceof String) {
                return RType.STRING;
            } else {
                return RType.NONE;
            }
        }

        public RType checkForeign(Object value) {
            if (value instanceof Boolean) {
                setType(RType.BOOLEAN);
            } else if (value instanceof Byte || value instanceof Integer || value instanceof Short) {
                setType(RType.INTEGER);
            } else if (value instanceof Double || value instanceof Float || value instanceof Long) {
                setType(RType.DOUBLE);
            } else if (value instanceof Character || value instanceof String) {
                setType(RType.STRING);
            } else if (value == RNull.instance) {
                setType(RType.NULL);
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
                } else if (this.type != check && check != RType.NULL) {
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

        public void process(Object value, Supplier<Node> getIsNull, Supplier<Node> getIsBoxed, Supplier<Node> getUnbox, Supplier<Foreign2R> getForeign2R, boolean onlyInspect)
                        throws UnsupportedMessageException {
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
            InteropTypeCheck.RType type = typeCheck.checkForeign(unboxedValue);
            if (onlyInspect && type == InteropTypeCheck.RType.NONE) {
                return;
            }
            if (!onlyInspect) {
                unboxedValue = getForeign2R.get().execute(unboxedValue);
                elements.add(unboxedValue);
            }
        }

        private boolean isMultiDim() {
            return dims != null && dims.size() > 1;
        }

        private boolean isOneDim() {
            return dims != null && dims.size() == 1;
        }

        private void addDimension(int depth, int size) {
            if (dims != null) {
                if (dims.size() == depth) {
                    dims.add(size);
                } else if (depth < dims.size()) {
                    if (dims.get(depth) != size) {
                        // had previously on the same depth an array with different length
                        // -> not rectangular, skip the dimensions
                        dims = null;
                    }
                }
            }
        }

    }
}
