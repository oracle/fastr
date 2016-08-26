/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.casts;

import static com.oracle.truffle.r.nodes.casts.CastUtils.samples;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.PredefFilters;
import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.VectorPredicateArgumentFilter;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class PredefFiltersSamplers implements PredefFilters {

    @Override
    public <T> ValuePredicateArgumentFilterSampler<T> sameAs(T x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> arg == x, samples(x), CastUtils.<T> samples());
    }

    @Override
    public <T> ValuePredicateArgumentFilterSampler<T> equalTo(T x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> Objects.equals(arg, x), samples(x), CastUtils.<T> samples());
    }

    @Override
    public <T, R extends T> TypePredicateArgumentFilterSampler<T, R> nullValue() {
        // return TypePredicateArgumentFilterSampler.fromLambda(x -> x == RNull.instance || x ==
        // null, CastUtils.<R> samples(null), CastUtils.<R> samples(), RNull.class);
        return new TypePredicateArgumentFilterSampler<>("nullValue()", x -> x == RNull.instance || x == null, CastUtils.<R> samples(null), CastUtils.<R> samples(), Collections.singleton(RNull.class),
                        true);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> notEmpty() {
        return new VectorPredicateArgumentFilterSampler<>("notEmpty()", x -> x.getLength() > 0, false, 0);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> singleElement() {
        return new VectorPredicateArgumentFilterSampler<>("singleElement()", x -> {
            return x.getLength() == 1;
        }, false, 0, 2);
    }

    @Override
    public VectorPredicateArgumentFilterSampler<RAbstractStringVector> elementAt(int index, String value) {
        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && value.equals(x.getDataAtAsObject(index)), false, 0, index);
    }

    @Override
    public VectorPredicateArgumentFilterSampler<RAbstractIntVector> elementAt(int index, int value) {
        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && value == (int) (x.getDataAtAsObject(index)), false, 0, index);
    }

    @Override
    public VectorPredicateArgumentFilterSampler<RAbstractDoubleVector> elementAt(int index, double value) {
        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && value == (double) (x.getDataAtAsObject(index)), false, 0, index);
    }

    @Override
    public VectorPredicateArgumentFilterSampler<RAbstractComplexVector> elementAt(int index, RComplex value) {
        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && value.equals(x.getDataAtAsObject(index)), false, 0, index);
    }

    @Override
    public VectorPredicateArgumentFilterSampler<RAbstractLogicalVector> elementAt(int index, byte value) {
        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && value == (byte) (x.getDataAtAsObject(index)), false, 0, index);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> matrix() {
        return new VectorPredicateArgumentFilterSampler<>("matrix", x -> x.isMatrix(), false);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> squareMatrix() {
        return new VectorPredicateArgumentFilterSampler<>("squareMatrix", x -> x.isMatrix() && x.getDimensions()[0] == x.getDimensions()[1], false, 3);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> dimEq(int dim, int x) {
        return new VectorPredicateArgumentFilterSampler<>("dimGt", v -> v.isMatrix() && v.getDimensions().length == dim && v.getDimensions()[dim] > x, false, dim - 1);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> dimGt(int dim, int x) {
        return new VectorPredicateArgumentFilterSampler<>("dimGt", v -> v.isMatrix() && v.getDimensions().length > dim && v.getDimensions()[dim] > x, false, dim - 1);
    }

    @Override
    public <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilterSampler<T> size(int s) {
        if (s == 0) {
            return new VectorPredicateArgumentFilterSampler<>("size(int)", x -> x.getLength() == s, false, s - 1, s + 1);
        } else {
            return new VectorPredicateArgumentFilterSampler<>("size(int)", x -> x.getLength() == s, false, 0, s - 1, s + 1);
        }
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Boolean> trueValue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> x, samples(Boolean.TRUE), samples(Boolean.FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Boolean> falseValue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> x, samples(Boolean.FALSE), samples(Boolean.TRUE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalTrue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> RRuntime.LOGICAL_TRUE == x, samples(RRuntime.LOGICAL_TRUE),
                        samples(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalFalse() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> RRuntime.LOGICAL_FALSE == x, samples(RRuntime.LOGICAL_FALSE),
                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> intNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> RRuntime.isNA(x), samples(RRuntime.INT_NA), samples(0));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Byte x) -> RRuntime.isNA(x), samples(RRuntime.LOGICAL_NA),
                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> doubleNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> RRuntime.isNA(x), samples(RRuntime.DOUBLE_NA), samples(0d));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> isFractional() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> !RRuntime.isNA(x) && !Double.isInfinite(x) && x != Math.floor(x), samples(0d), samples(RRuntime.DOUBLE_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> stringNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String x) -> RRuntime.isNA(x), samples(RRuntime.STRING_NA), samples(""));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> eq(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg != null && arg.intValue() == x, samples(x), CastUtils.<Integer> samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> eq(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg != null && arg.doubleValue() == x, samples(x), CastUtils.<Double> samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilter<String> eq(String x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg != null && arg.equals(x), samples(x), CastUtils.samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gt(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg != null && arg > x, samples(x + 1), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> gt(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg != null && arg > x, CastUtils.<Double> samples(), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> gte(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg != null && arg >= x, samples(x), samples(x - 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> lt(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg != null && arg < x, samples(x - 1), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> lt(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg < x, CastUtils.<Double> samples(), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> lte(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg <= x, samples(x), samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> length(int l) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg != null && arg.length() == l, samples(sampleString(l)),
                        samples(sampleString(l + 1)));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthGt(int l) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg != null && arg.length() > l, samples(sampleString(l + 1)),
                        samples(sampleString(l)));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthLt(int l) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg != null && arg.length() < l, samples(sampleString(l - 1)),
                        samples(sampleString(l)));
    }

    @Override
    public <R> TypePredicateArgumentFilterSampler<Object, R> instanceOf(Class<R> cls) {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> cls.isInstance(x), CastUtils.<R> samples(), samples(null), cls);
    }

    @Override
    public <R extends RAbstractIntVector> TypePredicateArgumentFilterSampler<Object, R> integerValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Integer || x instanceof RAbstractIntVector, CastUtils.<R> samples(), CastUtils.<Object> samples(null),
                        RAbstractIntVector.class,
                        Integer.class);
    }

    @Override
    public <R extends RAbstractStringVector> TypePredicateArgumentFilterSampler<Object, R> stringValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof String ||
                        x instanceof RAbstractStringVector, CastUtils.<R> samples(), CastUtils.<Object> samples(null), RAbstractStringVector.class, String.class);
    }

    @Override
    public <R extends RAbstractDoubleVector> TypePredicateArgumentFilterSampler<Object, R> doubleValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Double ||
                        x instanceof RAbstractDoubleVector, CastUtils.<R> samples(), CastUtils.<Object> samples(null), RAbstractDoubleVector.class, Double.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends RAbstractLogicalVector> TypePredicateArgumentFilterSampler<Object, R> logicalValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Byte ||
                        x instanceof RAbstractLogicalVector,
                        samples((R) RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_TRUE), (R) RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE),
                                        (R) RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_NA)),
                        CastUtils.samples(null), RAbstractLogicalVector.class,
                        Byte.class);
    }

    @Override
    public <R extends RAbstractComplexVector> TypePredicateArgumentFilterSampler<Object, R> complexValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof RAbstractComplexVector, RAbstractComplexVector.class, RComplex.class);
    }

    @Override
    public <R extends RAbstractRawVector> TypePredicateArgumentFilterSampler<Object, R> rawValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof RRaw ||
                        x instanceof RAbstractRawVector, RAbstractRawVector.class, RRaw.class);
    }

    @Override
    public <R> TypePredicateArgumentFilterSampler<Object, R> anyValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> true, Object.class, Object.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, String> scalarStringValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof String, CastUtils.<String> samples(), CastUtils.<Object> samples(null), String.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Integer> scalarIntegerValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Integer, CastUtils.<Integer> samples(), CastUtils.<Object> samples(null), Integer.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Double> scalarDoubleValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Double, CastUtils.<Double> samples(), CastUtils.<Object> samples(null), Double.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Byte> scalarLogicalValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Byte, samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA), CastUtils.<Object> samples(null),
                        Byte.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, RComplex> scalarComplexValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof RComplex, RComplex.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, RMissing> missingValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> RMissing.instance == x, RMissing.class);
    }

    private static String sampleString(int len) {
        if (len <= 0) {
            return "";
        } else {
            char[] ch = new char[len];
            Arrays.fill(ch, 'a');
            return String.valueOf(ch);
        }
    }
}
