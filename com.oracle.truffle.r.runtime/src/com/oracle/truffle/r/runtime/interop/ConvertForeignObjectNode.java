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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.InspectForeignArrayNode.ArrayInfo;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * <p>
 * Converts foreign objects to a vector or a list.
 * <ul>
 * <li><b>Homogenous arrays</b> are converted implicitly to a corresponding atomic vector or to a
 * list if explicitely requested.<br>
 * <li><b>Heterogenous arrays</b> are always converted to a list.<br>
 * <li><b>Non-array objects</b> having keys are converted into a named list <b>only</b> if
 * explicitely requested.<br>
 * </ul>
 * </p>
 * 
 * <p>
 * <b>Multi dimensional arrays</b> are, depending on the <b>recursive</b> parameter, either
 * converted recursively or only the first dimension array is read and a list with the particular
 * foreign objects is returned.
 * </p>
 * 
 * <p>
 * Rectangular <b>array dimensions</b> (or a n-dim equivalent) are, depending on the
 * <b>dropDimensions</b> parameter, either honored by the conversion or a flat vector/list is
 * returned.
 * </p>
 */
@ImportStatic({Message.class, RRuntime.class, RType.class})
public abstract class ConvertForeignObjectNode extends RBaseNode {

    @Child protected Node hasSizeNode = Message.HAS_SIZE.createNode();
    @Child protected Node readNode;
    @Child protected Foreign2R foreign2RNode;
    @Child protected Node keyInfoNode;
    @Child private ConvertForeignObjectNode recurseNode;

    public static ConvertForeignObjectNode create() {
        return ConvertForeignObjectNodeGen.create();
    }

    protected abstract Object execute(Object obj, boolean recursive, boolean dropDimensions, RType type);

    /**
     * Converts the provided foreign array to a vector or list.
     * <p>
     * The returned vector type will be implicitly determined by the values from the foreign array.
     * </p>
     * <p>
     * All dimensions will be taken in count and resolved recursively.
     * </p>
     *
     * @param truffleObject foreign array
     * @return a vector or list if <code>truffleObject</code> is a foreign array otherwise
     *         <code>truffleObject</code>
     */
    public Object convert(TruffleObject truffleObject) {
        return convert(truffleObject, true);
    }

    /**
     * Converts the provided foreign array to a vector or list.
     * <p>
     * The returned vector type will be implicitly determined by the values from the foreign array.
     * </p>
     *
     * @param truffleObject foreign array
     * @param recursive if <code>true</code> then the dimensions in a multi dimensional array will
     *            be taken in count and resolved recursively, otherwise only the first array
     *            dimension will be read and returned in a list.
     * @return a vector or list if <code>truffleObject</code> is a foreign array, otherwise
     *         <code>truffleObject</code>
     *
     */
    public Object convert(TruffleObject truffleObject, boolean recursive) {
        return convert(truffleObject, recursive, false);
    }

    /**
     * Converts the provided foreign array to a vector or list.
     * <p>
     * The returned vector type will be implicitly determined by the values from the foreign array.
     * </p>
     *
     * @param truffleObject foreign array
     * @param recursive if <code>true</code> then the dimensions in a multi dimensional array will
     *            be taken in count and resolved recursively, otherwise only the first array
     *            dimension will be read and returned in a list.
     * @param dropDimensions if <code>true</code> a flat vector or list without dimensions will be
     *            returned. <b>Note</b> that the positioning of the particular values in the result
     *            vector will be done by columns, as this is the default e.g. when creating a R
     *            matrix.
     * @return a vector if <code>truffleObject</code> is a foreign array, otherwise
     *         <code>truffleObject</code>
     *
     */
    public Object convert(TruffleObject truffleObject, boolean recursive, boolean dropDimensions) {
        return execute(truffleObject, recursive, dropDimensions, RType.Any);
    }

    /**
     * Converts the provided foreign object to a list.
     * 
     * <p>
     * If the provided object isn't an array, then a named list will be created from it's keys and
     * their values.
     * </p>
     * 
     * @param truffleObject foreign object
     * @param recursive if <code>true</code> then the dimensions in a multi dimensional array will
     *            be taken in count and resolved recursively, otherwise only the first array
     *            dimension will be read and returned in a list.
     * @param dropDimensions if <code>true</code> a flat list without dimensions will be returned.
     *            <b>Note</b> that the positioning of the particular values in the result will be
     *            done by columns, as this is the default e.g. when creating a R matrix.
     * @return a list if obj is a foreign array, otherwise obj
     */
    public Object convertToList(TruffleObject truffleObject, boolean recursive, boolean dropDimensions) {
        return execute(truffleObject, recursive, dropDimensions, RType.List);
    }

