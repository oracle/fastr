/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;

public final class RDataFactory {

    private static final RIntVector EMPTY_INT_VECTOR = createIntVector(0);
    private static final RDoubleVector EMPTY_DOUBLE_VECTOR = createDoubleVector(0);
    private static final RLogicalVector EMPTY_LOGICAL_VECTOR = createLogicalVector(0);
    private static final RStringVector EMPTY_STRING_VECTOR = createStringVector(0);
    private static final RComplexVector EMPTY_COMPLEX_VECTOR = createComplexVector(0);
    private static final RRawVector EMPTY_RAW_VECTOR = createRawVector(0);
    public static final boolean INCOMPLETE_VECTOR = false;
    public static final boolean COMPLETE_VECTOR = true;

    public static RIntVector createIntVector(int length) {
        return createIntVector(new int[length], false);
    }

    public static RIntVector createIntVector(int[] data, boolean complete) {
        return createIntVector(data, complete, null, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims) {
        return createIntVector(data, complete, dims, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, Object names) {
        return createIntVector(data, complete, null, names);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims, Object names) {
        return traceDataCreated(new RIntVector(data, complete, dims, names));
    }

    public static RDoubleVector createDoubleVector(int length) {
        return createDoubleVector(new double[length], false);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete) {
        return createDoubleVector(data, complete, null, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims) {
        return createDoubleVector(data, complete, dims, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, Object names) {
        return createDoubleVector(data, complete, null, names);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims, Object names) {
        return traceDataCreated(new RDoubleVector(data, complete, dims, names));
    }

    public static RRawVector createRawVector(int length) {
        return createRawVector(new byte[length]);
    }

    public static RRawVector createRawVector(byte[] data) {
        return createRawVector(data, null, null);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims) {
        return createRawVector(data, dims, null);
    }

    public static RRawVector createRawVector(byte[] data, Object names) {
        return createRawVector(data, null, names);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims, Object names) {
        return traceDataCreated(new RRawVector(data, dims, names));
    }

    public static RComplexVector createComplexVector(int length) {
        return createComplexVector(new double[length << 1], false, null, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete) {
        return createComplexVector(data, complete, null, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims) {
        return createComplexVector(data, complete, dims, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, Object names) {
        return createComplexVector(data, complete, null, names);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims, Object names) {
        return traceDataCreated(new RComplexVector(data, complete, dims, names));
    }

    public static RStringVector createStringVector(String value) {
        return createStringVector(new String[]{value}, true, null, null);
    }

    public static RStringVector createStringVector(int length) {
        return createStringVector(createAndfillStringVector(length, ""), false, null, null);
    }

    private static String[] createAndfillStringVector(int length, String string) {
        String[] strings = new String[length];
        for (int i = 0; i < length; i++) {
            strings[i] = string;
        }
        return strings;
    }

    public static RStringVector createStringVector(String[] data, boolean complete) {
        return createStringVector(data, complete, null, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims) {
        return createStringVector(data, complete, dims, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, Object names) {
        return createStringVector(data, complete, null, names);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims, Object names) {
        return traceDataCreated(new RStringVector(data, complete, dims, names));
    }

    public static RLogicalVector createLogicalVector(int length) {
        return createLogicalVector(new byte[length], false, null, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete) {
        return createLogicalVector(data, complete, null, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims) {
        return createLogicalVector(data, complete, dims, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, Object names) {
        return createLogicalVector(data, complete, null, names);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims, Object names) {
        return traceDataCreated(new RLogicalVector(data, complete, dims, names));
    }

    public static RIntSequence createAscendingRange(int start, int end) {
        assert start <= end;
        return traceDataCreated(new RIntSequence(start, 1, end - start + 1));
    }

    public static RIntSequence createDescendingRange(int start, int end) {
        assert start > end;
        return traceDataCreated(new RIntSequence(start, -1, start - end + 1));
    }

    public static RIntSequence createIntSequence(int start, int stride, int length) {
        return traceDataCreated(new RIntSequence(start, stride, length));
    }

    public static RDoubleSequence createAscendingRange(double start, double end) {
        assert start <= end;
        return traceDataCreated(new RDoubleSequence(start, 1, (int) end - (int) start + 1));
    }

    public static RDoubleSequence createDescendingRange(double start, double end) {
        assert start > end;
        return traceDataCreated(new RDoubleSequence(start, -1, ((int) (start - end) + 1)));
    }

    public static RDoubleSequence createDoubleSequence(double start, double stride, int length) {
        return traceDataCreated(new RDoubleSequence(start, stride, length));
    }

    private static <T> T traceDataCreated(T data) {
        return data;
    }

    public static RIntVector createEmptyIntVector() {
        return EMPTY_INT_VECTOR;
    }

    public static RDoubleVector createEmptyDoubleVector() {
        return EMPTY_DOUBLE_VECTOR;
    }

    public static RStringVector createEmptyStringVector() {
        return EMPTY_STRING_VECTOR;
    }

    public static RComplexVector createEmptyComplexVector() {
        return EMPTY_COMPLEX_VECTOR;
    }

    public static RLogicalVector createEmptyLogicalVector() {
        return EMPTY_LOGICAL_VECTOR;
    }

    public static RRawVector createEmptyRawVector() {
        return EMPTY_RAW_VECTOR;
    }

    public static RComplex createComplex(double realPart, double imaginaryPart) {
        return traceDataCreated(new RComplex(realPart, imaginaryPart));
    }

    public static RRaw createRaw(byte value) {
        return traceDataCreated(new RRaw(value));
    }

    public static RStringVector createStringVectorFromScalar(String operand) {
        return createStringVector(new String[]{operand}, RRuntime.isComplete(operand));
    }

    public static RLogicalVector createLogicalVectorFromScalar(boolean data) {
        return createLogicalVector(new byte[]{data ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE}, COMPLETE_VECTOR);
    }

    public static RLogicalVector createLogicalVectorFromScalar(byte operand) {
        return createLogicalVector(new byte[]{operand}, RRuntime.isComplete(operand));
    }

    public static RIntVector createIntVectorFromScalar(int operand) {
        return createIntVector(new int[]{operand}, RRuntime.isComplete(operand));
    }

    public static RDoubleVector createDoubleVectorFromScalar(double operand) {
        return createDoubleVector(new double[]{operand}, RRuntime.isComplete(operand));
    }

    public static RComplexVector createComplexVectorFromScalar(RComplex operand) {
        return createComplexVector(new double[]{operand.getRealPart(), operand.getImaginaryPart()}, !operand.isNA());
    }

    public static RRawVector createRawVectorFromScalar(RRaw operand) {
        return createRawVector(new byte[]{operand.getValue()});
    }

    public static RComplex createComplexRealOne() {
        return createComplex(1.0, 0.0);
    }

    public static RList createList(Object[] data) {
        return createList(data, null, null);
    }

    public static RComplex createComplexZero() {
        return createComplex(0.0, 0.0);
    }

    public static RList createList(Object[] data, int[] newDimensions) {
        return createList(data, newDimensions, null);
    }

    public static RList createList(Object[] data, Object names) {
        return createList(data, null, names);
    }

    public static RList createList() {
        return createList(new Object[0], null, null);
    }

    public static RList createList(Object[] data, int[] newDimensions, Object names) {
        return traceDataCreated(new RList(data, false, newDimensions, names));
    }

    public static RDataFrame createDataFrame(RVector vector) {
        return traceDataCreated(new RDataFrame(vector));
    }

    public static RExpression createExpression(RList list) {
        return traceDataCreated(new RExpression(list));
    }

    public static RVector createObjectVector(Object[] data, boolean completeVector) {
        if (data.length < 1) {
            return null;
        }
        if (data[0] instanceof Double) {
            double[] result = new double[data.length];
            for (int i = 0; i < data.length; ++i) {
                result[i] = (double) data[i];
            }
            return RDataFactory.createDoubleVector(result, completeVector);
        } else if (data[0] instanceof Byte) {
            byte[] result = new byte[data.length];
            for (int i = 0; i < data.length; ++i) {
                result[i] = (byte) data[i];
            }
            return RDataFactory.createLogicalVector(result, completeVector);
        }
        Utils.fail("unimplemented object vector type: " + data[0].getClass().getSimpleName());
        return null;
    }

    public static RSymbol createSymbol(String name) {
        return traceDataCreated(new RSymbol(name));
    }

    public static RLanguage createLanguage(Object rep) {
        return traceDataCreated(new RLanguage(rep));
    }

    public static RPromise createPromise(Object rep, REnvironment env, Object defaultRep) {
        return traceDataCreated(new RPromise(EvalPolicy.PROMISED, env, rep, defaultRep));
    }

    public static RPromise createPromise(Object rep) {
        return createPromise(rep, null, null);
    }

}