    /**
     * Determines whether the provided object is a foreign array or not.
     * 
     * @param obj
     * @return <code>true</code> if the provided object is an array, otherwise <code>false</code>
     */
    public boolean isForeignArray(Object obj) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSizeNode, (TruffleObject) obj);
    }

    /**
     * Determines whether the provided object is a foreign array or not.
     * 
     * @param obj
     * @param hasSizeNode
     * @return <code>true</code> if the provided object is an array, otherwise <code>false</code>
     */
    public static boolean isForeignArray(Object obj, Node hasSizeNode) {
        return RRuntime.isForeignObject(obj) && ForeignAccess.sendHasSize(hasSizeNode, (TruffleObject) obj);
    }

    /**
     * Creates a vector. It has to be assured by the caller that the elements and type corresponds
     * to each other. Null values are replaced by NA.
     *
     * @param elements vector elements
     * @param type the resulting vector type. Allowed values are
     *            <ul>
     *            <li>Logical</li>
     *            <li>Integer</li>
     *            <li>Double</li>
     *            <li>Character</li>
     *            <li>List</li>
     *            </ul>
     * @return a vector
     */
    @TruffleBoundary
    public static RAbstractVector asAbstractVector(Object[] elements, RType type) {
        return asAbstractVector(elements, null, type, true);
    }

    static RAbstractVector asAbstractVector(Object[] elements, int[] dims, RType type, boolean dropDimensions) {
        int size = elements.length;
        switch (type) {
            case Logical:
                WriteArray<byte[]> wba = (byte[] array, int resultIdx, int sourceIdx, boolean[] complete) -> {
                    Object value = elements[sourceIdx];
                    array[resultIdx] = value == RNull.instance ? RRuntime.LOGICAL_NA : ((Number) value).byteValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                byte[] byteArray = new byte[size];
                if (dims != null) {
                    return createVector(dims, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete, dropDimensions ? null : dims));
                } else {
                    return createFlatVector(size, byteArray, wba, (complete) -> RDataFactory.createLogicalVector(byteArray, complete));
                }
            case Double:
                WriteArray<double[]> wda = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = elements[sourceIdx];
                    array[resultIdx] = value == RNull.instance ? RRuntime.DOUBLE_NA : ((Number) value).doubleValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                double[] doubleArray = new double[size];
                if (dims != null) {
                    return createVector(dims, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete, dropDimensions ? null : dims));
                } else {
                    return createFlatVector(size, doubleArray, wda, (complete) -> RDataFactory.createDoubleVector(doubleArray, complete));
                }
            case Integer:
                WriteArray<int[]> wia = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = elements[sourceIdx];
                    array[resultIdx] = value == RNull.instance ? RRuntime.INT_NA : ((Number) value).intValue();
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                int[] intArray = new int[size];
                if (dims != null) {
                    return createVector(dims, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete, dropDimensions ? null : dims));
                } else {
                    return createFlatVector(size, intArray, wia, (complete) -> RDataFactory.createIntVector(intArray, complete));
                }
            case Character:
                WriteArray<String[]> wsa = (array, resultIdx, sourceIdx, complete) -> {
                    Object value = elements[sourceIdx];
                    array[resultIdx] = value == RNull.instance ? RRuntime.STRING_NA : String.valueOf(value);
                    complete[0] &= !RRuntime.isNA(array[resultIdx]);
                };
                String[] stringArray = new String[size];
                if (dims != null) {
                    return createVector(dims, stringArray, wsa, (complete) -> RDataFactory.createStringVector(stringArray, complete, dropDimensions ? null : dims));
                } else {
                    return createFlatVector(size, stringArray, wsa, (complete) -> RDataFactory.createStringVector(stringArray, complete));
                }
            case List:
            case Null:
                // return createList(elements, null, dropDimensions ? null : dims, size);
                if (dims != null) {
                    WriteArray<Object[]> wa = (array, resultIdx, sourceIdx, complete) -> {
                        array[resultIdx] = elements[sourceIdx];
                    };
                    Object[] array = new Object[size];
                    return createVector(dims, array, wa, (complete) -> RDataFactory.createList(array, dropDimensions ? null : dims));
                } else {
                    return RDataFactory.createList(elements);
                }
            default:
                throw RInternalError.shouldNotReachHere();
        }
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

    @TruffleBoundary
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

    @Specialization(guards = {"isForeignArray(truffleObject)", "type != List"})
    protected Object convertArray(TruffleObject truffleObject, boolean recursive, boolean dropDimensions, @SuppressWarnings("unused") RType type,
                    @Cached("create()") InspectForeignArrayNode inspectTruffleObject,
                    @Cached("create()") CopyForeignArrayNode copyArray) {
        ArrayInfo arrayInfo = new ArrayInfo();
        inspectTruffleObject.execute(truffleObject, recursive, arrayInfo, 0);

        RType inspectedType = arrayInfo.getType();
        switch (inspectedType) {
            case Logical:
                if (arrayInfo.isOneDim()) {
                    return new RForeignBooleanWrapper(truffleObject);
                } else {
                    return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), dropDimensions);
                }
            case Double:
                if (arrayInfo.isOneDim()) {
                    return new RForeignDoubleWrapper(truffleObject);
                } else {
                    return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), dropDimensions);
                }
            case Integer:
                if (arrayInfo.isOneDim()) {
                    return new RForeignIntWrapper(truffleObject);
                } else {
                    return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), dropDimensions);
                }
            case Character:
                if (arrayInfo.isOneDim()) {
                    return new RForeignStringWrapper(truffleObject);
                } else {
                    return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), dropDimensions);
                }
            case List:
            case Null:
                return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), false);
            default:
                throw RInternalError.shouldNotReachHere("did not handle properly: " + inspectedType);
        }
    }

    @Specialization(guards = {"isForeignArray(truffleObject)", "type == List"})
    protected Object convertArrayToList(TruffleObject truffleObject, boolean recursive, @SuppressWarnings("unused") boolean dropDimensions, @SuppressWarnings("unused") RType type,
                    @Cached("create()") InspectForeignArrayNode inspectTruffleObject,
                    @Cached("create()") CopyForeignArrayNode copyArray) {
        ArrayInfo arrayInfo = new ArrayInfo();
        inspectTruffleObject.execute(truffleObject, recursive, arrayInfo, 0);

        RType inspectedType = arrayInfo.getType();
        if (inspectedType != RType.List) {
            // as if in as.list(atomicVector/Matrix) - e.g. as.list(matrix(1:4, c(2,2)))
            // => results in a flat list
            return copyArray.toVector(truffleObject, recursive, type, arrayInfo.getDims(), true);
        } else {
            // as if in as.list(heterogenous Matrix)
            // - e.g. l<-list(1,'a',2,3);dim(l)<-c(2,2);as.list(matrix(l,c(2,2)))
            // => keeps the lists dimensions
            return copyArray.toVector(truffleObject, recursive, arrayInfo.getType(), arrayInfo.getDims(), false);
        }
    }

    @Specialization(guards = {"isForeignObject(truffleObject)", "!isForeignArray(truffleObject)", "type == List"})
    @TruffleBoundary
    protected Object convertObjectToList(TruffleObject truffleObject, boolean recursive, boolean dropDimensions, @SuppressWarnings("unused") RType type,
                    @Cached("create()") GetForeignKeysNode namesNode) {
        Object namesObj = namesNode.execute(truffleObject, false);
        if (namesObj == RNull.instance) {
            return RDataFactory.createList();
        }
        RStringVector names = (RStringVector) namesObj;
        List<Object> elements = new ArrayList<>();
        List<Object> elementNames = new ArrayList<>();
        for (int i = 0; i < names.getLength(); i++) {
            String name = names.getDataAt(i);
            try {
                int keyInfo = ForeignAccess.sendKeyInfo(getKeyInfoNode(), truffleObject, name);
                if (KeyInfo.isReadable(keyInfo) && !KeyInfo.isInvocable(keyInfo)) {
                    Object o = ForeignAccess.sendRead(getReadNode(), truffleObject, name);
                    o = getForeign2RNode().execute(o);
                    if (isForeignArray(o, hasSizeNode)) {
                        o = getRecurseNode().execute(o, recursive, dropDimensions, RType.Any);
                    }
                    elements.add(o);
                    elementNames.add(name);
                }
            } catch (UnknownIdentifierException | UnsupportedMessageException ex) {
                throw error(RError.Message.GENERIC, "error while converting truffle object to list: " + ex.getMessage());
            }
        }
        return RDataFactory.createList(elements.toArray(new Object[elements.size()]), RDataFactory.createStringVector(elementNames.toArray(new String[elementNames.size()]), true));
    }

    @Specialization(guards = {"doNotConvert(obj, type)"})
    protected Object doObject(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean recursive, @SuppressWarnings("unused") boolean dropDimensions,
                    @SuppressWarnings("unused") RType type) {
        return obj;
    }

    protected boolean doNotConvert(Object obj, RType type) {
        return !RRuntime.isForeignObject(obj) || (!isForeignArray(obj) && type != RType.List);
    }

    private ConvertForeignObjectNode getRecurseNode() {
        if (recurseNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recurseNode = insert(create());
        }
        return recurseNode;
    }

    private Node getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readNode = insert(Message.READ.createNode());
        }
        return readNode;
    }

    private Node getKeyInfoNode() {
        if (keyInfoNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            keyInfoNode = insert(Message.KEY_INFO.createNode());
        }
        return keyInfoNode;
    }

    private Foreign2R getForeign2RNode() {
        if (foreign2RNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreign2RNode = insert(Foreign2RNodeGen.create());
        }
        return foreign2RNode;
    }

}
